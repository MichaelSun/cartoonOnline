package com.plugin.database.dao;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

/**
 * @Date 2012-08-23
 */
public class DatabaseManager {
	private static final String TAG = DatabaseManager.class.getSimpleName();
	private static final boolean DEBUG = DBConfig.DEBUG;

	@SuppressWarnings("unused")
	private Context mContext;

	private static DatabaseManager gDbManager;

	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	public static synchronized DatabaseManager getInstance(Context context) {
		if (gDbManager == null) {
			gDbManager = new DatabaseManager(context.getApplicationContext());
		}

		return gDbManager;
	}

	private DatabaseManager(Context context) {
		mContext = context;
		if (mDbHelper == null) {
			mDbHelper = new DatabaseHelper(context);
		}
		if (mDb == null) {
			mDb = mDbHelper.getWritableDatabase();
		}
	}

	public void close() {
		mDbHelper.close();
	}

	public SQLiteDatabase getWritableDatabase() {
		return mDb;
	}

	public <T> Dao<T> getDao(Class<T> clazz) {
		return mDbHelper.getDao(clazz);
	}
	
	static class SqliteDBMetaDataFactory {
		public static final String SCHEMA_TABLE = "sqlite_master";
		public static final SqliteDBMetaDataFactory INSTANCE = new SqliteDBMetaDataFactory();
		Map<String, BaseTableMetaData> mTableMetaData = null;

		private SqliteDBMetaDataFactory() {
		}

		public static SqliteDBMetaDataFactory getInstance() {
			return INSTANCE;
		}

		public synchronized Map<String, BaseTableMetaData> queryTableMetaData(SQLiteDatabase db) {
			if (mTableMetaData == null) {
				mTableMetaData = getTableMetaData(db);
			}
			return this.mTableMetaData;
		}

		Map<String, BaseTableMetaData> getTableMetaData(SQLiteDatabase db) {
			Cursor cursor = db.query(SCHEMA_TABLE, null, null, null, null, null, null);
			Map<String, BaseTableMetaData> map = new HashMap<String, BaseTableMetaData>();
			cursor.moveToFirst();
			while (cursor.moveToNext()) {
				BaseTableMetaData data = new BaseTableMetaData(cursor);
				map.put(data.name, data);
			}
			return map;
		}
	}

	static class DatabaseHelper extends SQLiteOpenHelper {
		private Context mContext;
		private final Map<Class<?>, TableMetaData> mRegisteredTable = new HashMap<Class<?>, TableMetaData>();
		private boolean hasOnCreateOrUpdate;

		public DatabaseHelper(Context context) {
			super(context, DBConfig.DATABASE_NAME, null, DBConfig.DATABASE_VERSION);
			init(context);
		}

		public DatabaseHelper(Context context, String name, CursorFactory factory, int version) {
			super(context, DBConfig.DATABASE_NAME, factory, DBConfig.DATABASE_VERSION);
			init(context);
		}

		private void init(Context context) {
			mContext = context;
			hasOnCreateOrUpdate = false;

			if (mRegisteredTable.size() == 0) {
				// Read configuration file for registered table.
				BufferedReader br = null;
				try {
					InputStream is = mContext.getAssets().open(DBConfig.DATABASE_CONFIG_FILE);
					br = new BufferedReader(new InputStreamReader(is));
					String line = "";
					while ((line = br.readLine()) != null) {
						registerTable(line);
					}

					br.close();
				} catch (IOException e) {
					LOGD("Read database config file error: " + e.getMessage());
				} finally {
					try {
						if (br != null) {
							br.close();
							br = null;
						}
					} catch (IOException e) {
						LOGD("Close BufferedReader Error: " + e.getMessage());
					}
				}
			}
		}

		/**
		 * Read tables from configuration file, then create them.
		 */
		@Override
		public void onCreate(SQLiteDatabase db) {
			LOGD("onCreate...");
			// Create registered table.
			for (Class<?> key : mRegisteredTable.keySet()) {
				LOGD("[[onCreate]] create table " + key);
				db.execSQL(TableStatements.getCreateTableSql(mRegisteredTable.get(key)));
			}
			
			hasOnCreateOrUpdate = true;
		}

		@Override
		public void onOpen(SQLiteDatabase db) {
			db.beginTransaction();
			try {
				Map<String, BaseTableMetaData> map = SqliteDBMetaDataFactory.getInstance().queryTableMetaData(db);
				if (!hasOnCreateOrUpdate) {
					if (map != null && map.size() > 0) {
						for (Class<?> key : mRegisteredTable.keySet()) {
							String createSql = TableStatements.getCreateTableSql(mRegisteredTable.get(key));
							if (DEBUG) {
								LOGD("** -----------------------------------------------");
								LOGD("** [[onOpen]] Database check for table : " + key.getName() + " and create sql : " + createSql + " >>>>>>>>>>");
								LOGD("** -----------------------------------------------");
							}
							String tableSql = (map.containsKey(key.getSimpleName()) && map.get(key.getSimpleName()) != null)
													? map.get(key.getSimpleName()).sql 
													: null;
							if (!TextUtils.isEmpty(createSql) && !TextUtils.isEmpty(tableSql) && !createSql.equals(tableSql)) {
								if (DEBUG) {
									LOGD("|| -----------------------------------------------");
									LOGD("|| Drop the table : " + key.getSimpleName() + "   with sql : " + TableStatements.getDropTableSql(mRegisteredTable.get(key)));
									LOGD("|| Drop reason : create sql is = " + createSql);
									LOGD("|| ***** new table sql is = " + tableSql);
									LOGD("|| -----------------------------------------------");
								}
								db.execSQL(TableStatements.getDropTableSql(mRegisteredTable.get(key)));
								db.execSQL(createSql);
							}
						}
					}
				}
				
				//check new table
				if ((map != null) && (map.size() < mRegisteredTable.size())) {
					for (Class<?> key : mRegisteredTable.keySet()) {
						if (!map.containsKey(key.getSimpleName())) {
							String createSql = TableStatements.getCreateTableSql(mRegisteredTable.get(key));
							if (DEBUG) {
								LOGD("|| -----------------------------------------------");
								LOGD("|| create the table for new : " + createSql);
								LOGD("|| -----------------------------------------------");
							}
							db.execSQL(createSql);
						}
					}
				}
				db.setTransactionSuccessful();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				db.endTransaction();
			}
		}
		/**
		 * Drop all tables and then create. Use in development only.
		 */
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if (newVersion > oldVersion) {
				for (Class<?> key : mRegisteredTable.keySet()) {
					db.execSQL(TableStatements.getDropTableSql(mRegisteredTable.get(key)));
				}
				this.onCreate(db);
			}
			hasOnCreateOrUpdate = true;
		}

		public <T> Dao<T> getDao(Class<T> clazz) {
			return new BaseDaoImpl<T>(mContext, getWritableDatabase(), mRegisteredTable.get(clazz));
		}

		private void registerTable(String className) {
			if (className == null) {
				return;
			}

			try {
				Class<?> clazz = Class.forName(className);
				mRegisteredTable.put(clazz, new TableMetaData(clazz));
			} catch (ClassNotFoundException e) {
				LOGD("[[registerTable]] Class not found, className=" + className);
			}
		}

		private void LOGD(String message) {
			if (DEBUG) {
				Log.i(TAG, message);
			}
		}

	}

}
