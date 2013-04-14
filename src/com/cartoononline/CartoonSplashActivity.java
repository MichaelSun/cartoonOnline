package com.cartoononline;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.youmi.android.offers.OffersManager;
import net.youmi.android.spot.SpotManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.cartoononline.fragment.MoreBookFragment;
import com.cartoononline.model.DownloadItemModel;
import com.cartoononline.model.DownloadModel;
import com.cartoononline.model.SessionModel;
import com.cartoononline.model.SessionReadModel;
import com.plugin.common.utils.CustomThreadPool;
import com.plugin.common.utils.CustomThreadPool.TaskWrapper;
import com.plugin.common.utils.DataModelBase.DataDownloadListener;
import com.plugin.common.utils.SingleInstanceBase;
import com.plugin.common.utils.UtilsConfig;
import com.plugin.common.utils.files.FileInfo;
import com.plugin.common.utils.files.FileOperatorHelper;
import com.plugin.common.utils.files.FileUtil;

public class CartoonSplashActivity extends BaseActivity {

    private static final boolean DEBUG = AppConfig.DEBUG;

    private SectionsPagerAdapter mSectionsPagerAdapter;

    private ViewPager mViewPager;

    private LayoutInflater mLayoutInflater;

    private ListView mReaderListView;

    private ListView mDownloadListView;

    private ProgressDialog mProgress;

    private List<SessionReadModel> mShowSessionList = new ArrayList<SessionReadModel>();

    private List<DownloadItemModel> mDownloadList = new ArrayList<DownloadItemModel>();

    private DownloadModel mDownloadModel;

    private SessionModel mSessionModel;

    private ReaderListAdapter mReaderListAdapter;

    private DownloadItemAdapter mDownlaodListAdapter;

    private int mCurPageIndex;

