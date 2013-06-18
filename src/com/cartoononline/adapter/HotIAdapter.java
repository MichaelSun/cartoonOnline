package com.cartoononline.adapter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.youmi.android.offers.OffersManager;
import net.youmi.android.offers.PointsManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.album.rosi.R;
import com.cartoononline.AlbumActivity;
import com.cartoononline.CRuntime;
import com.cartoononline.Config;
import com.cartoononline.CustomCycleBitmapOpration;
import com.cartoononline.SessionInfo;
import com.cartoononline.SettingManager;
import com.cartoononline.Utils;
import com.cartoononline.api.DownloadAlbumRequest;
import com.cartoononline.api.DownloadAlbumResponse;
import com.cartoononline.model.DownloadItemModel;
import com.cartoononline.model.DownloadModel;
import com.cartoononline.model.HotItemModel;
import com.cartoononline.model.HotModel;
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
import com.plugin.internet.InternetUtils;
import com.umeng.analytics.MobclickAgent;

public class HotIAdapter extends BaseAdapter implements OnStateChangedListener {

    private List<HotItemModel> mDownloadItemModelList = new ArrayList<HotItemModel>();

    private Set<Integer> mDownloaddItemHashCode = new HashSet<Integer>();

    private LayoutInflater mLayoutInflater;

    private ICacheManager<Bitmap> mCacheManager = CacheFactory.getCacheManager(CacheFactory.TYPE_CACHE.TYPE_IMAGE);

    private Set<ImageView> mIconImageViewList;

    private ImageDownloader mImageDownloader;

    private FileDownloader mFileDownloader;

    private Activity mActivity;

    private ProgressDialog mProgress;

    private ProgressDialog mUnZipProgress;

    private Animation mFadeInAnim;

    private boolean mIsFling;

    private Context mContext;

    private CustomCycleBitmapOpration mCustomCycleBitmapOpration = null;
    
    private HashSet<ImageView> mCoverImageView = new HashSet<ImageView>();

