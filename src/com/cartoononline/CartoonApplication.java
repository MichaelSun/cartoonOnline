package com.cartoononline;

import com.plugin.common.utils.DeviceInfo;
import com.plugin.common.utils.UtilsConfig;
import com.plugin.common.utils.SingleInstanceBase.SingleInstanceManager;

import android.app.Application;

public class CartoonApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        SingleInstanceManager.getInstance().init(this.getApplicationContext());
        UtilsConfig.DEVICE_INFO = new DeviceInfo(this);
    }
}
