package com.plugin.database.dao;

import com.plugin.database.dao.annotations.AutoIncrement;
import com.plugin.database.dao.annotations.PrimaryKey;

/**
 * 数据库实体类基类
 */
public class EntityBase {

	/**
	 * 对应数据库'_ID'字段
	 */
	@PrimaryKey()
	@AutoIncrement()
	protected long _ID;

	public EntityBase() {
	}

	public long get_ID() {
		return _ID;
	}

	public void set_ID(long _ID) {
		this._ID = _ID;
	}

}
