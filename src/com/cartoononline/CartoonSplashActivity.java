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
import android.widget.ListView;
import android.widget.TextView;

import com.cartoononline.ReaderListAdapter.ReaderItem;
import com.plugin.common.utils.UtilsConfig;
import com.plugin.common.utils.DeviceInfo;
import com.plugin.common.utils.SingleInstanceBase.SingleInstanceManager;
import com.plugin.common.utils.image.ImageUtils;

public class CartoonSplashActivity extends BaseActivity {

    private SectionsPagerAdapter mSectionsPagerAdapter;

    private ViewPager mViewPager;

    private LayoutInflater mLayoutInflater;

    private ListView mReaderListView;
    
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

        SingleInstanceManager.getInstance().init(getApplicationContext());
        UtilsConfig.DEVICE_INFO = new DeviceInfo(this);
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
            final TextView textView = new TextView(getActivity());
            textView.setGravity(Gravity.CENTER);
            textView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));

            int index = getArguments().getInt(ARG_SECTION_NUMBER);
            switch (index) {
            case 1:
                return makeReaderView(mLayoutInflater);
            }

            return textView;
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
                UtilsConfig.LOGD(sinfo.toString());
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

//    private List<ReaderItem> makeReaderItems() {
//        List<ReaderItem> ret = new ArrayList<ReaderItem>();
//        
////        File root = new File(Config.ROOT_DIR);
////        if (!root.exists() && !root.mkdirs()) {
////            return ret;
////        }
//        
//        File infoFile = new File(Config.SDCARD_INFO_FILE);
//        boolean saveExist = true;
//        if (!infoFile.exists()) {
//            saveExist = Utils.saveAssetsFileToDest(this.getApplicationContext(), "content/item_name.ini",
//                    Config.SDCARD_INFO_FILE);
//        }
//
//        if (saveExist) {
//            INIFile iniFile = new INIFile(Config.SDCARD_INFO_FILE);
//            int count = iniFile.getIntegerProperty("infos", "count");
//            String nameStr = iniFile.getStringProperty("infos", "name");
//            String[] names = nameStr.split(";");
//            List<Bitmap> btList = loadListContextItems();
//            if (names == null || names.length != count || btList.size() != count) {
//                return ret;
//            }
//            
//            for (int index = 0; index < count; ++index) {
//                ReaderItem item = new ReaderItem();
//                item.description = names[index];
//                item.image = btList.get(index);
//                ret.add(item);
//            }
//        }
//
//        return ret;
//    }

//    private ArrayList<Bitmap> loadListContextItems() {
//        ArrayList<Bitmap> ret = new ArrayList<Bitmap>();
//        try {
//            String[] itemArrayOrg = getAssets().list("content");
//            ArrayList<String> items = new ArrayList<String>();
//            for(String item : itemArrayOrg) {
//                if (!item.equals("item_name.ini")) {
//                    items.add(item);
//                }
//            }
//            String[] itemArray = new String[items.size()];
//            items.toArray(itemArray);
//            
//            if (itemArray != null && itemArray.length > 0) {
//                itemArray = sortByInt(itemArray);
//
//                for (String item : itemArray) {
//                    Bitmap itemBt = Utils.loadBitmapFromAsset(getApplicationContext(), "content/" + item + "/icon.jpg");
//
//                    if (itemBt != null) {
//                        try {
//                            Bitmap roundBt = ImageUtils.createRoundedBitmap(itemBt);
//                            if (roundBt != null) {
//                                ret.add(roundBt);
//                            }
//
//                            itemBt.recycle();
//                            itemBt = null;
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return ret;
//    }

//    private String[] sortByInt(String[] src) {
//        String[] ret = new String[src.length];
//        for (String data : src) {
//            int index = Integer.valueOf(data);
//            ret[index - 1] = String.valueOf(index);
//        }
//
//        return ret;
//    }

}
