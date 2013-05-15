/**
 * SyncDBTableAccessHelper.java
 */
package com.plugin.database.dao.helper;

import java.util.List;

import android.content.Context;

/**
 * @author Guoqing Sun Mar 18, 20135:06:16 PM
 * @param <T>
 */
public class SyncDBTableAccessHelper<T> extends DBTableAccessHelper<T> {

	/**
	 * @param context
	 * @param cl
	 */
	public SyncDBTableAccessHelper(Context context, Class<T> cl) {
		super(context, cl);
	}
	
	@Override
	public synchronized long getCount() {
		return super.getCount();
	}
	
	@Override
	public synchronized List<T> queryItems() {
		return super.queryItems();
	}
	
	@Override
	public synchronized List<T> queryItems(String selection, String... selectionArgs) {
		return super.queryItems(selection, selectionArgs);
	}
	
	@Override
	public synchronized T queryItem(T searchItem) {
		return super.queryItem(searchItem);
	}
	
	@Override
	public synchronized List<T> queryLimit(int start, int length) {
		return super.queryLimit(start, length);
	}
	
	@Override
	public synchronized boolean insert(T item) {
		return super.insert(item);
	}
	
	@Override
	public synchronized boolean blukInsert(T[] items) {
		return super.blukInsert(items);
	}
	
	@Override
	public synchronized boolean blukInsertOrReplace(T[] items) {
		return super.blukInsertOrReplace(items);
	}
	
	@Override
	public synchronized boolean insertOrReplace(T item) {
		return super.insertOrReplace(item);
	}
	
	@Override
	public synchronized boolean update(T item) {
		return super.update(item);
	}
	
	@Override
	public boolean delete(String selection, String selectionArgs) {
		return super.delete(selection, selectionArgs);
	}
	
	@Override
	public synchronized boolean delete(T item) {
		return super.delete(item);
	}
	
	@Override
	public synchronized boolean delete(T[] items) {
		return super.delete(items);
	}
	
	@Override
	public synchronized boolean deleteAll() {
		return super.deleteAll();
	}
	
	@Override
	public synchronized boolean search(T searchItem) {
		return super.search(searchItem);
	}

}
