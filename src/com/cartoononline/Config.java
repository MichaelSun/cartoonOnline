package com.cartoononline;

import com.plugin.common.utils.UtilsConfig;
import com.plugin.common.utils.files.DiskManager;

public class Config {

    public static final boolean DEBUG = false;

    public static final boolean SPOT_ADVIEW_SHOW = false;

    public static final boolean OPEN_HOT = true;

    public static boolean BOOK_REVIEW = false;

    public static final boolean WALL_DEBUG = true && DEBUG;

    public static final String JIFENBAO_DOWNLOAD_URL = "http://bcs.duapp.com/jifenbao/jifenbao-release.apk?sign=MBO:27302677c46c1c5b7795853ba23d0329:0yCmmYSUIxd0kvaSYF9l8JtRw8U%3D";

    public static final String ROOT_DIR = DiskManager.tryToFetchCachePathByType(DiskManager.DiskCacheType.PICTURE);

    public static final String SESSION_REFIX = "session";

    public static final String BOOK_DOWNLOAD_DIR = "/sdcard/book_download/";

    public static String CURRENT_PACKAGE_NAME;

    public static final String INI_FILE = "info.ini";

    public static int INDEX = -1;
    public static final String[] YOUMI_APP_ID = new String[] { "f3d95e27c4e24153", "556af9dfcb127141",
            "f112e41a8f0b1e6d", "6e40438e92373bcb", "e46163352bdba86c" };
    public static final String[] YOUMI_APP_SECRET_KEY = new String[] { "82417303556b6b77", "a3b8473046176ba2",
            "22effcfb92694f35", "4fff1820c9c76e06", "d1764ac7070d96dd" };
    public static final String[] PACKAGE_NAME = new String[] { "com.album.legnew", "com.michael.manhua",
            "com.album.rosinil", "com.read.book", "com.read.booknew" };
    public static final String[] DOMAIN_NAME = new String[] { "psave", "xiee", "rosi", "bookread", "bookread" };

    public static final String KEY_SHOW_WALL = "show_app_wall";
    public static final String KEY_ADVIEW = "_adview";

    public static final boolean ADVIEW_SHOW = true;

    public static final String JIFENBAP_PACKAGE_NAME = "com.jifen.point";
    
    // umeng event
    public static final String DOWNLOAD_ALUBM = "download_album";
    public static final String DOWNLOAD_ALUBM_HOT = "download_album_hot";
    public static final String CURRENT_POINT = "current_point";
    public static final String OPEN_WITH_PUSH = "open_with_push";
    public static final String OPEN_ALUBM = "open_album";
    public static final String RATE_APP = "rate_app";
    public static final String OPEN_FB_DOWNLOAD = "open_fb_download";
    public static final String OPEN_BOOK_SELF = "open_book_self";

    public static int DOWNLOAD_NEED_POINT = 10;
    public static final int DEFAULT_POINT = 0;
    public static final int CLOSE_ADVIEW_POINT = 20;

    // public static final String PACKAGE_CARTOON = PACKAGE_NAME[0];
    // public static final String PACKAGE_XIEE = PACKAGE_NAME[1];
    // public static final String PACKAGE_ROSI = PACKAGE_NAME[2];
    // public static final String PACKAGE_BOOK = PACKAGE_NAME[3];
    // public static final String PACKAGE_BOOK_NEW = PACKAGE_NAME[4];

    public static boolean APP_STARTED = false;

    public static void LOGD(String msg) {
        if (DEBUG) {
            UtilsConfig.LOGD(msg);
        }
    }

}
