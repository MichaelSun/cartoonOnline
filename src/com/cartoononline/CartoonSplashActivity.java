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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cartoononline.api.NewSessionRequest;
import com.cartoononline.api.NewSessionResponse;
import com.cartoononline.api.NewSessionResponse.SessionItem;
import com.cartoononline.model.DownloadItemModel;
import com.cartoononline.model.SessionReadModel;
import com.plugin.common.utils.CustomThreadPool;
import com.plugin.common.utils.CustomThreadPool.TaskWrapper;
import com.plugin.common.utils.UtilsConfig;
import com.plugin.common.utils.files.FileDownloader.DownloadListener;
import com.plugin.common.utils.files.FileInfo;
import com.plugin.common.utils.files.FileOperatorHelper;
import com.plugin.common.utils.files.FileUtil;
import com.plugin.common.utils.image.ImageDownloader;
import com.plugin.common.utils.image.ImageDownloader.ImageFetchRequest;
import com.plugin.common.utils.image.ImageDownloader.ImageFetchResponse;
import com.plugin.common.utils.image.ImageUtils;
import com.plugin.database.dao.helper.DBTableAccessHelper;
import com.plugin.internet.InternetUtils;

public class CartoonSplashActivity extends BaseActivity {

    private static final boolean DEBUG = AppConfig.DEBUG;

    private SectionsPagerAdapter mSectionsPagerAdapter;

    private ViewPager mViewPager;

    private LayoutInflater mLayoutInflater;

    private ListView mReaderListView;

    private ListView mDownloadListView;

    private View mDownloadView;

    private ProgressDialog mProgress;

    private DBTableAccessHelper<SessionReadModel> mHelper;
    private DBTableAccessHelper<DownloadItemModel> mDownloadHelper;

    private List<SessionReadModel> mShowSessionList;

    private List<DownloadItemModel> mDownloadList;

    private ReaderListAdapter mReaderListAdapter;

    private DownloadItemAdapter mDownlaodListAdapter;

    private int mCurPageIndex;

