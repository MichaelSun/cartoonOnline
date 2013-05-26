package com.cartoononline;

import java.util.HashMap;

import com.plugin.common.utils.Environment;
import com.plugin.common.utils.UtilsConfig;
import com.umeng.analytics.MobclickAgent;

import android.content.Context;
import net.youmi.android.offers.EarnPointsOrderList;
import net.youmi.android.offers.PointsReceiver;

public class MyPointsReceiver extends PointsReceiver {

    @Override
    protected void onEarnPoints(Context context, EarnPointsOrderList appList) {
        HashMap<String, String> extra = new HashMap<String, String>();
        if (appList != null) {
            extra.put("downloadName", appList.getCurrencyName());
        } else {
            extra.put("downloadName", "Null");
        }
        extra.put("packageName", Environment.getPackageName(context));
        MobclickAgent.onEvent(context, Config.CURRENT_POINT, extra);
        MobclickAgent.flush(context);
        
        if (Config.DEBUG) {
            UtilsConfig.LOGD("[[onEarnPoints]] downloadName : " + appList.getCurrencyName() + 
                    " packageName : " + Environment.getPackageName(context));
        }
    }

    @Override
    protected void onViewPoints(Context arg0) {
    }

}
