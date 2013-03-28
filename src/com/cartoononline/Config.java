package com.cartoononline;

import com.plugin.common.utils.files.DiskManager;

public class Config {

    public static final boolean DEBUG = true;
    
    public static final String ROOT_DIR = DiskManager.tryToFetchCachePathByType(DiskManager.DiskCacheType.PICTURE);
    
    public static final String SDCARD_INFO_FILE = ROOT_DIR + "info.ini";
    
}