    public static final int REFRESH_READER_LIST = 10001;
    public static final int NOTIFY_DATA_CHANGED = 10002;
    public static final int NOTIFY_DOWNLOAD_CHANGED = 10003;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case REFRESH_READER_LIST:
                mShowSessionList = makeReaderItems();
                mHandler.sendEmptyMessage(NOTIFY_DATA_CHANGED);
                if (mProgress.isShowing()) {
                    mProgress.dismiss();
                }
                break;
            case NOTIFY_DATA_CHANGED:
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
                    mDownlaodListAdapter = new DownloadItemAdapter(mDownloadList, mLayoutInflater);
                    mDownloadListView.setAdapter(mDownlaodListAdapter);
                } else {
                    mDownlaodListAdapter.setData(mDownloadList);
                }
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

        mHelper = new DBTableAccessHelper<SessionReadModel>(this.getApplicationContext(), SessionReadModel.class);
        mDownloadHelper = new DBTableAccessHelper<DownloadItemModel>(this.getApplicationContext(), DownloadItemModel.class);

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
                if (mCurPageIndex == 1) {
                    mProgress.show();
                }
                CustomThreadPool.getInstance().excute(new TaskWrapper(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            NewSessionResponse response = InternetUtils.request(getApplicationContext(), new NewSessionRequest(0));
                            LOGD("[[:::::::::]] response = " + response);
                            
                            if (response.items != null) {
                                DownloadItemModel[] saveData = new DownloadItemModel[response.items.length];
                                for (int index = 0; index < response.items.length; ++index) {
                                    SessionItem item = response.items[index];
                                    DownloadItemModel ditem = new DownloadItemModel();
                                    ditem.coverUrl = item.imageUrl;
                                    ditem.description = item.description;
                                    ditem.downloadUrl = item.downloadUrl;
                                    ditem.downloadUrlHashCode = item.downloadUrl.hashCode();
                                    ditem.sessionName = item.name;
                                    ditem.size = item.size;
                                    saveData[index] = ditem;
                                }
                                
                                mDownloadHelper.blukInsertOrReplace(saveData);
                                
                                mDownloadList.clear();
                                mDownloadList = mDownloadHelper.queryItems();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        
                        mHandler.sendEmptyMessage(NOTIFY_DOWNLOAD_CHANGED);
                    }
                }));
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
                                mHelper.insertOrReplace(m);
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
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mHandler.sendEmptyMessage(CartoonSplashActivity.REFRESH_READER_LIST);
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

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
            case 0:
                return new ReaderFragment();
            case 1:
                return new DownloadFragment();
            }
            
            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
            case 0:
                return "Reader";
            case 1:
                return "Download";
            case 2:
                return "More";
            }
            return null;
        }
    }

    public class ReaderFragment extends Fragment {

        public ReaderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return makeReaderView(mLayoutInflater);
        }

        @Override
        public void onResume() {
            super.onResume();
        }
    }

    public class DownloadFragment extends Fragment {

        public DownloadFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return makeDownloadView(mLayoutInflater);
        }

    }

    private View makeReaderView(LayoutInflater layoutInflater) {
        mReaderListView = (ListView) layoutInflater.inflate(R.layout.main_list, null);
        mShowSessionList = makeReaderItems();
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
                mHelper.update(m);
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

                                mHelper.delete(m);
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
        mDownloadList = makeDownloadItems();

        mHandler.sendEmptyMessage(NOTIFY_DOWNLOAD_CHANGED);

        return mDownloadListView;
    }

    private View makeDownloadViewTest(LayoutInflater layoutInflater) {
        mDownloadView = layoutInflater.inflate(R.layout.download_content, null);
        final ProgressBar progress = (ProgressBar) mDownloadView.findViewById(R.id.progress);
        final TextView textView1 = (TextView) mDownloadView.findViewById(R.id.textView1);

        // 整个文佳大小需要从服务器下载
        final int totalSize = 181843;
        progress.setMax(totalSize);
        textView1.setText("0%");

        View bt = mDownloadView.findViewById(R.id.download);
        bt.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                final ImageFetchRequest request = new ImageFetchRequest(
                        "http://image.zcool.com.cn/img3/55/58/1364540828542.jpg");
                ImageDownloader.getInstance(getApplicationContext()).postRequest(request, new DownloadListener() {

                    @Override
                    public void onDownloadProcess(int fileSize, final int downloadSize) {
                        UtilsConfig.LOGD("Total file size = " + fileSize + " has download size = " + downloadSize);

                        // test cancel downalod
                        if (stopCount < 2 && downloadSize > 4096 * 4) {
                            UtilsConfig.LOGD("try to cancel download when download size = " + 4096 * 4);
                            stopCount++;
                            request.cancelDownload();
                        }

                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                progress.setProgress(downloadSize);
                                textView1.setText(String.valueOf(((int) ((1.0) * downloadSize / totalSize * 100)))
                                        + "%");
                            }

                        });
                    }

                    @Override
                    public void onDownloadFinished(int status, Object response) {
                        if (status == ImageDownloader.DOWNLOAD_SUCCESS) {
                            final ImageFetchResponse r = (ImageFetchResponse) response;

                            UtilsConfig.LOGD(response + "");
                            if (r != null && r.getmBt() != null) {
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        ImageView iv = (ImageView) mDownloadView.findViewById(R.id.show);
                                        iv.setImageBitmap(r.getmBt());
                                    }
                                });
                            }
                        }
                    }

                });
            }
        });

        return mDownloadView;
    }

    private List<DownloadItemModel> makeDownloadItems() {
        List<DownloadItemModel> ret = new ArrayList<DownloadItemModel>();
        
        List<DownloadItemModel> lists = mDownloadHelper.queryItems();
        if (lists != null) {
            for (DownloadItemModel item : lists) {
                if (!TextUtils.isEmpty(item.localFullPath)) {
                    File localFile = new File(item.localFullPath);
                    if (localFile.exists()) {
                        item.status = DownloadItemModel.UNDOWNLOAD;
                    }
                    //TODO: check if the file is unziped
                } else {
                    item.status = DownloadItemModel.UNDOWNLOAD;
                }
            }
        }
        
        ret.addAll(lists);
        return ret;
    }
    
    private List<SessionReadModel> makeReaderItems() {
        List<SessionReadModel> ret = new ArrayList<SessionReadModel>();
        // File root = new File(AppConfig.ROOT_DIR);
        // String[] files = root.list(new FilenameFilter() {
        //
        // @Override
        // public boolean accept(File dir, String filename) {
        // if (!filename.startsWith("session")) {
        // return false;
        // }
        //
        // String fileFullname = dir.getAbsolutePath() + "/" + filename;
        // File f = new File(fileFullname);
        // if (f.isDirectory()) {
        // return true;
        // }
        // return false;
        // }
        //
        // });

        List<SessionReadModel> lists = mHelper.queryItems();
        if (lists != null) {
            for (SessionReadModel m : lists) {
                File localFile = new File(m.localFullPath);
                if (localFile.exists() && localFile.isDirectory()) {
                    m.coverBt = ImageUtils.loadBitmapWithSizeCheck(new File(m.coverPath));
                    ret.add(m);
                }
            }
        }

        // if (files != null && files.length > 0) {
        // for (String name : files) {
        // ReaderItem item = new ReaderItem();
        // SessionInfo sinfo = Utils.getSessionInfo(AppConfig.ROOT_DIR + name);
        // UtilsConfig.LOGD( "" + sinfo);
        // if (sinfo != null) {
        // item.name = sinfo.name;
        // item.description = sinfo.description;
        // item.image = ImageUtils.loadBitmapWithSizeCheck(new
        // File(AppConfig.ROOT_DIR + name + "/" + sinfo.cover));
        // item.time = sinfo.time;
        //
        // ret.add(item);
        // mListSessionInfos.add(sinfo);
        // }
        // }
        // }

        if (AppConfig.DEBUG) {
            for (SessionReadModel item : ret) {
                UtilsConfig.LOGD(item.toString());
            }
        }

        return ret;
    }

    private static void LOGD(String msg) {
        if (AppConfig.DEBUG) {
            UtilsConfig.LOGD(msg);
        }
    }
}
