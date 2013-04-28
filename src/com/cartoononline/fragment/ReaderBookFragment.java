package com.cartoononline.fragment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.GridView;

import com.cartoononline.AlbumActivity;
import com.cartoononline.AppConfig;
import com.cartoononline.CRuntime;
import com.cartoononline.R;
import com.cartoononline.SessionInfo;
import com.cartoononline.SettingManager;
import com.cartoononline.Utils;
import com.cartoononline.adapter.ReaderListAdapter;
import com.cartoononline.model.DownloadItemModel;
import com.cartoononline.model.DownloadModel;
import com.cartoononline.model.SessionModel;
import com.cartoononline.model.SessionReadModel;
import com.plugin.common.utils.CustomThreadPool;
import com.plugin.common.utils.CustomThreadPool.TaskWrapper;
import com.plugin.common.utils.DataModelBase.DataDownloadListener;
import com.plugin.common.utils.SingleInstanceBase;
import com.plugin.common.utils.UtilsConfig;
import com.plugin.common.utils.files.DiskManager;
import com.plugin.common.utils.files.DiskManager.DiskCacheType;
import com.plugin.common.utils.files.FileInfo;
import com.plugin.common.utils.files.FileOperatorHelper;
import com.plugin.common.utils.files.FileUtil;

public class ReaderBookFragment extends Fragment implements FragmentStatusInterface {

    private GridView mGridView;

    private SessionModel mSessionModel;
    
    private DownloadModel mDownloadModel;

    private List<SessionReadModel> mShowSessionList = new ArrayList<SessionReadModel>();

    private ReaderListAdapter mReaderListAdapter;

    private Context mContext;
    
    private LayoutInflater mLayoutInflater;
    
    private ProgressDialog mProgress;
    
    private Activity mActivity;
    
    private int mImageThumbSize;
    private int mImageThumbSpacing;

    private static final int NOTIFY_DATA_CHANGED = 10002;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case NOTIFY_DATA_CHANGED:
                if (mReaderListAdapter == null) {
                    mReaderListAdapter = new ReaderListAdapter(mShowSessionList, mLayoutInflater,
                            mContext.getApplicationContext());
                    if (mGridView != null) {
                        mGridView.setAdapter(mReaderListAdapter);
                    }
                } else {
                    mReaderListAdapter.setReadItems(mShowSessionList);
                }
                
