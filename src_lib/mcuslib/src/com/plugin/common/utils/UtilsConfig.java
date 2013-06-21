/**
 * Config.java
 */
package com.plugin.common.utils;

import com.plugin.common.utils.SingleInstanceBase.SingleInstanceManager;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

/**
 * @author Guoqing Sun Mar 11, 20133:08:25 PM
 */
public class UtilsConfig {
	
	public static final boolean UTILS_DEBUG = false;
	
	public static final boolean DEBUG_NETWORK_ST = false & UTILS_DEBUG;
	public static final boolean RELEASE_UPLOAD_CRASH_LOG = !UTILS_DEBUG;
	
	public static final String CURRENT_PACKAGE_NAME = "com.read.book";
	
	public static final int BITMAP_COMPRESS_LOW = 80;
	public static final int BITMAP_COMPRESS_MEDIUM = 90;
	public static final int BITMAP_COMPRESS_HIGH = 100;
	public static final String DISK_DIR_PATH = Environment.getExternalStorageDirectory() + "/." + CURRENT_PACKAGE_NAME + "/";
	public static final String DISK_LOG_PATH = Environment.getExternalStorageDirectory() + "/." + CURRENT_PACKAGE_NAME + "/";
	/*
	 * 各种图片CacheManager的Category
	 */
	public static final String IMAGE_CACHE_CATEGORY_USER_HEAD_ROUNDED = "user_head_rounded";
	public static final String IMAGE_CACHE_CATEGORY_RAW = "image_cache_category_source";
	public static final String IMAGE_CACHE_CATEGORY_THUMB = "image_cache_category_thumb";
	
	public static DeviceInfo DEVICE_INFO;
	
	/**
	 * init the utils lib
	 * 
	 * @param context
	 */
	public static void init(Context context) {
	    if (context != null) {
	        SingleInstanceManager.getInstance().init(context.getApplicationContext());
	        UtilsConfig.DEVICE_INFO = new DeviceInfo(context.getApplicationContext());
	    }
	}

	public static void LOGD(String msg, boolean withExtraInfo) {
		if (UtilsConfig.UTILS_DEBUG) {
			String method = "";
			if (withExtraInfo) {
				StackTraceElement ste = Thread.currentThread().getStackTrace()[4];
				String invokeMethodName = ste.getMethodName();
				String fileName = ste.getFileName();
				long line = ste.getLineNumber();
				if (!TextUtils.isEmpty(invokeMethodName)) {
					method = fileName + "::" + invokeMethodName + "::" + line;
				}
			}
			DebugLog.d(method, msg);
		}
	}

	public static void LOGD(String msg, Throwable t) {
		if (UtilsConfig.UTILS_DEBUG) {
			StackTraceElement ste = Thread.currentThread().getStackTrace()[4];
			String invokeMethodName = ste.getMethodName();
			String fileName = ste.getFileName();
			long line = ste.getLineNumber();
			String method = "com.plugin.common.utils.Config";
			if (!TextUtils.isEmpty(invokeMethodName)) {
				method = fileName + "::" + invokeMethodName + "::" + line;
			}
			DebugLog.d(method, msg, t);
		}
	}

	public static void LOGD(String msg) {
		LOGD(msg, true);
	}
	
	public static void LOGD_WITH_TIME(String msg) {
		LOGD(msg + " >>>>>> TIME : " + com.plugin.common.utils.Environment.debugFormatTime(System.currentTimeMillis()));
	}

	/**
	 * 流量统计关键字
	 */
	public static final String NETWORK_STATISTICS_TYPE = "newtwork_st";

	public static final String NETWORK_STATISTICS_CATEGORY_IMAGE = "newtwork_image";

	public static final String NETWORK_STATISTICS_UP = "up";

	public static final String NETWORK_STATISTICS_DOWN = "down";

}
