package com.cartoononline;

import net.youmi.android.AdManager;
import net.youmi.android.offers.OffersManager;
import android.app.Application;

import com.plugin.common.utils.DeviceInfo;
import com.plugin.common.utils.SingleInstanceBase.SingleInstanceManager;
import com.plugin.common.utils.UtilsConfig;

public class CartoonApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        UtilsConfig.init(getApplicationContext());
        
        initYoumi();
    }
    
    private void initYoumi() {
        AdManager.getInstance(this.getApplicationContext()).init(AppConfig.YOUMI_APP_ID, AppConfig.YOUMI_APP_SECRET_KEY, true);
        OffersManager.getInstance(this.getApplicationContext()).onAppLaunch();
        SingleInstanceManager.getInstance().init(getApplicationContext());
    }
}
