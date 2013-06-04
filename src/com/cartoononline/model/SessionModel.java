package com.cartoononline.model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;

import com.plugin.common.cache.CacheFactory;
import com.plugin.common.cache.ICacheManager;
import com.plugin.common.utils.DataModelBase;
import com.plugin.common.utils.UtilsConfig;
import com.plugin.common.utils.image.ImageUtils;
import com.plugin.database.dao.helper.DBTableAccessHelper;

public class SessionModel extends DataModelBase {

    private DBTableAccessHelper<SessionReadModel> mHelper;

    private boolean mDataChanged = true;

    private ICacheManager<Bitmap> mCacheManager;

    private HashMap<Integer, SessionReadModel> mSessionMapData = new HashMap<Integer, SessionReadModel>();

    @Override
    public void init(Context context) {
        super.init(context);
        mHelper = new DBTableAccessHelper<SessionReadModel>(context, SessionReadModel.class);
        mCacheManager = CacheFactory.getCacheManager(CacheFactory.TYPE_CACHE.TYPE_IMAGE);
    }

    public boolean isDataChanged() {
        return mDataChanged;
    }

    public void insertOrRelace(SessionReadModel item) {
        mHelper.insertOrReplace(item);
        mDataChanged = true;

        if (item != null && !TextUtils.isEmpty(item.srcURI)) {
            int downloadHashCode = item.srcURI.hashCode();
            int findKey = 0;
            for (Integer key : mSessionMapData.keySet()) {
                SessionReadModel data = mSessionMapData.get(key);
                int downloadCode = data.srcURI.hashCode();
                if (key != null && downloadCode == downloadHashCode) {
                    findKey = key;
                    break;
                }
            }
            if (findKey != 0) {
                mSessionMapData.remove(findKey);
            }
            mSessionMapData.put(downloadHashCode, item);
        }
    }

    public void updateItem(SessionReadModel item) {
        mHelper.update(item);
        mDataChanged = true;

        if (item != null && !TextUtils.isEmpty(item.srcURI)) {
            int downloadHashCode = item.srcURI.hashCode();
            int findKey = 0;
            for (Integer key : mSessionMapData.keySet()) {
                SessionReadModel data = mSessionMapData.get(key);
                int downloadCode = data.srcURI.hashCode();
                if (key != null && downloadCode == downloadHashCode) {
                    findKey = key;
                    break;
                }
            }
            if (findKey != 0) {
                mSessionMapData.remove(findKey);
                mSessionMapData.put(downloadHashCode, item);
            }
        }
    }

    public void deleteItem(SessionReadModel item) {
        mHelper.delete(item);
        mDataChanged = true;

        if (item != null && !TextUtils.isEmpty(item.srcURI)) {
            int downloadHashCode = item.srcURI.hashCode();
            int findKey = 0;
            for (Integer key : mSessionMapData.keySet()) {
                SessionReadModel data = mSessionMapData.get(key);
                int downloadCode = data.srcURI.hashCode();
                if (key != null && downloadCode == downloadHashCode) {
                    findKey = key;
                    break;
                }
            }
            if (findKey != 0) {
                mSessionMapData.remove(findKey);
            }
        }
    }

    @Override
    public void asyncLoadDataServer(DataDownloadListener l) {

    }

    public List<SessionReadModel> syncLoadDataLocal() {
        List<SessionReadModel> ret = mHelper.queryItems();
        if (ret != null) {
            mSessionMapData.clear();
            for (SessionReadModel data : ret) {
                mSessionMapData.put(data.srcURI.hashCode(), data);
            }
        }

        return ret;
    }

    public SessionReadModel syncQueryDataLocalBy(int srcIdHashCode) {
        if (mSessionMapData != null) {
            for (SessionReadModel data : mSessionMapData.values()) {
                if (data != null && data.srcURI.hashCode() == srcIdHashCode) {
                    return data;
                }
            }
        }

        return null;
    }

    @Override
    public void asyncLoadDataLocal(final DataDownloadListener l) {
        this.asyncWork(new Runnable() {
            @Override
            public void run() {
                List<SessionReadModel> ret = new ArrayList<SessionReadModel>();
                List<SessionReadModel> lists = mHelper.queryItems();
                if (lists != null) {
                    for (SessionReadModel m : lists) {
                        File localFile = new File(m.localFullPath);
                        if (localFile.exists() && localFile.isDirectory()) {
                            Bitmap bt = mCacheManager.getResource(UtilsConfig.IMAGE_CACHE_CATEGORY_RAW, m.coverPath);
                            if (bt == null) {
                                m.coverBt = ImageUtils.loadBitmapWithSizeCheck(new File(m.coverPath));
                                mCacheManager.putResource(UtilsConfig.IMAGE_CACHE_CATEGORY_RAW, m.coverPath, m.coverBt);
                                m.coverBt = null;
                            }
                            ret.add(m);
                        }
                    }
                }

                mSessionMapData.clear();
                for (SessionReadModel data : lists) {
                    mSessionMapData.put(data.srcURI.hashCode(), data);
                }

                if (l != null) {
                    l.onDataLoadSuccess(ret);
                }
                mDataChanged = false;
            }
        });
    }

}
