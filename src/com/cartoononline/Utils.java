package com.cartoononline;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.album.legnew.R;
import com.cartoononline.api.LoginRequest;
import com.cartoononline.api.LoginResponse;
import com.cartoononline.api.RegisteRequest;
import com.cartoononline.api.RegisteResponse;
import com.cartoononline.api.UploadPointRequest;
import com.cartoononline.api.UploadPointResponse;
import com.cartoononline.model.SessionReadModel;
import com.plugin.common.utils.CustomThreadPool;
import com.plugin.common.utils.INIFile;
import com.plugin.common.utils.UtilsConfig;
import com.plugin.common.utils.files.FileOperatorHelper;
import com.plugin.common.utils.zip.ZipUtil;
import com.plugin.internet.InternetUtils;
import com.umeng.analytics.MobclickAgent;

public class Utils {

    private static final boolean DEBUG = Config.DEBUG;

    private static final String SESSION_KEY = "infos";

    public interface PointFetchListener {
        void onPointFetchSuccess(int current);

        void onPointFetchFailed(int code, String data);
    }

    public interface PointUploadListener {
        void onPointUploadSuccess(int currentPoint);

        void onPointUploadFailed(int code, String data);
    }

    public interface RegisteListener {
        void onRegisteSuccess(int currentPoint);

        void onRegisteFailed(int code, String data);
    }

