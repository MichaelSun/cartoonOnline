/**
 * BitmapUtils.java
 */
package com.plugin.common.utils.image;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;

import com.plugin.common.cache.CacheFactory;
import com.plugin.common.utils.UtilsConfig;

/**
 * @author Guoqing Sun Dec 12, 20125:20:20 PM
 */
public class BitmapUtils {

    public static final int THUMBNAIL_BITMAP_SIZE = 72;
    public static final int USER_HEAD_STANDARD_SIZE = 150;

    @Deprecated()
    public static boolean makeThumbnail(Bitmap bt, String category, String key) {
        try {
            if (bt != null) {
                Bitmap microBt = ThumbnailUtils.extractThumbnail(bt, THUMBNAIL_BITMAP_SIZE, THUMBNAIL_BITMAP_SIZE);
                if (microBt != null && !microBt.isRecycled()) {
                    CacheFactory.getCacheManager(CacheFactory.TYPE_CACHE.TYPE_IMAGE).putResource(
                            UtilsConfig.IMAGE_CACHE_CATEGORY_THUMB, key, microBt);
                }
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
    
    public static boolean makeThumbnailAndCached(Bitmap bt, String key) {
        try {
            if (bt != null) {
                Bitmap microBt = ThumbnailUtils.extractThumbnail(bt, THUMBNAIL_BITMAP_SIZE, THUMBNAIL_BITMAP_SIZE);
                if (microBt != null && !microBt.isRecycled()) {
                    CacheFactory.getCacheManager(CacheFactory.TYPE_CACHE.TYPE_IMAGE).putResource(
                            UtilsConfig.IMAGE_CACHE_CATEGORY_THUMB, key, microBt);
                }
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}
