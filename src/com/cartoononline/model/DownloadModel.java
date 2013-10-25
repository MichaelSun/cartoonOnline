package com.cartoononline.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;

import com.cartoononline.Config;
import com.cartoononline.SettingManager;
import com.cartoononline.api.NewSessionRequest1;
import com.cartoononline.api.NewSessionResponse;
import com.cartoononline.api.NewSessionResponse.SessionItem;
import com.plugin.common.utils.DataModelBase;
import com.plugin.common.utils.UtilsConfig;
import com.plugin.database.dao.helper.DBTableAccessHelper;
import com.plugin.database.dao.helper.SyncDBTableAccessHelper;
import com.plugin.internet.InternetUtils;
import com.plugin.internet.core.RequestBase;

public class DownloadModel extends DataBaseInterface {

    private static HashMap<Integer, DownloadModel> gDMMap = new HashMap<Integer, DownloadModel>();

    public static DownloadModel getDownloadModelFactory(int category, Context context) {
        DownloadModel ret = gDMMap.get(category);
        if (ret == null) {
            ret = new DownloadModel(category);
            ret.init(context);
            gDMMap.put(category, ret);
        }

        return ret;
    }

    public static void removeDownloadModel(int category) {
        gDMMap.remove(category);
    }

    public static void clearAllDwonloadModel() {
        gDMMap.clear();
    }

    private int mCurPage;

    private boolean mOnLoading;

    private DBTableAccessHelper<DownloadItemModel> mDownloadHelper;

    private boolean mDataChanged;

    private int mCategory;

    public DownloadModel(int category) {
        mCategory = category;
    }

    @Override
    public void init(Context context) {
        super.init(context);
        mCurPage = 0;
        mOnLoading = false;
        mDownloadHelper = new SyncDBTableAccessHelper<DownloadItemModel>(context, DownloadItemModel.class);
        mDataChanged = false;
    }

    public int getCategory() {
        return mCategory;
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
        List<DownloadItemModel> ret = mDownloadHelper.queryItems("downloadUrlHashCode = ?",
                String.valueOf(searchObj.downloadUrlHashCode));
        if (ret != null && ret.size() > 0) {
            return ret.get(0);
        }

        return null;
    }

    public void increasePageNo() {
        mCurPage++;
    }

    public void resetPageNo() {
        mCurPage = 0;
        SettingManager.getInstance().setHasMore(false);
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
                    RequestBase<NewSessionResponse> request = new NewSessionRequest1(mCurPage, 20, Config.DOMAIN_NAME[mCategory]);
                    NewSessionResponse response = InternetUtils.request(mContext, request);
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
                                ditem.time = item.time;
                                ditem.downloadCount = item.count;
                                ditem.category = mCategory;
                                saveData[index] = ditem;

                                timeIndex++;
                            }

                            List<DownloadItemModel> old = mDownloadHelper.queryItems("category = ?", String.valueOf(mCategory));
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
                            if (mCurPage == 0 && old != null) {
//                                mDownloadHelper.deleteAll();
                                DownloadItemModel[] itemsDelete = new DownloadItemModel[old.size()];
                                old.toArray(itemsDelete);
                                mDownloadHelper.delete(itemsDelete);
                            }
                            mDownloadHelper.blukInsertOrReplace(saveData);
                            ret = mDownloadHelper.queryItems("category = ?", String.valueOf(mCategory));

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
                List<DownloadItemModel> ret = mDownloadHelper.queryItems("category = ?", String.valueOf(mCategory));

                mDataChanged = false;
                if (l != null) {
                    l.onDataLoadSuccess(ret);
                }
            }
        });
    }

    @Override
    public void clearLocalData() {
        mDownloadHelper.delete("category = ?", String.valueOf(mCategory));
        setDataChanged(true);
        resetPageNo();
    }

}
