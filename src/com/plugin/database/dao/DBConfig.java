package com.plugin.database.dao;

import com.plugin.common.utils.Config;

public class DBConfig {
	public static final boolean DEBUG = true && Config.UTILS_DEBUG;
	
	public static final String DATABASE_NAME = "data.db";

	public static final int DATABASE_VERSION = 21;

	public static final String DATABASE_CONFIG_FILE = "db_config.properties";
}