    private static final int LOAD_FROM_LOACAL = -1;
    private static final int LOAD_FROM_SERVER = -2;

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
                int hashCode = msg.arg1;
                if (mIconImageViewList != null && mIconImageViewList.size() > 0) {
                    for (ImageView i : mIconImageViewList) {
                        int code = (Integer) i.getTag();
                        if (hashCode == code) {
                            i.setImageBitmap((Bitmap) msg.obj);
                            if (!mIsFling && msg.arg2 == LOAD_FROM_SERVER) {
                                if (mFadeInAnim != null) {
                                    i.startAnimation(mFadeInAnim);
                                }
                            }
                        }
                    }
                }
                break;
            case REFRESH_LIST:
                notifyDataSetChanged();
                if (mProgress != null && mProgress.isShowing()) {
                    mProgress.dismiss();
                }
                // try to unzip
                asyncUnzipSession((HotItemModel) msg.obj);
                break;
            case DISMISS_DIALOG:
                if (mProgress != null) {
                    mProgress.dismiss();
                }
                break;
            case DISMISS_UNZIP_DIALOG:
                mUnZipProgress.dismiss();
                break;
            case DELETE_ITEM_REFRESH:
                notifyDataSetChanged();
                deleteSession((HotItemModel) msg.obj);
                break;
            }
        }
    };

    public HotIAdapter(Activity a, List<HotItemModel> data, LayoutInflater lf) {
        mActivity = a;
        if (mActivity != null) {
            mContext = mActivity.getApplicationContext();
        }
        mLayoutInflater = lf;
        mIconImageViewList = new HashSet<ImageView>();
        mImageDownloader = SingleInstanceBase.getInstance(ImageDownloader.class);
        mFileDownloader = SingleInstanceBase.getInstance(FileDownloader.class);
        initProgressBar();
        initUnZipProgressBar();

        if (a != null) {
            mFadeInAnim = AnimationUtils.loadAnimation(a.getApplicationContext(), R.anim.fade_in);
        }

        if (data != null) {
            for (HotItemModel item : data) {
                mDownloadItemModelList.add(item);
                mDownloaddItemHashCode.add(item.downloadUrlHashCode);
            }
        }
    }

    public void setFlingState(boolean fling) {
        mIsFling = fling;
    }

    private void deleteSession(HotItemModel r) {
        if (r == null) {
            return;
        }

        String localPath = r.getLocalFullPath();
        if (TextUtils.isEmpty(localPath)) {
            return;
        }

        String downloadUrl = r.getDownloadUrl();
        String unzipTarget = null;
        int pos = downloadUrl.indexOf(Config.SESSION_REFIX);
        if (pos != -1) {
            int endPos = downloadUrl.lastIndexOf(".zip");
            if (endPos != -1) {
                unzipTarget = downloadUrl.substring(pos, endPos);
            }
        }

        if (!TextUtils.isEmpty(unzipTarget)) {
            String targetPath = Config.ROOT_DIR + unzipTarget + File.separator;
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

    private void asyncUnzipSession(final HotItemModel r) {
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
                int pos = downloadUrl.indexOf(Config.SESSION_REFIX);
                if (pos != -1) {
                    int endPos = downloadUrl.lastIndexOf(".zip");
                    if (endPos != -1) {
                        unzipTarget = downloadUrl.substring(pos, endPos);
                    }
                }

                if (!TextUtils.isEmpty(unzipTarget)) {
                    String targetPath = Config.ROOT_DIR + unzipTarget + File.separator;
                    File targetFile = new File(targetPath);
                    if (targetFile.exists()) {
                        FileInfo info = FileUtil.getFileInfo(targetFile);
                        FileOperatorHelper.DeleteFile(info);
                    }

                    if (!targetFile.exists()) {
                        if (Utils.unzipSrcToTarget(localPath, Config.ROOT_DIR)) {
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
                                SingleInstanceBase.getInstance(HotModel.class).updateItemModel(r);
                                SingleInstanceBase.getInstance(HotModel.class).setDataChanged(false);

                                DownloadItemModel searchItem = new DownloadItemModel();
                                searchItem.downloadUrlHashCode = r.downloadUrlHashCode;
                                DownloadItemModel result = SingleInstanceBase.getInstance(DownloadModel.class).getItem(
                                        searchItem);
                                if (result != null) {
                                    updateDownloadItem(result, r);
                                    SingleInstanceBase.getInstance(DownloadModel.class).updateItemModel(result);
                                    SingleInstanceBase.getInstance(DownloadModel.class).setDataChanged(true);
                                }

                                mHandler.sendEmptyMessage(DISMISS_UNZIP_DIALOG);

                                return;
                            }
                        }
                    }
                }

                mHandler.sendEmptyMessage(DISMISS_UNZIP_DIALOG);
            }
        }));
    }

    private void updateDownloadItem(DownloadItemModel src, HotItemModel data) {
        src.coverBt = data.coverBt;
        src.coverUrl = data.coverUrl;
        src.description = data.description;
        src.downloadCount = data.downloadCount;
        src.downloadUrl = data.downloadUrl;
        src.localFullPath = data.localFullPath;
        src.sessionName = data.sessionName;
        src.size = data.size;
        src.status = data.status;
        src.time = data.time;
    }

    public void setData(List<HotItemModel> data) {
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
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View ret = convertView;
        ViewHolder holder = null;
        if (ret == null) {
            ret = mLayoutInflater.inflate(R.layout.hot_item, null);
            holder = new ViewHolder();
            holder.icon = (ImageView) ret.findViewById(R.id.item_icon);
            holder.description = (TextView) ret.findViewById(R.id.description);
            holder.size = (TextView) ret.findViewById(R.id.size);
            holder.statusIcon = (ImageView) ret.findViewById(R.id.status_icon);
            holder.newsIcon = (ImageView) ret.findViewById(R.id.news_tips);
            holder.downloadTV = (TextView) ret.findViewById(R.id.download_count);
            ret.setTag(holder);
        } else {
            holder = (ViewHolder) ret.getTag();
        }

        HotItemModel item = mDownloadItemModelList.get(position);
        holder.description.setText(item.description);
        holder.size.setText(item.size);

        if (item.status == DownloadItemModel.DOWNLOADED) {
            holder.statusIcon.setImageResource(R.drawable.delete_button);
        } else if (item.status == DownloadItemModel.UNDOWNLOAD) {
            holder.statusIcon.setImageResource(R.drawable.download_button);
        } else if (item.status == DownloadItemModel.UNZIPED) {
            holder.statusIcon.setImageResource(R.drawable.download_button);
        } else {
            holder.statusIcon.setImageResource(R.drawable.info);
        }

        if (CRuntime.CUR_FORMAT_TIME.equals(item.time)) {
            holder.newsIcon.setVisibility(View.VISIBLE);
        } else {
            holder.newsIcon.setVisibility(View.GONE);
        }

        if (item.downloadCount > 0) {
            holder.downloadTV.setText(String.format(mContext.getString(R.string.download_count_text),
                    String.valueOf(item.downloadCount)));
        }

        // set icon image
        Bitmap icon = mCacheManager.getResourceFromMem(UtilsConfig.IMAGE_CACHE_CATEGORY_RAW, item.coverUrl);
        if (icon != null) {
            holder.icon.setImageBitmap(icon);
            mIconImageViewList.remove(holder.icon);
        } else {
            holder.icon.setImageBitmap(null);
            holder.icon.clearAnimation();
//            if (!mIsFling) {
                asyncLoadImage(holder, item.coverUrl);
//            }
        }
        this.mCoverImageView.add(holder.icon);

        setViewListener(ret, position, item, holder);

        return ret;
    }

    private void asyncLoadImage(final ViewHolder holder, final String url) {
        if (TextUtils.isEmpty(url) || holder == null) {
            return;
        }

        holder.icon.setTag(url.hashCode());
        mIconImageViewList.add(holder.icon);
        CustomThreadPool.asyncWork(new Runnable() {
            @Override
            public void run() {
                Bitmap icon = mCacheManager.getResource(UtilsConfig.IMAGE_CACHE_CATEGORY_RAW, url);
                if (icon != null) {
                    Message msg = Message.obtain();
                    msg.what = REFRESH_ICONS;
                    msg.arg1 = url.hashCode();
                    msg.arg2 = LOAD_FROM_LOACAL;
                    msg.obj = icon;
                    mHandler.sendMessage(msg);
                } else {
                    mImageDownloader.postRequest(new ImageFetchRequest(url, mCustomCycleBitmapOpration),
                            new DownloadListener() {

                                @Override
                                public void onDownloadProcess(int fileSize, int downloadSize) {
                                }

                                @Override
                                public void onDownloadFinished(int status, Object response) {
                                    if (response != null && status == ImageDownloader.DOWNLOAD_SUCCESS) {
                                        ImageFetchResponse r = (ImageFetchResponse) response;
                                        Message msg = new Message();
                                        msg.what = REFRESH_ICONS;
                                        msg.arg1 = r.getDownloadUrl().hashCode();
                                        msg.arg2 = LOAD_FROM_SERVER;
                                        msg.obj = r.getmBt();
                                        mHandler.sendMessage(msg);
                                    }
                                }

                            });
                }
            }
        });
    }

    private void setViewListener(View view, final int position, final HotItemModel item, ViewHolder holder) {
        View.OnClickListener itemOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDownloadItemModelList != null && position < mDownloadItemModelList.size()) {
                    if (item.status == DownloadItemModel.UNDOWNLOAD) {
                        if (Config.INDEX != 0) {
                            if (!checkeOfferWallShouldShow()) {
                                return;
                            }
                        }

                        AlertDialog dialog = new AlertDialog.Builder(mActivity)
                                .setMessage(
                                        String.format(mActivity.getString(R.string.download_tips), item.description))
                                .setNegativeButton(R.string.cancel, null)
                                .setPositiveButton(R.string.download, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (!TextUtils.isEmpty(item.downloadUrl)) {
                                            HashMap<String, String> extra = new HashMap<String, String>();
                                            extra.put("name", item.description);
                                            MobclickAgent.onEvent(mContext, Config.DOWNLOAD_ALUBM_HOT, extra);
                                            MobclickAgent.flush(mContext);

                                            if (mProgress != null && !mProgress.isShowing()) {
                                                mProgress.show();
                                            }

                                            int pos = item.downloadUrl.lastIndexOf("session");
                                            int endPost = item.downloadUrl.lastIndexOf(".zip");
                                            if (pos != -1 && endPost != -1) {
                                                String index = item.downloadUrl.substring(pos + "session".length(),
                                                        endPost);
                                                increaseDownloadCount(Integer.valueOf(index));
                                            }

                                            mFileDownloader.postRequest(new DownloadRequest(item.downloadUrl),
                                                    new DownloadListener() {

                                                        @Override
                                                        public void onDownloadProcess(int fileSize, int downloadSize) {

                                                        }

                                                        @Override
                                                        public void onDownloadFinished(int status, Object response) {
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

                                                                // spend point
                                                                int localPoint = SettingManager.getInstance()
                                                                        .getPointInt();
                                                                if (localPoint >= 5) {
                                                                    SettingManager.getInstance().setPointInt(
                                                                            localPoint - 5);
                                                                } else {
                                                                    SettingManager.getInstance().setPointInt(0);
                                                                    PointsManager.getInstance(mContext).spendPoints(5);
                                                                }
                                                            }

                                                            mHandler.sendEmptyMessage(DISMISS_DIALOG);
                                                        }

                                                    });
                                        }
                                    }
                                }).create();
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

        View.OnClickListener itemClickOpen = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mDownloadItemModelList != null && position < mDownloadItemModelList.size()) {
                    int downloadHasdCode = item.getDownloadUrlHashCode();
                    if (downloadHasdCode != 0) {
                        SessionModel sm = SingleInstanceBase.getInstance(SessionModel.class);
                        if (sm != null) {
                            SessionReadModel data = sm.syncQueryDataLocalBy(downloadHasdCode);
                            if (data != null) {
                                Intent intent = new Intent();
                                intent.setClass(mContext, AlbumActivity.class);
                                intent.putExtra(AlbumActivity.KEY_INDEX, data.localFullPath);
                                intent.putExtra(AlbumActivity.KEY_SESSION_NAME, data.sessionName);
                                intent.putExtra(AlbumActivity.KEY_DESC, data.description);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                mContext.startActivity(intent);

                                data.isRead = 1;
                                sm.updateItem(data);
                            }
                        }
                    }
                }
            }
        };

        if (item.status == DownloadItemModel.UNDOWNLOAD) {
            holder.statusIcon.setOnClickListener(itemOnClickListener);
        } else {
            holder.statusIcon.setOnClickListener(itemClickDelete);
        }

        if (item.status == DownloadItemModel.UNDOWNLOAD) {
            view.setOnClickListener(itemOnClickListener);
        } else {
            view.setOnClickListener(itemClickOpen);
        }

        view.setOnLongClickListener(itemLongClickListener);
    }

    private void increaseDownloadCount(final int fileIndex) {
        CustomThreadPool.asyncWork(new Runnable() {
            @Override
            public void run() {
                try {
                    DownloadAlbumResponse response = InternetUtils.request(mContext, new DownloadAlbumRequest(
                            Config.DOMAIN_NAME[Config.INDEX], String.valueOf(fileIndex)));
                    if (response != null) {
                        UtilsConfig.LOGD(response.toString());
                    } else {
                        UtilsConfig.LOGD("response is null");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private boolean checkeOfferWallShouldShow() {
        int localPoint = SettingManager.getInstance().getPointInt();
        int serverPoint = PointsManager.getInstance(mContext).queryPoints();
        int point = localPoint + serverPoint;
        if (point >= 5 || !Config.ADVIEW_SHOW) {
            return true;
        } else {
            String tips = String.format(mContext.getString(R.string.offer_download_tips), point);
            View view = mLayoutInflater.inflate(R.layout.offer_tips_view, null);
            TextView tv = (TextView) view.findViewById(R.id.tips);
            tv.setText(tips);
            AlertDialog dialog = new AlertDialog.Builder(mActivity).setTitle(R.string.tips_title).setView(view)
                    .setPositiveButton(R.string.download, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            OffersManager.getInstance(mActivity).showOffersWall();

                            MobclickAgent.onEvent(mContext, "download_app_open");
                            MobclickAgent.flush(mContext);
                        }
                    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            MobclickAgent.onEvent(mContext, "download_app_cancel");
                            MobclickAgent.flush(mContext);
                        }
                    }).create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
            return false;
        }
    }

    private void onDeleteItem(HotItemModel item) {
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

        item.status = HotItemModel.UNDOWNLOAD;

        Message msg = new Message();
        msg.what = DELETE_ITEM_REFRESH;
        msg.obj = item;
        mHandler.sendMessage(msg);
    }

    private void initProgressBar() {
        if (mProgress == null && mActivity != null) {
            mProgress = new ProgressDialog(mActivity);
            mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgress.setMessage("正在下载中，请稍后...");
            mProgress.setCanceledOnTouchOutside(false);
        }
    }

    private void initUnZipProgressBar() {
        if (mUnZipProgress == null && mActivity != null) {
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
        ImageView newsIcon;
        TextView downloadTV;
    }

    private static void LOGD(String msg) {
        if (Config.DEBUG) {
            UtilsConfig.LOGD(msg);
        }
    }

    @Override
    public void onPause() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onStop() {
        for (ImageView view : mCoverImageView) {
            view.setImageBitmap(null);
        }
    }

    @Override
    public void onDestroy() {
        Config.LOGD("[[HotIAdapter::onDestroy]]");
        
        for (ImageView view : mCoverImageView) {
            view.setImageBitmap(null);
        }
        mCoverImageView.clear();
        
        this.mActivity = null;
        mIconImageViewList = null;
        mDownloadItemModelList.clear();
    }
}