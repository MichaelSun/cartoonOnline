package com.cartoononline;

import com.plugin.common.utils.files.DiskManager;

public class Config {

    public static final boolean DEBUG = false;
    
    public static final boolean WALL_DEBUG = true && DEBUG;
    
    public static final boolean DIS_ADVIEW_DEBUG = true && DEBUG;
    
    public static final String ROOT_DIR = DiskManager.tryToFetchCachePathByType(DiskManager.DiskCacheType.PICTURE);
    
    public static final String SESSION_REFIX = "session";
    
    public static final String DOWNLOAD_DIR = ROOT_DIR + "download/";
    
    public static final String INI_FILE = "info.ini";
    
    public static int INDEX = 0;
    public static final String[] YOUMI_APP_ID = new String[] { "debbd13b5914aa88", "556af9dfcb127141", "eb90ce5fc3bca339" };
    public static final String[] YOUMI_APP_SECRET_KEY = new String[] { "b9eff83f525ef086", "a3b8473046176ba2", "af8d30ec5dbc40aa" };
    public static final String[] PACKAGE_NAME = new String[] { "com.album.leg", "com.michael.manhua", "com.michael.rosi" };
    public static final String[] PUSH_TAG = new String[] { "yaojing", "manhua", "rosi" };
    public static final String[] DOMAIN_NAME = new String[] { "psave", "xiee", "rosi" };
    
    public static final String KEY_SHOW_WALL = "show_app_wall";
    public static final String KEY_ADVIEW = "_adview";
    
    public static final boolean ADVIEW_SHOW = true;
    
    //umeng event
    public static final String DOWNLOAD_ALUBM = "download_album";
    public static final String CURRENT_POINT = "current_point";
    public static final String OPEN_WITH_PUSH = "open_with_push";
    public static final String OPEN_ALUBM = "open_album";
    
    public static final int DEFAULT_POINT = 5;
    public static final int CLOSE_ADVIEW_POINT = 20;
    
    public static final String PACKAGE_CARTOON = PACKAGE_NAME[0];
    public static final String PACKAGE_XIEE = PACKAGE_NAME[1];
    public static final String PACKAGE_ROSI = PACKAGE_NAME[2];
    
    public static boolean APP_STARTED = false;
    
}
