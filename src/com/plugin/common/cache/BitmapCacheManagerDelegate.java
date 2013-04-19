/**
 * BitmapCacheManagerDelegate.java
 */
package com.plugin.common.cache;

import java.io.InputStream;

import android.graphics.Bitmap;

import com.plugin.common.cache.CacheFactory.Option;
import com.plugin.common.utils.UtilsConfig;
import com.plugin.common.utils.image.BitmapUtils;

/**
 * @author Guoqing Sun Jan 22, 20134:02:00 PM
 */

final class BitmapCacheManagerDelegate implements ICacheManager<Bitmap> {
    
    private static BitmapCacheManagerDelegate gBitmapCacheManagerDelegate;

    private static Object mIOLockObject = new Object();
    
    private Option mOption;

    private ICacheManager<Bitmap> mBigBitmapCacheManager = new BigBitmapCacheManager();

    private ICacheManager<Bitmap> mThumbnailBitmapCacheManager = new ThumbnailBitmapCacheManager();

    public static BitmapCacheManagerDelegate getInstance() {
        if (gBitmapCacheManagerDelegate == null) {
            synchronized (mIOLockObject) {
                if (gBitmapCacheManagerDelegate == null) {
                    gBitmapCacheManagerDelegate = new BitmapCacheManagerDelegate();
                }
            }
        }

        return gBitmapCacheManagerDelegate;
    }

    private BitmapCacheManagerDelegate() {
        mOption = new Option();
        mOption.needThumbnail = true;
    }
    
    public void init(Option opt) {
        mOption = opt;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.sound.dubbler.cache.ICacheManager#getResource(java.lang.String,
     * java.lang.String)
     */
    @Override
    public Bitmap getResource(String category, String key) {
        if (UtilsConfig.IMAGE_CACHE_CATEGORY_THUMB.equals(category)) {
            return mThumbnailBitmapCacheManager.getResource(category, key);
        } else {
            // 其他的目前使用BigCacheManager
            Bitmap ret = mBigBitmapCacheManager.getResource(category, key);
            if (mOption.needThumbnail && ret != null) {
                Bitmap thumbRet = mThumbnailBitmapCacheManager.getResource(UtilsConfig.IMAGE_CACHE_CATEGORY_THUMB, key);
                if (thumbRet == null) {
                    BitmapUtils.makeThumbnail(ret, UtilsConfig.IMAGE_CACHE_CATEGORY_THUMB, key);
                }
            }
            
            return ret;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sound.dubbler.cache.ICacheManager#getResourceFromMem(java.lang.String
     * , java.lang.String)
     */
    @Override
    public Bitmap getResourceFromMem(String category, String key) {
        if (mOption.needThumbnail && UtilsConfig.IMAGE_CACHE_CATEGORY_THUMB.equals(category)) {
            return mThumbnailBitmapCacheManager.getResourceFromMem(category, key);
        } else {
            return mBigBitmapCacheManager.getResourceFromMem(category, key);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sound.dubbler.cache.ICacheManager#getResourcePath(java.lang.String,
     * java.lang.String)
     */
    @Override
    public String getResourcePath(String category, String key) {
        if (mOption.needThumbnail && UtilsConfig.IMAGE_CACHE_CATEGORY_THUMB.equals(category)) {
            return mThumbnailBitmapCacheManager.getResourcePath(category, key);
        } else {
            return mBigBitmapCacheManager.getResourcePath(category, key);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sound.dubbler.cache.ICacheManager#putResource(java.lang.String,
     * java.lang.String, java.lang.Object)
     */
    @Override
    public String putResource(String category, String key, Bitmap res) {
        if (mOption.needThumbnail && UtilsConfig.IMAGE_CACHE_CATEGORY_THUMB.equals(category)) {
            return mThumbnailBitmapCacheManager.putResource(category, key, res);
        } else {
            return mBigBitmapCacheManager.putResource(category, key, res);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sound.dubbler.cache.ICacheManager#putResource(java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @Override
    public String putResource(String category, String key, CharSequence sourceFullFile) {
        if (mOption.needThumbnail && UtilsConfig.IMAGE_CACHE_CATEGORY_THUMB.equals(category)) {
            return mThumbnailBitmapCacheManager.putResource(category, key, sourceFullFile);
        } else {
            return mBigBitmapCacheManager.putResource(category, key, sourceFullFile);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sound.dubbler.cache.ICacheManager#putResource(java.lang.String,
     * java.lang.String, java.io.InputStream)
     */
    @Override
    public String putResource(String category, String key, InputStream is) {
        if (mOption.needThumbnail && UtilsConfig.IMAGE_CACHE_CATEGORY_THUMB.equals(category)) {
            return mThumbnailBitmapCacheManager.putResource(category, key, is);
        } else {
            return mBigBitmapCacheManager.putResource(category, key, is);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sound.dubbler.cache.ICacheManager#releaseResource(java.lang.String)
     */
    @Override
    public void releaseResource(String category) {
        if (mOption.needThumbnail && UtilsConfig.IMAGE_CACHE_CATEGORY_THUMB.equals(category)) {
            mThumbnailBitmapCacheManager.releaseResource(category);
        } else {
            mBigBitmapCacheManager.releaseResource(category);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sound.dubbler.cache.ICacheManager#releaseResource(java.lang.String,
     * java.lang.String)
     */
    @Override
    public void releaseResource(String category, String key) {
        if (mOption.needThumbnail && UtilsConfig.IMAGE_CACHE_CATEGORY_THUMB.equals(category)) {
            mThumbnailBitmapCacheManager.releaseResource(category, key);
        } else {
            mBigBitmapCacheManager.releaseResource(category, key);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sound.dubbler.cache.ICacheManager#releaseAllResource()
     */
    @Override
    public void releaseAllResource() {
        if (mOption.needThumbnail) {
            mThumbnailBitmapCacheManager.releaseAllResource();
        }
        mBigBitmapCacheManager.releaseAllResource();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sound.dubbler.cache.ICacheManager#clearResource()
     */
    @Override
    public void clearResource() {
        if (mOption.needThumbnail) {
            mThumbnailBitmapCacheManager.clearResource();
        }
        mBigBitmapCacheManager.clearResource();
    }

	/* (non-Javadoc)
	 * @see com.plugin.cache.ICacheManager#setCacheStrategy(com.plugin.cache.ICacheStrategy)
	 */
	@Override
	public ICacheStrategy setCacheStrategy(ICacheStrategy strategy) {
		if (strategy == null) {
			throw new IllegalArgumentException("strategy can not be NULL");
		}
		
		if (mOption.needThumbnail) {
		    mThumbnailBitmapCacheManager.setCacheStrategy(strategy);
		}
		return mBigBitmapCacheManager.setCacheStrategy(strategy);
	}

}
