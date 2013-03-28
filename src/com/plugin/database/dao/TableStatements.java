package com.plugin.database.dao;

import java.util.ArrayList;
import java.util.List;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

/**
 * @Date 2012-08-23
 */
class TableStatements {
	private static final String TAG = TableStatements.class.getSimpleName();
	private static final boolean DEBUG = DBConfig.DEBUG;

	final SQLiteDatabase db;
	final String tablename;
	final List<ColumnMetaData> allColumns;
	final List<ColumnMetaData> pkColumns;
	final List<ColumnMetaData> orderByColumns;
	final List<ColumnMetaData> uniqueColumns;

	private SQLiteStatement insertStatement;
	private SQLiteStatement insertOrReplaceStatement;
	private SQLiteStatement updateStatement;
	private SQLiteStatement deleteStatement;

	private String selectAll;
	private String selectByKey;
	private String selectByRowId;
	private String selectByLimit;
	private String selectByOrder;

	public TableStatements(SQLiteDatabase db, String tablename, List<ColumnMetaData> allColumns,
			List<ColumnMetaData> pkColumns, List<ColumnMetaData> orderByColumns, List<ColumnMetaData> uniqueColumns) {
		this.db = db;
		this.tablename = tablename;
		this.allColumns = allColumns;
		this.pkColumns = pkColumns;
		this.orderByColumns = orderByColumns;
		this.uniqueColumns = uniqueColumns;
	}

	private List<ColumnMetaData> getCanInsertColumns() {
		if (pkColumns == null || allColumns == null) {
			return allColumns;
		}
		
		List<ColumnMetaData> ret = new ArrayList<ColumnMetaData>();
		ret.addAll(allColumns);
		for (ColumnMetaData c : pkColumns) {
			if (c.isAutoIncrement) {
				ret.remove(c);
			}
		}
		
		return ret;
	}
	
	public SQLiteStatement getInsertStatement() {
		if (insertStatement == null) {
			String sql = createSqlInsert("INSERT INTO ", tablename, getCanInsertColumns());
			insertStatement = db.compileStatement(sql);
		}

		if (DEBUG) {
			LOGD("[[getInsertStatement]] SQL : " + insertStatement.toString());
		}

		return insertStatement;
	}

	public SQLiteStatement getInsertOrReplaceStatement() {
		if (insertOrReplaceStatement == null) {
			String sql = createSqlInsert("INSERT OR REPLACE INTO ", tablename, getCanInsertColumns());
			insertOrReplaceStatement = db.compileStatement(sql);
		}

		if (DEBUG) {
			LOGD("[[getInsertOrReplaceStatement]] SQL : " + insertOrReplaceStatement.toString());
		}

		return insertOrReplaceStatement;
	}

	public SQLiteStatement getDeleteStatement() {
		if (deleteStatement == null) {
			String sql = createSqlDelete(tablename, makeMainColumns());
			deleteStatement = db.compileStatement(sql);
		}

		if (DEBUG) {
			LOGD("[[getDeleteStatement]] SQL : " + deleteStatement.toString());
		}

		return deleteStatement;
	}

	public SQLiteStatement getUpdateStatement() {
		if (updateStatement == null) {
			String sql = createSqlUpdate(tablename, allColumns, makeMainColumns());
			updateStatement = db.compileStatement(sql);
		}

		if (DEBUG) {
			LOGD("[[getUpdateStatement]] SQL : " + updateStatement.toString());
		}

		return updateStatement;
	}

	public String getSelectAll() {
		if (selectAll == null) {
			// 增加_ID字段
			selectAll = createSqlSelect(tablename, "T", allColumns, orderByColumns);
		}

		if (DEBUG) {
			LOGD("[[getSelectAll]] SQL : " + selectAll);
		}

		return selectAll;
	}

	public String getSelectByKey() {
		if (selectByKey == null) {
			StringBuilder builder = new StringBuilder(getSelectAll());
			builder.append(" WHERE ");
			appendColumnsEqValue(builder, "T", getColumnNamesFromMetaData(makeMainColumns()));
			selectByKey = builder.toString();
		}

		if (DEBUG) {
			LOGD("[[getSelectByKey]] SQL : " + selectByKey);
		}

		return selectByKey;
	}

