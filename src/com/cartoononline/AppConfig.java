package com.cartoononline;

import com.plugin.common.utils.files.DiskManager;

public class AppConfig {

    public static final boolean DEBUG = true;
    
    public static final String ROOT_DIR = DiskManager.tryToFetchCachePathByType(DiskManager.DiskCacheType.PICTURE);
    
    public static final String INTERNAL_SESSION_ONE = ROOT_DIR + "session1/";
    
    public static final String INI_FILE = "info.ini";
    
}
