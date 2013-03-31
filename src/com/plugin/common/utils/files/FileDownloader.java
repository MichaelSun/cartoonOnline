package com.plugin.common.utils.files;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;

import com.plugin.common.utils.CustomThreadPool;
import com.plugin.common.utils.CustomThreadPool.TaskWrapper;
import com.plugin.common.utils.CustomThreadPool.ThreadPoolSnapShot;
import com.plugin.common.utils.Destroyable;
import com.plugin.common.utils.Environment;
import com.plugin.common.utils.NotifyHandlerObserver;
import com.plugin.common.utils.SingleInstanceBase;
import com.plugin.common.utils.UtilsConfig;
import com.plugin.common.utils.files.DiskManager.DiskCacheType;
import com.plugin.common.utils.image.ImageDownloader;
import com.plugin.internet.InternetUtils;
import com.plugin.internet.core.HttpRequestHookListener;

public class FileDownloader extends SingleInstanceBase implements Runnable, Destroyable, HttpRequestHookListener {

    private static final String TAG = FileDownloader.class.getSimpleName();
    private static final boolean DEBUG = true & UtilsConfig.UTILS_DEBUG;

    private static final boolean SUPPORT_RANGED = true;

    private static final String INPUT_STREAM_CACHE_PATH = DiskManager
            .tryToFetchCachePathByType(DiskCacheType.INPUTSTREAM_BIG_FILE_CACHE);

    /**
     * 当下载一个文件的时候，通过一个URL生成下载文件的本地的文件名字
     * 
     * @author michael
     *
     */
    public static interface DownloadFilenameCreateListener {
        /**
         * 为一个下载URL生成本地的文件路径，注意：要保证生成文件名字的唯一性
         * 生成的文件会下载到 big_file_cache 文件夹下面
         * 
         * @param downloadUrl
         * @return
         */
        String onFilenameCreateWithDownloadUrl(String downloadUrl);
    }
    
    private final class DefaultDownloadUrlEncodeListener implements DownloadFilenameCreateListener {

        @Override
        public String onFilenameCreateWithDownloadUrl(String downloadUrl) {
            int pos = downloadUrl.lastIndexOf(".");
            int sliptor = downloadUrl.lastIndexOf(File.separator);
            if (pos != -1 && sliptor != -1 && pos > sliptor) {
                String prefix = downloadUrl.substring(0, pos);
                return prefix.replace(":", "+").replace("/", "_").replace(".", "-") + downloadUrl.substring(pos);
            }
            return downloadUrl.replace(":", "+").replace("/", "_").replace(".", "-");
        }
        
    }
    
    private DownloadFilenameCreateListener mDownloadFilenameCreateListener = new DefaultDownloadUrlEncodeListener();
    
    public static final int DOWNLOAD_SUCCESS = 10001;
    public static final int DOWNLOAD_FAILED = 20001;

    public static interface DownloadListener {
        
        void onDownloadProcess(int fileSize, int downloadSize);
        
        void onDownloadFinished(int status, Object response);
    }

    private static class DownloadListenerObj {
        public final DownloadListener mDownloadListener;

        public final String mFileUrl;

        public final int code;

        DownloadListenerObj(String url, DownloadListener listener) {
            mDownloadListener = listener;
            mFileUrl = url;
            code = mFileUrl.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            DownloadListenerObj downloadObj = (DownloadListenerObj) obj;
            if (downloadObj.code == code && downloadObj.mDownloadListener == mDownloadListener) {
                return true;
            }

            return false;
        }
    }

    private List<DownloadListenerObj> mListenerList;

    public static class DownloadRequest {
        public static final int STATUS_NORMAL = 1000;
        public static final int STATUS_CANCEL = 1001;

        public enum DOWNLOAD_TYPE {
            RAW, IMAGE
        }

        protected String mDownloadUrl;
        protected int mUrlHashCode;
        protected DOWNLOAD_TYPE mType;
        protected int mStatus;

        private AtomicBoolean requestIsOperating = new AtomicBoolean(false);

