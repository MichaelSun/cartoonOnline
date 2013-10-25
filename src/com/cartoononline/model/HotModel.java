package com.cartoononline.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;

import com.cartoononline.Config;
import com.cartoononline.SettingManager;
import com.cartoononline.api.HotSessionRequest;
import com.cartoononline.api.NewSessionResponse;
import com.cartoononline.api.NewSessionResponse.SessionItem;
import com.plugin.common.utils.DataModelBase;
import com.plugin.common.utils.UtilsConfig;
import com.plugin.database.dao.helper.DBTableAccessHelper;
import com.plugin.database.dao.helper.SyncDBTableAccessHelper;
import com.plugin.internet.InternetUtils;
import com.plugin.internet.core.RequestBase;

public class HotModel extends DataBaseInterface {

    private static HashMap<Integer, HotModel> gDMMap = new HashMap<Integer, HotModel>();

    public static HotModel getDownloadModelFactory(int category, Context context) {
        HotModel ret = gDMMap.get(category);
        if (ret == null) {
            ret = new HotModel(category);
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

    private DBTableAccessHelper<HotItemModel> mDownloadHelper;

    private boolean mDataChanged;

    private int mCategory;

    public HotModel(int category) {
        mCategory = category;
    }

    @Override
    public void init(Context context) {
        super.init(context);
        mCurPage = 0;
        mOnLoading = false;
        mDownloadHelper = new SyncDBTableAccessHelper<HotItemModel>(context, HotItemModel.class);
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

    public HotItemModel getItem(HotItemModel searchObj) {
        List<HotItemModel> ret = mDownloadHelper.queryItems("downloadUrlHashCode = ?",
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
        SettingManager.getInstance().setHasMore(true);
        mDataChanged = false;
    }

    public void deleteItemModel(HotItemModel item) {
        if (item != null) {
            mDataChanged = true;
            mDownloadHelper.delete(item);
        }
    }

    public void updateItemModel(HotItemModel item) {
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
                    RequestBase<NewSessionResponse> request = new HotSessionRequest(Config.DOMAIN_NAME[mCategory]);
                    NewSessionResponse response = InternetUtils.request(mContext, request);
                    UtilsConfig.LOGD("[[:::::::::]] response = " + response);

                    if (response != null) {
                        SettingManager.getInstance().setHasMore(response.hasmore);
                        if (response.items != null) {
                            HotItemModel[] saveData = new HotItemModel[response.items.length];
                            int timeIndex = 0;
                            for (int index = 0; index < saveData.length; ++index) {
                                SessionItem item = response.items[index];
                                HotItemModel ditem = new HotItemModel();
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

                            List<HotItemModel> old = mDownloadHelper.queryItems("category = ?", String.valueOf(mCategory));
                            if (old != null && old.size() > 0) {
                                HashMap<Integer, HotItemModel> map = new HashMap<Integer, HotItemModel>();
                                for (HotItemModel item : old) {
                                    map.put(item.downloadUrlHashCode, item);
                                }

                                for (HotItemModel iItem : saveData) {
                                    HotItemModel oldItem = map.get(iItem.downloadUrlHashCode);
                                    if (oldItem != null) {
                                        iItem.localFullPath = oldItem.localFullPath;
                                    }
                                }
                            }

                            List<HotItemModel> ret = new ArrayList<HotItemModel>();
                            if (mCurPage == 0) {
                                HotItemModel[] itemsDelete = new HotItemModel[old.size()];
                                old.toArray(itemsDelete);
                                mDownloadHelper.delete(itemsDelete);
//                                mDownloadHelper.deleteAll();
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
                List<HotItemModel> ret = mDownloadHelper.queryItems("category = ?", String.valueOf(mCategory));

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
