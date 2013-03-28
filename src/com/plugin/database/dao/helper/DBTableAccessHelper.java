/**
 * DBTableAccessHelper.java
 */
package com.plugin.database.dao.helper;

import java.util.List;

import android.content.Context;

import com.plugin.common.utils.Config;
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
			Config.LOGD("[[getCount]] count = " + ret);
			return ret;
		} else {
			return mDaoObj.count();
		}
	}
	
	public List<T> queryItems() {
		if (DBConfig.DEBUG) {
			List<T> ret = mDaoObj.queryRaw(mCl, "");
			Config.LOGD("[[queryItems]] >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
			if (ret != null) {
				for (T item : ret) {
					Config.LOGD(item.toString());
					Config.LOGD("=======================================================");
				}
			}
			Config.LOGD("[[queryItems]] <<<<<<<<<<<<<<<<<<<<<<<<<<");
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
			Config.LOGD("[[queryItems]] " + " ==== selection : " + selection + " args : " + args);
			List<T> ret = mDaoObj.queryRaw(mCl, selection, selectionArgs);

			Config.LOGD("[[queryItems]] " + " ==== selection : " + selection + " args : " + args
					+ " data = " + ret);
			return ret;
		} else {
			return mDaoObj.queryRaw(mCl, selection, selectionArgs);
		}
	}
	
	public List<T> queryLimit(int start, int length) {
		if (DBConfig.DEBUG) {
			List<T> ret = mDaoObj.loadByLimit(start, length);
			Config.LOGD("[[queryLimit]] " + ret);
			Config.LOGD(" start : " + start + " length : " + length);
			return ret;
		} else {
			return mDaoObj.loadByLimit(start, length);
		}
	}
	
	public T queryItem(T searchItem) {
		if (DBConfig.DEBUG) {
			T ret = mDaoObj.load(searchItem);
			Config.LOGD("[[queryItem]]" + ret);
			Config.LOGD("searck item : " + searchItem);
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
			Config.LOGD("[[insert]]" + item);
		}
		
		mDaoObj.insert(item);
		
		return true;
	}
	
	public boolean blukInsert(T[] items) {
		if (items == null) {
			return false;
		}
		
		if (DBConfig.DEBUG) {
			Config.LOGD("[[blukInsert]] >>>>>>>>>>>>>>>>>>>>>>>>" + items);
			for (T item : items) {
				Config.LOGD(item.toString());
			}
			Config.LOGD("[[blukInsert]] <<<<<<<<<<<<<<<<<<<<<<<<" + items);
		}
		
		mDaoObj.batchInsert(items);
		
		return true;
	}
	
	public boolean blukInsertOrReplace(T[] items) {
		if (items == null) {
			return false;
		}
		
		if (DBConfig.DEBUG) {
			Config.LOGD("[[blukInsertOrReplace]] " + items);
		}
		
		return mDaoObj.batchInsertOrReplace(items);
	}
	
	public boolean insertOrReplace(T item) {
		if (item == null) {
			return false;
		}
		
		if (DBConfig.DEBUG) {
			Config.LOGD("[[insertOrReplace]] " + item);
		}
		
		return (mDaoObj.insertOrReplace(item) != -1);
	}
	
	public boolean update(T item) {
		if (item == null) {
			return false;
		}
		
		if (DBConfig.DEBUG) {
			Config.LOGD("[[update]] " + item);
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
			Config.LOGD("[[delete]] " + item);
		}
		
		mDaoObj.delete(item);
		
		return true;
	}
	
	public boolean delete(T[] items) {
		if (items == null) {
			return false;
		}
		
		if (DBConfig.DEBUG) {
			Config.LOGD("[[delete]] " + items);
		}
		
		mDaoObj.batchDelete(items);
		
		return true;
	}
	
	public boolean deleteAll() {
		if (DBConfig.DEBUG) {
			Config.LOGD("[[deleteAll]]");
		}
		
		mDaoObj.deleteAll();
		
		return true;
	}
	
	public boolean search(T searchItem) {
		T searchObj = mDaoObj.load(searchItem);
		
		if (DBConfig.DEBUG) {
			Config.LOGD("[[search]] origin search item = " + searchItem);
			Config.LOGD("[[search]] item = " + searchObj);
		}
		
		return searchObj != null;
	}
	
}
