package com.cartoononline.adapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.Toast;

import com.album.leg.R;
import com.cartoononline.CRuntime;
import com.cartoononline.CartoonSplashActivity;
import com.cartoononline.CartoonSplashActivity.LoginInterfaceListener;
import com.cartoononline.Config;
import com.cartoononline.SettingManager;
import com.cartoononline.Utils;
import com.cartoononline.Utils.PointFetchListener;

public class AdapterUtils {

    public static void showAppShouldDownloadWall(final Activity a, int point) {
        if (a == null) {
            return;
        }
        if (Utils.isAvilible(a.getApplicationContext(), "com.jifen.point")) {
            String tips = String.format(a.getString(R.string.offer_download_tips), Config.DOWNLOAD_NEED_POINT, point, Config.DOWNLOAD_NEED_POINT);
            AlertDialog dialog = new AlertDialog.Builder(a)
                                        .setTitle(R.string.tips_title)
                                        .setMessage(tips)
                                        .setPositiveButton(R.string.open_jifenbao, new DialogInterface.OnClickListener() {
                                            
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                Utils.lanuchJifenBao(a.getApplicationContext());
                                            }
                                        })
                                        .setNegativeButton(R.string.cancel, null)
                                        .create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        } else {
            AlertDialog dialog = new AlertDialog.Builder(a)
                                        .setTitle(R.string.download_jifenbao_title)
                                        .setMessage(R.string.download_jifenbao_tips)
                                        .setPositiveButton(R.string.download_jifenbao, new DialogInterface.OnClickListener() {
                                            
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (a != null) {
                                                    Utils.downloadJifenbao(a.getApplicationContext());
                                                }
                                            }
                                        })
                                        .setNegativeButton(R.string.confirm, null)
                                        .create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }
    }
    
    
    public static void asyncCheckPoint(final Activity a, final ProgressDialog progressDialog, final PointCheckInterface l) {
        int localPoint = SettingManager.getInstance().getPointInt();
        if (localPoint >= Config.DOWNLOAD_NEED_POINT || !Config.ADVIEW_SHOW) {
            if (l != null) {
                l.onCanDownlaod();
            }

            return;
        }

        if (a == null) {
            return;
        }

        if (TextUtils.isEmpty(SettingManager.getInstance().getUserName())
                || TextUtils.isEmpty(SettingManager.getInstance().getPassword())) {
            if (a != null) {
                ((CartoonSplashActivity) a).showPointWithAccountCheck(new LoginInterfaceListener() {
                            @Override
                            public void onLoginSuccess(int currentPoint) {
                                CRuntime.ACCOUNT_POINT_INFO.currentPoint = currentPoint;
                                CRuntime.ACCOUNT_POINT_INFO.username = SettingManager.getInstance().getUserName();
                                asyncCheckPoint(a, progressDialog, l);
                            }

                            @Override
                            public void onLoginFailed(int code) {
                            }
                        });
            }
            return;
        }

        if (progressDialog != null) {
            progressDialog.show();
        }
        Utils.asyncFetchCurrentPoint(a.getApplicationContext(), SettingManager.getInstance().getUserName(), SettingManager.getInstance()
                .getPassword(), new PointFetchListener() {

            @Override
            public void onPointFetchSuccess(int current) {
                int localPoint = SettingManager.getInstance().getPointInt();
                CRuntime.ACCOUNT_POINT_INFO.currentPoint = current;
                final int totalPoint = localPoint + CRuntime.ACCOUNT_POINT_INFO.currentPoint;
                if (totalPoint >= Config.DOWNLOAD_NEED_POINT || !Config.ADVIEW_SHOW) {
                    if (l != null && a != null) {
                        a.runOnUiThread(new Runnable() {
                            public void run() {
                                if (progressDialog != null) {
                                    progressDialog.dismiss();
                                }
                                l.onCanDownlaod();
                            }
                        });
                    }
                } else {
                    if (l != null && a != null) {
                        a.runOnUiThread(new Runnable() {
                            public void run() {
                                if (progressDialog != null) {
                                    progressDialog.dismiss();
                                }
                                l.onShouldShowPointWall(totalPoint);
                            }
                        });
                    }
                }
            }

            @Override
            public void onPointFetchFailed(int code, String data) {
                if (a != null) {
                    a.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            if (progressDialog != null) {
                                progressDialog.dismiss();
                            }
                            Toast.makeText(a, R.string.error_sync_point, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }

}
