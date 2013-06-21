package com.cartoononline;

import java.io.File;

import net.youmi.android.AdManager;
import net.youmi.android.offers.OffersManager;
import android.app.Application;

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

        Config.CURRENT_PACKAGE_NAME = Environment.getPackageName(this);
        if (Config.PACKAGE_CARTOON.equals(Config.CURRENT_PACKAGE_NAME)) {
            Config.INDEX = 0;
        } else if (Config.PACKAGE_XIEE.equals(Config.CURRENT_PACKAGE_NAME)) {
            Config.INDEX = 1;
        } else if (Config.PACKAGE_ROSI.equals(Config.CURRENT_PACKAGE_NAME)) {
            Config.INDEX = 2;
        } else if (Config.PACKAGE_BOOK.equals(Config.CURRENT_PACKAGE_NAME)) {
            Config.INDEX = 3;
            Config.BOOK_REVIEW = true;
        }

        UtilsConfig.init(getApplicationContext());
        SettingManager.getInstance().init(getApplicationContext());
        Option opt = new Option();
        CacheFactory.init(opt);

        initYoumi();
        initUMeng();
        
        File file = new File(Config.BOOK_DOWNLOAD_DIR);
        if (file.exists() && file.isFile()) {
            file.delete();
        }
        
        if (file.exists() && file.isDirectory()) {
            return;
        }
        
        file.mkdirs();
    }

    private void initYoumi() {
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
