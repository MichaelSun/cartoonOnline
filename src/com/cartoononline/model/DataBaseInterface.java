package com.cartoononline.model;

import android.content.Context;
import com.plugin.common.utils.CustomThreadPool;

/**
 * Created with IntelliJ IDEA.
 * User: michael
 * Date: 13-10-23
 * Time: PM10:52
 * To change this template use File | Settings | File Templates.
 */
public abstract class DataBaseInterface {

    public interface DataDownloadListener {

        void onDataLoadSuccess(Object loadData);

        void onDataLoadFailed(Object errorData);
    }

    protected Context mContext;

    protected void asyncWork(Runnable run) {
        CustomThreadPool.getInstance().excute(new CustomThreadPool.TaskWrapper(run));
    }

    public void init(Context context) {
        mContext = context;
    }

    public abstract void asyncLoadDataServer(DataDownloadListener l);

    public abstract void asyncLoadDataLocal(DataDownloadListener l);

    public abstract void clearLocalData();
}
