package com.plugin.database.dao;

import java.util.List;

/**
 * @Date 2012-08-20
 */
public interface Dao<T> {

	/*
	 * 返回数据表的条目
	 */
	public long count();

	/**
	 * 插入一个数据对象
	 * 
	 * @param entity
	 * @return
	 */
	public long insert(T entity);

	/**
	 * 插入或更新
	 * 
	 * @param entity
	 * @return
	 */
	public long insertOrReplace(T entity);
	
	public boolean batchInsertOrReplace(T... entities);

	/**
	 * 插入一组对象
	 * 
	 * @param entities
	 */
	public void batchInsert(T... entities);

	public void update(T entity);
	
	public boolean delete(T entity);

	public boolean batchDelete(T... entities);

	public void deleteAll();

	public T load(T entity);

	public List<T> loadAll();

	public List<T> loadByLimit(int start, int length);

	public List<T> queryRaw(Class<T> entityClass, String where, String... selectionArgs);

	public List<T> queryRawByLimit(Class<T> entityClass, int start, int length, String where, String... selectionArgs);

	public int deleteRaw(Class<T> entityClass, String where, String... selectionArgs);

	public void executeRaw(String sql);

	public void executeRaw(String sql, Object[] args);
}
