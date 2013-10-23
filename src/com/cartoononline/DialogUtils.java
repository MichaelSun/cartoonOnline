package com.cartoononline;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.TextUtils;

import com.album.legnew.R;

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
                                Utils.downloadJifenbao(a.getApplicationContext());
                            }
                        }).setNegativeButton(R.string.confirm, null).create();
        dialog.show();
    }

}
