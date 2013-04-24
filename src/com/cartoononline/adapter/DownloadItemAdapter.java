package com.cartoononline.adapter;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cartoononline.AppConfig;
import com.cartoononline.CustomCycleBitmapOpration;
import com.cartoononline.R;
import com.cartoononline.SessionInfo;
import com.cartoononline.Utils;
import com.cartoononline.R.drawable;
import com.cartoononline.R.id;
import com.cartoononline.R.layout;
import com.cartoononline.R.string;
import com.cartoononline.model.DownloadItemModel;
import com.cartoononline.model.DownloadModel;
import com.cartoononline.model.SessionModel;
import com.cartoononline.model.SessionReadModel;
import com.plugin.common.cache.CacheFactory;
import com.plugin.common.cache.ICacheManager;
import com.plugin.common.utils.CustomThreadPool;
import com.plugin.common.utils.CustomThreadPool.TaskWrapper;
import com.plugin.common.utils.SingleInstanceBase;
import com.plugin.common.utils.UtilsConfig;
import com.plugin.common.utils.files.FileDownloader;
import com.plugin.common.utils.files.FileDownloader.DownloadListener;
import com.plugin.common.utils.files.FileDownloader.DownloadRequest;
import com.plugin.common.utils.files.FileDownloader.DownloadResponse;
import com.plugin.common.utils.files.FileInfo;
import com.plugin.common.utils.files.FileOperatorHelper;
import com.plugin.common.utils.files.FileUtil;
import com.plugin.common.utils.image.ImageDownloader;
import com.plugin.common.utils.image.ImageDownloader.ImageFetchRequest;
import com.plugin.common.utils.image.ImageDownloader.ImageFetchResponse;

public class DownloadItemAdapter extends BaseAdapter {

    private List<DownloadItemModel> mDownloadItemModelList;

    private LayoutInflater mLayoutInflater;

    private ICacheManager<Bitmap> mCacheManager = CacheFactory.getCacheManager(CacheFactory.TYPE_CACHE.TYPE_IMAGE);

    private Set<ImageView> mIconImageViewList;

    private ImageDownloader mImageDownloader;

    private FileDownloader mFileDownloader;

    private Activity mActivity;

    private ProgressDialog mProgress;

    private ProgressDialog mUnZipProgress;
    
//    private CustomCycleBitmapOpration mCustomCycleBitmapOpration = new CustomCycleBitmapOpration();
    private CustomCycleBitmapOpration mCustomCycleBitmapOpration = null;

