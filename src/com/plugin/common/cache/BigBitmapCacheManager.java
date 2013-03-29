/**
 * BigBitmapCacheManager.java
 */
package com.plugin.common.cache;

import android.support.v4.util.LruCache;
import android.text.TextUtils;

import com.plugin.common.utils.UtilsConfig;
import com.plugin.common.utils.files.FileOperatorHelper;
import com.plugin.common.utils.files.FileUtil;
import com.plugin.common.utils.image.BitmapUtils;

/**
 * @author Guoqing Sun Jan 22, 20133:30:27 PM
 */
final class BigBitmapCacheManager extends AbsBitmapCacheManager {

	@Override
	LruCache<String, BitmapObject> makeLruCacheObj() {
		return new LruCache<String, BitmapObject>(UtilsConfig.DEVICE_INFO.image_cache_size) {
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
					if (UtilsConfig.IMAGE_CACHE_CATEGORY_USER_HEAD_ROUNDED.equals(oldValue.category)
							&& BitmapUtils.USER_HEAD_STANDARD_SIZE == oldValue.bt.getWidth()
							&& BitmapUtils.USER_HEAD_STANDARD_SIZE == oldValue.bt.getHeight()) {
						// save the head bt obj for reuse
						if (ENABLE_BITMAP_REUSE) {
							if (mBitmapReusedObjectObj != null) {
								if (mBitmapReusedObjectObj.count < 10) {
									mBitmapReusedObjectObj.reusedList.add(oldValue);
									mBitmapReusedObjectObj.count++;
								}
							}
						}
					} else {
						oldValue.bt = null;
						oldValue = null;
					}
				}
				oldValue = null;
			}
		};
	}

	@Override
	BitmapObject searchReusedBitmapObj(String category) {
		if (UtilsConfig.IMAGE_CACHE_CATEGORY_USER_HEAD_ROUNDED.equals(category)) {
			return loopupOneReusedBitmap(mBitmapReusedObjectObj, BitmapUtils.USER_HEAD_STANDARD_SIZE,
					BitmapUtils.USER_HEAD_STANDARD_SIZE);
		}
		return null;
	}

	@Override
	public String putResource(String category, String key, CharSequence sourceFullFile) {
		if (UtilsConfig.IMAGE_CACHE_CATEGORY_USER_HEAD_ROUNDED.equals(category)) {
			try {
				synchronized (mIOLockObject) {
					String path = BitmapDiskTools.getBitmapSavePath(makeFileKeyName(category, key));
					if (!TextUtils.isEmpty(path) && FileOperatorHelper.moveFile(sourceFullFile.toString(), path)) {
						if (DEBUG) {
							LOGD("[[flushResourceByFile]] success move file from : " + sourceFullFile + " ||||| to : "
									+ path);
						}
					}

					return path;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}

		return super.putResource(category, key, sourceFullFile);
	}

}
