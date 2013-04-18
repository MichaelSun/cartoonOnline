package com.cartoononline;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.baidu.android.pushservice.PushConstants;
import com.baidu.android.pushservice.PushManager;

public class PushServiceReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(PushConstants.ACTION_RECEIVER_NOTIFICATION_CLICK)) {
            Intent i = new Intent();
            i.setClass(context, CartoonSplashActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        } else if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            PushManager.startWork(context, PushConstants.LOGIN_TYPE_API_KEY, Utils.getMetaValue(context, "api_key"));
        }
    }

}
