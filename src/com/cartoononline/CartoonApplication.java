package com.cartoononline;

import java.util.ArrayList;
import java.util.List;

import net.youmi.android.AdManager;
import net.youmi.android.offers.OffersManager;
import android.app.Application;

import com.baidu.android.pushservice.PushConstants;
import com.baidu.android.pushservice.PushManager;
import com.plugin.common.cache.CacheFactory;
import com.plugin.common.cache.CacheFactory.Option;
import com.plugin.common.utils.Environment;
import com.plugin.common.utils.SingleInstanceBase.SingleInstanceManager;
import com.plugin.common.utils.UtilsConfig;
import com.umeng.analytics.MobclickAgent;

public class CartoonApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        String packageName = Environment.getPackageName(this);
        if (Config.PACKAGE_CARTOON.equals(packageName)) {
            Config.INDEX = 0;
        } else if (Config.PACKAGE_XIEE.equals(packageName)) {
            Config.INDEX = 1;
        }

        UtilsConfig.init(getApplicationContext());
        SettingManager.getInstance().init(getApplicationContext());
        Option opt = new Option();
        CacheFactory.init(opt);

        initYoumi();
        initUMeng();

        PushManager.startWork(getApplicationContext(), PushConstants.LOGIN_TYPE_API_KEY,
                Utils.getMetaValue(this, "api_key"));
        List<String> tags = new ArrayList<String>();
        tags.add(Config.PUSH_TAG[Config.INDEX]);
        PushManager.setTags(getApplicationContext(), tags);
    }

    private void initYoumi() {
        // YoumiAdManager.getInstance(this.getApplicationContext()).init(Config.YOUMI_APP_ID[Config.INDEX],
        // Config.YOUMI_APP_SECRET_KEY[Config.INDEX], false);
        AdManager.getInstance(this).init(Config.YOUMI_APP_ID[Config.INDEX], Config.YOUMI_APP_SECRET_KEY[Config.INDEX],
                false);
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
