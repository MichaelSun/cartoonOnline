package com.cartoononline.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;

import com.cartoononline.SettingManager;
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
    
    private DBTableAccessHelper<DownloadItemModel> mDownloadHelper;
    
    private boolean mDataChanged;
    
    @Override
    protected void init(Context context) {
        super.init(context);
        mCurPage = 0;
        mOnLoading = false;
        mDownloadHelper = new DBTableAccessHelper<DownloadItemModel>(context, DownloadItemModel.class);
        mDataChanged = false;
    }
    
    public boolean isDataChanged() {
        return mDataChanged;
    }
    
    public void setDataChanged(boolean changed) {
        mDataChanged = changed;
    }

    public boolean hasMore() {
        return SettingManager.getInstance().getHasMore();
    }
    
    public DownloadItemModel getItem(DownloadItemModel searchObj) {
//        return mDownloadHelper.queryItem(searchObj);
        List<DownloadItemModel> ret = mDownloadHelper.queryItems("downloadUrlHashCode = ?", String.valueOf(searchObj.downloadUrlHashCode));
        if (ret != null && ret .size() > 0) {
            return ret.get(0);
        }
        
        return null;
    }
    
    public void increasePageNo() {
        mCurPage++;
    }
    
    public void resetPageNo() {
        mCurPage = 0;
        SettingManager.getInstance().setHasMore(true);
        mDataChanged = false;
    }
    
    public void deleteItemModel(DownloadItemModel item) {
        if (item != null) {
            mDataChanged = true;
            mDownloadHelper.delete(item);
        }
    }
    
    public void updateItemModel(DownloadItemModel item) {
        if (item != null) {
            mDataChanged = true;
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
                    NewSessionResponse response = InternetUtils.request(mContext, new NewSessionRequest(mCurPage, 20));
                    UtilsConfig.LOGD("[[:::::::::]] response = " + response);
                    
                    if (response != null) {
                        SettingManager.getInstance().setHasMore(response.hasmore);
                        if (response.items != null) {
                            DownloadItemModel[] saveData = new DownloadItemModel[response.items.length];
                            int timeIndex = 0;
                            for (int index = 0; index < saveData.length; ++index) {
                                SessionItem item = response.items[index];
                                DownloadItemModel ditem = new DownloadItemModel();
                                ditem.coverUrl = item.imageUrl;
                                ditem.description = item.description;
                                ditem.downloadUrl = item.downloadUrl;
                                ditem.downloadUrlHashCode = item.downloadUrl.hashCode();
                                ditem.sessionName = item.name;
                                ditem.size = item.size;
                                ditem.downloadTime = System.currentTimeMillis() + timeIndex;
                                saveData[index] = ditem;
                                
                                timeIndex++;
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
                            
                            List<DownloadItemModel> ret = new ArrayList<DownloadItemModel>();
                            if (mCurPage == 0) {
                                mDownloadHelper.deleteAll();
                            }
                            mDownloadHelper.blukInsertOrReplace(saveData);
                            ret = mDownloadHelper.queryItems();
                            
                            UtilsConfig.LOGD("[[:::::::::]] after replace code, lit  = " + ret);
                            
                            if (l != null) {
                                l.onDataLoadSuccess(ret);
                            }
                        }
                        
                        mOnLoading = false;
                        mDataChanged = false;
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                mOnLoading = false;
                mDataChanged = false;
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
                
                mDataChanged = false;
                if (l != null) {
                    l.onDataLoadSuccess(ret);
                }
            }
        });
    }
    
}
