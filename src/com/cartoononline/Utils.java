package com.cartoononline;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Handler;
import android.text.TextUtils;

import com.plugin.common.utils.UtilsConfig;
import com.plugin.common.utils.CustomThreadPool;
import com.plugin.common.utils.CustomThreadPool.TaskWrapper;
import com.plugin.common.utils.INIFile;
import com.plugin.common.utils.files.FileOperatorHelper;
import com.plugin.common.utils.zip.ZipUtil;

public class Utils {
    
    private static final String SESSION_KEY = "infos";
    
    public static final void asyncUnzipInternalSessions(final Context context, final Handler mHandler) {
        CustomThreadPool.getInstance().excute(new TaskWrapper(new Runnable() {

            @Override
            public void run() {
                try {
                    InputStream is = context.getAssets().open("session1.zip");
                    Utils.unzipInputToTarget(is, AppConfig.ROOT_DIR);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                try {
                    Thread.sleep(2000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                mHandler.sendEmptyMessage(CartoonSplashActivity.REFRESH_READER_LIST);
            }
            
        }));
    }
    
    public static final SessionInfo getSessionInfo(String sessionPath) {
        UtilsConfig.LOGD("[[getSessionInfo]] sesssion path = " + sessionPath);
        
        if (!TextUtils.isEmpty(sessionPath)) {
            File sFile = new File(sessionPath);
            String path = sessionPath + "/" + AppConfig.INI_FILE;
            File sINI = new File(path);
            if (!sFile.exists() || !sINI.exists()) {
                return null;
            }
            
            UtilsConfig.LOGD("[[getSessionInfo]] get ini info now");
            
            INIFile iniFile = new INIFile(sessionPath + "/" + AppConfig.INI_FILE);
            SessionInfo ret = new SessionInfo();
            ret.name = iniFile.getStringProperty(SESSION_KEY, "name");
            ret.time = iniFile.getStringProperty(SESSION_KEY, "time");
            ret.cover = iniFile.getStringProperty(SESSION_KEY, "cover");
            ret.description = iniFile.getStringProperty(SESSION_KEY, "description");
            ret.path = sessionPath + "/";
            ret.sessionName = sessionPath.substring(AppConfig.ROOT_DIR.length());
            
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
    
    public static String[] getFileCountUnderDir(Context context, String dir) {
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
    
    public static Bitmap loadBitmapFromAsset (Context context, String resName) {
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