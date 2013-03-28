/**
 * ThumbnailBitmapCacheManager.java
 */
package com.plugin.common.cache;

import android.support.v4.util.LruCache;

import com.plugin.common.utils.files.FileUtil;
import com.plugin.common.utils.image.BitmapUtils;

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
		return new LruCache<String, BitmapObject>(1024 * 1024) {
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
							&& oldValue.bt.getWidth() == BitmapUtils.THUMBNAIL_BITMAP_SIZE
							&& oldValue.bt.getHeight() == BitmapUtils.THUMBNAIL_BITMAP_SIZE) {
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
		return loopupOneReusedBitmap(mBitmapReusedObjectObj, BitmapUtils.THUMBNAIL_BITMAP_SIZE,
				BitmapUtils.THUMBNAIL_BITMAP_SIZE);
	}

}