    private static final int REFRESH_ICONS = 1;
    private static final int REFRESH_LIST = 2;
    private static final int DISMISS_DIALOG = 3;
    private static final int DISMISS_UNZIP_DIALOG = 4;
    private static final int DELETE_ITEM_REFRESH = 5;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case REFRESH_ICONS:
                ImageFetchResponse r = (ImageFetchResponse) msg.obj;
                if (mIconImageViewList != null && mIconImageViewList.size() > 0) {
                    for (ImageView i : mIconImageViewList) {
                        String url = (String) i.getTag();
                        if (r.getDownloadUrl().equals(url)) {
                            i.setImageBitmap(r.getmBt());
                        }
                    }
                }
                break;
            case REFRESH_LIST:
                notifyDataSetChanged();
                if (mProgress.isShowing()) {
                    mProgress.dismiss();
                }
                // try to unzip
                asyncUnzipSession((DownloadItemModel) msg.obj);
                break;
            case DISMISS_DIALOG:
                mProgress.dismiss();
                break;
            case DISMISS_UNZIP_DIALOG:
                mUnZipProgress.dismiss();
                break;
            case DELETE_ITEM_REFRESH:
                notifyDataSetChanged();
                deleteSession((DownloadItemModel) msg.obj);
                break;
            }
        }
    };

    public DownloadItemAdapter(Activity a, List<DownloadItemModel> data, LayoutInflater lf) {
        mActivity = a;
        mDownloadItemModelList = data;
        mLayoutInflater = lf;
        mIconImageViewList = new HashSet<ImageView>();
        mImageDownloader = SingleInstanceBase.getInstance(ImageDownloader.class);
        mFileDownloader = SingleInstanceBase.getInstance(FileDownloader.class);
        initProgressBar();
        initUnZipProgressBar();
    }

    private void deleteSession(DownloadItemModel r) {
        if (r == null) {
            return;
        }
        
        String localPath = r.getLocalFullPath();
        if (TextUtils.isEmpty(localPath)) {
            return;
        }

        String downloadUrl = r.getDownloadUrl();
        String unzipTarget = null;
        int pos = downloadUrl.indexOf(AppConfig.SESSION_REFIX);
        if (pos != -1) {
            int endPos = downloadUrl.lastIndexOf(".zip");
            if (endPos != -1) {
                unzipTarget = downloadUrl.substring(pos, endPos);
            }
        }
        
        if (!TextUtils.isEmpty(unzipTarget)) {
            String targetPath = AppConfig.ROOT_DIR + unzipTarget + File.separator;
            File targetFile = new File(targetPath);
            if (targetFile.exists()) {
                FileInfo info = FileUtil.getFileInfo(targetFile);
                FileOperatorHelper.DeleteFile(info);
            }
            
            if (!TextUtils.isEmpty(targetPath)) {
                SessionReadModel m = new SessionReadModel();
                m.localFullPathHashCode = targetPath.hashCode();
                SingleInstanceBase.getInstance(SessionModel.class).deleteItem(m);
            }
        }
    }
    
    private void asyncUnzipSession(final DownloadItemModel r) {
        mUnZipProgress.show();
        CustomThreadPool.getInstance().excute(new TaskWrapper(new Runnable() {
            @Override
            public void run() {
                String localPath = r.getLocalFullPath();
                if (TextUtils.isEmpty(localPath)) {
                    return;
                }

                String downloadUrl = r.getDownloadUrl();
                String unzipTarget = null;
                int pos = downloadUrl.indexOf(AppConfig.SESSION_REFIX);
                if (pos != -1) {
                    int endPos = downloadUrl.lastIndexOf(".zip");
                    if (endPos != -1) {
                        unzipTarget = downloadUrl.substring(pos, endPos);
                    }
                }

                if (!TextUtils.isEmpty(unzipTarget)) {
                    String targetPath = AppConfig.ROOT_DIR + unzipTarget + File.separator;
                    File targetFile = new File(targetPath);
                    if (targetFile.exists()) {
                        FileInfo info = FileUtil.getFileInfo(targetFile);
                        FileOperatorHelper.DeleteFile(info);
                    }

                    if (!targetFile.exists()) {
                        if (Utils.unzipSrcToTarget(localPath, AppConfig.ROOT_DIR)) {
                            SessionInfo sInfo = Utils.getSessionInfo(targetPath);
                            if (sInfo != null) {
                                SessionReadModel m = new SessionReadModel();
                                m.isRead = 0;
                                m.localFullPath = targetPath;
                                m.coverPath = sInfo.cover;
                                m.description = sInfo.description;
                                m.name = sInfo.name;
                                m.sessionName = sInfo.sessionName;
                                m.srcURI = r.getDownloadUrl();
                                m.sessionMakeTime = sInfo.time;
                                m.unzipTime = System.currentTimeMillis();
                                m.localFullPathHashCode = m.localFullPath.hashCode();
                                SingleInstanceBase.getInstance(SessionModel.class).insertOrRelace(m);
                                SingleInstanceBase.getInstance(DownloadModel.class).updateItemModel(r);
                                SingleInstanceBase.getInstance(DownloadModel.class).setDataChanged(false);

                                mHandler.sendEmptyMessage(DISMISS_UNZIP_DIALOG);

                                return;
                            }
                        }
                    }
                }

                // File local = new File(localPath);
                // local.delete();
                // r.localFullPath = null;
                // r.status = DownloadItemModel.UNDOWNLOAD;

                mHandler.sendEmptyMessage(DISMISS_UNZIP_DIALOG);
            }
        }));
    }

    public void setData(List<DownloadItemModel> data) {
        mDownloadItemModelList = data;
        this.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        if (mDownloadItemModelList != null) {
            return mDownloadItemModelList.size();
        }

        return 0;
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View ret = convertView;
        ViewHolder holder = null;
        if (ret == null) {
            ret = mLayoutInflater.inflate(R.layout.download_item, null);
            holder = new ViewHolder();
            holder.icon = (ImageView) ret.findViewById(R.id.item_icon);
            holder.description = (TextView) ret.findViewById(R.id.description);
            holder.size = (TextView) ret.findViewById(R.id.size);
            holder.statusIcon = (ImageView) ret.findViewById(R.id.status_icon);
            ret.setTag(holder);
        } else {
            holder = (ViewHolder) ret.getTag();
        }

        DownloadItemModel item = mDownloadItemModelList.get(position);
        holder.description.setText(item.description);
        holder.size.setText(item.size);

        if (item.status == DownloadItemModel.DOWNLOADED) {
            // holder.status.setText(R.string.downloaded);
            holder.statusIcon.setImageResource(R.drawable.delete_button);
        } else if (item.status == DownloadItemModel.UNDOWNLOAD) {
            // holder.status.setText(R.string.undownload);
            holder.statusIcon.setImageResource(R.drawable.download_button);
        } else if (item.status == DownloadItemModel.UNZIPED) {
            // holder.status.setText(R.string.unziped);
            holder.statusIcon.setImageResource(R.drawable.download_button);
        } else {
            // holder.status.setText(R.string.unknown);
            holder.statusIcon.setImageResource(R.drawable.info);
        }
        // holder.status.setBackgroundResource(R.color.download_status);

        // set icon image
        Bitmap icon = mCacheManager.getResource(UtilsConfig.IMAGE_CACHE_CATEGORY_RAW, item.coverUrl);
        if (icon != null) {
            holder.icon.setImageBitmap(icon);
            mIconImageViewList.remove(holder.icon);
        } else {
            holder.icon.setImageBitmap(null);
            if (!TextUtils.isEmpty(item.coverUrl)) {
                holder.icon.setTag(item.coverUrl);
                mIconImageViewList.add(holder.icon);
                mImageDownloader.postRequest(new ImageFetchRequest(item.coverUrl, mCustomCycleBitmapOpration), new DownloadListener() {

                    @Override
                    public void onDownloadProcess(int fileSize, int downloadSize) {
                    }

                    @Override
                    public void onDownloadFinished(int status, Object response) {
                        if (response != null && status == ImageDownloader.DOWNLOAD_SUCCESS) {
                            Message msg = new Message();
                            msg.what = REFRESH_ICONS;
                            msg.obj = response;
                            mHandler.sendMessage(msg);
                        }
                    }

                });
            }
        }

        setViewListener(ret, position, item, holder);

        return ret;
    }

    private void setViewListener(View view, final int position, final DownloadItemModel item, ViewHolder holder) {
        View.OnClickListener itemOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDownloadItemModelList != null && position < mDownloadItemModelList.size()) {
                    if (item.status == DownloadItemModel.UNDOWNLOAD) {
                        AlertDialog dialog = new AlertDialog.Builder(mActivity)
                                                    .setMessage(String.format(mActivity.getString(R.string.download_tips), item.sessionName))
                                                    .setNegativeButton(R.string.cancel, null)
                                                    .setPositiveButton(R.string.download, new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            if (!TextUtils.isEmpty(item.downloadUrl)) {
                                                                if (!mProgress.isShowing()) {
                                                                    mProgress.show();
                                                                }
                                                                
                                                                mFileDownloader.postRequest(new DownloadRequest(item.downloadUrl), 
                                                                        new DownloadListener() {

                                                                            @Override
                                                                            public void onDownloadProcess(int fileSize,
                                                                                    int downloadSize) {
                                                                                
                                                                            }

                                                                            @Override
                                                                            public void onDownloadFinished(int status,
                                                                                    Object response) {
                                                                                if (status == FileDownloader.DOWNLOAD_SUCCESS
                                                                                        && response != null) {
                                                                                    LOGD("Download success, respose = " + response);
                                                                                    DownloadResponse r = (DownloadResponse) response;
                                                                                    item.status = DownloadItemModel.DOWNLOADED;
                                                                                    item.localFullPath = r.getRawLocalPath();
                                                                                    
                                                                                    Message msg = new Message();
                                                                                    msg.what = REFRESH_LIST;
                                                                                    msg.obj = item;
                                                                                    mHandler.sendMessage(msg);
                                                                                    return;
                                                                                }
                                                                                
                                                                                mHandler.sendEmptyMessage(DISMISS_DIALOG);
                                                                            }
                                                                    
                                                                });
                                                            }
                                                        }
                                                    })
                                                    .create();
                        dialog.show();
                    }
                }
            }
        };
        
        View.OnLongClickListener itemLongClickListener = new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                if (mDownloadItemModelList != null && position < mDownloadItemModelList.size()) {
                    final String local = item.getLocalFullPath();
                    if (!TextUtils.isEmpty(local)) {
                        AlertDialog dialog = new AlertDialog.Builder(mActivity)
                                .setMessage(String.format(mActivity.getString(R.string.delete_tips), item.sessionName))
                                .setNegativeButton(R.string.cancel, null)
                                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        onDeleteItem(item);
                                    }
                                }).create();
                        dialog.show();
                    }
                }
                return true;
            }
        };
        
        
        View.OnClickListener itemClickDelete = new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                if (mDownloadItemModelList != null && position < mDownloadItemModelList.size()) {
                    final String local = item.getLocalFullPath();
                    if (!TextUtils.isEmpty(local)) {
                        AlertDialog dialog = new AlertDialog.Builder(mActivity)
                                .setMessage(String.format(mActivity.getString(R.string.delete_tips), item.sessionName))
                                .setNegativeButton(R.string.cancel, null)
                                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        onDeleteItem(item);
                                    }
                                }).create();
                        dialog.show();
                    }
                }
            }
        };
        
        
        if (item.status == DownloadItemModel.UNDOWNLOAD) {
            holder.statusIcon.setOnClickListener(itemOnClickListener);
        } else {
            holder.statusIcon.setOnClickListener(itemClickDelete);
        }
        
        view.setOnClickListener(itemOnClickListener);
        view.setOnLongClickListener(itemLongClickListener);
    }
    
    private void onDeleteItem(DownloadItemModel item) {
        if (!TextUtils.isEmpty(item.getLocalFullPath())) {
            File localFile = new File(item.localFullPath);
            localFile.delete();
        }
        if (!TextUtils.isEmpty(item.coverUrl)) {
            String imagePath = mCacheManager.getResourcePath(UtilsConfig.IMAGE_CACHE_CATEGORY_RAW, item.coverUrl);
            File file = new File(imagePath);
            file.delete();
            mCacheManager.releaseResource(UtilsConfig.IMAGE_CACHE_CATEGORY_RAW, item.coverUrl);
        }
        
        item.status = DownloadItemModel.UNDOWNLOAD;
//        SingleInstanceBase.getInstance(DownloadModel.class).updateItemModel(item);
        
        Message msg = new Message();
        msg.what = DELETE_ITEM_REFRESH;
        msg.obj = item;
        mHandler.sendMessage(msg);
    }

    private void initProgressBar() {
        if (mProgress == null) {
            mProgress = new ProgressDialog(mActivity);
            mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgress.setMessage("正在下载中，请稍后...");
            mProgress.setCanceledOnTouchOutside(false);
        }
    }

    private void initUnZipProgressBar() {
        if (mUnZipProgress == null) {
            mUnZipProgress = new ProgressDialog(mActivity);
            mUnZipProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mUnZipProgress.setMessage("正在解压中，请稍后...");
            mUnZipProgress.setCanceledOnTouchOutside(false);
        }
    }

    private class ViewHolder {
        ImageView icon;
        TextView description;
        TextView size;
        ImageView statusIcon;
    }

    private static void LOGD(String msg) {
        if (AppConfig.DEBUG) {
            UtilsConfig.LOGD(msg);
        }
    }
}
