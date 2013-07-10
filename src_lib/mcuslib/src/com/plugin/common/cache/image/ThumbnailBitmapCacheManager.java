/**
 * ThumbnailBitmapCacheManager.java
 */
package com.plugin.common.cache.image;

import android.support.v4.util.LruCache;

import com.plugin.common.utils.files.FileUtil;

/**
 * @author Guoqing Sun Jan 22, 20133:51:39 PM
 */
final class ThumbnailBitmapCacheManager extends AbsBitmapCacheManager {

    /*
     * (non-Javadoc)
     * 
     * @see com.sound.dubbler.cache.AbsBitmapCacheManager#makeLruCacheObj()
     */
    @Override
    LruCache<String, BitmapObject> makeLruCacheObj() {
        return new LruCache<String, BitmapObject>(1024 * 1024 * 2) {
            @Override
            protected int sizeOf(String key, BitmapObject value) {
                if (value != null) {
                    if (DEBUG) {
                        LOGD("[[sizeOf]] <<<<<<<<<< bitmap object get size = " + FileUtil.convertStorage(value.btSize)
                                + " >>>>>>>>>>>");
                    }
                    return value.btSize;
                }

                return BitmapObject.ObjdefaultSize();
            }

            @Override
            protected void entryRemoved(boolean evicted, String key, BitmapObject oldValue, BitmapObject newValue) {
                if (DEBUG && oldValue != null) {
                    LOGD("[[entryRemoved]] evicted = " + evicted + " key = " + key + " old bitmap = " + oldValue
                            + " old bitmap size = " + FileUtil.convertStorage(oldValue.btSize)
                            + " >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + " >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"
                            + " >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + " >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"
                            + " >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + " >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"
                            + " >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + " >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"
                            + " current size = " + curCacheSize(mLruCache) + " ===========");
                }

                if (oldValue != null && oldValue.bt != null) {
                    if (ENABLE_BITMAP_REUSE && mBitmapReusedObjectObj != null && (mBitmapReusedObjectObj.count < 11)
                            && oldValue.bt.getWidth() == mOption.thumbnailSize
                            && oldValue.bt.getHeight() == mOption.thumbnailSize) {
                        mBitmapReusedObjectObj.reusedList.add(oldValue);
                        mBitmapReusedObjectObj.count++;
                    }
                }
                oldValue = null;
            }
        };
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sound.dubbler.cache.AbsBitmapCacheManager#searchReusedBitmapObj(java
     * .lang.String)
     */
    @Override
    BitmapObject searchReusedBitmapObj(String category) {
        return loopupOneReusedBitmap(mBitmapReusedObjectObj, mOption.thumbnailSize, mOption.thumbnailSize);
    }

}
