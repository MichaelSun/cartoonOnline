/**
 * CycleBitmapOpration.java
 */
package com.cartoononline;

import android.graphics.Bitmap;

import com.plugin.common.utils.image.ImageDownloader.BitmapOperationListener;
import com.plugin.common.utils.image.ImageUtils;

/**
 * @author Guoqing Sun Nov 28, 201212:02:13 PM
 */
public final class CustomCycleBitmapOpration implements BitmapOperationListener {

    /* (non-Javadoc)
     * @see com.sound.dubbler.utils.image.ImageDownloader.BitmapOperationListener#onAfterBitmapDownload(android.graphics.Bitmap)
     */
    @Override
    public Bitmap onAfterBitmapDownload(Bitmap downloadBt) {
        if (downloadBt != null && !downloadBt.isRecycled()) {
            return ImageUtils.createRoundedCornerBitmap(downloadBt, 108, 108, 0.4f, true, true, true, true);
        }
        
        return null;
    }

}
