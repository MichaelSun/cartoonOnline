package com.cartoononline;

import com.plugin.common.utils.UtilsConfig;
import com.plugin.common.utils.files.DiskManager;

public class Config {

    public static final boolean DEBUG = false;

    public static final boolean OPEN_HOT = false;
    
    public static boolean BOOK_REVIEW = false;

    public static final boolean WALL_DEBUG = true && DEBUG;

    public static final String ROOT_DIR = DiskManager.tryToFetchCachePathByType(DiskManager.DiskCacheType.PICTURE);

    public static final String SESSION_REFIX = "session";

    public static final String BOOK_DOWNLOAD_DIR = "/sdcard/book_download/";
    
    public static String CURRENT_PACKAGE_NAME;

    public static final String INI_FILE = "info.ini";

    public static int INDEX = 0;
    public static final String[] YOUMI_APP_ID = new String[] { "e3ff884a6cc7b41a", 
                                                               "556af9dfcb127141",
                                                               "bebe748a9e64ef29",
                                                               "bebe748a9e64ef29"};
    public static final String[] YOUMI_APP_SECRET_KEY = new String[] { "26ab38202db7d6fc", 
                                                                       "a3b8473046176ba2",
                                                                       "310968f5f26311a4",
                                                                       "310968f5f26311a4"};
    public static final String[] PACKAGE_NAME = new String[] { "com.cartoononline", 
                                                               "com.michael.manhua",
                                                               "com.album.rosi", 
                                                               "com.read.book"};
    public static final String[] DOMAIN_NAME = new String[] { "psave", "xiee", "rosi", "bookread" };

    public static final String KEY_SHOW_WALL = "show_app_wall";
    public static final String KEY_ADVIEW = "_adview";

    public static final boolean ADVIEW_SHOW = true;

    // umeng event
    public static final String DOWNLOAD_ALUBM = "download_album";
    public static final String DOWNLOAD_ALUBM_HOT = "download_album_hot";
    public static final String CURRENT_POINT = "current_point";
    public static final String OPEN_WITH_PUSH = "open_with_push";
    public static final String OPEN_ALUBM = "open_album";
    public static final String RATE_APP = "rate_app";

    public static int DOWNLOAD_NEED_POINT = 5;
    public static final int DEFAULT_POINT = 5;
    public static final int CLOSE_ADVIEW_POINT = 20;

    public static final String PACKAGE_CARTOON = PACKAGE_NAME[0];
    public static final String PACKAGE_XIEE = PACKAGE_NAME[1];
    public static final String PACKAGE_ROSI = PACKAGE_NAME[2];
    public static final String PACKAGE_BOOK = PACKAGE_NAME[3];

    public static boolean APP_STARTED = false;

    public static void LOGD(String msg) {
        if (DEBUG) {
            UtilsConfig.LOGD(msg);
        }
    }

}
