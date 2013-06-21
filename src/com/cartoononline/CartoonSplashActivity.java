package com.cartoononline;

import java.util.HashMap;

import net.youmi.android.banner.AdSize;
import net.youmi.android.banner.AdView;
import net.youmi.android.offers.OffersManager;
import net.youmi.android.offers.PointsManager;
import net.youmi.android.spot.SpotManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.read.book.R;
import com.cartoononline.fragment.DownloadFragment;
import com.cartoononline.fragment.FragmentStatusInterface;
import com.cartoononline.fragment.HotFragment;
import com.cartoononline.fragment.ReaderBookFragment;
import com.cartoononline.model.DownloadModel;
import com.cartoononline.model.HotModel;
import com.plugin.common.cache.CacheFactory;
import com.plugin.common.cache.ICacheManager;
import com.plugin.common.cache.ICacheStrategy;
import com.plugin.common.utils.Environment;
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
    
    private HotModel mHotModel;

    private HashMap<Integer, Fragment> mItemsMap = new HashMap<Integer, Fragment>();

    private boolean mShowAppWallInfo = true;

    private boolean mForceShowDownload = false;

    private static final int FROCE_DOWNLOAD_SHOW = 1;
    private static final int FORCE_REFRESH_ADVIEW = 2;
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
            case FORCE_REFRESH_ADVIEW:
                if (!SettingManager.getInstance().getShowAdView()) {
                    LinearLayout adLayout = (LinearLayout) findViewById(R.id.ad_region);
                    if (adLayout != null) {
                        adLayout.removeAllViews();
                        adLayout.setVisibility(View.GONE);
                    }
                } else {
                    initAdView();
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
        mHotModel = SingleInstanceBase.getInstance(HotModel.class);
        
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
                    releaseRes(1);
                    releaseRes(2);
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
                    releaseRes(0);
                    releaseRes(2);
                    break;
                case 2:
                    if (mItemsMap.containsKey(2)) {
                        Fragment f = mItemsMap.get(2);
                        if (f != null && f instanceof FragmentStatusInterface) {
                            if (!mForceShowDownload) {
                                ((FragmentStatusInterface) f).onShow();
                            } else {
                                mHotModel.resetPageNo();
                                ((FragmentStatusInterface) f).onForceRefresh();
                            }
                        }
                    }
                    mForceShowDownload = false;
                    releaseRes(0);
                    releaseRes(1);
                    break;
                }
            }

        });

        if (getIntent() != null) {
            mForceShowDownload = getIntent().getBooleanExtra(KEY_FORECE_DOWNLOAD_SHOW, false);
        }

        initAdView();
        if (!Config.APP_STARTED) {
            showCloseTipsDialog();
            Config.APP_STARTED = true;
        }
    }
    
    private void releaseRes(int index) {
        if (mItemsMap.containsKey(index)) {
            Fragment f = mItemsMap.get(index);
            if (f != null && f instanceof FragmentStatusInterface) {
                ((FragmentStatusInterface) f).onStopShow();
            }
        }
    }

    private void showCloseTipsDialog() {
        if (SettingManager.getInstance().getShowAdView()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.tips_title);
            builder.setMessage(R.string.tips_adview_close);
            builder.setPositiveButton(R.string.confirm, null);
            builder.setNegativeButton(R.string.close_now, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    showAdViewSettingDialog();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private void initAdView() {
        AdView adView = new AdView(this, AdSize.SIZE_320x50);
        LinearLayout adLayout = (LinearLayout) findViewById(R.id.ad_region);
        adLayout.removeAllViews();
        adLayout.setVisibility(View.VISIBLE);
        adLayout.addView(adView);
    }

    @Override
    protected void onStart() {
        super.onStart();
        SpotManager.getInstance(this).loadSpotAds();

//        CustomThreadPool.getInstance().excute(new TaskWrapper(new Runnable() {
//            @Override
//            public void run() {
//                String ret = MobclickAgent.getConfigParams(CartoonSplashActivity.this.getApplicationContext(),
//                        Config.KEY_SHOW_WALL);
//                if (!TextUtils.isEmpty(ret) && ret.equals("true")) {
//                    mShowAppWallInfo = true;
//                }
//
//                String metaChannel = getString(R.string.umeng_params_channel);
//                String adViewShow = MobclickAgent.getConfigParams(CartoonSplashActivity.this.getApplicationContext(),
//                        metaChannel + Config.KEY_ADVIEW);
//
//                LOGD(">>>>>>>> adViewShow = " + adViewShow);
//                if (!TextUtils.isEmpty(adViewShow) && adViewShow.equals("true")) {
//                    Config.ADVIEW_SHOW = true;
//                } else {
//                    Config.ADVIEW_SHOW = false;
//                }
//            }
//        }));

        if (mForceShowDownload) {
            MobclickAgent.onEvent(this.getApplicationContext(), Config.OPEN_WITH_PUSH);
            MobclickAgent.flush(this.getApplicationContext());
            mHandler.removeMessages(FROCE_DOWNLOAD_SHOW);
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
            MobclickAgent.onEvent(this.getApplicationContext(), Config.OPEN_WITH_PUSH);
            MobclickAgent.flush(this.getApplicationContext());
            mHandler.removeMessages(FROCE_DOWNLOAD_SHOW);
            mHandler.sendEmptyMessageDelayed(FROCE_DOWNLOAD_SHOW, 400);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        CRuntime.CUR_FORMAT_TIME = CRuntime.composeTime();

        mCacheManager.setCacheStrategy(mDefICacheStrategy);

        if (mItemsMap.containsKey(mCurPageIndex)) {
            Fragment f = mItemsMap.get(mCurPageIndex);
            if (f != null && f instanceof FragmentStatusInterface) {
                ((FragmentStatusInterface) f).onShow();
            }
        }
        
        if (!SettingManager.getInstance().getShowAdView()) {
            LinearLayout adLayout = (LinearLayout) findViewById(R.id.ad_region);
            if (adLayout != null) {
                adLayout.removeAllViews();
                adLayout.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        
        releaseRes(0);
        releaseRes(1);
        releaseRes(2);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        OffersManager.getInstance(this).onAppExit();
        mCacheManager.releaseAllResource();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            overridePendingTransition(R.anim.activity_slide_in_from_left, R.anim.activity_slide_out_to_right);
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        if (mShowAppWallInfo) {
            getSupportMenuInflater().inflate(R.menu.detail_actionbar, menu);
        } else {
            getSupportMenuInflater().inflate(R.menu.action_refresh, menu);
        }
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, com.actionbarsherlock.view.MenuItem item) {
        switch (item.getItemId()) {
        case R.id.rate:
            MobclickAgent.onEvent(getApplicationContext(), Config.RATE_APP);
            MobclickAgent.flush(getApplicationContext());
            RateDubblerHelper.getInstance(getApplicationContext()).OpenApp(Config.CURRENT_PACKAGE_NAME);
            break;
        case R.id.about:
            showAboutDialog();
            break;
        case R.id.close_adview:
            showAdViewSettingDialog();
            break;
        case R.id.wall_info:
            if (mShowAppWallInfo) {
                showWallInfoDialog();
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
            case 2:
                mHotModel.resetPageNo();
                if (mItemsMap.containsKey(2)) {
                    Fragment f = mItemsMap.get(2);
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

    private void showAboutDialog() {
        String version = Environment.getVersionName(getApplicationContext());
        String versionStr = String.format(getString(R.string.version_info), version);
        View v = this.getLayoutInflater().inflate(R.layout.about_view, null);
        TextView versionTV = (TextView) v.findViewById(R.id.version);
        versionTV.setText(versionStr);
        
        AlertDialog dialog = new AlertDialog.Builder(this)
                                    .setTitle(R.string.about)
                                    .setView(v)
                                    .setPositiveButton(R.string.confirm, null)
                                    .setNegativeButton(R.string.btn_rate, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            MobclickAgent.onEvent(getApplicationContext(), Config.RATE_APP);
                                            MobclickAgent.flush(getApplicationContext());
                                            RateDubblerHelper.getInstance(getApplicationContext()).OpenApp(Config.CURRENT_PACKAGE_NAME);
                                        }
                                    })
                                    .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }
    
    private void showAdViewSettingDialog() {
        final int localPoint = SettingManager.getInstance().getPointInt();
        int point = PointsManager.getInstance(this).queryPoints();
        final int totalPoint = localPoint + point;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.adview_setting);

        if (!SettingManager.getInstance().getShowAdView()) {
            builder.setMessage(R.string.adview_open);
            builder.setPositiveButton(R.string.open, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    SettingManager.getInstance().setShowAdView(true);

                    MobclickAgent.onEvent(CartoonSplashActivity.this.getApplicationContext(), "open_adview");
                    MobclickAgent.flush(CartoonSplashActivity.this.getApplicationContext());

                    mHandler.sendEmptyMessage(FORCE_REFRESH_ADVIEW);
                }
            });
        } else {
            if (totalPoint > Config.CLOSE_ADVIEW_POINT) {
                builder.setMessage(String.format(getString(R.string.adview_close_tips), totalPoint));
                builder.setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int leftPoint = Config.CLOSE_ADVIEW_POINT;
                        if (localPoint > 0) {
                            leftPoint = leftPoint - localPoint;
                        }
                        if (leftPoint < 0) {
                            leftPoint = -leftPoint;
                            SettingManager.getInstance().setPointInt(leftPoint);
                        } else {
                            SettingManager.getInstance().setPointInt(0);
                        }

                        PointsManager.getInstance(CartoonSplashActivity.this).spendPoints(leftPoint);

                        SettingManager.getInstance().setShowAdView(false);

                        MobclickAgent.onEvent(CartoonSplashActivity.this.getApplicationContext(), "close_adview");
                        MobclickAgent.flush(CartoonSplashActivity.this.getApplicationContext());

                        mHandler.sendEmptyMessage(FORCE_REFRESH_ADVIEW);
                    }
                });
            } else {
                if (SettingManager.getInstance().getShowAdView()) {
                    View view = this.getLayoutInflater().inflate(R.layout.offer_tips_view, null);
                    TextView tv = (TextView) view.findViewById(R.id.tips);
                    tv.setText(String.format(getString(R.string.adview_close_nopoint_tips), totalPoint));
                    builder.setView(view);

                    builder.setPositiveButton(R.string.download, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            OffersManager.getInstance(CartoonSplashActivity.this).showOffersWall();

                            MobclickAgent.onEvent(CartoonSplashActivity.this.getApplicationContext(),
                                    "download_app_open");
                            MobclickAgent.flush(CartoonSplashActivity.this.getApplicationContext());
                        }
                    });
                }
            }

        }
        builder.setNegativeButton(R.string.cancel, null);

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void showWallInfoDialog() {
        int localPoint = SettingManager.getInstance().getPointInt();

        int point = PointsManager.getInstance(this).queryPoints();
        String tips = String.format(getString(R.string.offer_info_detail), point + localPoint);
        View view = this.getLayoutInflater().inflate(R.layout.offer_tips_view, null);
        TextView tv = (TextView) view.findViewById(R.id.tips);
        tv.setText(tips);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(R.string.tips_title).setView(view)
                .setPositiveButton(R.string.download, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        OffersManager.getInstance(CartoonSplashActivity.this).showOffersWall();

                        MobclickAgent.onEvent(CartoonSplashActivity.this.getApplicationContext(), "download_app_open");
                        MobclickAgent.flush(CartoonSplashActivity.this.getApplicationContext());
                    }
                }).setNegativeButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void initActionbar() {
        mActionBar = getSupportActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(false);
        mActionBar.setDisplayShowTitleEnabled(true);
        mActionBar.setTitle(R.string.app_name);
        mActionBar.setIcon(R.drawable.icon);
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        private String[] mTitleArray;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            mTitleArray = CartoonSplashActivity.this.getResources().getStringArray(R.array.title_array);
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
                    mItemsMap.put(position, new HotFragment());
                    break;
                }
            }

            return mItemsMap.get(position);
        }

        @Override
        public int getCount() {
            if (Config.OPEN_HOT) {
                return 3;
            } else {
                return 2;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
            case 0:
                return getString(R.string.local);
            case 1:
                return mTitleArray[Config.INDEX];
            case 2:
                return getString(R.string.hot);
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
