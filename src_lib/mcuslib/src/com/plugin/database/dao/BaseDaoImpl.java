package com.plugin.database.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;
import android.util.Log;

/**
 * @Date 2012-08-20
 */
class BaseDaoImpl<T> implements Dao<T> {
	private static final String TAG = "BaseDaoImpl";
	private static final boolean DEBUG = DBConfig.DEBUG;

	private final SQLiteDatabase mWritableDb;
	private final TableMetaData mTableMetaData;
	private final TableStatements mStatements;

	public BaseDaoImpl(Context context, SQLiteDatabase db, TableMetaData tableMetaData) {
		mWritableDb = db;
		mTableMetaData = tableMetaData;
		mStatements = new TableStatements(mWritableDb, mTableMetaData.tableName, mTableMetaData.allColumns,
				mTableMetaData.pkColumns, mTableMetaData.orderByColumns, mTableMetaData.uniqueColumns);
	}

	@Override
	public long count() {
		return DatabaseUtils.queryNumEntries(mWritableDb, mTableMetaData.tableName);
	}

	@Override
	public long insert(T entity) {
		SQLiteStatement insertStatement = mStatements.getInsertStatement();
		if (insertStatement != null) {
			insertStatement.clearBindings();
		}
		bindColumnsWithoutAutoKey(insertStatement, entity, 0, mStatements);

		long rowId = insertStatement.executeInsert();
		return rowId;
	}

	@Override
	public void batchInsert(T... entities) {
		if (entities == null || entities.length == 0) {
			return;
		}

		batchInsert(Arrays.asList(entities));
	}

	private void batchInsert(Iterable<T> entities) {
		SQLiteStatement insertStatement = mStatements.getInsertStatement();
		mWritableDb.beginTransaction();
		try {
			for (T entity : entities) {
				if (DEBUG) {
					LOGD(":::::: one item begin ::::::");
				}
				if (insertStatement != null) {
					insertStatement.clearBindings();
				}
				bindColumnsWithoutAutoKey(insertStatement, entity, 0, mStatements);
				insertStatement.execute();
				if (DEBUG) {
					LOGD("----- one item end -----");
				}
			}

			mWritableDb.setTransactionSuccessful();
		} finally {
			mWritableDb.endTransaction();
		}
	}

	@Override
	public boolean batchInsertOrReplace(T... entities) {
		try {
			mWritableDb.beginTransaction();
			for (T entity : entities) {
				insertOrReplace(entity);
			}
			mWritableDb.setTransactionSuccessful();
		} finally {
			mWritableDb.endTransaction();
		}

		return true;
	}

	@Override
	public long insertOrReplace(T entity) {
		exceptionCheck();

		SQLiteStatement insertOrReplaceStatement = mStatements.getInsertOrReplaceStatement();
		if (insertOrReplaceStatement != null) {
			insertOrReplaceStatement.clearBindings();
		}
		bindColumnsWithoutAutoKey(insertOrReplaceStatement, entity, 0, mStatements);

		long rowId = insertOrReplaceStatement.executeInsert();
		return rowId;
	}

	@Override
	public void update(T entity) {
		exceptionCheck();

		SQLiteStatement updateStatement = mStatements.getUpdateStatement();
		List<ColumnMetaData> allColumns = mStatements.allColumns;

		if (updateStatement != null) {
			updateStatement.clearBindings();
			bindColumns(updateStatement, entity, 0, allColumns);
			bindColumns(updateStatement, entity, allColumns.size(), makeMainColumns());

			updateStatement.execute();
		}
	}

	@Override
	public boolean delete(T entity) {
		exceptionCheck();

		SQLiteStatement deleteStatement = mStatements.getDeleteStatement();
		List<ColumnMetaData> columns = makeMainColumns();

		if (deleteStatement != null) {

			deleteStatement.clearBindings();
			int length = columns.size();
			for (int i = 0; i < length; i++) {
				DaoUtil.bindColumn(deleteStatement, i + 1, DaoUtil.getValueByColumnMetaData(columns.get(i), entity));
			}



			deleteStatement.execute();
		}
		


		return true;
	}

	@Override
	public boolean batchDelete(T... entities) {
		// 不依次调用delete(entity)，直接重构一条sql语句，效率更高
		if (entities == null || entities.length == 0) {
			return false;
		}

		try {
			mWritableDb.beginTransaction();
			for (T e : entities) {
				delete(e);
			}
			mWritableDb.setTransactionSuccessful();
		} finally {
			mWritableDb.endTransaction();
		}

		return true;
	}

	@Override
	public void deleteAll() {
		mWritableDb.execSQL(TableStatements.getDeleteSql(mTableMetaData.tableType));
	}