    public static final int REFRESH_READER_LIST = 10001;
    public static final int NOTIFY_DATA_CHANGED = 10002;
    public static final int NOTIFY_DOWNLOAD_CHANGED = 10003;
    public static final int DISSMISS_PROGRESS = 10004;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case REFRESH_READER_LIST:
                break;
            case NOTIFY_DATA_CHANGED:
                if (mProgress.isShowing()) {
                    mProgress.dismiss();
                }
                if (mReaderListAdapter == null) {
                    mReaderListAdapter = new ReaderListAdapter(mShowSessionList, mLayoutInflater,
                            CartoonSplashActivity.this.getApplicationContext());
                    mReaderListView.setAdapter(mReaderListAdapter);
                } else {
                    mReaderListAdapter.setReadItems(mShowSessionList);
                }
                break;
            case NOTIFY_DOWNLOAD_CHANGED:
                if (mDownlaodListAdapter == null) {
                    mDownlaodListAdapter = new DownloadItemAdapter(CartoonSplashActivity.this, mDownloadList,
                            mLayoutInflater);
                    mDownloadListView.setAdapter(mDownlaodListAdapter);
                } else {
                    mDownlaodListAdapter.setData(mDownloadList);
                }
                if (mProgress.isShowing()) {
                    mProgress.dismiss();
                }
                break;
            case DISSMISS_PROGRESS:
                if (mProgress.isShowing()) {
                    mProgress.dismiss();
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLayoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mDownloadModel = SingleInstanceBase.getInstance(DownloadModel.class);
        mSessionModel = SingleInstanceBase.getInstance(SessionModel.class);

        initActionbar();
        initProgressBar();

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
                    if (mSessionModel.isDataChanged()) {
                        loadSessionData();
                    }
                    break;
                case 1:
                    if (mDownloadList == null || mDownloadList.size() == 0) {
                        loadDownloadData(false);
                    }
                    break;
                }
            }

        });

        asyncCheckInternalContent();
    }

    @Override
    protected void onStart() {
        super.onStart();
        SpotManager.getInstance(this).loadSpotAds();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mHandler.sendEmptyMessage(NOTIFY_DATA_CHANGED);
    }

    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        getSupportMenuInflater().inflate(R.menu.detail_actionbar, menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, com.actionbarsherlock.view.MenuItem item) {
        switch (item.getItemId()) {
        case R.id.more_apps:
            OffersManager.getInstance(getApplicationContext()).showOffersWall();
            break;
        case R.id.action_load:
            switch (mCurPageIndex) {
            case 0:
                asyncCheckInternalContent();
                break;
            case 1:
                mDownloadModel.resetPageNo();
                loadDownloadData(true);
                break;
            }
            break;
        }

        return true;
    }

    private void asyncCheckInternalContent() {
        mProgress.show();
        CustomThreadPool.getInstance().excute(new TaskWrapper(new Runnable() {
            @Override
            public void run() {
                checkInternalContent();
            }
        }));
    }

    private void checkInternalContent() {
        if (DEBUG) {
            UtilsConfig.LOGD("[[checkInternalContent]]");
        }

        String[] filenames = Utils.getFileCountUnderAssetsDir(this, "");
        if (filenames != null) {
            for (String name : filenames) {
                if (DEBUG) {
                    UtilsConfig.LOGD("[[checkInternalContent]] now check file : " + name);
                }

                if (name.startsWith(AppConfig.SESSION_REFIX)) {
                    String sname = name.substring(0, name.lastIndexOf(".zip"));
                    String targetPath = AppConfig.ROOT_DIR + sname + File.separator;
                    SessionInfo sInfo = Utils.getSessionInfo(targetPath);
                    if (sInfo == null) {
                        if (Utils.syncUnzipInternalSessions(getApplicationContext(), name)) {
                            sInfo = Utils.getSessionInfo(targetPath);
                            if (sInfo != null) {
                                SessionReadModel m = new SessionReadModel();
                                m.isRead = 0;
                                m.localFullPath = targetPath;
                                m.coverPath = sInfo.cover;
                                m.description = sInfo.description;
                                m.name = sInfo.name;
                                m.sessionName = sInfo.sessionName;
                                m.srcURI = "assets/" + name;
                                m.sessionMakeTime = sInfo.time;
                                m.unzipTime = System.currentTimeMillis();
                                m.localFullPathHashCode = m.localFullPath.hashCode();
                                mSessionModel.insertOrRelace(m);
                            }
                        } else {
                            File tFile = new File(targetPath);
                            tFile.deleteOnExit();
                        }
                    }
                }
            }
        }

        try {
            Thread.sleep(200);
        } catch (Exception e) {
            e.printStackTrace();
        }

        loadSessionData();
    }

    private void initActionbar() {
        mActionBar = getSupportActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(false);
        mActionBar.setDisplayShowTitleEnabled(true);
        mActionBar.setTitle(R.string.app_name);
        mActionBar.setIcon(R.drawable.icon);
    }

    private void initProgressBar() {
        if (mProgress == null) {
            mProgress = new ProgressDialog(this);
            mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgress.setMessage("正在加载中，请稍后...");
        }
    }

    public void loadDownloadData(final boolean repalceOld) {
        mProgress.show();
        mDownloadModel.asyncLoadDataServer(new DataDownloadListener() {

            @Override
            public void onDataLoadSuccess(Object loadData) {
                if (loadData != null) {
                    if (repalceOld) {
                        mDownloadList.clear();
                    }
                    mDownloadList.addAll((List<DownloadItemModel>) loadData);
                    checkDownloadItemStatus(mDownloadList);
                    mHandler.sendEmptyMessage(NOTIFY_DOWNLOAD_CHANGED);
                }
            }

            @Override
            public void onDataLoadFailed(Object errorData) {
                LOGD("failed to load page " + (Integer) errorData);
                mHandler.sendEmptyMessage(DISSMISS_PROGRESS);
            }

        });
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private ArrayList<Fragment> mItems;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            mItems = new ArrayList<Fragment>();
        }

        @Override
        public Fragment getItem(int position) {
            LOGD("[[getitem]] position = " + position + " mItems size = " + mItems.size());

            if (mItems.size() <= position) {
                switch (position) {
                case 0:
                    mItems.add(new ReaderFragment());
                case 1:
                    mItems.add(new DownloadFragment());
                case 2:
                    mItems.add(new MoreBookFragment());
                }
            }

            return mItems.get(position);
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

    public class ReaderFragment extends Fragment {

        public ReaderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            LOGD("[[ReaderFragment::onCreateView]]");

            return makeReaderView(mLayoutInflater);
        }

        @Override
        public void onResume() {
            super.onResume();
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            
            mReaderListAdapter = null;
            LOGD("[[ReaderFragment::onDestroyView]]");
        }
    }

    public class DownloadFragment extends Fragment {

        public DownloadFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            LOGD("[[DownloadFragment::onCreateView]]");
            return makeDownloadView(mLayoutInflater);
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            mDownlaodListAdapter = null;
        }
    }

    private void loadSessionData() {
        mSessionModel.asyncLoadDataLocal(new DataDownloadListener() {

            @Override
            public void onDataLoadSuccess(Object loadData) {
                if (loadData != null) {
                    mShowSessionList.clear();
                    mShowSessionList.addAll((List<SessionReadModel>) loadData);

                    mHandler.sendEmptyMessage(NOTIFY_DATA_CHANGED);
                }
            }

            @Override
            public void onDataLoadFailed(Object errorData) {
                // TODO Auto-generated method stub

            }

        });
    }

    private View makeReaderView(LayoutInflater layoutInflater) {
        mReaderListView = (ListView) layoutInflater.inflate(R.layout.main_list, null);
        loadSessionData();
        mReaderListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SessionReadModel m = mShowSessionList.get(position);

                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), AlbumActivity.class);
                intent.putExtra(AlbumActivity.KEY_INDEX, m.localFullPath);
                intent.putExtra(AlbumActivity.KEY_SESSION_NAME, m.sessionName);
                startActivity(intent);

                m.isRead = 1;
                mSessionModel.updateItem(m);
            }
        });
        mReaderListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final SessionReadModel m = mShowSessionList.get(position);
                AlertDialog dialog = new AlertDialog.Builder(CartoonSplashActivity.this)
                        .setMessage(String.format(getString(R.string.delete_tips), m.name))
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                FileInfo finfo = FileUtil.getFileInfo(m.localFullPath);
                                FileOperatorHelper.DeleteFile(finfo);

                                if (!TextUtils.isEmpty(m.srcURI) && !m.srcURI.startsWith("assets")) {
                                    try {
                                        DownloadItemModel dm = new DownloadItemModel();
                                        dm.setDownloadUrlHashCode(m.srcURI.hashCode());
                                        DownloadItemModel data = mDownloadModel.getItem(dm);
                                        mDownloadModel.deleteItemModel(dm);

                                        File zipFile = new File(data.localFullPath);
                                        zipFile.delete();

                                        asyncLoadDataLocal();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }

                                mSessionModel.deleteItem(m);
                                mShowSessionList.remove(m);
                                mHandler.sendEmptyMessage(NOTIFY_DATA_CHANGED);
                            }
                        }).create();
                dialog.show();

                return true;
            }

        });
        mHandler.sendEmptyMessage(NOTIFY_DATA_CHANGED);

        return mReaderListView;
    }

    int stopCount = 0;

    private View makeDownloadView(LayoutInflater layoutInflater) {
        mDownloadListView = (ListView) layoutInflater.inflate(R.layout.main_list, null);
        asyncLoadDataLocal();

        return mDownloadListView;
    }

    private void asyncLoadDataLocal() {
        mDownloadModel.asyncLoadDataLocal(new DataDownloadListener() {

            @Override
            public void onDataLoadSuccess(Object loadData) {
                if (loadData != null) {
                    List<DownloadItemModel> ret = (List<DownloadItemModel>) loadData;
                    checkDownloadItemStatus(ret);
                    mDownloadList.clear();
                    mDownloadList.addAll(ret);
                    mHandler.sendEmptyMessage(NOTIFY_DOWNLOAD_CHANGED);
                } else {
                    mDownloadList.clear();
                    mHandler.sendEmptyMessage(NOTIFY_DOWNLOAD_CHANGED);
                }
            }

            @Override
            public void onDataLoadFailed(Object errorData) {
                mHandler.sendEmptyMessage(DISSMISS_PROGRESS);
            }

        });
    }

    private void checkDownloadItemStatus(List<DownloadItemModel> data) {
        for (DownloadItemModel item : data) {
            if (!TextUtils.isEmpty(item.localFullPath)) {
                File localFile = new File(item.localFullPath);
                if (localFile.exists()) {
                    item.status = DownloadItemModel.DOWNLOADED;
                } else {
                    item.status = DownloadItemModel.UNDOWNLOAD;
                }
                // TODO: check if the file is unziped
            } else {
                item.status = DownloadItemModel.UNDOWNLOAD;
            }
        }
    }

    // private List<SessionReadModel> makeReaderItems() {
    // List<SessionReadModel> ret = new ArrayList<SessionReadModel>();
    // List<SessionReadModel> lists = mHelper.queryItems();
    // if (lists != null) {
    // for (SessionReadModel m : lists) {
    // File localFile = new File(m.localFullPath);
    // if (localFile.exists() && localFile.isDirectory()) {
    // m.coverBt = ImageUtils.loadBitmapWithSizeCheck(new File(m.coverPath));
    // ret.add(m);
    // }
    // }
    // }
    //
    // if (AppConfig.DEBUG) {
    // for (SessionReadModel item : ret) {
    // UtilsConfig.LOGD(item.toString());
    // }
    // }
    //
    // return ret;
    // }

    private static void LOGD(String msg) {
        if (AppConfig.DEBUG) {
            UtilsConfig.LOGD(msg);
        }
    }
}
