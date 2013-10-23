/**
 * WebImageView.java
 */
package com.plugin.common.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.plugin.common.cache.CacheFactory;
import com.plugin.common.cache.ICacheManager;
import com.plugin.common.mucslib.R;
import com.plugin.common.utils.CustomThreadPool;
import com.plugin.common.utils.UtilsConfig;
import com.plugin.common.utils.files.FileDownloader;
import com.plugin.common.utils.files.FileDownloader.DownloadRequest;
import com.plugin.common.utils.image.ImageDownloader;
import com.plugin.common.utils.image.ImageDownloader.ImageFetchRequest;
import com.plugin.common.utils.image.ImageDownloader.ImageFetchResponse;

/**
 * @author Guoqing Sun Dec 27, 201211:32:15 AM
 */
public class WebImageView extends ImageView {

    private ICacheManager<Bitmap> mImageCache;

    private ImageDownloader mImageDownloaer;

    protected String mCategory;

    private Animation mAnimation;

    private boolean mHasAnimation;

    private String mUrl;

    private Drawable mDefaultSrc;

    private DownloadRequest mCurrentDownloadRequest;

    private static final int LOCAL_LOAD_IMAGE_SUCCESS = 10000;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.obj == null) {
                return;
            }

            switch (msg.what) {
            case FileDownloader.NOTIFY_DOWNLOAD_SUCCESS:
                ImageFetchResponse response = (ImageFetchResponse) msg.obj;
                if (response.getDownloadUrl().equals(mUrl) && response.getmBt() != null) {
                    setImageBitmap(response.getmBt(), true);
                    // after set the image view from web, just unRegiste the
                    // handler
                    unRegistehandler();
                }
                break;
            case FileDownloader.NOTIFY_DOWNLOAD_FAILED:
                break;
            case LOCAL_LOAD_IMAGE_SUCCESS:
                setImageBitmap((Bitmap) msg.obj, true);
                break;
            }
        }
    };

    public WebImageView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public WebImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WebImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WebImageView, defStyle, 0);
        int n = a.getIndexCount();
        for (int i = 0; i < n; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
            case R.styleable.WebImageView_network_animation:
                int res = a.getResourceId(attr, -1);
                if (res > 0) {
                    mAnimation = AnimationUtils.loadAnimation(context.getApplicationContext(), res);
                    if (mAnimation != null) {
                        mHasAnimation = true;
                    }
                }
                break;
            }
        }

        // if (mAnimation == null) {
        // throw new IllegalArgumentException("无效动画效果");
        // }

        mImageCache = (ICacheManager<Bitmap>) CacheFactory.getCacheManager(CacheFactory.TYPE_CACHE.TYPE_IMAGE);
        mImageDownloaer = ImageDownloader.getInstance(context.getApplicationContext());

        mCategory = UtilsConfig.IMAGE_CACHE_CATEGORY_RAW;

        mDefaultSrc = this.getDrawable();
    }

    @Override
    public void setImageResource(int resId) {
        unRegistehandler();
        if (mCurrentDownloadRequest != null) {
            mCurrentDownloadRequest.cancelDownload();
        }
        if (mHasAnimation) {
            this.clearAnimation();
        }
        super.setImageResource(resId);
    }

    @Override
    public void setImageURI(Uri uri) {
        setImageURI(uri, true);
    }
    
    public void setImageURI(Uri uri, boolean forceOriginLoad) {
        unRegistehandler();

        if (uri != null) {
            String path = uri.getPath();
            if (!TextUtils.isEmpty(path) && path.toLowerCase().startsWith("http")) {
                this.setImageUrl(uri.getPath(), forceOriginLoad);
            }
        } else {
            if (mCurrentDownloadRequest != null) {
                mCurrentDownloadRequest.cancelDownload();
            }
            if (mHasAnimation) {
                this.clearAnimation();
            }
            super.setImageURI(uri);
        }
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        unRegistehandler();
        if (mCurrentDownloadRequest != null) {
            mCurrentDownloadRequest.cancelDownload();
        }
        if (mHasAnimation) {
            this.clearAnimation();
        }
        super.setImageDrawable(drawable);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        unRegistehandler();
        if (mCurrentDownloadRequest != null) {
            mCurrentDownloadRequest.cancelDownload();
        }
        setImageBitmap(bm, false);
    }

    private void setImageBitmap(Bitmap bm, boolean withAnim) {
        BitmapDrawable btDraw = (BitmapDrawable) this.getDrawable();
        super.setImageBitmap(bm);
        if (mHasAnimation) {
            this.clearAnimation();
            if ((Build.VERSION.SDK_INT >= 14) && withAnim
                    && ((bm != null && btDraw == null) || (bm != null && btDraw != null && btDraw.getBitmap() != bm))) {
                this.startAnimation(mAnimation);
            }
        }
    }

    /**
     * 
     * @param url
     * @param syncLoad
     *            标示支持同步加载，如果不支持同步加载的，就是fastload模式
     */
    private void setImageUrl(final String url, final boolean forceOriginLoad) {
        if (!TextUtils.isEmpty(url)) {
            mUrl = url;
            Bitmap bt = mImageCache.getResourceFromMem(mCategory, url);
            if (bt == null) {
                CustomThreadPool.asyncWork(new Runnable() {
                    @Override
                    public void run() {
                        Bitmap bt = null;
                        if (!forceOriginLoad) {
                            bt = mImageCache.getResource(UtilsConfig.IMAGE_CACHE_CATEGORY_THUMB, url);
                        }
                        if (bt == null) {
                            bt = mImageCache.getResource(mCategory, url);
                        }
                        if (bt == null) {
                            registeHandler();
                            if (mCurrentDownloadRequest != null) {
                                mCurrentDownloadRequest.cancelDownload();
                            }
                            mCurrentDownloadRequest = new ImageFetchRequest(DownloadRequest.DOWNLOAD_TYPE.IMAGE, url,
                                    UtilsConfig.IMAGE_CACHE_CATEGORY_RAW);
                            mImageDownloaer.postRequest(mCurrentDownloadRequest);
                        } else {
                            Message msg = Message.obtain();
                            msg.what = LOCAL_LOAD_IMAGE_SUCCESS;
                            msg.obj = bt;
                            mHandler.sendMessageDelayed(msg, 30);
                        }
                    }
                });

                if (bt != null) {
                    setImageBitmap(bt, false);
                } else {
                    if (mHasAnimation) {
                        this.clearAnimation();
                    }
                    this.setImageDrawable(mDefaultSrc);
                }
            } else {
                setImageBitmap(bt, false);
            }
        }
    }

    /**
     * 不支持从磁盘读thumb
     * 
     * @param url
     * @param syncLoad
     */
    // public void setImageUrlNoThumb(String url, boolean syncLoad) {
    // if (!TextUtils.isEmpty(url)) {
    // mUrl = url;
    // Bitmap bt = mImageCache.getResourceFromMem(mCategory, url);
    // if (bt == null) {
    // bt =
    // mImageCache.getResourceFromMem(UtilsConfig.IMAGE_CACHE_CATEGORY_THUMB,
    // url);
    // if (bt == null) {
    // if (syncLoad) {
    // bt = mImageCache.getResource(mCategory, url);
    // }
    // if (bt != null) {
    // setImageBitmap(bt, false);
    // } else {
    // if (mHasAnimation) {
    // this.clearAnimation();
    // }
    // this.setImageDrawable(mDefaultSrc);
    // }
    // } else {
    // setImageBitmap(bt, false);
    // }
    // } else {
    // setImageBitmap(bt, false);
    // }
    //
    // if (syncLoad && bt == null) {
    // registeHandler();
    // mImageDownloaer.postRequest(new
    // ImageFetchRequest(DownloadRequest.DOWNLOAD_TYPE.IMAGE, url,
    // UtilsConfig.IMAGE_CACHE_CATEGORY_USER_HEAD_ROUNDED, new
    // CycleBitmapOpration()));
    // }
    // }
    // }

    private void registeHandler() {
        if (mImageDownloaer != null) {
            mImageDownloaer.registeSuccessHandler(mHandler);
            mImageDownloaer.registeFailedHandler(mHandler);
        }
    }

    private void unRegistehandler() {
        if (mImageDownloaer != null) {
            mImageDownloaer.unRegisteSuccessHandler(mHandler);
            mImageDownloaer.unRegisteFailedHandler(mHandler);
        }
    }
}