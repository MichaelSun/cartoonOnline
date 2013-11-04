package com.cartoononline.sildeActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.actionbarsherlock.view.MenuItem;
import com.album.mmall1.R;
import com.cartoononline.*;
import com.cartoononline.api.LoginRequest;
import com.cartoononline.api.LoginResponse;
import com.cartoononline.fragment.DownloadFragment;
import com.cartoononline.fragment.FragmentStatusInterface;
import com.cartoononline.fragment.HotFragment;
import com.cartoononline.model.DownloadModel;
import com.cartoononline.model.HotModel;
import com.plugin.common.cache.CacheFactory;
import com.plugin.common.cache.ICacheManager;
import com.plugin.common.cache.ICacheStrategy;
import com.plugin.common.utils.CustomThreadPool;
import com.plugin.common.utils.Environment;
import com.plugin.internet.InternetUtils;
import com.umeng.analytics.MobclickAgent;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: michael
 * Date: 13-10-23
 * Time: PM1:38
 * To change this template use File | Settings | File Templates.
 */
public class CartoonMainActivity extends BaseActivity {

    public interface LoginInterfaceListener {

        void onLoginSuccess(int currentPoint);

        void onLoginFailed(int code);
    }

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;

    private ActionBarDrawerToggle mDrawerToggle;

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    private String[] mPlanetTitles;
    private int mCurPageIndex;
    private int mCurSelectedCategory;

    private HashMap<Integer, Fragment> mItemsMap = new HashMap<Integer, Fragment>();

    private ICacheStrategy mDefICacheStrategy;

    private ICacheManager mCacheManager;

    private String mCurrentTitle;

    private boolean mForceShowDownload = false;

    private ProgressDialog mProgressDialog;

    private View mListFooterView;

    private View mListTopView;

    private ImageView mLoginIconImageView;

    private TextView mLoginContentTV;

    private View mPointRegion;

    private TextView mPoint;

    private com.actionbarsherlock.view.Menu mMenu;