                if (mProgress != null) {
                    mProgress.dismiss();
                }
                break;
            }
        }
    };

    public ReaderBookFragment() {
    }
    
    public ReaderBookFragment(Activity a) {
        mActivity = a;
        mContext = a.getApplicationContext();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSessionModel = SingleInstanceBase.getInstance(SessionModel.class);
        mDownloadModel = SingleInstanceBase.getInstance(DownloadModel.class);
        
        mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
        mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mLayoutInflater = inflater;
        
        View ret = inflater.inflate(R.layout.reader_view, null);
        mGridView = (GridView) ret.findViewById(R.id.gridView);
        
        mGridView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
//                if (mAdapter.getNumColumns() == 0) {
//                    final int numColumns = (int) Math.floor(mGridView.getWidth()
//                            / (mImageThumbSize + mImageThumbSpacing));
//                    if (numColumns > 0) {
//                        final int columnWidth = (mGridView.getWidth() / numColumns) - mImageThumbSpacing;
//                        mAdapter.setNumColumns(numColumns);
//                        mAdapter.setItemHeight(columnWidth);
//                    }
//                }
            }
        });
        initView();
        initProgressBar();
        asyncCheckInternalContent();
        
        return ret;
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        mActivity = null;
        mReaderListAdapter = null;
    }
    
    private void initProgressBar() {
        if (mProgress == null && mActivity != null) {
            mProgress = new ProgressDialog(mActivity);
            mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgress.setMessage("正在加载中，请稍后...");
            mProgress.setCanceledOnTouchOutside(false);
        }
    }
    
    private void initView() {
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SessionReadModel m = mShowSessionList.get(position);

                Intent intent = new Intent();
                intent.setClass(mContext, AlbumActivity.class);
                intent.putExtra(AlbumActivity.KEY_INDEX, m.localFullPath);
                intent.putExtra(AlbumActivity.KEY_SESSION_NAME, m.sessionName);
                intent.putExtra(AlbumActivity.KEY_DESC, m.description);
                startActivity(intent);

                m.isRead = 1;
                mSessionModel.updateItem(m);
            }
        });
        mGridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final SessionReadModel m = mShowSessionList.get(position);
                AlertDialog dialog = new AlertDialog.Builder(getActivity())
                        .setMessage(String.format(getString(R.string.delete_tips), m.name))
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                FileInfo finfo = FileUtil.getFileInfo(m.localFullPath);
                                FileOperatorHelper.DeleteFile(finfo);

                                if (!TextUtils.isEmpty(m.srcURI) && !m.srcURI.startsWith("assets")) {
                                    try {
                                        DownloadItemModel dm = new DownloadItemModel();
                                        dm.setDownloadUrlHashCode(m.srcURI.hashCode());
                                        
                                        UtilsConfig.LOGD("on Delete item : " + dm.toString());
                                        DownloadItemModel data = mDownloadModel.getItem(dm);
//                                        mDownloadModel.deleteItemModel(dm);
                                        
                                        File zipFile = new File(data.localFullPath);
                                        zipFile.delete();

                                        if (data != null) {
                                            data.localFullPath = null;
                                            data.status = DownloadItemModel.UNDOWNLOAD;
                                            mDownloadModel.updateItemModel(data);
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }

                                mSessionModel.deleteItem(m);
                                mShowSessionList.remove(m);
                                mHandler.sendEmptyMessage(NOTIFY_DATA_CHANGED);
                            }
                        }).create();
                dialog.show();

                return true;
            }

        });
    }
    
    private void asyncCheckInternalContent() {
        if (!CRuntime.IS_INIT.get() && mProgress != null) {
            mProgress.show();
        }
        CustomThreadPool.getInstance().excute(new TaskWrapper(new Runnable() {
            @Override
            public void run() {
                int preVersion = SettingManager.getInstance().getPreVersion();
                if (preVersion == 0 || preVersion < UtilsConfig.DEVICE_INFO.versionCode) {
                    //delete old assets infos
                    String deleteName = AppConfig.ROOT_DIR + "session1/";
                    FileInfo finfo = FileUtil.getFileInfo(deleteName);
                    FileOperatorHelper.DeleteFile(finfo);
                    deleteName = AppConfig.ROOT_DIR + "session0/";
                    finfo = FileUtil.getFileInfo(deleteName);
                    FileOperatorHelper.DeleteFile(finfo);
                }
                SettingManager.getInstance().setVersion(UtilsConfig.DEVICE_INFO.versionCode);
                
                checkInternalContent();
            }
        }));
    }

    private void loadSessionData() {
        mSessionModel.asyncLoadDataLocal(new DataDownloadListener() {

            @Override
            public void onDataLoadSuccess(Object loadData) {
                if (loadData != null) {
                    mShowSessionList.clear();
                    mShowSessionList.addAll((List<SessionReadModel>) loadData);

                    mHandler.sendEmptyMessage(NOTIFY_DATA_CHANGED);
                }
            }

            @Override
            public void onDataLoadFailed(Object errorData) {
                // TODO Auto-generated method stub
            }

        });
    }
    
    private void checkInternalContent() {
        if (AppConfig.DEBUG) {
            UtilsConfig.LOGD("[[checkInternalContent]]");
        }
        
        if (CRuntime.IS_INIT.get()) {
            loadSessionData();
            return;
        } else {
            CRuntime.IS_INIT.set(true);
        }

        String[] filenames = Utils.getFileCountUnderAssetsDir(mContext, "");
        if (filenames != null) {
            
            List<SessionReadModel> oldData = mSessionModel.syncLoadDataLocal();
            
            for (String name : filenames) {
                if (AppConfig.DEBUG) {
                    UtilsConfig.LOGD("[[checkInternalContent]] now check file : " + name);
                }

                if (name.startsWith(AppConfig.SESSION_REFIX)) {
                    String sname = name.substring(0, name.lastIndexOf(".zip"));
                    String targetPath = AppConfig.ROOT_DIR + sname + File.separator;
                    SessionInfo sInfo = Utils.getSessionInfo(targetPath);
                    if (sInfo == null) {
                        if (Utils.syncUnzipInternalSessions(mContext, name)) {
                            sInfo = Utils.getSessionInfo(targetPath);
                            if (sInfo != null) {
                                SessionReadModel m = new SessionReadModel();
                                m.isRead = 0;
                                m.localFullPath = targetPath;
                                m.coverPath = sInfo.cover;
                                m.description = sInfo.description;
                                m.name = sInfo.name;
                                m.sessionName = sInfo.sessionName;
                                m.srcURI = "assets/" + name;
                                m.sessionMakeTime = sInfo.time;
                                m.unzipTime = System.currentTimeMillis();
                                m.localFullPathHashCode = m.localFullPath.hashCode();
                                mSessionModel.insertOrRelace(m);
                            }
                        } else {
                            File tFile = new File(targetPath);
                            tFile.deleteOnExit();
                        }
                    } else {
                        if (oldData != null) {
                            int hashCode = targetPath.hashCode();
                            boolean contain = false;
                            for (SessionReadModel d : oldData) {
                                if (d.localFullPathHashCode == hashCode) {
                                    contain = true;
                                }
                            }
                            
                            if (!contain) {
                                SessionReadModel m = new SessionReadModel();
                                m.isRead = 0;
                                m.localFullPath = targetPath;
                                m.coverPath = sInfo.cover;
                                m.description = sInfo.description;
                                m.name = sInfo.name;
                                m.sessionName = sInfo.sessionName;
                                m.srcURI = "assets/" + name;//TODO: this session maybe download from server
                                m.sessionMakeTime = sInfo.time;
                                m.unzipTime = System.currentTimeMillis();
                                m.localFullPathHashCode = m.localFullPath.hashCode();
                                mSessionModel.insertOrRelace(m);
                            }
                        }
                    }
                }
            }
        }

        try {
            Thread.sleep(200);
        } catch (Exception e) {
            e.printStackTrace();
        }

        loadSessionData();
    }

    @Override
    public void onShow() {
        if (mSessionModel != null && mSessionModel.isDataChanged()) {
            loadSessionData();
        }        
    }

    @Override
    public void onForceRefresh() {
        mHandler.sendEmptyMessage(NOTIFY_DATA_CHANGED);
    }
}
