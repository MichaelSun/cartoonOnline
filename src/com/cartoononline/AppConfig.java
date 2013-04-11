package com.cartoononline;

import com.plugin.common.utils.files.DiskManager;

public class AppConfig {

    public static final boolean DEBUG = true;
    
    public static final String ROOT_DIR = DiskManager.tryToFetchCachePathByType(DiskManager.DiskCacheType.PICTURE);
    
    public static final String SESSION_REFIX = "session";
    
    public static final String DOWNLOAD_DIR = ROOT_DIR + "download/";
    
    public static final String INI_FILE = "info.ini";
    
    public static final String YOUMI_APP_ID = "53e13f6906527b33";
    public static final String YOUMI_APP_SECRET_KEY = "e1703eadcc64253b";
    
}
