package com.cartoononline;

import java.util.HashMap;

import net.youmi.android.spot.SpotManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.TextUtils;

import com.cartoononline.fragment.DownloadFragment;
import com.cartoononline.fragment.FragmentStatusInterface;
import com.cartoononline.fragment.MoreBookFragment;
import com.cartoononline.fragment.ReaderBookFragment;
import com.cartoononline.model.DownloadModel;
import com.plugin.common.cache.CacheFactory;
import com.plugin.common.cache.ICacheManager;
import com.plugin.common.cache.ICacheStrategy;
import com.plugin.common.utils.CustomThreadPool;
import com.plugin.common.utils.CustomThreadPool.TaskWrapper;
import com.plugin.common.utils.SingleInstanceBase;
import com.plugin.common.utils.UtilsConfig;
import com.umeng.analytics.MobclickAgent;

public class CartoonSplashActivity extends BaseActivity {

    private static final boolean DEBUG = Config.DEBUG;

    public static final String KEY_FORECE_DOWNLOAD_SHOW = "force_download_show";

    private SectionsPagerAdapter mSectionsPagerAdapter;

    private ViewPager mViewPager;

    private int mCurPageIndex;

    private ICacheStrategy mDefICacheStrategy;

    private ICacheManager mCacheManager;

    private DownloadModel mDownloadModel;

    private HashMap<Integer, Fragment> mItemsMap = new HashMap<Integer, Fragment>();

    private boolean mShowAppWall = false;
    
    private boolean mForceShowDownload = false;
    
