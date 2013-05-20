package com.cartoononline;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.michael.manhua.R;

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

    public int getPointInt() {
        if (Config.WALL_DEBUG) {
            return 0;
        }
        return mSharedPreferences.getInt(mContext.getString(R.string.offer_point), 10);
    }
    
    public void setPointInt(int point) {
        mEditor.putInt(mContext.getString(R.string.offer_point), point);
        mEditor.commit();
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
