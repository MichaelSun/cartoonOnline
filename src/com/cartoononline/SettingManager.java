package com.cartoononline;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SettingManager {

    private static SettingManager gSettingManager;
    private Context mContext;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;

    public static synchronized SettingManager getInstance() {
        if (gSettingManager == null) {
            gSettingManager = new SettingManager();
        }
        return gSettingManager;
    }

    public void init(Context context) {
        if (context != null) {
            mContext = context.getApplicationContext();
        }
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        mEditor = mSharedPreferences.edit();
    }

    public void setHasMore(boolean hasMore) {
        mEditor.putBoolean(mContext.getString(R.string.has_more), hasMore);
        mEditor.commit();
    }

    public boolean getHasMore() {
        return mSharedPreferences.getBoolean(mContext.getString(R.string.has_more), false);
    }

    public void setVersion(int version) {
        mEditor.putInt(mContext.getString(R.string.pre_version), version);
        mEditor.commit();
    }

    public int getPreVersion() {
        return mSharedPreferences.getInt(mContext.getString(R.string.pre_version), 0);
    }
}
