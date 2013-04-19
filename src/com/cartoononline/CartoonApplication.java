package com.cartoononline;

import net.youmi.android.AdManager;
import net.youmi.android.offers.OffersManager;
import android.app.Application;

import com.baidu.android.pushservice.PushConstants;
import com.baidu.android.pushservice.PushManager;
import com.plugin.common.cache.CacheFactory;
import com.plugin.common.cache.CacheFactory.Option;
import com.plugin.common.utils.SingleInstanceBase.SingleInstanceManager;
import com.plugin.common.utils.UtilsConfig;
import com.umeng.analytics.MobclickAgent;

public class CartoonApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        UtilsConfig.init(getApplicationContext());
        SettingManager.getInstance().init(getApplicationContext());
        Option opt = new Option();
        CacheFactory.init(opt);

        initYoumi();
        initUMeng();

        PushManager.startWork(getApplicationContext(), PushConstants.LOGIN_TYPE_API_KEY,
                Utils.getMetaValue(this, "api_key"));
    }

    private void initYoumi() {
        AdManager.getInstance(this.getApplicationContext()).init(AppConfig.YOUMI_APP_ID,
                AppConfig.YOUMI_APP_SECRET_KEY, false);
        OffersManager.getInstance(this.getApplicationContext()).onAppLaunch();
        SingleInstanceManager.getInstance().init(getApplicationContext());
    }

    private void initUMeng() {
        MobclickAgent.setSessionContinueMillis(60 * 1000);
        MobclickAgent.setDebugMode(false);
        com.umeng.common.Log.LOG = false;

        MobclickAgent.updateOnlineConfig(this);

        MobclickAgent.onError(this);
    }
}
