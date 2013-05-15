/**
 * DataStorageType.java
 */
package com.plugin.database.dao.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Guoqing Sun Mar 18, 20134:00:03 PM
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DataStorageType {
	public abstract String type();
}