        public DownloadRequest(String downloadUrl) {
            this(DOWNLOAD_TYPE.RAW, downloadUrl);
        }

        public DownloadRequest(DOWNLOAD_TYPE type, String downloadUrl) {
            if (TextUtils.isEmpty(downloadUrl)) {
                throw new IllegalArgumentException("download url can't be empty");
            }

            mDownloadUrl = downloadUrl;
            mType = type;
            mStatus = STATUS_NORMAL;
            mUrlHashCode = mDownloadUrl.hashCode();
        }

        @Override
        public String toString() {
            return "DownloadRequest [mDownloadUrl=" + mDownloadUrl + ", mUrlHashCode=" + mUrlHashCode + ", mType="
                    + mType + ", mStatus=" + mStatus + ", requestIsOperating=" + requestIsOperating + "]";
        }

    }

    public static class DownloadResponse {
        private String mDownloadUrl;
        private String mRawLocalPath;

        private DownloadRequest mRequest;

        private DownloadResponse() {
        }

        public String getLocalPath() {
            return mRawLocalPath;
        }

        public String getDownloadUrl() {
            return mDownloadUrl;
        }

        public DownloadRequest getRequest() {
            return mRequest;
        }
    }

    public static interface WorkListener {
        void onProcessWork(Runnable r);
    }

    public static final int NOTIFY_DOWNLOAD_SUCCESS = -20000;
    public static final int NOTIFY_DOWNLOAD_FAILED = -40000;

    private static final int DEFAULT_KEEPALIVE = 5 * 1000;

    private final NotifyHandlerObserver mSuccessHandler = new NotifyHandlerObserver(NOTIFY_DOWNLOAD_SUCCESS);
    private final NotifyHandlerObserver mFailedHandler = new NotifyHandlerObserver(NOTIFY_DOWNLOAD_FAILED);
    private Object objLock = new Object();
    boolean bIsStop = true;

    boolean bIsWaiting = false;
    private ArrayList<DownloadRequest> mRequestList;
    private Context mContext;
    private long mKeepAlive;

    private WorkListener mWorkListener = new WorkListener() {
        @Override
        public void onProcessWork(Runnable r) {
            if (r != null) {
                CustomThreadPool.getInstance().excuteWithSpecialThread(FileDownloader.class.getSimpleName(),
                        new TaskWrapper(r));
            }
        }
    };

    public static FileDownloader getInstance(Context context) {
        return SingleInstanceBase.getInstance(FileDownloader.class);
    }

    protected FileDownloader() {
        super();
    }

    private void processWorks() {
        mWorkListener.onProcessWork(this);
    }

    public void registeSuccessHandler(Handler handler) {
        mSuccessHandler.registeObserver(handler);
    }

    public void registeFailedHandler(Handler handler) {
        mFailedHandler.registeObserver(handler);
    }

    public void unRegisteSuccessHandler(Handler handler) {
        mSuccessHandler.unRegisteObserver(handler);
    }

    public void unRegisteFailedHandler(Handler handler) {
        mFailedHandler.unRegisteObserver(handler);
    }
    
    public DownloadFilenameCreateListener setDownloadUrlEncodeListener(DownloadFilenameCreateListener l) {
        DownloadFilenameCreateListener ret = mDownloadFilenameCreateListener;
        mDownloadFilenameCreateListener = l;
        
        return ret;
    }

    public synchronized Boolean isStopped() {
        return bIsStop;
    }

    public boolean postRequest(DownloadRequest request, DownloadListener l) {
        if (mRequestList == null || request == null || TextUtils.isEmpty(request.mDownloadUrl) || l == null) {
            return false;
        }

        DownloadListenerObj downloadObj = new DownloadListenerObj(request.mDownloadUrl, l);
        boolean contain = false;
        for (DownloadListenerObj obj : mListenerList) {
            if (downloadObj.equals(obj)) {
                contain = true;
            }
        }
        if (!contain) {
            mListenerList.add(downloadObj);
        }

        return postRequest(request);
    }

