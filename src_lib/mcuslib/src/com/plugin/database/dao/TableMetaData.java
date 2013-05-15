package com.plugin.database.dao;

import java.util.ArrayList;
import java.util.List;

/**
 * @Date 2012-08-23
 */
class TableMetaData {
	final Class<?> tableType;
	final String tableName;
	final List<ColumnMetaData> allColumns;
	final List<ColumnMetaData> pkColumns;
	final List<ColumnMetaData> orderByColumns;
	final List<ColumnMetaData> autoIncrementColumns;
	final List<ColumnMetaData> uniqueColumns;

	public TableMetaData(Class<?> type) {
		this.tableType = type;
		this.tableName = DaoUtil.getTableName(type);
		this.allColumns = DaoUtil.getAllColumns(type);

		this.pkColumns = new ArrayList<ColumnMetaData>();
		this.orderByColumns = new ArrayList<ColumnMetaData>();
		this.autoIncrementColumns = new ArrayList<ColumnMetaData>();
		this.uniqueColumns = new ArrayList<ColumnMetaData>();
		
		for (ColumnMetaData column : allColumns) {
			// 不支持嵌套类设置主键或排序
			if (column.isPrimaryKey && !column.hasSubColumns) {
				this.pkColumns.add(column);
			}

			if (column.orderBy != null && !column.hasSubColumns) {
				this.orderByColumns.add(column);
			}
			
			if (column.isAutoIncrement && !column.hasSubColumns) {
				this.autoIncrementColumns.add(column);
			}
			
			if (column.isUnique && !column.hasSubColumns) {
				this.uniqueColumns.add(column);
			}
		}
	}
}