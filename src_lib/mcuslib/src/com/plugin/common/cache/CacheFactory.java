package com.plugin.common.cache;

public class CacheFactory {

    public static final class Option {
        public boolean needThumbnail;
    }
    
    public enum TYPE_CACHE {
        TYPE_IMAGE,
        TYPE_STRING,
        TYPE_FILE
    }
    
    public static ICacheManager getCacheManager(TYPE_CACHE type) {
        switch (type) {
        case TYPE_IMAGE:
            return BitmapCacheManagerDelegate.getInstance();
        case TYPE_STRING:
            return StringCacheManager.getInstance();
        case TYPE_FILE:
            //TODO: should add file cache
            break;
        }
        
        throw new IllegalArgumentException("Cache type not supported");
    }
    
    public static void init(Option opt) {
        BitmapCacheManagerDelegate.getInstance().init(opt);
    }
    
}
