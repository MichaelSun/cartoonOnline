package com.plugin.common.utils;

import com.plugin.common.utils.CustomThreadPool.TaskWrapper;

import android.content.Context;

public abstract class DataModelBase extends SingleInstanceBase {
    
    public interface DataDownloadListener {
        
        void onDataLoadSuccess(Object loadData);
        
        void onDataLoadFailed(Object errorData);
    }

    protected Context mContext;
    
    @Override
    protected void init(Context context) {
        mContext = context;
    }
    
    protected void asyncWork(Runnable run) {
        CustomThreadPool.getInstance().excute(new TaskWrapper(run));
    }
    
    public abstract void asyncLoadDataServer(DataDownloadListener l);
    
    public abstract void asyncLoadDataLocal(DataDownloadListener l);

}