    public boolean postRequest(DownloadRequest request) {
        if (mRequestList == null || request == null || TextUtils.isEmpty(request.mDownloadUrl)) {
            return false;
        }

        if (DEBUG) {
            UtilsConfig.LOGD_WITH_TIME("<<<<< [[postRequest]] >>>>> ::::::::: " + request.toString());
        }
        synchronized (mRequestList) {
            boolean contain = false;
            for (DownloadRequest r : mRequestList) {
                if (r.mUrlHashCode == request.mUrlHashCode) {
                    contain = true;
                    break;
                }
            }
            if (!contain) {
                // mRequestList.add(request);
                // 将最新添加的任务放在下载队列的最前面
                mRequestList.add(0, request);

                if (DEBUG) {
                    UtilsConfig.LOGD("postRequest, add request : " + request.toString() + " into download list");
                }
            }
            bIsStop = false;

            ThreadPoolSnapShot tss = CustomThreadPool.getInstance().getSpecialThreadSnapShot(
                    ImageDownloader.class.getSimpleName());
            if (tss == null) {
                return false;
            } else {
                if (tss.taskCount < tss.ALLOWED_MAX_TAKS) {
                    if (DEBUG) {
                        UtilsConfig.LOGD("entry into [[postRequest]] to start process ");
                    }
                    processWorks();
                }
            }
        }
        if (DEBUG) {
            UtilsConfig.LOGD_WITH_TIME("<<<<< [[postRequest]]  end synchronized (mRequestList) >>>>>");
        }

        synchronized (objLock) {
            if (bIsWaiting) {
                bIsWaiting = false;

                if (DEBUG) {
                    UtilsConfig.LOGD("try to notify download process begin");
                }
                objLock.notify();
            }
        }

        if (DEBUG) {
            UtilsConfig.LOGD_WITH_TIME("<<<<< [[postRequest]]  end synchronized (objLock) >>>>>");
        }

        return true;
    }
    
    protected boolean checkInputStreamDownloadFile(String filePath) {
        return true;
    }

