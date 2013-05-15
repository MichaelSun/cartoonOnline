/**
 * BaseTableMetaData.java
 */
package com.plugin.database.dao;

import android.database.Cursor;

/**
 * @author dingwei.chen 2013-3-6下午1:36:55
 */
public class BaseTableMetaData {

	public String type;
	public String sql;
	public String name;

	public BaseTableMetaData(Cursor cursor) {
		int indexType = cursor.getColumnIndex("type");
		int indexName = cursor.getColumnIndex("name");
		int indexSql = cursor.getColumnIndex("sql");
		type = cursor.getString(indexType);
		name = cursor.getString(indexName);
		sql = cursor.getString(indexSql);
	}

	public BaseTableMetaData(String name, String sql) {
		this.type = "table";
		this.name = name;
		this.sql = sql;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		// TODO Auto-generated method stub
		if (o instanceof BaseTableMetaData || o == null) {
			// DebugLog.log("db", " o = "+o);
			return false;
		}
		BaseTableMetaData temp = (BaseTableMetaData) o;
		// DebugLog.log("db",this.sql+"~~~~~"+temp.sql);
		return this.sql.equals(temp.sql);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "[type = " + type + ",\r\n name =" + name + ",\r\n{" + sql + "}]";
	}

}
