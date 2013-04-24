package com.cartoononline.fragment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.TextView;

import com.cartoononline.R;
import com.cartoononline.adapter.DownloadItemAdapter;
import com.cartoononline.model.DownloadItemModel;
import com.cartoononline.model.DownloadModel;
import com.plugin.common.utils.DataModelBase.DataDownloadListener;
import com.plugin.common.utils.SingleInstanceBase;

public class DownloadFragment extends Fragment implements FragmentStatusInterface {
    
    private GridView mDownloadGridView;
    
    private LayoutInflater mLayoutInflater;
    
    private DownloadModel mDownloadModel;

    private DownloadItemAdapter mDownlaodListAdapter;
    
    private List<DownloadItemModel> mDownloadList = new ArrayList<DownloadItemModel>();
    
    private Activity mActivity;
    
    private Context mContext;
    
    private ProgressDialog mProgress;
    
    private TextView mFooterView;
    
    private static final int NOTIFY_DOWNLOAD_CHANGED = 10003;
    private static final int DISSMISS_PROGRESS = 10004;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case NOTIFY_DOWNLOAD_CHANGED:
                if (mDownlaodListAdapter == null) {
                    mDownlaodListAdapter = new DownloadItemAdapter(mActivity, mDownloadList,
                            mLayoutInflater);
                    if (mDownloadGridView != null) {
                        mDownloadGridView.setAdapter(mDownlaodListAdapter);
                    }
                } else {
                    mDownlaodListAdapter.setData(mDownloadList);
                }
                if (mFooterView != null) {
                    if (mDownloadModel.hasMore()) {
                        mFooterView.setText(R.string.more_tips);
                        mFooterView.setOnClickListener(mLoadMoreListener);
                    } else {
                        mFooterView.setText(R.string.no_more_tips);
                        mFooterView.setOnClickListener(null);
                    }
                }
                if (mProgress != null) {
                    mProgress.dismiss();
                }
                break;
            case DISSMISS_PROGRESS:
                if (mProgress != null) {
                    mProgress.dismiss();
                }
                break;
            }
        }
    };
    
    public DownloadFragment() {
    }
    
    public DownloadFragment(Activity a) {
        mActivity = a;
        mContext = a.getApplicationContext();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDownloadModel = SingleInstanceBase.getInstance(DownloadModel.class);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        initProgressBar();
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
                mHandler.sendEmptyMessage(DISSMISS_PROGRESS);
            }

        });
    }
    
    private View makeDownloadView(LayoutInflater layoutInflater) {
        View ret = layoutInflater.inflate(R.layout.download_view, null);
        mDownloadGridView = (GridView) ret.findViewById(R.id.gridView);
        mFooterView = (TextView) ret.findViewById(R.id.info);
        asyncLoadDataLocal();

        return ret;
    }
    
    private void initProgressBar() {
        if (mProgress == null) {
            mProgress = new ProgressDialog(mActivity);
            mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgress.setMessage("正在加载中，请稍后...");
            mProgress.setCanceledOnTouchOutside(false);
        }
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
            mDownloadModel.increasePageNo();
            mProgress.show();
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
    };

    @Override
    public void onShow() {
        if (mDownloadList == null || mDownloadList.size() == 0 || mDownloadModel.isDataChanged()) {
            if (mDownloadList == null || mDownloadList.size() == 0) {
                //on local data
                loadDownloadDataServer(true);
            } else {
                asyncLoadDataLocal();
            }
        }        
    }

    @Override
    public void onForceRefresh() {
        loadDownloadDataServer(true);        
    }
}
