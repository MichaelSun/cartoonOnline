package com.plugin.database.dao;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;
import android.util.Log;

import com.plugin.database.dao.annotations.AutoIncrement;
import com.plugin.database.dao.annotations.DataStorageType;
import com.plugin.database.dao.annotations.Ignore;
import com.plugin.database.dao.annotations.OrderBy;
import com.plugin.database.dao.annotations.PrimaryKey;
import com.plugin.database.dao.annotations.Unique;

/**
 * @Date 2012-08-23
 */
class DaoUtil {
	private static final String TAG = "DaoUtil";
	private static final boolean DEBUG = DBConfig.DEBUG;

	public static final String GETTER_PREFIX = "get";
	public static final String SETTER_PREFIX = "set";
	public static final String BOOLEAN_GETTER_PREFIX = "is";

	public static final String INTEGER = "INTEGER";
	public static final String REAL = "REAL";
	public static final String TEXT = "TEXT";

	public static final Map<Class<?>, String> classToTypeMap = new HashMap<Class<?>, String>();

	static {
		classToTypeMap.put(String.class, TEXT);
		classToTypeMap.put(int.class, INTEGER);
		classToTypeMap.put(Integer.class, INTEGER);
		classToTypeMap.put(long.class, INTEGER);
		classToTypeMap.put(Long.class, INTEGER);
		classToTypeMap.put(float.class, REAL);
		classToTypeMap.put(Float.class, REAL);
		classToTypeMap.put(double.class, REAL);
		classToTypeMap.put(Double.class, REAL);
		classToTypeMap.put(boolean.class, INTEGER);
		classToTypeMap.put(Boolean.class, INTEGER);
	}

	public static String getTableName(Class<?> type) {
		return type.getSimpleName();
	}

	public static List<ColumnMetaData> getAllColumns(Class<?> type) {
		return getAllColumns(type, null);
	}

	private static List<ColumnMetaData> getAllColumns(Class<?> type, ColumnMetaData parentColumn) {
		List<ColumnMetaData> columns = new ArrayList<ColumnMetaData>();
		Class superClass = type.getSuperclass();
		Field[] superFields = superClass != null ? superClass.getDeclaredFields() : null;
		
		if (DEBUG) {
			LOGD("[[getAllColumns]] >>>>>>>>>>>>>>>>>>");
			LOGD(" super class name : " + type.getClass().getSuperclass());
			LOGD(" Declared Fields : ");
			if (superClass != null) {
				for (Field f : type.getSuperclass().getDeclaredFields()) {
					LOGD(" Field : " + f.getName());
				}
			}
			LOGD(">>>>>>>>>>>>>>>>>>>>>>");
			LOGD(" class name : " + type.getClass());
			LOGD(" Declared Fields : ");
			for (Field f : type.getDeclaredFields()) {
				LOGD(" Field : " + f.getName());
			}
			LOGD(">>>>>>>>>>>>>>>>>>>>>>");
		}
		
		if (superFields != null) {
			columns.addAll(getColumnsByFields(superFields, parentColumn));
		}
		columns.addAll(getColumnsByFields(type.getDeclaredFields(), parentColumn));

		return columns;
	}
	
	private static List<ColumnMetaData> getColumnsByFields(Field[] fields, ColumnMetaData parentColumn) {
		List<ColumnMetaData> ret = new ArrayList<ColumnMetaData>();
		
		for (Field field : fields) {
			// 忽略@Ignore字段
			if (field.isAnnotationPresent(Ignore.class)) {
				continue;
			}
			
			Class<?> fieldType = null;
			if (field.isAnnotationPresent(DataStorageType.class)) {
				String type = field.getAnnotation(DataStorageType.class).type();
				if (!TextUtils.isEmpty(type) && classToTypeMap.values().contains(type)) {
					for (Class<?> key : classToTypeMap.keySet()) {
						if (classToTypeMap.get(key).equals(type)) {
							fieldType = key;
							break;
						}
					}
				}
			} else {
				fieldType = field.getType();
			}
			
			boolean isArray = fieldType.isArray();
			fieldType = isArray ? fieldType.getComponentType() : fieldType;

			boolean hasSubColumns = !classToTypeMap.containsKey(fieldType);
			ColumnMetaData column = new ColumnMetaData(parentColumn, 
										field.getName(),
										field.isAnnotationPresent(PrimaryKey.class), 
										fieldType, 
										isArray,
										field.isAnnotationPresent(OrderBy.class) ? field.getAnnotation(OrderBy.class).order() : null,
									    hasSubColumns ? true : false, 
									    field.isAnnotationPresent(AutoIncrement.class),
									    field.isAnnotationPresent(Unique.class));
			// 添加列
			ret.add(column);
			// 如有是嵌套类，则添加嵌套类的列
			if (hasSubColumns) {
				ret.addAll(getAllColumns(fieldType, column));
			}
		}
		
		return ret;
	}

