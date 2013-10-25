package com.cartoononline;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.album.mmall.R;

public class SettingManager {

    private static SettingManager gSettingManager;
    private Context mContext;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;
    
    private static final String USER_NAME = "u";
    private static final String PASSWORD = "p";

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
    
    public String getUserName() {
        return mSharedPreferences.getString(USER_NAME, null);
    }
    
    public void setUserName(String username) {
        mEditor.putString(USER_NAME, username);
        mEditor.commit();
    }
    
    public String getPassword() {
        return mSharedPreferences.getString(PASSWORD, null);
    }
    
    public void setPassword(String password) {
        mEditor.putString(PASSWORD, password);
        mEditor.commit();
    }

    public boolean getShowAdView() {
        if (Config.DEBUG) {
            return false;
        }
        
        return this.mSharedPreferences.getBoolean(mContext.getString(R.string.show_adview), true);
    }
    
    public void setShowAdView(boolean show) {
        mEditor.putBoolean(mContext.getString(R.string.show_adview), show);
        mEditor.commit();
    }
    
    public int getPrePoint() {
        return this.mSharedPreferences.getInt(mContext.getString(R.string.pre_point), 0);
    }
    
    public void setPrePoint(int point) {
        mEditor.putInt(mContext.getString(R.string.pre_point), point);
        mEditor.commit();
    }
    
    public int getPointInt() {
        if (Config.WALL_DEBUG) {
            return 200;
        }
        return mSharedPreferences.getInt(mContext.getString(R.string.offer_point), Config.BOOK_REVIEW ? 10 : Config.DEFAULT_POINT);
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
