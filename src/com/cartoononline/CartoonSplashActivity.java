package com.cartoononline;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cartoononline.ReaderListAdapter.ReaderItem;
import com.plugin.common.utils.UtilsConfig;
import com.plugin.common.utils.image.ImageDownloader;
import com.plugin.common.utils.image.ImageDownloader.DownloadListener;
import com.plugin.common.utils.image.ImageDownloader.ImageFetchRequest;
import com.plugin.common.utils.image.ImageDownloader.ImageFetchResponse;
import com.plugin.common.utils.image.ImageUtils;

public class CartoonSplashActivity extends BaseActivity {

    private SectionsPagerAdapter mSectionsPagerAdapter;

    private ViewPager mViewPager;

    private LayoutInflater mLayoutInflater;

    private ListView mReaderListView;
    
    private View mDownloadView;
    
    private ProgressDialog mProgress;
    
    private List<SessionInfo> mListSessionInfos;
    
    public static final int REFRESH_READER_LIST = 10001;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case REFRESH_READER_LIST:
                mReaderListView.setAdapter(new ReaderListAdapter(makeReaderItems(), mLayoutInflater));
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

        initActionbar();
        initProgressBar();
        mListSessionInfos = new ArrayList<SessionInfo>();

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        
        if (Utils.getSessionInfo(AppConfig.INTERNAL_SESSION_ONE) == null) {
            mProgress.show();
            //not exist
            Utils.asyncUnzipInternalSessions(this.getApplicationContext(), mHandler);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // getMenuInflater().inflate(R.menu.activity_main, menu);
        // return true;
        return super.onCreateOptionsMenu(menu);
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
            // getItem is called to instantiate the fragment for the given page.
            // Return a DummySectionFragment (defined as a static inner class
            // below) with the page number as its lone argument.
            Fragment fragment = new DummySectionFragment();
            Bundle args = new Bundle();
            args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, position + 1);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
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

    public class DummySectionFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        public static final String ARG_SECTION_NUMBER = "section_number";

        public DummySectionFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            // Create a new TextView and set its text to the fragment's section
            // number argument value.
            final Button btView = new Button(getActivity());
            btView.setGravity(Gravity.CENTER);
            btView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));

            int index = getArguments().getInt(ARG_SECTION_NUMBER);
            switch (index) {
            case 1:
                return makeReaderView(mLayoutInflater);
            case 2:
                return makeDownloadView(mLayoutInflater);
            }

            return btView;
        }
    }
    
    private View makeReaderView(LayoutInflater layoutInflater) {
        mReaderListView = (ListView) layoutInflater.inflate(R.layout.main_list, null);
        mReaderListView.setAdapter(new ReaderListAdapter(makeReaderItems(), layoutInflater));
        mReaderListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SessionInfo info = mListSessionInfos.get(position);
                
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), AlbumActivity.class);
                intent.putExtra(AlbumActivity.KEY_INDEX, info.path);
                intent.putExtra(AlbumActivity.KEY_SESSION_NAME, info.sessionName);
                startActivity(intent);
            }
        });
        
        return mReaderListView;
    }
    
    int stopCount = 0;
    private View makeDownloadView(LayoutInflater layoutInflater) {
        mDownloadView = layoutInflater.inflate(R.layout.download_content, null);
        final ProgressBar progress = (ProgressBar) mDownloadView.findViewById(R.id.progress);
        final TextView textView1 = (TextView) mDownloadView.findViewById(R.id.textView1);
        
        //整个文佳大小需要从服务器下载
        final int totalSize = 181843;
        progress.setMax(totalSize);
        textView1.setText("0%");
        
        View bt = mDownloadView.findViewById(R.id.download);
        bt.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                final ImageFetchRequest request = new ImageFetchRequest("http://image.zcool.com.cn/img3/55/58/1364540828542.jpg");
                ImageDownloader.getInstance(getApplicationContext()).postRequest(
                        request, 
                        new DownloadListener() {

                            @Override
                            public void onDownloadProcess(int fileSize, final int downloadSize) {
                                UtilsConfig.LOGD("Total file size = " + fileSize + " has download size = " + downloadSize);
                                
                                //test cancel downalod
                                if (stopCount < 2 && downloadSize > 4096 * 4) {
                                    UtilsConfig.LOGD("try to cancel download when download size = " + 4096 * 4);
                                    stopCount++;
                                    request.cancelDownload();
                                }
                                
                                runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        progress.setProgress(downloadSize);
                                        textView1.setText(String.valueOf(((int) ((1.0) * downloadSize / totalSize * 100))) + "%");
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
    
    private List<ReaderItem> makeReaderItems() {
        List<ReaderItem> ret = new ArrayList<ReaderItem>();
        File root = new File(AppConfig.ROOT_DIR);
        String[] files = root.list(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String filename) {
                if (!filename.startsWith("session")) {
                    return false;
                }
                
                String fileFullname = dir.getAbsolutePath() + "/" + filename;
                File f = new File(fileFullname);
                if (f.isDirectory()) {
                    return true;
                }
                return false;
            }
            
        });
        
        if (files != null && files.length > 0) {
            for (String name : files) {
                ReaderItem item = new ReaderItem();
                SessionInfo sinfo = Utils.getSessionInfo(AppConfig.ROOT_DIR + name);
                UtilsConfig.LOGD( "" + sinfo);
                if (sinfo != null) {
                    item.name = sinfo.name;
                    item.description = sinfo.description;
                    item.image = ImageUtils.loadBitmapWithSizeCheck(new File(AppConfig.ROOT_DIR + name + "/" + sinfo.cover));
                    item.time = sinfo.time;
                    
                    ret.add(item);
                    mListSessionInfos.add(sinfo);
                }
            }
        }
        
        if (AppConfig.DEBUG) {
            for (ReaderItem item : ret) {
                UtilsConfig.LOGD(item.toString());
            }
        }
        
        return ret;
    }

}