	@Override
	public T load(T entity) {
		exceptionCheck();

		String selectByKeySql = mStatements.getSelectByKey();
		Cursor cursor = mWritableDb.rawQuery(selectByKeySql,
				new String[] { String.valueOf(DaoUtil.getValueByColumnMetaData(makeMainColumns().get(0), entity)) });

		return DaoUtil.readSingleEntityFromCursor(mTableMetaData.tableType, cursor, mTableMetaData);
	}

	@Override
	public List<T> loadAll() {
		String selectByAllSql = mStatements.getSelectAll();
		Cursor cursor = mWritableDb.rawQuery(selectByAllSql, null);

		return DaoUtil.readListEntityFromCursor(mTableMetaData.tableType, cursor, mTableMetaData);
	}

	@Override
	public List<T> queryRaw(Class<T> entityClass, String where, String... selectionArgs) {
		String selectAll = mStatements.getSelectAll();
		int orderByIndex = selectAll.indexOf("ORDER BY");
		StringBuilder sb = new StringBuilder();
		if (orderByIndex != -1) {
			sb.append(selectAll.substring(0, orderByIndex));
		} else {
			sb.append(selectAll);
		}
		sb.append(" ");
		if (!TextUtils.isEmpty(where)) {
			sb.append("WHERE ").append(where);
		}
		sb.append(" ");
		if (orderByIndex != -1) {
			sb.append(selectAll.substring(orderByIndex));
		}

		Cursor cursor = mWritableDb.rawQuery(sb.toString(), selectionArgs);

		return DaoUtil.readListEntityFromCursor(mTableMetaData.tableType, cursor, mTableMetaData);
	}

	@Override
	public List<T> queryRawByLimit(Class<T> entityClass, int start, int length, String where, String... selectionArgs) {
		String selectAll = mStatements.getSelectAll();
		int orderByIndex = selectAll.indexOf("ORDER BY");
		StringBuilder sb = new StringBuilder();
		if (orderByIndex != -1) {
			sb.append(selectAll.substring(0, orderByIndex));
		} else {
			sb.append(selectAll);
		}
		sb.append(" ");
		if (!TextUtils.isEmpty(where)) {
			sb.append("WHERE ");
			sb.append(where != null ? "" : where);
		}
		sb.append(" ");
		if (orderByIndex != -1) {
			sb.append(selectAll.substring(orderByIndex));
		}
		sb.append(" LIMIT ");
		sb.append(start);
		sb.append(", ");
		sb.append(length);

		Cursor cursor = mWritableDb.rawQuery(sb.toString(), selectionArgs);

		return DaoUtil.readListEntityFromCursor(mTableMetaData.tableType, cursor, mTableMetaData);
	}

	@Override
	public List<T> loadByLimit(int start, int length) {
		String selectByLimitSql = mStatements.getSelectByLimit(start, length);
		Cursor cursor = mWritableDb.rawQuery(selectByLimitSql, null);

		return DaoUtil.readListEntityFromCursor(mTableMetaData.tableType, cursor, mTableMetaData);
	}

	@Override
	public int deleteRaw(Class<T> entityClass, String whereClause, String... whereArgs) {
		int r = mWritableDb.delete(mTableMetaData.tableName, whereClause, whereArgs);
		return r;
	}

	@Override
	public void executeRaw(String sql) {
		mWritableDb.execSQL(sql);
	}

	@Override
	public void executeRaw(String sql, Object[] args) {
		mWritableDb.execSQL(sql, args);
	}

	private void bindColumns(SQLiteStatement statement, T entity, int index, List<ColumnMetaData> columns) {
		int length = columns.size();
		for (int i = 0; i < length; i++) {
			DaoUtil.bindColumn(statement, index + i + 1, DaoUtil.getValueByColumnMetaData(columns.get(i), entity));
		}
	}

	private void bindColumnsWithoutAutoKey(SQLiteStatement statement, T entity, int index, TableStatements statements) {
		List<ColumnMetaData> all = new ArrayList<ColumnMetaData>();
		all.addAll(statements.allColumns);
		List<ColumnMetaData> pk = statements.pkColumns;
		for (ColumnMetaData c : pk) {
			if (c.isAutoIncrement) {
				all.remove(c);
			}
		}

		int length = all.size();
		for (int i = 0; i < length; i++) {
			DaoUtil.bindColumn(statement, index + i + 1, DaoUtil.getValueByColumnMetaData(all.get(i), entity));
		}
	}

	private List<ColumnMetaData> makeMainColumns() {
		return mStatements.pkColumns.size() > 0 ? mStatements.pkColumns : mStatements.uniqueColumns;
	}

	private void exceptionCheck() {
		if (mStatements.pkColumns.size() == 0 && mStatements.uniqueColumns.size() == 0) {
			throw new DaoException("You can invoke this method on Model without PrimaryKey and UniqueKey");
		}
	}

	private static void LOGD(String message) {
		if (DEBUG) {
			Log.i(TAG, message);
		}
	}
}
