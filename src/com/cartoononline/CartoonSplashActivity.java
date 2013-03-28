package com.cartoononline;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.cartoononline.ReaderListAdapter.ReaderItem;
import com.plugin.common.utils.INIFile;
import com.plugin.common.utils.image.ImageUtils;

public class CartoonSplashActivity extends BaseActivity {

    private SectionsPagerAdapter mSectionsPagerAdapter;

    private ViewPager mViewPager;

    private LayoutInflater mLayoutInflater;

    private ListView mReaderListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLayoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        initActionbar();

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
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
                mReaderListView = (ListView) mLayoutInflater.inflate(R.layout.main_list, null);
                mReaderListView.setAdapter(new ReaderListAdapter(makeReaderItems(), mLayoutInflater));
                return mReaderListView;
            }

            return textView;
        }
    }

    private List<ReaderItem> makeReaderItems() {
        List<ReaderItem> ret = new ArrayList<ReaderItem>();
        
//        File root = new File(Config.ROOT_DIR);
//        if (!root.exists() && !root.mkdirs()) {
//            return ret;
//        }
        
        File infoFile = new File(Config.SDCARD_INFO_FILE);
        boolean saveExist = true;
        if (!infoFile.exists()) {
            saveExist = Utils.saveAssetsFileToDest(this.getApplicationContext(), "content/item_name.ini",
                    Config.SDCARD_INFO_FILE);
        }

        if (saveExist) {
            INIFile iniFile = new INIFile(Config.SDCARD_INFO_FILE);
            int count = iniFile.getIntegerProperty("infos", "count");
            String nameStr = iniFile.getStringProperty("infos", "name");
            String[] names = nameStr.split(";");
            List<Bitmap> btList = loadListContextItems();
            if (names == null || names.length != count || btList.size() != count) {
                return ret;
            }
            
            for (int index = 0; index < count; ++index) {
                ReaderItem item = new ReaderItem();
                item.description = names[index];
                item.image = btList.get(index);
                ret.add(item);
            }
        }

        return ret;
    }

    private ArrayList<Bitmap> loadListContextItems() {
        ArrayList<Bitmap> ret = new ArrayList<Bitmap>();
        try {
            String[] itemArrayOrg = getAssets().list("content");
            ArrayList<String> items = new ArrayList<String>();
            for(String item : itemArrayOrg) {
                if (!item.equals("item_name.ini")) {
                    items.add(item);
                }
            }
            String[] itemArray = new String[items.size()];
            items.toArray(itemArray);
            
            if (itemArray != null && itemArray.length > 0) {
                itemArray = sortByInt(itemArray);

                for (String item : itemArray) {
                    Bitmap itemBt = Utils.loadBitmapFromAsset(getApplicationContext(), "content/" + item + "/icon.jpg");

                    if (itemBt != null) {
                        try {
                            Bitmap roundBt = ImageUtils.createRoundedBitmap(itemBt);
                            if (roundBt != null) {
                                ret.add(roundBt);
                            }

                            itemBt.recycle();
                            itemBt = null;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ret;
    }

    private String[] sortByInt(String[] src) {
        String[] ret = new String[src.length];
        for (String data : src) {
            int index = Integer.valueOf(data);
            ret[index - 1] = String.valueOf(index);
        }

        return ret;
    }

}