	public String getSelectByRowId() {
		if (selectByRowId == null) {
			selectByRowId = getSelectAll() + " WHERE ROWID=?";
		}

		if (DEBUG) {
			LOGD("[[getSelectByRowId]] SQL : " + selectByRowId);
		}

		return selectByRowId;
	}

	public String getSelectByLimit(int start, int length) {
		if (selectByLimit == null) {
			StringBuilder builder = new StringBuilder(getSelectAll());
			builder.append(" LIMIT ");
			builder.append(start);
			builder.append(", ");
			builder.append(length);
			selectByLimit = builder.toString();
		}

		if (DEBUG) {
			LOGD("[[getSelectByLimit]] SQL : " + selectByLimit);
		}

		return selectByLimit;
	}

	public String getSelectByOrder() {
		if (selectByOrder == null) {
			StringBuilder builder = new StringBuilder(getSelectAll());
			builder.append(" ORDER BY ");
			selectByOrder = builder.toString();
		}

		if (DEBUG) {
			LOGD("[[getSelectByOrder]] SQL : " + selectByOrder);
		}

		return selectByOrder;
	}
	
	private List<ColumnMetaData> makeMainColumns() {
		return pkColumns.size() > 0 ? pkColumns : this.uniqueColumns;
	}

	private StringBuilder appendColumn(StringBuilder builder, String column) {
		builder.append('\'').append(column).append('\'');
		return builder;
	}

	private StringBuilder appendColumn(StringBuilder builder, String tableAlias, String column) {
		builder.append(tableAlias).append(".'").append(column).append('\'');
		return builder;
	}

	private StringBuilder appendColumns(StringBuilder builder, String tableAlias, String[] columns) {
		int length = columns.length;
		for (int i = 0; i < length; i++) {
			appendColumn(builder, tableAlias, columns[i]);
			if (i < length - 1) {
				builder.append(',');
			}
		}
		return builder;
	}

	private StringBuilder appendColumns(StringBuilder builder, String[] columns) {
		int length = columns.length;
		for (int i = 0; i < length; i++) {
			builder.append('\'').append(columns[i]).append('\'');
			if (i < length - 1) {
				builder.append(',');
			}
		}
		return builder;
	}

	private StringBuilder appendPlaceholders(StringBuilder builder, int count) {
		for (int i = 0; i < count; i++) {
			if (i < count - 1) {
				builder.append("?,");
			} else {
				builder.append('?');
			}
		}
		return builder;
	}

	private StringBuilder appendColumnsEqualPlaceholders(StringBuilder builder, String[] columns) {
		for (int i = 0; i < columns.length; i++) {
			appendColumn(builder, columns[i]).append("=?");
			if (i < columns.length - 1) {
				builder.append(',');
			}
		}
		return builder;
	}

	private StringBuilder appendColumnsEqValue(StringBuilder builder, String tableAlias, String[] columns) {
		for (int i = 0; i < columns.length; i++) {
			appendColumn(builder, tableAlias, columns[i]).append("=?");
			if (i < columns.length - 1) {
				builder.append(',');
			}
		}
		return builder;
	}

	private String createSqlInsert(String insertInto, String tablename, List<ColumnMetaData> insertColumns) {
		String[] columns = getColumnNamesFromMetaData(insertColumns);
		StringBuilder builder = new StringBuilder(insertInto);
		builder.append(tablename).append(" (");
		appendColumns(builder, columns);
		builder.append(") VALUES (");
		appendPlaceholders(builder, columns.length);
		builder.append(')');

		// LOGD("[[createSqlInsert]] sql = " + builder.toString());

		return builder.toString();
	}

