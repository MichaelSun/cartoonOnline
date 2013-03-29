/**
 * DBTableAccessHelper.java
 */
package com.plugin.database.dao.helper;

import java.util.List;

import android.content.Context;

import com.plugin.common.utils.UtilsConfig;
import com.plugin.database.dao.DBConfig;
import com.plugin.database.dao.Dao;
import com.plugin.database.dao.DatabaseManager;

/**
 * @author Guoqing Sun Feb 22, 20136:15:06 PM
 */
public class DBTableAccessHelper<T> {

	protected Dao<T> mDaoObj;
	
	private Class<T> mCl;
	
	public DBTableAccessHelper(Context context, Class<T> cl) {
		mDaoObj = DatabaseManager.getInstance(context.getApplicationContext()).getDao(cl);
		mCl = cl;
	}
	
	public long getCount() {
		if (DBConfig.DEBUG) {
			long ret = mDaoObj.count();
			UtilsConfig.LOGD("[[getCount]] count = " + ret);
			return ret;
		} else {
			return mDaoObj.count();
		}
	}
	
	public List<T> queryItems() {
		if (DBConfig.DEBUG) {
			List<T> ret = mDaoObj.queryRaw(mCl, "");
			UtilsConfig.LOGD("[[queryItems]] >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
			if (ret != null) {
				for (T item : ret) {
					UtilsConfig.LOGD(item.toString());
					UtilsConfig.LOGD("=======================================================");
				}
			}
			UtilsConfig.LOGD("[[queryItems]] <<<<<<<<<<<<<<<<<<<<<<<<<<");
			return ret;
		} else {
			return mDaoObj.queryRaw(mCl, "");
		}
	}
	
	public List<T> queryItems(String selection, String... selectionArgs) {
		if (DBConfig.DEBUG) {
			String args = "";
			if (selectionArgs != null) {
				for (String a : selectionArgs) {
					args += a + " ";
				}
			}
			UtilsConfig.LOGD("[[queryItems]] " + " ==== selection : " + selection + " args : " + args);
			List<T> ret = mDaoObj.queryRaw(mCl, selection, selectionArgs);

			UtilsConfig.LOGD("[[queryItems]] " + " ==== selection : " + selection + " args : " + args
					+ " data = " + ret);
			return ret;
		} else {
			return mDaoObj.queryRaw(mCl, selection, selectionArgs);
		}
	}
	
	public List<T> queryLimit(int start, int length) {
		if (DBConfig.DEBUG) {
			List<T> ret = mDaoObj.loadByLimit(start, length);
			UtilsConfig.LOGD("[[queryLimit]] " + ret);
			UtilsConfig.LOGD(" start : " + start + " length : " + length);
			return ret;
		} else {
			return mDaoObj.loadByLimit(start, length);
		}
	}
	
	public T queryItem(T searchItem) {
		if (DBConfig.DEBUG) {
			T ret = mDaoObj.load(searchItem);
			UtilsConfig.LOGD("[[queryItem]]" + ret);
			UtilsConfig.LOGD("searck item : " + searchItem);
			return ret;
		} else {
			return mDaoObj.load(searchItem);
		}
	}
	
	public boolean insert(T item) {
		if (item == null) {
			return false;
		}
		
		if (DBConfig.DEBUG) {
			UtilsConfig.LOGD("[[insert]]" + item);
		}
		
		mDaoObj.insert(item);
		
		return true;
	}
	
	public boolean blukInsert(T[] items) {
		if (items == null) {
			return false;
		}
		
		if (DBConfig.DEBUG) {
			UtilsConfig.LOGD("[[blukInsert]] >>>>>>>>>>>>>>>>>>>>>>>>" + items);
			for (T item : items) {
				UtilsConfig.LOGD(item.toString());
			}
			UtilsConfig.LOGD("[[blukInsert]] <<<<<<<<<<<<<<<<<<<<<<<<" + items);
		}
		
		mDaoObj.batchInsert(items);
		
		return true;
	}
	
	public boolean blukInsertOrReplace(T[] items) {
		if (items == null) {
			return false;
		}
		
		if (DBConfig.DEBUG) {
			UtilsConfig.LOGD("[[blukInsertOrReplace]] " + items);
		}
		
		return mDaoObj.batchInsertOrReplace(items);
	}
	
	public boolean insertOrReplace(T item) {
		if (item == null) {
			return false;
		}
		
		if (DBConfig.DEBUG) {
			UtilsConfig.LOGD("[[insertOrReplace]] " + item);
		}
		
		return (mDaoObj.insertOrReplace(item) != -1);
	}
	
	public boolean update(T item) {
		if (item == null) {
			return false;
		}
		
		if (DBConfig.DEBUG) {
			UtilsConfig.LOGD("[[update]] " + item);
		}
		
		mDaoObj.update(item);
		
		return true;
	}
	
	public boolean delete(String selection, String selectionArgs) {
		return mDaoObj.deleteRaw(mCl, selection, selectionArgs) != 0 ? true : false;
	}
	
	public boolean delete(T item) {
		if (item == null) {
			return false;
		}
		
		if (DBConfig.DEBUG) {
			UtilsConfig.LOGD("[[delete]] " + item);
		}
		
		mDaoObj.delete(item);
		
		return true;
	}
	
	public boolean delete(T[] items) {
		if (items == null) {
			return false;
		}
		
		if (DBConfig.DEBUG) {
			UtilsConfig.LOGD("[[delete]] " + items);
		}
		
		mDaoObj.batchDelete(items);
		
		return true;
	}
	
	public boolean deleteAll() {
		if (DBConfig.DEBUG) {
			UtilsConfig.LOGD("[[deleteAll]]");
		}
		
		mDaoObj.deleteAll();
		
		return true;
	}
	
	public boolean search(T searchItem) {
		T searchObj = mDaoObj.load(searchItem);
		
		if (DBConfig.DEBUG) {
			UtilsConfig.LOGD("[[search]] origin search item = " + searchItem);
			UtilsConfig.LOGD("[[search]] item = " + searchObj);
		}
		
		return searchObj != null;
	}
	
}
