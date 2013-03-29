package com.cartoononline;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;

import com.plugin.common.cache.CacheFactory;
import com.plugin.common.cache.ICacheManager;
import com.plugin.common.cache.ICacheStrategy;
import com.polites.android.GestureImageView;

public class AlbumActivity extends BaseActivity {
    
    public static final String KEY_INDEX = "index";
    public static final String KEY_SESSION_NAME = "sessionName";

    private LayoutInflater mLayoutInflater;
    private ViewPager mViewPager;
    private String mPath;
    private String mSessionName;
    
    private ICacheManager<Bitmap> mCacheManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.content_view);
        
        mLayoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPath = getIntent().getStringExtra(KEY_INDEX);
        mSessionName = getIntent().getStringExtra(KEY_SESSION_NAME);
        mCacheManager = CacheFactory.getCacheManager(CacheFactory.TYPE_CACHE.TYPE_IMAGE);
        mCacheManager.setCacheStrategy(new ICacheStrategy() {

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
    }
    
    private void initActionbar() {
        mActionBar = getSupportActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setDisplayShowTitleEnabled(true);
        mActionBar.setTitle(R.string.app_name);
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
                    if (!filename.equals("icon.jpg") && !filename.equals("info.ini")
                            && filename.endsWith(".jpg")) {
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
                ((ViewPager) arg0).removeView(mViewArray.get(arg1));
            }
        }

        @Override
        public void finishUpdate(View arg0) {
        }

        @Override
        public int getCount() {
            return mCount;
        }

        @Override
        public Object instantiateItem(View arg0, int pos) {
            if (mViewArray.size() > pos) {
                View v = mViewArray.get(pos);
                Holder holder = (Holder) v.getTag();
                holder.imageView.setImageBitmap(mCacheManager.getResource(mSessionName, String.valueOf(pos + 1)));
                ((ViewPager) arg0).addView(v);
            } else {
                View addView = mLayoutInflater.inflate(R.layout.content_item, null);
                Holder holder = new Holder();
                GestureImageView image = (GestureImageView) addView.findViewById(R.id.view_photo_gallery_item_image);
                holder.imageView = image;
                addView.setTag(holder);
                
                image.setIsCrop(true);
                image.setImageBitmap(mCacheManager.getResource(mSessionName, String.valueOf(pos + 1)));

                mViewArray.add(addView);
                ((ViewPager) arg0).addView(mViewArray.get(pos));
            }
            
            return mViewArray.get(pos);
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
            GestureImageView imageView;
        }
    }
    
}
