package com.cartoononline;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.text.TextUtils;

import com.plugin.common.utils.files.FileOperatorHelper;

public class Utils {

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