    private static final int UPDATE_LOGIN_NAME = 1000;
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_LOGIN_NAME:
                    String name = SettingManager.getInstance().getUserName();
                    if (TextUtils.isEmpty(name)) {
                        name = getString(R.string.login_text);
                        mPointRegion.setVisibility(View.GONE);
                    } else {
                        mPointRegion.setVisibility(View.VISIBLE);
                        mPoint.setText(String.format(getString(R.string.user_point), CRuntime.ACCOUNT_POINT_INFO.currentPoint));
                    }
                    mLoginContentTV.setText(name);
                    break;
            }
        }

    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.cartoon_main_silde_activity);
        mPlanetTitles = getResources().getStringArray(R.array.title_array);

        CRuntime.ACCOUNT_POINT_INFO.currentPoint = SettingManager.getInstance().getCurrentPoint();

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setCanceledOnTouchOutside(false);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        mListFooterView = getLayoutInflater().inflate(R.layout.drawer_list_about, null);
        mDrawerList.addFooterView(mListFooterView);

        mListTopView = getLayoutInflater().inflate(R.layout.drawer_list_top, null);
        mDrawerList.addHeaderView(mListTopView);

        mLoginIconImageView = (ImageView) mListTopView.findViewById(R.id.login);
        mLoginContentTV = (TextView) mListTopView.findViewById(R.id.name);
        mPointRegion = mListTopView.findViewById(R.id.point_region);
        mPoint = (TextView) mListTopView.findViewById(R.id.point);

        mLoginIconImageView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                checkLoginInfo();
            }
        });

        mListFooterView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAboutDialog();
            }
        });

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, R.id.text, mPlanetTitles) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View ret = super.getView(position, convertView, parent);

                ImageView image = (ImageView) ret.findViewById(R.id.icon);
                switch (position) {
                    case 0:
                        image.setImageResource(R.drawable.icon_rosi);
                        break;
                    case 1:
                        image.setImageResource(R.drawable.icon_leg);
                        break;
                    case 2:
                        image.setImageResource(R.drawable.icon_manhua);
                        break;
                    case 3:
                        image.setImageResource(R.drawable.icon_disi);
                }

                return ret;
            }
        });
        mDrawerList.setOnItemClickListener(new ListView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    checkLoginInfo();
                    return;
                }

                selectItem(position);
            }
        });

        mDrawerToggle = new ActionBarDrawerToggle(this,                  /* host Activity */
                                                     mDrawerLayout,         /* DrawerLayout object */
                                                     R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                                                     R.string.drawer_open,  /* "open drawer" description for accessibility */
                                                     R.string.drawer_open  /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(mCurrentTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                mHandler.sendEmptyMessage(UPDATE_LOGIN_NAME);
                getActionBar().setTitle(R.string.drawer_open);
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        initActionbar();
        initCache();
        initContentData();

        mCurrentTitle = mPlanetTitles[Config.CURRENT_DOMAIN];
        getActionBar().setIcon(R.drawable.icon_rosi);
        selectItem(1);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        mDrawerToggle.syncState();
    }

    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        getSupportMenuInflater().inflate(R.menu.detail_actionbar, menu);
//        getMenuInflater().inflate(R.menu.detail_actionbar, menu);
        mMenu = menu;
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
                    mDrawerLayout.closeDrawer(mDrawerList);
                    mMenu.findItem(R.id.action_load).setVisible(true);
                    mMenu.findItem(R.id.detail_action_more).setVisible(true);
                } else {
                    mDrawerLayout.openDrawer(mDrawerList);
                    mMenu.findItem(R.id.action_load).setVisible(false);
                    mMenu.findItem(R.id.detail_action_more).setVisible(false);
                }
                return true;
        }

        onCustomMenuSelected(item);

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();

        CRuntime.CUR_FORMAT_TIME = CRuntime.composeTime();

        if (mItemsMap.containsKey(mCurPageIndex)) {
            Fragment f = mItemsMap.get(mCurPageIndex);
            if (f != null && f instanceof FragmentStatusInterface) {
                ((FragmentStatusInterface) f).onShow();
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
        mCacheManager.releaseAllResource();
        Config.CURRENT_DOMAIN = 0;

        SettingManager.getInstance().setCurrentPoint(CRuntime.ACCOUNT_POINT_INFO.currentPoint);
    }

    private void checkLoginInfo() {
        SettingManager sm = SettingManager.getInstance();
        if (TextUtils.isEmpty(sm.getUserName()) || TextUtils.isEmpty(sm.getPassword())) {
            showPointWithAccountCheck(new LoginInterfaceListener() {
                @Override
                public void onLoginSuccess(int currentPoint) {
                    CRuntime.ACCOUNT_POINT_INFO.currentPoint = currentPoint;
                    CRuntime.ACCOUNT_POINT_INFO.username = SettingManager.getInstance().getUserName();
                    mHandler.sendEmptyMessage(UPDATE_LOGIN_NAME);
                }

                @Override
                public void onLoginFailed(int code) {
                    mHandler.sendEmptyMessage(UPDATE_LOGIN_NAME);
                }
            });
        } else {
            showPointWithAccountCheck(new LoginInterfaceListener() {

                @Override
                public void onLoginSuccess(final int currentPoint) {
                    CRuntime.ACCOUNT_POINT_INFO.currentPoint = currentPoint;
                    CRuntime.ACCOUNT_POINT_INFO.username = SettingManager.getInstance().getUserName();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showWallInfoDialog(currentPoint);
                        }
                    });
                }

                @Override
                public void onLoginFailed(int code) {
                }
            });
        }
    }

    private void initCache() {
        mCacheManager = CacheFactory.getCacheManager(CacheFactory.TYPE_CACHE.TYPE_IMAGE);
        mDefICacheStrategy = mCacheManager.setCacheStrategy(new ICacheStrategy() {

            @Override
            public String onMakeFileKeyName(String category, String key) {
                return null;
            }

            @Override
            public String onMakeImageCacheFullPath(String rootPath, String key, String ext) {
//                if (DEBUG) {
//                    LOGD("[[CartoonSplashActivity::onMakeImageCacheFullPath]] rootPath = " + rootPath + " key = " + key + " ext = " + ext);
//                }

                return null;
            }

        });
        mCacheManager.setCacheStrategy(mDefICacheStrategy);
    }

    private void initContentData() {
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrollStateChanged(int arg0) {
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }

            @Override
            public void onPageSelected(int arg0) {
                mCurPageIndex = arg0;
                switch (mCurPageIndex) {
//                    case 0:
//                        if (mItemsMap.containsKey(0)) {
//                            Fragment f = mItemsMap.get(0);
//                            if (f != null && f instanceof FragmentStatusInterface) {
//                                ((FragmentStatusInterface) f).onShow();
//                            }
//                        }
//                        releaseRes(1);
//                        releaseRes(2);
//                        break;
                    case 0:
                        if (mItemsMap.containsKey(0)) {
                            Fragment f = mItemsMap.get(0);
                            if (f != null && f instanceof FragmentStatusInterface) {
                                if (!mForceShowDownload) {
                                    ((FragmentStatusInterface) f).onShow();
                                } else {
                                    DownloadModel dm = DownloadModel.getDownloadModelFactory(Config.CURRENT_DOMAIN, getApplicationContext());
                                    if (dm != null) {
                                        dm.resetPageNo();
                                    }
                                    ((FragmentStatusInterface) f).onForceRefresh();
                                }
                            }
                        }
                        mForceShowDownload = false;
                        releaseRes(1);
                        break;
                    case 1:
                        if (mItemsMap.containsKey(1)) {
                            Fragment f = mItemsMap.get(1);
                            if (f != null && f instanceof FragmentStatusInterface) {
                                if (!mForceShowDownload) {
                                    ((FragmentStatusInterface) f).onShow();
                                } else {
                                    HotModel hm = HotModel.getDownloadModelFactory(Config.CURRENT_DOMAIN, getApplicationContext());
                                    if (hm != null) {
                                        hm.resetPageNo();
                                    }
                                    ((FragmentStatusInterface) f).onForceRefresh();
                                }
                            }
                        }
                        mForceShowDownload = false;
                        releaseRes(0);
                        break;
                }
            }

        });
    }

    private void initActionbar() {
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
    }

    private void selectItem(int position) {
        position = position - 1;
        if (mCurSelectedCategory != position) {
            mCurrentTitle = mPlanetTitles[position];
            mCurSelectedCategory = position;

            switch (position) {
                case 0:
                    Config.CURRENT_DOMAIN = 0;
                    getActionBar().setIcon(R.drawable.icon_rosi);
                    break;
                case 1:
                    Config.CURRENT_DOMAIN = 1;
                    getActionBar().setIcon(R.drawable.icon_leg);
                    break;
                case 2:
                    Config.CURRENT_DOMAIN = 2;
                    getActionBar().setIcon(R.drawable.icon_manhua);
                    break;
                case 3:
                    Config.CURRENT_DOMAIN = 3;
                    getActionBar().setIcon(R.drawable.icon_disi);
                    break;
            }

            mSectionsPagerAdapter.notifyDataSetChanged();

            for (Fragment f : mItemsMap.values()) {
                if (f instanceof FragmentStatusInterface) {
                    ((FragmentStatusInterface) f).onDataSourceChanged();
                }
            }

//            if (mItemsMap.size() > mCurPageIndex) {
//                Fragment f = mItemsMap.get(mCurPageIndex);
//                if (f instanceof FragmentStatusInterface) {
////                    ((FragmentStatusInterface) f).onShow();
//                    ((FragmentStatusInterface) f).onDataSourceChanged();
//                }
//            }
        }

        mDrawerLayout.closeDrawer(mDrawerList);
    }

    private void onCustomMenuSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.rate:
                MobclickAgent.onEvent(getApplicationContext(), Config.RATE_APP);
                MobclickAgent.flush(getApplicationContext());
                RateDubblerHelper.getInstance(getApplicationContext()).OpenApp(Config.CURRENT_PACKAGE_NAME);
                break;
            case R.id.about:
                showAboutDialog();
                break;
            case R.id.download_jifen:
                // Utils.downloadJifenbao(getApplicationContext());
                DialogUtils.showJifenBaoDownloadDialog(this, null);
                break;
            case R.id.wall_info:
//                if (mShowAppWallInfo) {
                showPointWithAccountCheck(new LoginInterfaceListener() {

                    @Override
                    public void onLoginSuccess(final int currentPoint) {
                        CRuntime.ACCOUNT_POINT_INFO.currentPoint = currentPoint;
                        CRuntime.ACCOUNT_POINT_INFO.username = SettingManager.getInstance().getUserName();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showWallInfoDialog(currentPoint);
                            }
                        });
                    }

                    @Override
                    public void onLoginFailed(int code) {
                    }
                });
//                }

                HashMap<String, String> extra = new HashMap<String, String>();
                extra.put("packageName", Config.CURRENT_PACKAGE_NAME);
                MobclickAgent.onEvent(getApplicationContext(), "show_wall_info", extra);
                MobclickAgent.flush(getApplicationContext());
                break;
            case R.id.action_load:
                switch (mCurPageIndex) {
                    case 0:
                        if (mItemsMap.containsKey(0)) {
                            Fragment f = mItemsMap.get(0);
                            if (f != null && f instanceof FragmentStatusInterface) {
                                ((FragmentStatusInterface) f).onForceRefresh();
                            }
                        }
                        break;
                    case 1:
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
    }

    private void releaseRes(int index) {
        if (mItemsMap.containsKey(index)) {
            Fragment f = mItemsMap.get(index);
            if (f != null && f instanceof FragmentStatusInterface) {
                ((FragmentStatusInterface) f).onStopShow();
            }
        }
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        private String[] mTitleArray;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            mTitleArray = getResources().getStringArray(R.array.title_array);
        }

        @Override
        public Fragment getItem(int position) {
            if (mItemsMap.size() <= position) {
                switch (position) {
                    case 0:
                        mItemsMap.put(position, new DownloadFragment());
                        break;
                    case 1:
                        mItemsMap.put(position, new HotFragment());
                        break;
                }
            }

            return mItemsMap.get(position);
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return mTitleArray[Config.CURRENT_DOMAIN];
                case 1:
                    return getString(R.string.hot);
            }
            return null;
        }
    }

    private void showAboutDialog() {
        String version = Environment.getVersionName(getApplicationContext());
        String versionStr = String.format(getString(R.string.version_info), version);
        View v = this.getLayoutInflater().inflate(R.layout.about_view, null);
        TextView versionTV = (TextView) v.findViewById(R.id.version);
        versionTV.setText(versionStr);

        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(R.string.about).setView(v)
                                 .setPositiveButton(R.string.confirm, null)
                                 .setNegativeButton(R.string.btn_rate, new DialogInterface.OnClickListener() {
                                     @Override
                                     public void onClick(DialogInterface dialog, int which) {
                                         MobclickAgent.onEvent(getApplicationContext(), Config.RATE_APP);
                                         MobclickAgent.flush(getApplicationContext());
                                         RateDubblerHelper.getInstance(getApplicationContext()).OpenApp(Config.CURRENT_PACKAGE_NAME);
                                     }
                                 }).create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    public void showPointWithAccountCheck(final LoginInterfaceListener l) {
        SettingManager sm = SettingManager.getInstance();
        if (TextUtils.isEmpty(sm.getUserName()) || TextUtils.isEmpty(sm.getPassword())) {
            View contentView = this.getLayoutInflater().inflate(R.layout.account_login, null);
            final EditText userNameEditText = (EditText) contentView.findViewById(R.id.username);
            final EditText passwordEditText = (EditText) contentView.findViewById(R.id.password);
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this).setTitle(R.string.login_title)
                                                    .setView(contentView).setPositiveButton(R.string.login, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        HashMap<String, String> extra = new HashMap<String, String>();
                        extra.put("packageName", Config.CURRENT_PACKAGE_NAME);
                        MobclickAgent.onEvent(getApplicationContext(), "login_jifenbao", extra);
                        MobclickAgent.flush(getApplicationContext());

                        final String userName = userNameEditText.getEditableText().toString();
                        final String password = passwordEditText.getEditableText().toString();
                        tryToLogin(userName, password, l);
                    }
                });
            if (false && Utils.isAvilible(getApplicationContext(), Config.JIFENBAP_PACKAGE_NAME)) {
                dialogBuilder.setNegativeButton(R.string.fetch_jifen_btn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Utils.lanuchJifenBao(getApplicationContext());
                    }
                });
            } else {
                dialogBuilder.setNegativeButton(R.string.registe_btn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String userName = userNameEditText.getEditableText().toString();
                        String password = passwordEditText.getEditableText().toString();
                        if (!TextUtils.isEmpty(userName) && !TextUtils.isEmpty(password)) {
                            mProgressDialog.setMessage(getString(R.string.registe_tips));
                            mProgressDialog.show();
                            Utils.registeAccount(getApplicationContext(), userName, password, new Utils.RegisteListener() {

                                @Override
                                public void onRegisteSuccess(final int currentPoint) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mProgressDialog.dismiss();
                                            showWallInfoDialog(currentPoint);
                                        }
                                    });
                                }

                                @Override
                                public void onRegisteFailed(final int code, String data) {
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            mProgressDialog.dismiss();
                                            switch (code) {
                                                case LoginResponse.CODE_USER_EXIST:
                                                    Toast.makeText(CartoonMainActivity.this, R.string.user_exist, Toast.LENGTH_LONG).show();
                                                    break;
                                                default:
                                                    Toast.makeText(CartoonMainActivity.this, R.string.registe_failed, Toast.LENGTH_LONG).show();
                                                    break;
                                            }
                                        }
                                    });
                                }

                            });
                        } else {
                            Toast.makeText(CartoonMainActivity.this, R.string.user_or_password_empty, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
            dialogBuilder.create().show();
        } else {
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.show();
            Utils.asyncFetchCurrentPoint(getApplicationContext(), sm.getUserName(), sm.getPassword(),
                                            new Utils.PointFetchListener() {
                                                @Override
                                                public void onPointFetchSuccess(final int current) {
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            mProgressDialog.dismiss();
                                                            showWallInfoDialog(current);
                                                        }
                                                    });
                                                }

                                                @Override
                                                public void onPointFetchFailed(int code, String data) {
                                                    runOnUiThread(new Runnable() {

                                                        @Override
                                                        public void run() {
                                                            mProgressDialog.dismiss();
                                                            Toast.makeText(CartoonMainActivity.this, R.string.error_sync_point,
                                                                              Toast.LENGTH_LONG).show();
                                                        }
                                                    });
                                                }
                                            });
        }
    }

    public void tryToLogin(final String username, final String password, final LoginInterfaceListener l) {
        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
            mProgressDialog.setMessage(getString(R.string.logining));
            mProgressDialog.show();

            CustomThreadPool.asyncWork(new Runnable() {

                @Override
                public void run() {
                    LoginRequest request = new LoginRequest(username, password);
                    try {
                        LoginResponse response = InternetUtils.request(getApplicationContext(), request);
                        if (response != null) {
                            Log.d(">>>>>>>", response.toString());

                            runOnUiThread(new Runnable() {
                                public void run() {
                                    mProgressDialog.dismiss();
                                }
                            });

                            switch (response.code) {
                                case LoginResponse.CODE_SUCCESS:
                                    final String point = response.data;
                                    SettingManager.getInstance().setUserName(username);
                                    SettingManager.getInstance().setPassword(password);
                                    MobclickAgent.onEvent(getApplicationContext(), "login");
                                    MobclickAgent.flush(getApplicationContext());
                                    if (l != null) {
                                        l.onLoginSuccess(Integer.valueOf(point));
                                    }
                                    break;
                                case LoginResponse.CODE_USER_NOT_EXIST:
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mProgressDialog.dismiss();
                                            // Toast.makeText(getApplicationContext(),
                                            // R.string.user_not_exist,
                                            // Toast.LENGTH_LONG).show();
                                            DialogUtils.showJifenBaoDownloadDialog(CartoonMainActivity.this,
                                                                                      getString(R.string.downalod_jifenbao_registe));
                                        }
                                    });
                                    break;
                                case LoginResponse.CODE_PASSWORD_ERROR:
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mProgressDialog.dismiss();
                                            Toast.makeText(getApplicationContext(), R.string.password_error,
                                                              Toast.LENGTH_LONG).show();
                                        }
                                    });
                                    break;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(), R.string.user_or_password_empty, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void showWallInfoDialog(int currentUserPoint) {
        int localPoint = SettingManager.getInstance().getPointInt();

        int point = currentUserPoint;
        String tips = String.format(getString(R.string.offer_info_detail), SettingManager.getInstance().getUserName(),
                                       point + localPoint, Config.DOWNLOAD_NEED_POINT);
        View view = this.getLayoutInflater().inflate(R.layout.offer_tips_view, null);
        TextView tv = (TextView) view.findViewById(R.id.tips);
        tv.setText(tips);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(R.string.tips_title).setView(view)
                                 .setPositiveButton(R.string.fetch_jifen_btn, new DialogInterface.OnClickListener() {

                                     @Override
                                     public void onClick(DialogInterface dialog, int which) {
                                         if (Utils.isAvilible(getApplicationContext(), Config.JIFENBAP_PACKAGE_NAME)) {
                                             Utils.lanuchJifenBao(getApplicationContext());
                                         } else {
                                             Utils.downloadJifenbao(getApplicationContext());
                                         }
                                     }
                                 })
                                 .setNegativeButton(R.string.logout, new DialogInterface.OnClickListener() {

                                     @Override
                                     public void onClick(DialogInterface dialog, int which) {
                                         SettingManager.getInstance().setUserName("");
                                         SettingManager.getInstance().setPassword("");
                                         CRuntime.ACCOUNT_POINT_INFO.currentPoint = 0;
                                         CRuntime.ACCOUNT_POINT_INFO.username = "";
                                         mHandler.sendEmptyMessage(UPDATE_LOGIN_NAME);
                                     }
                                 }).create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }
}