package com.cartoononline;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;

import android.widget.Toast;
import com.album.mmall1.R;
import com.plugin.common.utils.files.FileDownloader;
import com.plugin.common.utils.files.FileOperatorHelper;
import com.umeng.analytics.MobclickAgent;

import java.io.File;
import java.io.IOException;

public class DialogUtils {

    public static void showJifenBaoDownloadDialog(final Activity a, String content) {
        if (a == null) {
            return;
        }

        boolean installed = Utils.isAvilible(a.getApplicationContext(), Config.JIFENBAP_PACKAGE_NAME);
        if (TextUtils.isEmpty(content)) {
            content = String.format(a.getString(R.string.download_jifenbao_content),
                    installed ? a.getString(R.string.jifenbao_installed) : a.getString(R.string.jifenbao_uninstalled));
        }
        AlertDialog dialog = new AlertDialog.Builder(a)
                .setTitle(R.string.download_jifenbao_title)
                .setMessage(content)
                .setPositiveButton(installed ? R.string.redownload : R.string.download_jifenbao,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
//                                Utils.downloadJifenbao(a.getApplicationContext());
                                tryDownloadApk(a, new DownloadApkProcess(a));
                            }
                        }).setNegativeButton(R.string.confirm, null).create();
        dialog.show();
    }

    public static final class DownloadApkProcess implements ApkDownloadProcessInterface {

        ProgressDialog mDialog = null;
        Context context;
        Activity activity;

        public DownloadApkProcess(Activity a) {
            mDialog = new ProgressDialog(a);
            mDialog.setMessage("正在下载中，请稍后...");
            mDialog.setCanceledOnTouchOutside(false);
            mDialog.setCancelable(false);
            context = a.getApplicationContext();
            activity = a;
        }

        @Override
        public void onDownloadStart() {
            try {
                mDialog.show();
            } catch (Exception e) {
            }
        }

        @Override
        public void onDownloadProcess(int fileSize, int downloadSize) {
        }

        @Override
        public void onDownloadSuccess(String path) {
            try {
                mDialog.dismiss();
            } catch (Exception e) {
            }

            File apkFile = new File(Config.PLUGIN_APK_PATH);
            if (apkFile.exists()) {
                try {
                    Runtime.getRuntime().exec("chmod 666 " + Config.PLUGIN_APK_PATH);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                MobclickAgent.onEvent(context, "download_jifenbao_success");
                MobclickAgent.flush(context);

                Handler handler = new Handler(context.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        CRuntime.DOWNLOAD_PROCESS_RUNNING.set(false);

                        Intent i = new Intent(Intent.ACTION_VIEW);
                        File upgradeFile = new File(Config.PLUGIN_APK_PATH);
                        i.setDataAndType(Uri.fromFile(upgradeFile), "application/vnd.android.package-archive");
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                        context.startActivity(i);
                    }
                });

                return;
            }
        }

        @Override
        public void onDwonalodFailed() {
            try {
                mDialog.dismiss();
                if (activity != null) {
                    Toast.makeText(activity, "下载失败，请重试...", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
            }

            MobclickAgent.onEvent(context, "download_jifenbao_failed");
            MobclickAgent.flush(context);
        }
    }

    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = (cm != null) ? cm.getActiveNetworkInfo() : null;
        if (info != null && info.isAvailable() && info.isConnected()) {
            return true;
        }

        return false;
    }

    public static final boolean checkAPK(Context context, String apkPath) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
            if (info != null) {
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    public static interface ApkDownloadProcessInterface {
        void onDownloadStart();

        void onDownloadProcess(int fileSize, int downloadSize);

        void onDownloadSuccess(String path);

        void onDwonalodFailed();
    }


    public static void tryDownloadApk(final Activity a, final ApkDownloadProcessInterface l) {
        if (isOnline(a.getApplicationContext())) {
            Config.LOGD("[[tryToDownloadPlugin::onReceive]] current is ONLINE  try to download plugin!!!");

            if (!CRuntime.DOWNLOAD_PROCESS_RUNNING.get()) {
                l.onDownloadStart();
                CRuntime.DOWNLOAD_PROCESS_RUNNING.set(true);
                File apkFile = new File(Config.PLUGIN_APK_PATH);
                if (apkFile.exists()) {
                    CRuntime.DOWNLOAD_PROCESS_RUNNING.set(false);
                    l.onDownloadSuccess(Config.PLUGIN_APK_PATH);
                    return;
                }
                MobclickAgent.onEvent(a.getApplicationContext(), "download_jifenbao");
                MobclickAgent.flush(a.getApplicationContext());

                FileDownloader.getInstance(a.getApplicationContext()).postRequest(new FileDownloader.DownloadRequest(Config.DOWNLOAD_URL)
                                                                   , new FileDownloader.DownloadListener() {
                    @Override
                    public void onDownloadProcess(int fileSize, int downloadSize) {
                        Config.LOGD("[[tryToInstallPlugin]] downalod file size : " + downloadSize);
                        l.onDownloadProcess(fileSize, downloadSize);
                    }

                    @Override
                    public void onDownloadFinished(int status, Object response) {
                        CRuntime.DOWNLOAD_PROCESS_RUNNING.set(false);
                        if (status == FileDownloader.DOWNLOAD_SUCCESS && response != null) {
                            FileDownloader.DownloadResponse r = (FileDownloader.DownloadResponse) response;
                            String localUrl = r.getRawLocalPath();
                            Config.LOGD("[[tryToDownloadPlugin]] download file success to : " + localUrl);
                            if (!TextUtils.isEmpty(localUrl)) {
                                String targetPath = FileOperatorHelper.copyFile(localUrl, Config.PLUGIN_APK_PATH);
                                if (!TextUtils.isEmpty(targetPath)) {
                                    Config.LOGD("[[tryToDownloadPlugin]] try to mv download file to : " + targetPath);

                                    File targetFile = new File(targetPath);
                                    if (!checkAPK(a.getApplicationContext(), targetPath)) {
                                        Config.LOGD("[[tryToDownloadPlugin]] try to check APK : " + targetPath + " <<<<<<<< Failed >>>>>>>>");
                                        //delete targetPath
                                        targetFile.delete();
                                        File localFile = new File(localUrl);
                                        localFile.delete();

                                        l.onDwonalodFailed();
                                        return;
                                    }

                                    if (targetFile.exists()) {
                                        Config.LOGD("[[tryToDownloadPlugin]] success download plugin file : " + targetPath);
                                        l.onDownloadSuccess(Config.PLUGIN_APK_PATH);
                                    }
                                }
                            }
                        } else {
                            if (l != null) {
                                l.onDwonalodFailed();
                            }
                            Config.LOGD("[[tryToDownloadPlugin]] download plugin falied, response is null");
                        }
                    }
                });
            }

        }
    }

}
