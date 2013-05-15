package com.plugin.database.dao;

import com.plugin.common.utils.UtilsConfig;

public class DBConfig {
	public static final boolean DEBUG = true && UtilsConfig.UTILS_DEBUG;
	
	public static final String DATABASE_NAME = "data.db";

	public static final int DATABASE_VERSION = 21;

	public static final String DATABASE_CONFIG_FILE = "db_config.properties";
}
