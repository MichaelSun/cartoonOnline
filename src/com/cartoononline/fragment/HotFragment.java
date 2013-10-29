package com.cartoononline.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.album.mmall1.R;
import com.cartoononline.Config;
import com.cartoononline.adapter.HotIAdapter;
import com.cartoononline.model.*;
import com.handmark.pulltorefresh.library.ILoadingLayout;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener2;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HotFragment extends Fragment implements FragmentStatusInterface {

    private Activity mActivity;

    private Context mContext;

    private LayoutInflater mLayoutInflater;

    private PullToRefreshListView mListView;

    private ListView mRealListView;

    private HotIAdapter mHotAdapter;

    private ILoadingLayout mILoadingLayout;

    private List<HotItemModel> mDownloadList = new ArrayList<HotItemModel>();

    private HotModel mDownloadModel;

    private TextView mEmptyTV;

    private Toast mToast;

    private boolean mIsFling;

    private static final int NOTIFY_DOWNLOAD_CHANGED = 10003;
    private static final int DISSMISS_PROGRESS = 10004;
    private static final int STOP_REFRESH = 10005;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case NOTIFY_DOWNLOAD_CHANGED:
                mListView.onRefreshComplete();
                if (mDownloadList != null && mDownloadList.size() > 0) {
                    mEmptyTV.setVisibility(View.GONE);
                }
                if (mHotAdapter == null) {
                    mHotAdapter = new HotIAdapter(mActivity, mDownloadList, mLayoutInflater);
                    if (mRealListView != null) {
                        mRealListView.setAdapter(mHotAdapter);
                    }
                } else {
                    mHotAdapter.setData(mDownloadList);
                }
                break;
            case DISSMISS_PROGRESS:
                mListView.onRefreshComplete();
                break;
            case STOP_REFRESH:
                mListView.onRefreshComplete();
                break;
            }
        }
    };

    public HotFragment() {
    }

    @Override
    public void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mActivity = getActivity();
        mContext = mActivity.getApplicationContext();

        mDownloadModel = HotModel.getDownloadModelFactory(Config.CURRENT_DOMAIN, mContext);
        
        mLayoutInflater = inflater;
        return makeHotView();
    }

    private View makeHotView() {
        View ret = mLayoutInflater.inflate(R.layout.hot_content, null);
        mListView = (PullToRefreshListView) ret.findViewById(R.id.pull_refresh_list);

        mListView.setOnRefreshListener(new OnRefreshListener2<ListView>() {

            @Override
            public void onPullDownToRefresh(PullToRefreshBase<ListView> refreshView) {
                loadDownloadDataServer(true);
            }

            @Override
            public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView) {
            }

        });

        mILoadingLayout = mListView.getLoadingLayoutProxy();
        mILoadingLayout.setLoadingDrawable(mContext.getResources().getDrawable(R.drawable.default_ptr_drawable));
        mILoadingLayout.setPullLabel(mContext.getString(R.string.pull_label));
        mILoadingLayout.setReleaseLabel(mContext.getString(R.string.release_label));
        mILoadingLayout.setLastUpdatedLabel(mContext.getString(R.string.pull_label1));

        mRealListView = mListView.getRefreshableView();
        mRealListView.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                switch (scrollState) {
                case SCROLL_STATE_FLING:
                case SCROLL_STATE_TOUCH_SCROLL:
                    mIsFling = true;
                    break;
                case SCROLL_STATE_IDLE:
                    mIsFling = false;
                    break;
                }

                if (mHotAdapter != null) {
                    mHotAdapter.setFlingState(mIsFling);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            }

        });

        this.mEmptyTV = (TextView) ret.findViewById(R.id.empty_tips);
        mEmptyTV.setVisibility(View.VISIBLE);
        asyncLoadDataLocal(false);

        return ret;
    }

    private void asyncLoadDataLocal(final boolean withForeLoad) {
        mDownloadModel.asyncLoadDataLocal(new DataBaseInterface.DataDownloadListener() {

            @Override
            public void onDataLoadSuccess(Object loadData) {
                if (loadData != null) {
                    List<HotItemModel> ret = (List<HotItemModel>) loadData;
                    checkDownloadItemStatus(ret);
                    mDownloadList.clear();
                    mDownloadList.addAll(ret);
                    mHandler.sendEmptyMessage(NOTIFY_DOWNLOAD_CHANGED);
                } else {
                    mDownloadList.clear();
                    mHandler.sendEmptyMessage(NOTIFY_DOWNLOAD_CHANGED);
                }

                if (withForeLoad && mDownloadList.size() == 0) {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            onForceRefresh();
                        }
                    }, 200);
                }
            }

            @Override
            public void onDataLoadFailed(Object errorData) {
                mHandler.sendEmptyMessage(DISSMISS_PROGRESS);
            }

        });
    }

    private void loadDownloadDataServer(final boolean repalceOld) {
        mDownloadModel = HotModel.getDownloadModelFactory(Config.CURRENT_DOMAIN, mContext);
        mDownloadModel.asyncLoadDataServer(new DataBaseInterface.DataDownloadListener() {

            @Override
            public void onDataLoadSuccess(Object loadData) {
                if (loadData != null) {
                    if (repalceOld) {
                        mDownloadList.clear();
                    }
                    mDownloadList.addAll((List<HotItemModel>) loadData);
                    checkDownloadItemStatus(mDownloadList);
                    mHandler.sendEmptyMessage(NOTIFY_DOWNLOAD_CHANGED);
                }
            }

            @Override
            public void onDataLoadFailed(Object errorData) {
                mHandler.sendEmptyMessage(DISSMISS_PROGRESS);
            }

        });
    }

    private void checkDownloadItemStatus(List<HotItemModel> data) {
        for (DownloadItemModel item : data) {
            if (!TextUtils.isEmpty(item.localFullPath)) {
                File localFile = new File(item.localFullPath);
                if (localFile.exists()) {
                    item.downloadStatus = DownloadItemModel.DOWNLOADED;
                } else {
                    item.downloadStatus = DownloadItemModel.UNDOWNLOAD;
                }
                // TODO: check if the file is unziped
            } else {
                item.downloadStatus = DownloadItemModel.UNDOWNLOAD;
            }
        }
    }

    private void onLoadMorePage() {
        mDownloadModel = HotModel.getDownloadModelFactory(Config.CURRENT_DOMAIN, mContext);
        mDownloadModel.increasePageNo();
        // mProgress.show();
        mDownloadModel.asyncLoadDataServer(new DataBaseInterface.DataDownloadListener() {

            @Override
            public void onDataLoadSuccess(Object loadData) {
                if (loadData != null) {
                    mDownloadList.clear();
                    mDownloadList.addAll((List<HotItemModel>) loadData);
                    checkDownloadItemStatus(mDownloadList);
                    mHandler.sendEmptyMessage(NOTIFY_DOWNLOAD_CHANGED);
                }
            }

            @Override
            public void onDataLoadFailed(Object errorData) {
                mHandler.sendEmptyMessage(DISSMISS_PROGRESS);
            }
        });
    }

    @Override
    public void onShow() {
        if (mDownloadList == null || mDownloadList.size() == 0 || mDownloadModel.isDataChanged()) {
            if (mDownloadList == null || mDownloadList.size() == 0) {
                mListView.setRefreshing();
            } else {
                asyncLoadDataLocal(true);
            }
        } else {
            if (mHotAdapter != null) {
                mHotAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onForceRefresh() {
        mListView.setRefreshing();
    }
    
    @Override
    public void onStopShow() {
        if (mHotAdapter != null) {
            mHotAdapter.onStop();
        }
    }

    @Override
    public void onClear() {
        if (mDownloadList != null) {
            mDownloadList.clear();
        }
    }

    @Override
    public void onDataSourceChanged() {
        if (mDownloadList != null) {
            mDownloadList.clear();
        }

        mDownloadModel = HotModel.getDownloadModelFactory(Config.CURRENT_DOMAIN, mContext);
        mRealListView.setSelection(0);
        asyncLoadDataLocal(true);
    }

    @Override
    public void onDestroyView() {
        Config.LOGD("[[HotFragment::onDestroyView]]");
        
        super.onDestroyView();
        if (mHotAdapter != null) {
            mHotAdapter.onDestroy();
        }
        mHotAdapter = null;
        mActivity = null;
    }

}
