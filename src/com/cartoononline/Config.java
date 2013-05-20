package com.cartoononline;

import com.plugin.common.utils.files.DiskManager;

public class Config {

    public static final boolean DEBUG = false;
    
    public static final String ROOT_DIR = DiskManager.tryToFetchCachePathByType(DiskManager.DiskCacheType.PICTURE);
    
    public static final String SESSION_REFIX = "session";
    
    public static final String DOWNLOAD_DIR = ROOT_DIR + "download/";
    
    public static final String INI_FILE = "info.ini";
    
    public static int INDEX = 0;
    public static final String[] YOUMI_APP_ID = new String[] { "e3ff884a6cc7b41a", "556af9dfcb127141" };
    public static final String[] YOUMI_APP_SECRET_KEY = new String[] { "26ab38202db7d6fc", "a3b8473046176ba2" };
    public static final String[] PACKAGE_NAME = new String[] { "com.cartoononline", "com.michael.manhua" };
    public static final String[] PUSH_TAG = new String[] { "yaojing", "manhua" };
    public static final String[] DOMAIN_NAME = new String[] { "psave", "xiee" };
    
    public static final String KEY_SHOW_WALL = "show_app_wall";
    public static final String KEY_ADVIEW = "_adview";
    
    public static boolean ADVIEW_SHOW = true;
    
    //umeng event
    public static final String DOWNLOAD_ALUBM = "download_album";
    public static final String CURRENT_POINT = "current_point";
    public static final String OPEN_WITH_PUSH = "open_with_push";
    public static final String OPEN_ALUBM = "open_album";
    
    public static final String PACKAGE_CARTOON = PACKAGE_NAME[0];
    public static final String PACKAGE_XIEE = PACKAGE_NAME[1];
    
}