	private String createSqlSelect(String tablename, String tableAlias, List<ColumnMetaData> selectColumns,
			List<ColumnMetaData> orderByColumns) {
		StringBuilder builder = new StringBuilder("SELECT ");
		if (tableAlias == null || tableAlias.length() < 0) {
			throw new DaoException("Table alias required");
		}
		String[] columns = getColumnNamesFromMetaData(selectColumns);
		appendColumns(builder, tableAlias, columns).append(" FROM ");
		builder.append(tablename).append(' ').append(tableAlias).append(' ');
		if (orderByColumns.size() > 0) {
			builder.append(" ORDER BY ");

			for (ColumnMetaData column : orderByColumns) {
				builder.append(tableAlias);
				builder.append(".'");
				builder.append(column.realColumnName);
				builder.append("' ");
				builder.append(column.orderBy);
				builder.append(",");
			}
		}

		// LOGD("[[createSqlSelect]] sql = " + builder.toString());

		return builder.substring(0, builder.length() - 1);
	}

	private String createSqlDelete(String tablename, List<ColumnMetaData> whereColumns) {
		StringBuilder builder = new StringBuilder("DELETE FROM ");
		builder.append(tablename);
		builder.append(" WHERE ");
		appendColumnsEqValue(builder, tablename, getColumnNamesFromMetaData(whereColumns));

		// LOGD("[[createSqlDelete]] sql = " + builder.toString());

		return builder.toString();
	}

	private String createSqlUpdate(String tablename, List<ColumnMetaData> updateColumns,
			List<ColumnMetaData> whereColumns) {
		StringBuilder builder = new StringBuilder("UPDATE ");
		builder.append(tablename).append(" SET ");
		appendColumnsEqualPlaceholders(builder, getColumnNamesFromMetaData(updateColumns));
		builder.append(" WHERE ");
		appendColumnsEqValue(builder, tablename, getColumnNamesFromMetaData(whereColumns));

		// LOGD("[[createSqlUpdate]] sql = " + builder.toString());

		return builder.toString();
	}

	public String[] getColumnNamesFromMetaData(List<ColumnMetaData> columns) {
		List<String> columnNames = new ArrayList<String>();

		for (ColumnMetaData column : columns) {
			if (!column.hasSubColumns) {
				columnNames.add(column.realColumnName);
			}
		}
		return columnNames.toArray(new String[columnNames.size()]);
	}

	public String[] getFieldNamesFromMetaData(List<ColumnMetaData> columns) {
		List<String> columnNames = new ArrayList<String>();

		for (ColumnMetaData column : columns) {
			if (!column.hasSubColumns) {
				columnNames.add(column.columnName);
			}
		}
		return columnNames.toArray(new String[columnNames.size()]);
	}

	public static String getDeleteSql(Class<?> type) {
		return new StringBuilder("DELETE FROM ").append(DaoUtil.getTableName(type)).toString();
	}

	public static String getDropTableSql(TableMetaData metaData) {
		StringBuilder sb = new StringBuilder();
		sb.append("DROP TABLE IF EXISTS ").append(metaData.tableName);

		return sb.toString();
	}

	public static String getCreateTableSql(TableMetaData metaData) {
		if (metaData == null) {
			return null;
		}

		StringBuilder sb = new StringBuilder(256);
		List<ColumnMetaData> allColumns = metaData.allColumns;
		sb.append("CREATE TABLE ").append(metaData.tableName).append(" (");
		for (ColumnMetaData column : allColumns) {
			if (column.hasSubColumns) {
				continue;
			}
			sb.append(column.realColumnName);
			if (column.isPrimaryKey) {
				sb.append(" INTEGER PRIMARY KEY");
				if (column.isAutoIncrement) {
					sb.append(" AUTOINCREMENT");
				}
			} else {
				sb.append(" ");
				sb.append(DaoUtil.classToTypeMap.get(column.type));
			}
			sb.append(", ");
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.deleteCharAt(sb.length() - 1);
		for (ColumnMetaData column : allColumns) {
			if (column.hasSubColumns) {
				continue;
			}

//			if (column.isUnique) {
//				sb.append(", ");
//				sb.append("UNIQUE (").append(column.columnName).append(")");
//			}
		}

		sb.append(")");

		if (DBConfig.DEBUG) {
			LOGD("[[getCreateSql]] sql = " + sb.toString());
		}

		return sb.toString();
	}

	private static void LOGD(String message) {
		if (DEBUG) {
			Log.d(TAG, "/***************************************");
			Log.d(TAG, "| " + message);
			Log.d(TAG, "\\***************************************");
		}
	}

}
