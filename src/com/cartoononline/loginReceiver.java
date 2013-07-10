package com.cartoononline;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

public class loginReceiver extends BroadcastReceiver {

    private static final String ACCOUNT_LOGIN = "com.jifenbao.account.login";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(ACCOUNT_LOGIN)) {
                String userName = intent.getStringExtra("u");
                String password = intent.getStringExtra("p");
                SettingManager.getInstance().init(context);
                if (!TextUtils.isEmpty(userName) && !TextUtils.isEmpty(password)
                        && (TextUtils.isEmpty(SettingManager.getInstance().getUserName())
                                || TextUtils.isEmpty(SettingManager.getInstance().getPassword()))) {
                    SettingManager.getInstance().setUserName(userName);
                    SettingManager.getInstance().setPassword(password);
                }
            }
        }
    }

}
