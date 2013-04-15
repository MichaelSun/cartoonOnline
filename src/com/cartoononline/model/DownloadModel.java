package com.cartoononline.model;

import java.util.HashMap;
import java.util.List;

import android.content.Context;

import com.cartoononline.api.NewSessionRequest;
import com.cartoononline.api.NewSessionResponse;
import com.cartoononline.api.NewSessionResponse.SessionItem;
import com.plugin.common.utils.DataModelBase;
import com.plugin.common.utils.UtilsConfig;
import com.plugin.database.dao.helper.DBTableAccessHelper;
import com.plugin.internet.InternetUtils;

public class DownloadModel extends DataModelBase {
    
    private int mCurPage;
    
    private boolean mOnLoading;
    
    private boolean mHasMore;
    
    private DBTableAccessHelper<DownloadItemModel> mDownloadHelper;
    
    @Override
    protected void init(Context context) {
        super.init(context);
        mCurPage = 0;
        mOnLoading = false;
        mDownloadHelper = new DBTableAccessHelper<DownloadItemModel>(context, DownloadItemModel.class);
        mHasMore = true;
    }

    public boolean hasMore() {
        return mHasMore;
    }
    
    public DownloadItemModel getItem(DownloadItemModel searchObj) {
        return mDownloadHelper.queryItem(searchObj);
    }
    
    public void increasePageNo() {
        mCurPage++;
    }
    
    public void resetPageNo() {
        mCurPage = 0;
        mHasMore = true;
    }
    
    public void deleteItemModel(DownloadItemModel item) {
        if (item != null) {
            mDownloadHelper.delete(item);
        }
    }
    
    public void updateItemModel(DownloadItemModel item) {
        if (item != null) {
            mDownloadHelper.update(item);
        }
    }
    
    @Override
    public void asyncLoadDataServer(final DataDownloadListener l) {
        if (mOnLoading) {
            return;
        }
        mOnLoading = true;
        asyncWork(new Runnable() {
            @Override
            public void run() {
                try {
                    NewSessionResponse response = InternetUtils.request(mContext, new NewSessionRequest(mCurPage));
                    UtilsConfig.LOGD("[[:::::::::]] response = " + response);
                    
                    if (response != null) {
                        mHasMore = response.hasmore;                 
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

                            List<DownloadItemModel> old = mDownloadHelper.queryItems();
                            if (old != null && old.size() > 0) {
                                HashMap<Integer, DownloadItemModel> map = new HashMap<Integer, DownloadItemModel>();
                                for (DownloadItemModel item : old) {
                                    map.put(item.downloadUrlHashCode, item);
                                }
                                
                                for (DownloadItemModel iItem : saveData) {
                                    DownloadItemModel oldItem = map.get(iItem.downloadUrlHashCode);
                                    if (oldItem != null) {
                                        iItem.localFullPath = oldItem.localFullPath;
                                    }
                                }
                            }
                            
                            mDownloadHelper.blukInsertOrReplace(saveData);
                            List<DownloadItemModel> ret = mDownloadHelper.queryItems();
                            UtilsConfig.LOGD("[[:::::::::]] after replace code, lit  = " + ret);
                            
                            if (l != null) {
                                l.onDataLoadSuccess(ret);
                            }
                        }
                        
                        mOnLoading = false;
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                mOnLoading = false;
                if (l != null) {
                    l.onDataLoadFailed(mCurPage);
                }
            }
        });
    }

    @Override
    public void asyncLoadDataLocal(final DataDownloadListener l) {
        asyncWork(new Runnable() {
            @Override
            public void run() {
                List<DownloadItemModel> ret = mDownloadHelper.queryItems();
                
                if (l != null) {
                    l.onDataLoadSuccess(ret);
                }
            }
        });
    }
    
}
