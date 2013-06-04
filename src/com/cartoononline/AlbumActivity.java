package com.cartoononline;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import net.youmi.android.banner.AdSize;
import net.youmi.android.banner.AdView;
import net.youmi.android.spot.SpotManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.album.disi.R;
import com.plugin.common.cache.CacheFactory;
import com.plugin.common.cache.ICacheManager;
import com.plugin.common.cache.ICacheStrategy;
import com.polites.android.RegionImageView;
import com.umeng.analytics.MobclickAgent;

public class AlbumActivity extends BaseActivity {

    public static final String KEY_INDEX = "index";
    public static final String KEY_SESSION_NAME = "sessionName";
    public static final String KEY_DESC = "desc";
 
    private LayoutInflater mLayoutInflater;
    private ViewPager mViewPager;
    private String mPath;
    private String mSessionName;
    private String mDescription;

    private ICacheManager<Bitmap> mCacheManager;

    private ICacheStrategy mOldICacheStrategy;
    
    private Handler mHandler = new Handler();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.content_view);

        mLayoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPath = getIntent().getStringExtra(KEY_INDEX);
        mSessionName = getIntent().getStringExtra(KEY_SESSION_NAME);
        mDescription = getIntent().getStringExtra(KEY_DESC);
        mCacheManager = CacheFactory.getCacheManager(CacheFactory.TYPE_CACHE.TYPE_IMAGE);
        mOldICacheStrategy = mCacheManager.setCacheStrategy(new ICacheStrategy() {

            @Override
            public String onMakeImageCacheFullPath(String rootPath, String key, String ext) {
                if (!rootPath.endsWith(File.separator)) {
                    rootPath = rootPath + File.separator;
                }

                return rootPath + key + ".jpg";
            }

            @Override
            public String onMakeFileKeyName(String category, String key) {
                StringBuilder sb = new StringBuilder(256);
                sb.append(category).append("/").append(key);
                return sb.toString();
            }

        });

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(new MyPagerAdapter());

        initActionbar();
        
        //umeng log
        HashMap<String, String> extra = new HashMap<String, String>();
        extra.put("name", mDescription);
        MobclickAgent.onEvent(this.getApplicationContext(), Config.OPEN_ALUBM, extra);
        
        if (SettingManager.getInstance().getShowAdView()) {
            initAdView();
        }
    }

    private void initAdView() {
        AdView adView = new AdView(this, AdSize.SIZE_320x50);
        LinearLayout adLayout = (LinearLayout) findViewById(R.id.ad_region);
        adLayout.setVisibility(View.VISIBLE);
        adLayout.addView(adView);
    }
    
    @Override
    public void onStart() {
        super.onStart();
        if (Config.ADVIEW_SHOW) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    SpotManager.getInstance(getApplicationContext()).showSpotAds(AlbumActivity.this);
                }
            }, 100);
            
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, com.actionbarsherlock.view.MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            break;
        default:
            break;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mViewPager.setAdapter(null);
        mViewPager = null;

        mCacheManager.setCacheStrategy(mOldICacheStrategy);
        mCacheManager.releaseAllResource();

        System.gc();
    }

    private void initActionbar() {
        mActionBar = getSupportActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setDisplayShowTitleEnabled(true);
        if (TextUtils.isEmpty(mDescription)) {
            mActionBar.setTitle(R.string.app_name);
        } else {
            mActionBar.setTitle(mDescription);
        }
        mActionBar.setIcon(R.drawable.icon);
    }

    class MyPagerAdapter extends PagerAdapter {

        private ArrayList<View> mViewArray;
        private int mCount;
        private String[] mFiles;
        private ArrayList<String> mFileList;

        MyPagerAdapter() {
            mViewArray = new ArrayList<View>();
            File f = new File(mPath);
            mFiles = f.list(new FilenameFilter() {

                @Override
                public boolean accept(File dir, String filename) {
                    if (!filename.equals("icon.jpg") && !filename.equals("info.ini") && filename.endsWith(".jpg")) {
                        return true;
                    }
                    return false;
                }

            });

            if (mFiles != null) {
                mCount = mFiles.length;
                mCount = mCount < 0 ? 0 : mCount;

                mFileList = new ArrayList<String>();
                for (String file : mFiles) {
                    mFileList.add(file);
                }
                Collections.sort(mFileList);
            }

        }

        @Override
        public void destroyItem(View arg0, int arg1, Object arg2) {
            if (mViewArray.size() > arg1) {
                View v = mViewArray.get(arg1);
                ((ViewPager) arg0).removeView(v);
                Holder holderC = (Holder) v.getTag();
                holderC.imageView.setImageBitmap(null);
                holderC.imageView.setImageFullPath(null);
            }
        }

        @Override
        public void finishUpdate(View arg0) {
        }

        @Override
        public int getCount() {
            return mCount;
        }

        private boolean startTop(Bitmap bt) {
            if (bt != null) {
                if (bt.getHeight() > (bt.getWidth() * 2)) {
                    return true;
                }
            }

            return false;
        }

        private boolean isCrop(Bitmap bt) {
            if (bt != null) {
                if (bt.getHeight() > (bt.getWidth() * 3)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public Object instantiateItem(View arg0, int pos) {
            View retView = null;
            if (mViewArray.size() > pos) {
                retView = mViewArray.get(pos);
                Holder holder = (Holder) retView.getTag();

                Bitmap show = mCacheManager.getResource(mSessionName, String.valueOf(pos + 1));
                holder.imageView.setIsCrop(isCrop(show));
                holder.imageView.setStartBeginTop(startTop(show));
                holder.imageView.setImageBitmap(show);
                holder.imageView.setImageFullPath(mCacheManager.getResourcePath(mSessionName, String.valueOf(pos + 1)));
                holder.pageCount.setText(String.format(getString(R.string.page_count), pos + 1, mCount));

                ((ViewPager) arg0).addView(retView);
            } else {
                retView = mLayoutInflater.inflate(R.layout.content_item, null);
                Holder holder = new Holder();
                RegionImageView image = (RegionImageView) retView.findViewById(R.id.view_photo_gallery_item_image);
                holder.imageView = image;
                holder.pageCount = (TextView) retView.findViewById(R.id.page_count);
                retView.setTag(holder);

                Bitmap show = mCacheManager.getResource(mSessionName, String.valueOf(pos + 1));
                image.setIsCrop(isCrop(show));
                holder.imageView.setStartBeginTop(startTop(show));
                image.setImageBitmap(show);
                image.setImageFullPath(mCacheManager.getResourcePath(mSessionName, String.valueOf(pos + 1)));
                
                holder.pageCount.setText(String.format(getString(R.string.page_count), pos + 1, mCount));
                mViewArray.add(retView);
                ((ViewPager) arg0).addView(retView);
            }

            return retView;
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }

        @Override
        public void restoreState(Parcelable arg0, ClassLoader arg1) {
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void startUpdate(View arg0) {
        }

        private class Holder {
            RegionImageView imageView;
            TextView pageCount;
        }
    }

}
