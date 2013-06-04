package com.cartoononline.fragment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.album.leg.R;
import com.cartoononline.adapter.DownloadItemAdapter;
import com.cartoononline.model.DownloadItemModel;
import com.cartoononline.model.DownloadModel;
import com.handmark.pulltorefresh.library.ILoadingLayout;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener2;
import com.handmark.pulltorefresh.library.PullToRefreshGridView;
import com.plugin.common.utils.DataModelBase.DataDownloadListener;
import com.plugin.common.utils.SingleInstanceBase;

public class DownloadFragment extends Fragment implements FragmentStatusInterface {
    
    private GridView mDownloadGridView;
    
    private TextView mEmptyTV;
    
    private LayoutInflater mLayoutInflater;
    
    private DownloadModel mDownloadModel;

    private DownloadItemAdapter mDownlaodListAdapter;
    
    private List<DownloadItemModel> mDownloadList = new ArrayList<DownloadItemModel>();
    
    private PullToRefreshGridView mPullRefreshGridView;
    
    private Activity mActivity;
    
    private Context mContext;
    
//    private ProgressDialog mProgress;
    
    private ILoadingLayout mILoadingLayout;
    
    private Toast mToast;

    private boolean mIsFling;
    
    private static final int NOTIFY_DOWNLOAD_CHANGED = 10003;
    private static final int DISSMISS_PROGRESS = 10004;
    private static final int STOP_REFRESH = 10005;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case NOTIFY_DOWNLOAD_CHANGED:
                mPullRefreshGridView.onRefreshComplete();
                if (mDownloadList != null && mDownloadList.size() > 0) {
                    mEmptyTV.setVisibility(View.GONE);
                }
                if (mDownlaodListAdapter == null) {
                    mDownlaodListAdapter = new DownloadItemAdapter(mActivity, mDownloadList,
                            mLayoutInflater);
                    if (mDownloadGridView != null) {
                        mDownloadGridView.setAdapter(mDownlaodListAdapter);
                    }
                } else {
                    mDownlaodListAdapter.setData(mDownloadList);
                }
                break;
            case DISSMISS_PROGRESS:
                mPullRefreshGridView.onRefreshComplete();
                break;
            case STOP_REFRESH:
                mPullRefreshGridView.onRefreshComplete();
                break;
            }
        }
    };
    
    public DownloadFragment() {
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mActivity = getActivity();
        mContext = mActivity.getApplicationContext();
        
        //init toast
        mToast = Toast.makeText(mContext, R.string.tips_no_more, Toast.LENGTH_LONG);
        mToast.setText(R.string.tips_no_more);
        mToast.setDuration(Toast.LENGTH_LONG);

        mDownloadModel = SingleInstanceBase.getInstance(DownloadModel.class);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        initProgressBar();
        mLayoutInflater = inflater;
        return makeDownloadView(mLayoutInflater);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mDownlaodListAdapter = null;
        mActivity = null;
    }
    
    private void loadDownloadDataServer(final boolean repalceOld) {
//        mProgress.show();
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
                mHandler.sendEmptyMessage(DISSMISS_PROGRESS);
            }

        });
    }
    
    private View makeDownloadView(LayoutInflater layoutInflater) {
        View ret = layoutInflater.inflate(R.layout.download_view, null);
        
        mPullRefreshGridView = (PullToRefreshGridView) ret.findViewById(R.id.pull_refresh_grid);
        mPullRefreshGridView.setScrollingWhileRefreshingEnabled(true);
        this.mEmptyTV = (TextView) ret.findViewById(R.id.empty_tips);
        mDownloadGridView = mPullRefreshGridView.getRefreshableView();
        mDownloadGridView.setOnScrollListener(new AbsListView.OnScrollListener() {

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
                
                if (mDownlaodListAdapter != null) {
                    mDownlaodListAdapter.setFlingState(mIsFling);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
            
        });
        
        mILoadingLayout = mPullRefreshGridView.getLoadingLayoutProxy();
        mILoadingLayout.setLoadingDrawable(mContext.getResources().getDrawable(R.drawable.default_ptr_drawable));
        mILoadingLayout.setPullLabel(mContext.getString(R.string.pull_label));
        mILoadingLayout.setReleaseLabel(mContext.getString(R.string.release_label));
        mILoadingLayout.setLastUpdatedLabel(mContext.getString(R.string.pull_label1));
        
        mPullRefreshGridView.setOnRefreshListener(new OnRefreshListener2<GridView>() {

            @Override
            public void onPullDownToRefresh(PullToRefreshBase<GridView> refreshView) {
                loadDownloadDataServer(true);
            }

            @Override
            public void onPullUpToRefresh(PullToRefreshBase<GridView> refreshView) {
                if (mDownloadModel.hasMore()) {
                    onLoadMorePage();
                } else {
                    mHandler.sendEmptyMessage(STOP_REFRESH);
                    if (mToast != null) {
                        mToast.show();
                    }
                }
            }
            
        });
        
        mEmptyTV.setVisibility(View.VISIBLE);
        asyncLoadDataLocal();

        return ret;
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
    
    private View.OnClickListener mLoadMoreListener = new View.OnClickListener() {
        
        @Override
        public void onClick(View v) {
            onLoadMorePage();
        }
    };
    
    private void onLoadMorePage() {
        mDownloadModel.increasePageNo();
//        mProgress.show();
        mDownloadModel.asyncLoadDataServer(new DataDownloadListener() {

            @Override
            public void onDataLoadSuccess(Object loadData) {
                if (loadData != null) {
                    mDownloadList.clear();
                    mDownloadList.addAll((List<DownloadItemModel>) loadData);
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
                //on local data
//                loadDownloadDataServer(true);
                mPullRefreshGridView.setRefreshing();
            } else {
                asyncLoadDataLocal();
            }
        }        
    }

    @Override
    public void onForceRefresh() {
//        loadDownloadDataServer(true);        
        mPullRefreshGridView.setRefreshing();
    }
}
