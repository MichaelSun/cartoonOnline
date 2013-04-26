package com.cartoononline;

import com.plugin.common.utils.files.DiskManager;

public class AppConfig {

    public static final boolean DEBUG = false;
    
    public static final String ROOT_DIR = DiskManager.tryToFetchCachePathByType(DiskManager.DiskCacheType.PICTURE);
    
    public static final String SESSION_REFIX = "session";
    
    public static final String DOWNLOAD_DIR = ROOT_DIR + "download/";
    
    public static final String INI_FILE = "info.ini";
    
    public static final String YOUMI_APP_ID = "e3ff884a6cc7b41a";
    public static final String YOUMI_APP_SECRET_KEY = "26ab38202db7d6fc";
    
    public static final String KEY_SHOW_WALL = "show_app_wall";
    
}
