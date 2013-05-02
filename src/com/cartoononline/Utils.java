package com.cartoononline;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;

import com.plugin.common.utils.UtilsConfig;
import com.plugin.common.utils.CustomThreadPool;
import com.plugin.common.utils.CustomThreadPool.TaskWrapper;
import com.plugin.common.utils.INIFile;
import com.plugin.common.utils.files.FileOperatorHelper;
import com.plugin.common.utils.zip.ZipUtil;

public class Utils {

    private static final boolean DEBUG = Config.DEBUG;

    private static final String SESSION_KEY = "infos";

    private static Object readKey(Context context, String keyName) {
        try {
            ApplicationInfo appi = context.getPackageManager().getApplicationInfo(context.getPackageName(),
                    PackageManager.GET_META_DATA);
            Bundle bundle = appi.metaData;
            Object value = bundle.get(keyName);
            return value;
        } catch (NameNotFoundException e) {
            return null;
        }
    }

    public static int getInt(Context context, String keyName) {
        return (Integer) readKey(context, keyName);
    }

    public static String getString(Context context, String keyName) {
        return (String) readKey(context, keyName);
    }

    public static Boolean getBoolean(Context context, String keyName) {
        return (Boolean) readKey(context, keyName);
    }

    public static Object get(Context context, String keyName) {
        return readKey(context, keyName);
    }

    public static String getMetaValue(Context context, String metaKey) {
        Bundle metaData = null;
        String apiKey = null;
        if (context == null || metaKey == null) {
            return null;
        }
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(),
                    PackageManager.GET_META_DATA);
            if (null != ai) {
                metaData = ai.metaData;
            }
            if (null != metaData) {
                apiKey = metaData.getString(metaKey);
            }
        } catch (NameNotFoundException e) {

        }
        return apiKey;
    }

    // public static final void asyncUnzipInternalSessions(final Context
    // context, final Handler mHandler) {
    // CustomThreadPool.getInstance().excute(new TaskWrapper(new Runnable() {
    //
    // @Override
    // public void run() {
    // try {
    // InputStream is = context.getAssets().open("session1.zip");
    // Utils.unzipInputToTarget(is, AppConfig.ROOT_DIR);
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    //
    // try {
    // Thread.sleep(2000);
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    //
    // mHandler.sendEmptyMessage(CartoonSplashActivity.REFRESH_READER_LIST);
    // }
    //
    // }));
    // }

    public static final boolean syncUnzipInternalSessions(Context context, String filename) {
        try {
            if (DEBUG) {
                UtilsConfig.LOGD("[[syncUnzipInternalSessions]] filename = " + filename);
            }

            InputStream is = context.getAssets().open(filename);
            return Utils.unzipInputToTarget(is, Config.ROOT_DIR);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static final SessionInfo getSessionInfo(String sessionPath) {
        UtilsConfig.LOGD("[[getSessionInfo]] sesssion path = " + sessionPath);

        if (!TextUtils.isEmpty(sessionPath)) {
            File sFile = new File(sessionPath);
            String path = sessionPath + "/" + Config.INI_FILE;
            File sINI = new File(path);
            if (!sFile.exists() || !sINI.exists()) {
                return null;
            }

            UtilsConfig.LOGD("[[getSessionInfo]] get ini info now");

            INIFile iniFile = new INIFile(sessionPath + "/" + Config.INI_FILE);
            SessionInfo ret = new SessionInfo();
            ret.name = iniFile.getStringProperty(SESSION_KEY, "name");
            ret.time = iniFile.getStringProperty(SESSION_KEY, "time");
            ret.cover = sessionPath + iniFile.getStringProperty(SESSION_KEY, "cover");
            ret.description = iniFile.getStringProperty(SESSION_KEY, "description");
            ret.path = sessionPath;
            ret.sessionName = sessionPath.substring(Config.ROOT_DIR.length());

            UtilsConfig.LOGD("[[getSessionInfo]] ret = " + ret.toString());

            return ret;
        }

        return null;
    }

    public static final boolean unzipInputToTarget(InputStream is, String targetDirPath) {
        if (!TextUtils.isEmpty(targetDirPath) && is != null) {
            File target = new File(targetDirPath);
            if (target.exists() && !target.isDirectory()) {
                target.delete();
            }
            if (!target.exists()) {
                target.mkdirs();
            }

            return ZipUtil.UnZipFile(is, targetDirPath);
        }

        return false;
    }

    public static final boolean unzipSrcToTarget(String src, String targetDirPath) {
        if (!TextUtils.isEmpty(targetDirPath) && !TextUtils.isEmpty(src)) {
            File target = new File(targetDirPath);
            if (target.exists() && !target.isDirectory()) {
                target.delete();
            }
            if (!target.exists()) {
                target.mkdirs();
            }

            return ZipUtil.UnZipFile(src, targetDirPath);
        }

        return false;
    }

    public static final boolean saveAssetsFileToDest(Context context, String fileName, String destPath) {
        if (!TextUtils.isEmpty(fileName) && !TextUtils.isEmpty(destPath)) {
            try {
                File saveFile = new File(destPath);
                if (saveFile.exists()) {
                    saveFile.delete();
                }
                saveFile.createNewFile();

                InputStream is = context.getAssets().open(fileName);
                if (!TextUtils.isEmpty(FileOperatorHelper.saveFileByISSupportAppend(destPath, is))) {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    public static String[] getFileCountUnderAssetsDir(Context context, String dir) {
        if (dir == null || context == null) {
            return null;
        }

        try {
            String[] files = context.getAssets().list(dir);
            return files;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Bitmap loadBitmapFromAsset(Context context, String resName) {
        if (resName == null) {
            throw new RuntimeException("resName MUST not be NULL");
        }

        InputStream is = null;
        try {
            is = context.getAssets().open(resName);
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inDensity = 160;
            Rect rect = new Rect();
            Bitmap bitmap = BitmapFactory.decodeStream(is, rect, opt);
            return bitmap;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
