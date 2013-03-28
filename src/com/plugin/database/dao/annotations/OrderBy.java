package com.plugin.database.dao.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 指定按照某个字段排序
 * 
 * 使用方法：@OrderBy(order="DESC")（降序） @OrderBy(order="ASC")（升序）
 * 
 * @Date 2012-08-23
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface OrderBy {
	public abstract String order();
}