    public static void lanuchJifenBao(Context context) {
        try {
//            Log.d("lanuch", "try to lanuch jifebao with : " + SettingManager.getInstance().getUserName());
            
            Intent intent = new Intent();
            intent.setAction("com.jifenbao.lanuch");
            if (!TextUtils.isEmpty(SettingManager.getInstance().getUserName())
                    && !TextUtils.isEmpty(SettingManager.getInstance().getPassword())) {
                intent.putExtra("u", SettingManager.getInstance().getUserName());
                intent.putExtra("p", SettingManager.getInstance().getPassword());
            }
            context.sendBroadcast(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }

        HashMap<String, String> extra = new HashMap<String, String>();
        extra.put("packageName", Config.CURRENT_PACKAGE_NAME);
        MobclickAgent.onEvent(context, "open_jifenbao", extra);
        MobclickAgent.flush(context);
    }

    public static void registeAccount(final Context context, final String username, final String password,
            final RegisteListener l) {
        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
            CustomThreadPool.asyncWork(new Runnable() {

                @Override
                public void run() {
                    try {
                        RegisteResponse response = InternetUtils.request(context,
                                new RegisteRequest(username, username));
                        if (response != null) {
                            HashMap<String, String> extra = new HashMap<String, String>();
                            switch (response.code) {
                            case LoginResponse.CODE_SUCCESS:
                                extra.put("packageName", Config.CURRENT_PACKAGE_NAME);
                                extra.put("code", "success");
                                MobclickAgent.onEvent(context, "registe_jifenbao", extra);
                                MobclickAgent.flush(context);
                                if (l != null) {
                                    l.onRegisteSuccess(0);
                                }
                                return;
                            case LoginResponse.CODE_USER_EXIST:
                                if (l != null) {
                                    l.onRegisteFailed(LoginResponse.CODE_USER_EXIST, null);
                                }
                                extra.put("packageName", Config.CURRENT_PACKAGE_NAME);
                                extra.put("code", "user_exists");
                                MobclickAgent.onEvent(context, "registe_jifenbao", extra);
                                MobclickAgent.flush(context);
                                return;
                            default:
                                extra.put("packageName", Config.CURRENT_PACKAGE_NAME);
                                extra.put("code", "failed_unknown");
                                MobclickAgent.onEvent(context, "registe_jifenbao", extra);
                                MobclickAgent.flush(context);
                                break;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();

                        HashMap<String, String> extra = new HashMap<String, String>();
                        extra.put("packageName", Config.CURRENT_PACKAGE_NAME);
                        extra.put("code", e.getMessage());
                        MobclickAgent.onEvent(context, "registe_jifenbao", extra);
                        MobclickAgent.flush(context);
                    }

                    if (l != null) {
                        l.onRegisteFailed(-1, null);
                    }
                }
            });
        }
    }

    public static void downloadJifenbao(Context context) {
        Uri downloadUri = Uri
                .parse("http://bcs.duapp.com/jifenbao/jifenbao-release.apk?sign=MBO:27302677c46c1c5b7795853ba23d0329:0yCmmYSUIxd0kvaSYF9l8JtRw8U%3D");
        Intent it = new Intent(Intent.ACTION_VIEW, downloadUri);
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(it, 0);
        boolean isIntentSafe = activities.size() > 0;

        // Start an activity if it's safe
        if (isIntentSafe) {
            context.startActivity(it);
        } else {
        }

        HashMap<String, String> extra = new HashMap<String, String>();
        extra.put("packageName", Config.CURRENT_PACKAGE_NAME);
        MobclickAgent.onEvent(context, "download_jifenbao", extra);
        MobclickAgent.flush(context);
    }

    public static void asyncFetchCurrentPoint(final Context context, final String userName, final String password,
            final PointFetchListener pointFetchListener) {
        CustomThreadPool.asyncWork(new Runnable() {

            @Override
            public void run() {
                LoginRequest request = new LoginRequest(userName, password);
                try {
                    LoginResponse response = InternetUtils.request(context, request);
                    if (response != null) {
                        Log.d(">>>>>>>", response.toString());
                        if (response.code == LoginResponse.CODE_SUCCESS) {
                            if (pointFetchListener != null) {
                                pointFetchListener.onPointFetchSuccess(Integer.valueOf(response.data));
                            }
                        } else {
                            if (pointFetchListener != null) {
                                pointFetchListener.onPointFetchFailed(response.code, response.data);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (pointFetchListener != null) {
                        pointFetchListener.onPointFetchFailed(LoginResponse.CODE_UNKNOWN, e.getMessage());
                    }
                }
            }
        });
    }

    public static void asyncUploadPoint(final Context context, final String userName, int currentPoint,
            final PointUploadListener l) {
        final int point = currentPoint > 0 ? currentPoint : 1;
        if (!TextUtils.isEmpty(userName) && currentPoint > 0) {
            CustomThreadPool.asyncWork(new Runnable() {

                @Override
                public void run() {
                    UploadPointRequest request = new UploadPointRequest(userName, String.valueOf(point));
                    try {
                        UploadPointResponse response = InternetUtils.request(context, request);
                        if (response != null) {
                            if (response.code == LoginResponse.CODE_SUCCESS) {
                                if (l != null) {
                                    l.onPointUploadSuccess(Integer.valueOf(response.data));
                                }
                            } else {
                                if (l != null) {
                                    l.onPointUploadFailed(response.code, response.data);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (l != null) {
                            l.onPointUploadFailed(LoginResponse.CODE_UNKNOWN, e.getMessage());
                        }
                    }
                }
            });
        }
    }

    public static void showDownloadFBDialog(final Activity a, final SessionReadModel m) {
        AlertDialog dialog = new AlertDialog.Builder(a).setTitle(R.string.tips_title)
                .setMessage(R.string.fb_download_tips)
                .setPositiveButton(R.string.fb_read_now, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        tryStartRead(a, m);

                        HashMap<String, String> extra = new HashMap<String, String>();
                        extra.put("name", m.description);
                        MobclickAgent.onEvent(a.getApplicationContext(), Config.OPEN_BOOK_SELF, extra);
                        MobclickAgent.flush(a.getApplicationContext());
                    }
                }).setNegativeButton(R.string.fb_download_now, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        RateDubblerHelper.getInstance(a.getApplicationContext()).OpenApp(
                                "org.geometerplus.zlibrary.ui.android");

                        MobclickAgent.onEvent(a.getApplicationContext(), Config.OPEN_FB_DOWNLOAD);
                        MobclickAgent.flush(a.getApplicationContext());
                    }
                }).create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    public static void tryStartRead(Activity a, SessionReadModel m) {
        if (m != null && !TextUtils.isEmpty(m.localFullPath)) {
            File file = new File(m.localFullPath);
            String[] files = file.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    if (filename.endsWith(".epub")) {
                        return true;
                    }
                    return false;
                }
            });

            if (files != null && file.length() > 0) {
                String filename = files[0];
                File downloadDirBook = new File(Config.BOOK_DOWNLOAD_DIR + filename);
                if (!downloadDirBook.exists()) {
                    FileOperatorHelper.copyFile(m.localFullPath + filename, Config.BOOK_DOWNLOAD_DIR + filename);
                }

                if (downloadDirBook.exists()) {
                    startReadBookIntent(a, Config.BOOK_DOWNLOAD_DIR + filename, filename);
                } else {
                    startReadBookIntent(a, m.localFullPath + filename, filename);
                }

                HashMap<String, String> extra = new HashMap<String, String>();
                extra.put("name", m.description);
                MobclickAgent.onEvent(a.getApplicationContext(), Config.OPEN_ALUBM, extra);
                MobclickAgent.flush(a.getApplicationContext());
            }
        }
    }

    public static boolean isAvilible(Context context, String packageName) {
        final PackageManager packageManager = context.getPackageManager();// 获取packagemanager
        List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);// 获取所有已安装程序的包信息
        List<String> pName = new ArrayList<String>();// 用于存储所有已安装程序的包名
        if (pinfo != null) {
            for (int i = 0; i < pinfo.size(); i++) {
                String pn = pinfo.get(i).packageName;
                pName.add(pn);
            }
        }
        return pName.contains(packageName);// 判断pName中是否有目标程序的包名，有TRUE，没有FALSE
    }

    public static void startReadBookIntent(Activity a, String bookSrc, String bookName) {
        if (!TextUtils.isEmpty(bookSrc)) {
            File file = new File(bookSrc);
            if (!file.exists()) {
                Toast.makeText(a, String.format(a.getString(R.string.book_not_find), bookName), Toast.LENGTH_LONG)
                        .show();
            } else {
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setData(Uri.fromFile(new File(bookSrc)));
                a.startActivity(intent);
            }
        }
    }

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