    private void waitforUrl() {
        try {
            synchronized (objLock) {
                if (mRequestList.size() == 0 && !bIsWaiting) {
                    bIsWaiting = true;

                    if (DEBUG) {
                        UtilsConfig.LOGD("entry into [[waitforUrl]] for " + DEFAULT_KEEPALIVE + "ms");
                    }
                    objLock.wait(mKeepAlive);

                    if (DEBUG) {
                        UtilsConfig.LOGD("leave [[waitforUrl]] for " + DEFAULT_KEEPALIVE + "ms");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (DEBUG) {
                UtilsConfig.LOGD("Excption : ", e);
            }
        }
        bIsWaiting = false;
    }

    private String pathFilter(String src) {
        return src.replace(":", "+").replace("/", "_").replace(".", "-");
    }

    @Override
    public void onCheckRequestHeaders(String requestUrl, HttpRequestBase request) {
        if (request == null) {
            throw new IllegalArgumentException("Http Request is null");
        }

        if (SUPPORT_RANGED) {
            // 目前只有大文件下载才会做此接口回调，在此回调中可以增加断点续传
            if (request instanceof HttpGet) {
                String saveFile = pathFilter(requestUrl);
                File bigCacheFile = new File(INPUT_STREAM_CACHE_PATH);
                if (!bigCacheFile.exists() || !bigCacheFile.isDirectory()) {
                    bigCacheFile.delete();
                    bigCacheFile.mkdirs();
                }

                File tempFile = new File(INPUT_STREAM_CACHE_PATH + saveFile);
                long fileSize = 0;
                if (tempFile.exists()) {
                    fileSize = tempFile.length();
                } else {
                    fileSize = 0;
                }

                request.addHeader("RANGE", "bytes=" + fileSize + "-");
            }
        }
    }

    @Override
    public String onInputStreamReturn(String requestUrl, InputStream is) {
        if (!Environment.isSDCardReady()) {
            UtilsConfig.LOGD("return because unmount the sdcard");
            return null;
        }

        if (is != null) {
            String saveUrl = pathFilter(requestUrl);
            File bigCacheFile = new File(INPUT_STREAM_CACHE_PATH);
            if (!bigCacheFile.exists() || !bigCacheFile.isDirectory()) {
                bigCacheFile.delete();
                bigCacheFile.mkdirs();
            }

            long curTime = 0;
            if (DEBUG) {
                UtilsConfig.LOGD("try to download from is to local path = " + INPUT_STREAM_CACHE_PATH + saveUrl
                        + " for orgin URL : " + requestUrl);
                curTime = System.currentTimeMillis();
            }
            
            //download file 
            int totalSize = 0;
            try {
                totalSize = is.available();
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            long downloadSize = 0;
            String savePath = null;
            String targetPath = INPUT_STREAM_CACHE_PATH + saveUrl;
            byte[] buffer = new byte[4096 * 2];
            File f = new File(targetPath);
            int len;
            OutputStream os = null;
            
            try {
                if (f.exists()) {
                    downloadSize = f.length();
                }
                
                os = new FileOutputStream(f, true);
                while ((len = is.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                    
                    //add listener to Notify UI
                    downloadSize += len;
                    handleProcess(requestUrl, totalSize, (int) downloadSize);
                    
                    DownloadRequest r = findCacelRequest(requestUrl);
                    if (r != null && r.mStatus == DownloadRequest.STATUS_CANCEL) {
                        UtilsConfig.LOGD("try to close is >>>>>>>>>>>>>>>>>>>>");
                        is.close();
                    }
                }
                savePath = targetPath;
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                buffer = null;
            }
            //end download
            
            try {
                is.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!TextUtils.isEmpty(savePath) && checkInputStreamDownloadFile(savePath)) {
                if (DEBUG) {
                    long successTime = System.currentTimeMillis();
                    UtilsConfig.LOGD("[[onInputStreamReturn]] save Request url : " + saveUrl
                            + " success ||||||| and the saved file size : "
                            + FileUtil.convertStorage(new File(savePath).length()) + ", save cost time = "
                            + (successTime - curTime) + "ms");
                }

                return savePath;
            } else {
                // 遗留文件，用于下次的断点下载
                if (DEBUG) {
                    UtilsConfig.LOGD("===== failed to downlaod requestUrl : " + requestUrl
                            + " beacuse the debug 断点 =====");
                }
                return null;
            }
        } else {
            if (DEBUG) {
                UtilsConfig.LOGD("===== failed to downlaod requestUrl : " + requestUrl
                        + " beacuse requestUrl is NULL =====");
            }
        }

        return null;
    }

    @Override
    public void onDestroy() {
        mSuccessHandler.removeAllObserver();
        mFailedHandler.removeAllObserver();
        synchronized (mRequestList) {
            mRequestList.clear();
        }

        mListenerList.clear();
    }

    @Override
    public void run() {
        InternetUtils.setHttpReturnListener(mContext, SingleInstanceBase.getInstance(ImageDownloader.class));

        while (!bIsStop) {
            waitforUrl();

            if (DEBUG) {
                UtilsConfig.LOGD_WITH_TIME("<<<<< [[run]] >>>>>");
            }
            synchronized (mRequestList) {
                if (mRequestList.size() == 0) {
                    // bIsRunning = false;
                    bIsStop = true;
                    break;
                }
            }

            if (DEBUG) {
                UtilsConfig.LOGD_WITH_TIME("<<<<< [[run]]  end synchronized (mRequestList) >>>>>");
            }

            DownloadRequest request = null;
            try {
                request = findRequestCanOperate(mRequestList);
                if (request == null) {
                    bIsStop = true;
                }
                if (request != null && request.mStatus != DownloadRequest.STATUS_CANCEL) {
                    if (DEBUG) {
                        UtilsConfig.LOGD("================ <<" + Thread.currentThread().getName() + ">> working on : ");
                        UtilsConfig.LOGD("begin operate one request : " + request.toString());
                        UtilsConfig.LOGD("============================================");
                    }

                    String cacheFile = InternetUtils.requestBigResourceWithCache(mContext, request.mDownloadUrl);
                    if (DEBUG) {
                        UtilsConfig.LOGD("----- after get the cache file : " + cacheFile + " =======");
                    }
                    if (!TextUtils.isEmpty(cacheFile)) {

                        DownloadResponse response = new DownloadResponse();
                        response.mDownloadUrl = request.mDownloadUrl;
                        response.mRawLocalPath = cacheFile;
                        response.mRequest = request;
                        
                        //notify success
                        mSuccessHandler.notifyAll(-1, -1, response);
                        handleResponseByListener(DOWNLOAD_SUCCESS, request.mDownloadUrl, response);
                        removeRequest(request);

                        continue;
                    }

                    handleResponseByListener(DOWNLOAD_FAILED, request.mDownloadUrl, request);
                    mFailedHandler.notifyAll(-1, -1, request);

                    if (DEBUG) {
                        UtilsConfig.LOGD("success end operate one request : " + request);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (DEBUG) {
                    UtilsConfig.LOGD("Exception : ", e);
                    UtilsConfig.LOGD("exception end operate one request : " + request);
                }
                
                handleResponseByListener(DOWNLOAD_FAILED, request.mDownloadUrl, request);
                mFailedHandler.notifyAll(-1, -1, request);
            }

            removeRequest(request);
        }

        InternetUtils.setHttpReturnListener(mContext, null);
    }

    private void handleResponseByListener(int status, String fetchUrl, Object notfiyObj) {
        if (mListenerList.size() > 0) {
            int curCode = fetchUrl.hashCode();
            LinkedList<DownloadListenerObj> removeObj = new LinkedList<DownloadListenerObj>();
            for (DownloadListenerObj d : mListenerList) {
                if (d.code == curCode) {
                    d.mDownloadListener.onDownloadFinished(status, notfiyObj);
                    removeObj.add(d);
                }
            }
            mListenerList.removeAll(removeObj);
        }
    }

    private DownloadRequest findRequestCanOperate(ArrayList<DownloadRequest> requestList) {
        if (DEBUG) {
            UtilsConfig.LOGD_WITH_TIME("<<<<< [[findRequestCanOperate]] >>>>>");
        }

        synchronized (requestList) {
            for (DownloadRequest r : requestList) {
                if (!r.requestIsOperating.get()) {
                    r.requestIsOperating.set(true);

                    if (DEBUG) {
                        UtilsConfig.LOGD_WITH_TIME("<<<<< [[findRequestCanOperate]] end findRequestCanOperate >>>>>");
                    }
                    return r;
                }
            }

            return null;
        }
    }

    private void removeRequest(DownloadRequest r) {
        synchronized (mRequestList) {
            mRequestList.remove(r);
        }
    }
    
    private void handleProcess(String requestUrl, int fileSize, int downloadSize) {
        int hashCode = requestUrl.hashCode();
        for (DownloadListenerObj l : mListenerList) {
            if (l.code == hashCode && l.mDownloadListener != null) {
                l.mDownloadListener.onDownloadProcess(fileSize, downloadSize);
            }
        }
    }
    
    private DownloadRequest findCacelRequest(String requestUrl) {
        int hashCode = requestUrl.hashCode();
        synchronized (mRequestList) {
            for (DownloadRequest r : mRequestList) {
                if (r.mUrlHashCode == hashCode) {
                    return r;
                }
            }
        }
        
        return null;
    }

    @Override
    protected void init(Context context) {
        mContext = context.getApplicationContext();
        mRequestList = new ArrayList<DownloadRequest>();
        bIsStop = false;
        mKeepAlive = DEFAULT_KEEPALIVE;
        mListenerList = Collections.synchronizedList(new LinkedList<DownloadListenerObj>());
    }

}
