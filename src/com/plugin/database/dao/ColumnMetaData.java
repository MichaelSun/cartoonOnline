package com.plugin.database.dao;

/**
 * @Date 2012-08-23
 */
class ColumnMetaData implements Comparable<ColumnMetaData> {

	final ColumnMetaData parentColumn;

	final String columnName;

	/**
	 * 在数据库中对应的字段全名，规则如下：如果不是嵌套类的字段，那么为字段名称，如果是嵌套类的字段，那么为形如A_B_C_字段名称的字符串，其中A，B，
	 * C为嵌套类字段名称
	 */
	final String realColumnName;

	final boolean isPrimaryKey;

	final Class<?> type;

	final boolean isArray;

	final String orderBy;

	final boolean isAutoIncrement;

	final boolean isUnique;

	/**
	 * 是否是复杂类型
	 */
	final boolean hasSubColumns;

	public ColumnMetaData(String columnName, boolean isPrimaryKey, Class<?> type, boolean isArray, String orderBy,
			boolean isAutoIncrement, boolean isUnique) {
		this(columnName, isPrimaryKey, type, isArray, orderBy, false, isAutoIncrement, isUnique);
	}

	public ColumnMetaData(String columnName, boolean isPrimaryKey, Class<?> type, boolean isArray, String orderBy,
			boolean hasSubColumns, boolean isAutoIncrement, boolean isUnique) {
		this(null, columnName, isPrimaryKey, type, isArray, orderBy, hasSubColumns, isAutoIncrement, isUnique);
	}

	public ColumnMetaData(ColumnMetaData parentColumn, String columnName, boolean isPrimaryKey, Class<?> type,
			boolean isArray, String orderBy, boolean hasSubColumns, boolean isAutoIncrement, boolean isUnique) {
		this.parentColumn = parentColumn;
		this.columnName = columnName;
		this.isPrimaryKey = isPrimaryKey;
		this.type = type;
		this.isArray = isArray;
		this.orderBy = orderBy;
		this.hasSubColumns = hasSubColumns;
		this.isAutoIncrement = isAutoIncrement;
		this.isUnique = isUnique;

		StringBuilder sb = new StringBuilder();
		sb.append(columnName);
		for (ColumnMetaData column = parentColumn; column != null; column = column.parentColumn) {
			sb.insert(0, "_");
			sb.insert(0, column.columnName);
		}
		this.realColumnName = sb.toString();
	}

	@Override
	public int compareTo(ColumnMetaData another) {
		return this.columnName.compareTo(another.columnName);
	}

}