    private static final int FROCE_DOWNLOAD_SHOW = 1;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case FROCE_DOWNLOAD_SHOW:
                if (mViewPager != null) {
                    int curIndex = mViewPager.getCurrentItem();
                    if (curIndex != 1) {
                        mViewPager.setCurrentItem(1);
                    } else {
                        mForceShowDownload = false;
                        if (mItemsMap.containsKey(1)) {
                            Fragment f = mItemsMap.get(1);
                            if (f != null && f instanceof FragmentStatusInterface) {
                                mDownloadModel.resetPageNo();
                                ((FragmentStatusInterface) f).onForceRefresh();
                            }
                        }
                    }
                }
                break;
            }
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobclickAgent.updateOnlineConfig(this.getApplicationContext());

        mCacheManager = CacheFactory.getCacheManager(CacheFactory.TYPE_CACHE.TYPE_IMAGE);
        mDefICacheStrategy = mCacheManager.setCacheStrategy(new ICacheStrategy() {

            @Override
            public String onMakeFileKeyName(String category, String key) {
                return null;
            }

            @Override
            public String onMakeImageCacheFullPath(String rootPath, String key, String ext) {
                return null;
            }

        });
        mCacheManager.setCacheStrategy(mDefICacheStrategy);

        mDownloadModel = SingleInstanceBase.getInstance(DownloadModel.class);

        initActionbar();
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(new OnPageChangeListener() {

            @Override
            public void onPageScrollStateChanged(int arg0) {
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }

            @Override
            public void onPageSelected(int arg0) {
                LOGD("<<<<< on select page : " + arg0 + " >>>>>");
                mCurPageIndex = arg0;
                switch (mCurPageIndex) {
                case 0:
                    if (mItemsMap.containsKey(0)) {
                        Fragment f = mItemsMap.get(0);
                        if (f != null && f instanceof FragmentStatusInterface) {
                            ((FragmentStatusInterface) f).onShow();
                        }
                    }
                    break;
                case 1:
                    if (mItemsMap.containsKey(1)) {
                        Fragment f = mItemsMap.get(1);
                        if (f != null && f instanceof FragmentStatusInterface) {
                            if (!mForceShowDownload) {
                                ((FragmentStatusInterface) f).onShow();
                            } else {
                                mDownloadModel.resetPageNo();
                                ((FragmentStatusInterface) f).onForceRefresh();
                            }
                        }
                    }
                    mForceShowDownload = false;
                    break;
                }
            }

        });
        
        if (getIntent() != null) {
            mForceShowDownload = getIntent().getBooleanExtra(KEY_FORECE_DOWNLOAD_SHOW, false);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        SpotManager.getInstance(this).loadSpotAds();
        
        CustomThreadPool.getInstance().excute(new TaskWrapper(new Runnable() {
            @Override
            public void run() {
                String ret = MobclickAgent.getConfigParams(CartoonSplashActivity.this.getApplicationContext(),
                        Config.KEY_SHOW_WALL);
                if (!TextUtils.isEmpty(ret) && ret.equals("true")) {
                    mShowAppWall = true;
                }
                
                String metaChannel = Utils.getString(CartoonSplashActivity.this.getApplicationContext(), "UMENG_CHANNEL");
                String adViewShow = MobclickAgent.getConfigParams(CartoonSplashActivity.this.getApplicationContext(),
                        metaChannel + Config.KEY_ADVIEW);
                
                LOGD(">>>>>>>> adViewShow = " + adViewShow);
                if (!TextUtils.isEmpty(adViewShow) && adViewShow.equals("true")) {
                    Config.ADVIEW_SHOW = true;
                }
            }
        }));
        
        if (mForceShowDownload) {
            mHandler.sendEmptyMessageDelayed(FROCE_DOWNLOAD_SHOW, 400);   
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        
        if (intent != null) {
            mForceShowDownload = intent.getBooleanExtra(KEY_FORECE_DOWNLOAD_SHOW, false);
        }
        
        if (mForceShowDownload) {
            mHandler.sendEmptyMessageDelayed(FROCE_DOWNLOAD_SHOW, 400);   
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();

        CRuntime.CUR_FORMAT_TIME = CRuntime.composeTime();
        
        mCacheManager.setCacheStrategy(mDefICacheStrategy);

        if (mCurPageIndex == 0) {
            if (mItemsMap.containsKey(0)) {
                Fragment f = mItemsMap.get(0);
                if (f != null && f instanceof FragmentStatusInterface) {
                    ((FragmentStatusInterface) f).onForceRefresh();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        OffersManager.getInstance(this).onAppExit();
        mCacheManager.releaseAllResource();
    }

    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        if (mShowAppWall) {
            getSupportMenuInflater().inflate(R.menu.detail_actionbar, menu);
        } else {
            getSupportMenuInflater().inflate(R.menu.action_refresh, menu);
        }
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, com.actionbarsherlock.view.MenuItem item) {
        switch (item.getItemId()) {
        case R.id.more_apps:
            if (mShowAppWall) {
//                OffersManager.getInstance(getApplicationContext()).showOffersWall();
            }
            break;
        case R.id.action_load:
            switch (mCurPageIndex) {
            case 0:
                if (mItemsMap.size() > 0) {
                    Fragment f = mItemsMap.get(0);
                    if (f != null && f instanceof FragmentStatusInterface) {
                        ((FragmentStatusInterface) f).onShow();
                    }
                }
                break;
            case 1:
                mDownloadModel.resetPageNo();
                if (mItemsMap.containsKey(1)) {
                    Fragment f = mItemsMap.get(1);
                    if (f != null && f instanceof FragmentStatusInterface) {
                        ((FragmentStatusInterface) f).onForceRefresh();
                    }
                }
                break;
            }
            break;
        }

        return true;
    }

    private void initActionbar() {
        mActionBar = getSupportActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(false);
        mActionBar.setDisplayShowTitleEnabled(true);
        mActionBar.setTitle(R.string.app_name);
        mActionBar.setIcon(R.drawable.icon);
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (mItemsMap.size() <= position) {
                switch (position) {
                case 0:
                    mItemsMap.put(position, new ReaderBookFragment());
                    break;
                case 1:
                    mItemsMap.put(position, new DownloadFragment());
                    break;
                case 2:
                    mItemsMap.put(position, new MoreBookFragment());
                    break;
                }
            }

            return mItemsMap.get(position);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
            case 0:
                return getString(R.string.local);
            case 1:
                return getString(R.string.server);
            case 2:
                return getString(R.string.more);
            }
            return null;
        }
    }

    private static void LOGD(String msg) {
        if (Config.DEBUG) {
            UtilsConfig.LOGD(msg);
        }
    }
}