	public static Object getValueByColumnMetaData(ColumnMetaData column, Object t) {
		if (t == null) {
			return null;
		}

		List<ColumnMetaData> parents = new LinkedList<ColumnMetaData>();
		for (ColumnMetaData c = column; c != null; c = c.parentColumn) {
			parents.add(0, c);
		}

		if (parents.size() == 0) {
			return null;
		}
		
		Object object = null;
		try {
			object = findGetMethod(t.getClass(), parents.get(0).columnName).invoke(t);
			if (object == null) {
				return null;
			}
			int length = parents.size();
			for (int i = 0; i < length - 1; i++) {
				object = findGetMethod(parents.get(i).type, parents.get(i + 1).columnName).invoke(object);
				if (object == null) {
					return null;
				}
			}
		} catch (IllegalArgumentException e) {
			LOGD("[[getValueByColumnMetaData]] IllegalArgumentException");
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			LOGD("[[getValueByColumnMetaData]] IllegalAccessException");
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			LOGD("[[getValueByColumnMetaData]] InvocationTargetException");
			e.printStackTrace();
		}

		if (DEBUG) {
			LOGD("[[getValueByColumnMetaData]] " + column.columnName + "=" + String.valueOf(object));
		}

		return object;
	}

	public static Object getValueByFieldName(String fieldName, Object entity) {
		try {
			Method method = findGetMethod(entity.getClass(), fieldName);
			return method.invoke(entity, new Object[] {});
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static void setValueByFieldName(String fieldName, Object fieldValue, Object entity) {
		try {
			Method method = findGetMethod(entity.getClass(), fieldName);
			method.invoke(entity, new Object[] { fieldValue });
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	private static Method findGetMethod(Class<?> clazz, String fieldName) {
		String suffix = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);

		Method getMethod = null;
		try {
			getMethod = clazz.getMethod(GETTER_PREFIX + suffix);
		} catch (NoSuchMethodException e) {
			LOGD("[[findGetMethod]] no method [" + GETTER_PREFIX + suffix + "]");
			e.printStackTrace();
		}

		if (getMethod == null) {
			try {
				getMethod = clazz.getMethod(BOOLEAN_GETTER_PREFIX + suffix);
			} catch (NoSuchMethodException e) {
				LOGD("[[findGetMethod]] no method [" + GETTER_PREFIX + suffix + "]");
				e.printStackTrace();
			}
		}

		return getMethod;
	}

	private static Method findSetMethod(Class<?> clazz, Class<?> fieldType, String fieldName) throws SecurityException,
			NoSuchMethodException {
		String methodName = SETTER_PREFIX + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
		return clazz.getMethod(methodName, fieldType);
	}

	public static <T> void bindColumn(SQLiteStatement statement, int index, Object value) {
		if (value instanceof String) {
			statement.bindString(index, String.valueOf(value));
		} else if (value instanceof Integer) {
			statement.bindLong(index, (Integer) value);
		} else if (value instanceof Long) {
			statement.bindLong(index, (Long) value);
		} else if (value instanceof Double) {
			statement.bindDouble(index, (Double) value);
		} else if (value instanceof Float) {
			statement.bindDouble(index, ((Float) value).doubleValue());
		} else if (value instanceof Boolean) {
			statement.bindLong(index, (((Boolean) value).booleanValue()) ? 1 : 0);
		}
	}

	public static <T> T readSingleEntityFromCursor(Class<?> entityClass, Cursor cursor, TableMetaData tableMetaData) {
		if (cursor == null || entityClass == null) {
			return null;
		}

		T t = null;
		cursor.moveToFirst();
		List<ColumnMetaData> allColumns = new ArrayList<ColumnMetaData>(tableMetaData.allColumns);

		try {
			if (cursor.isAfterLast() == false) {
				t = readEntityFromCursor(entityClass, cursor, allColumns);
			}
		} finally {
			cursor.close();
		}

		return t;
	}

	public static <T> List<T> readListEntityFromCursor(Class<?> entityClass, Cursor cursor, TableMetaData tableMetaData) {
		if (cursor == null || entityClass == null) {
			return null;
		}

		List<T> result = new ArrayList<T>();

		cursor.moveToFirst();
		List<ColumnMetaData> allColumns = new ArrayList<ColumnMetaData>(tableMetaData.allColumns);

		try {
			while (cursor.isAfterLast() == false) {
				T t = readEntityFromCursor(entityClass, cursor, allColumns);
				result.add(t);

				cursor.moveToNext();
			}
		} finally {
			cursor.close();
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	private static <T> T readEntityFromCursor(Class<?> entityClass, Cursor cursor, List<ColumnMetaData> allColumns) {
		T t = null;
		try {
			t = ((Class<T>) entityClass).newInstance();
			Method method = null;
			for (ColumnMetaData column : allColumns) {
				// 如果是嵌套类型，则创建实体
				if (column.hasSubColumns) {
					List<ColumnMetaData> parents = new LinkedList<ColumnMetaData>();
					for (ColumnMetaData c = column; c != null; c = c.parentColumn) {
						parents.add(0, c);
					}

					int length = parents.size();
					Object object = null;
					Class<?> columnType = column.type;
					String columnName = column.columnName;
					if (length > 1) {
						object = findGetMethod(entityClass, parents.get(0).columnName).invoke(t);
						for (int i = 0; i < length - 2; i++) {
							object = findGetMethod(parents.get(i).type, parents.get(i + 1).columnName).invoke(object);
						}
						method = findSetMethod(parents.get(length - 2).type, columnType, columnName);
					} else {
						object = t;
						method = findSetMethod(entityClass, columnType, columnName);
					}
					method.setAccessible(true);
					method.invoke(object, new Object[] { columnType.newInstance() });
				} else {
					if (column.parentColumn == null) {
						Class<?> columnType = column.type;
						String columnName = column.columnName;
						method = findSetMethod(entityClass, columnType, columnName);
						method.setAccessible(true);
						method.invoke(t, new Object[] { readColumnFromCursor(cursor, columnName, columnType) });
					} else {
						List<ColumnMetaData> parents = new LinkedList<ColumnMetaData>();
						for (ColumnMetaData c = column; c != null; c = c.parentColumn) {
							parents.add(0, c);
						}

						Object object = findGetMethod(entityClass, parents.get(0).columnName).invoke(t);
						int length = parents.size();
						for (int i = 0; i < length - 2; i++) {
							object = findGetMethod(parents.get(i).type, parents.get(i + 1).columnName).invoke(object);
						}

						Class<?> columnType = parents.get(length - 1).type;
						String columnName = parents.get(length - 1).columnName;
						String realColumnName = parents.get(length - 1).realColumnName;
						method = findSetMethod(parents.get(length - 2).type, columnType, columnName);
						method.setAccessible(true);
						method.invoke(object, new Object[] { readColumnFromCursor(cursor, realColumnName, columnType) });
					}
				}
			}
		} catch (InstantiationException e) {
			LOGD("[[readEntityFromCursor]] InstantiationException");
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			LOGD("[[readEntityFromCursor]] IllegalAccessException");
			e.printStackTrace();
		} catch (SecurityException e) {
			LOGD("[[readEntityFromCursor]] SecurityException");
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			LOGD("[[readEntityFromCursor]] NoSuchMethodException");
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			LOGD("[[readEntityFromCursor]] IllegalArgumentException");
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			LOGD("[[readEntityFromCursor]] InvocationTargetException");
			e.printStackTrace();
		}

		return t;
	}

	private static Object readColumnFromCursor(Cursor cursor, String columnName, Class<?> columnType) {
//		if (DEBUG) {
//			LOGD("[[readColumnFromCursor]] columnName = " + columnName + "\t columnType = " + columnType);
//		}

		int columnIndex = cursor.getColumnIndex(columnName);
		if (columnType == int.class || columnType == Integer.class) {
			return cursor.getInt(columnIndex);
		} else if (columnType == long.class || columnType == Long.class) {
			return cursor.getLong(columnIndex);
		} else if (columnType == double.class || columnType == Double.class) {
			return cursor.getDouble(columnIndex);
		} else if (columnType == float.class || columnType == Float.class) {
			return cursor.getFloat(columnIndex);
		} else if (columnType == boolean.class || columnType == Boolean.class) {
			return (cursor.getInt(columnIndex) == 1) ? true : false;
		} else if (columnType == String.class) {
			return cursor.getString(columnIndex);
		} else {
			// complex type. Impossible.
			return null;
		}
	}

	private static void LOGD(String message) {
		if (DEBUG) {
			Log.i(TAG, message);
		}
	}
}
