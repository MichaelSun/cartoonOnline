package com.plugin.common.utils.image;

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
import android.graphics.Bitmap;
import android.os.Handler;
import android.text.TextUtils;

import com.plugin.common.cache.CacheFactory;
import com.plugin.common.cache.ICacheManager;
import com.plugin.common.utils.CustomThreadPool;
import com.plugin.common.utils.CustomThreadPool.TaskWrapper;
import com.plugin.common.utils.CustomThreadPool.ThreadPoolSnapShot;
import com.plugin.common.utils.Destroyable;
import com.plugin.common.utils.Environment;
import com.plugin.common.utils.NotifyHandlerObserver;
import com.plugin.common.utils.SingleInstanceBase;
import com.plugin.common.utils.UtilsConfig;
import com.plugin.common.utils.files.DiskManager;
import com.plugin.common.utils.files.DiskManager.DiskCacheType;
import com.plugin.common.utils.files.FileOperatorHelper;
import com.plugin.common.utils.files.FileUtil;
import com.plugin.internet.InternetUtils;
import com.plugin.internet.core.HttpRequestHookListener;

/**
 * 用于后台获取图片，默认使用RRThreadPool作为服务线程，当队列中的服务已经完成以后，等待
 * keepalive设置的时间，如果超出这个时间没有响应，就会释放RRThreadPool中的线程给其他的 模块服务。
 * 
 * 为了提高下载显示效率，默认将新加入的下载任务添加到当前下载队列的最前面。
 * 
 * @author Guoqing Sun Sep 11, 20125:04:07 PM
 */
public class ImageDownloader extends SingleInstanceBase implements Runnable, Destroyable, HttpRequestHookListener {
	private static final String TAG = "[[ImageDownloader]]";
	private static final boolean DEBUG = true & UtilsConfig.UTILS_DEBUG;

	private static final boolean SUPPORT_RANGED = true;

	private static final String DEFAULT_RAW_IMAGE_CATEGORY = UtilsConfig.IMAGE_CACHE_CATEGORY_RAW;

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
	
	public static interface BitmapOperationListener {
		Bitmap onAfterBitmapDownload(Bitmap downloadBt);
	}
	
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
	
	public static final class ImageFetchRequest {
		public static final int TYPE_IMAGE_SMALL = 0;
		public static final int TYPE_IMAGE_MEDIUM = 1;
		public static final int TYPE_IMAGE_LARGE = 2;

		public static final int STATUS_NORMAL = 1000;
		public static final int STATUS_CANCEL = 1001;

		private String mFetchBtUrl;
		private int mUrlHashCode;
		private int mType;
		private int mStatus;
		private String mCategory;
		private BitmapOperationListener mBitmapOperationListener;
		
		private AtomicBoolean requestIsOperating = new AtomicBoolean(false);

		/*
		 * 通过URL下载，默认的type是 MEDIUM，默认的category是 DEFAULT_RAW_IMAGE_CATEGORY
		 */
		public ImageFetchRequest(String downloadUrl) {
			this(TYPE_IMAGE_MEDIUM, downloadUrl);
		}

		/*
		 * 默认的category是 DEFAULT_RAW_IMAGE_CATEGORY
		 */
		public ImageFetchRequest(int type, String downloadUrl) {
			this(type, downloadUrl, DEFAULT_RAW_IMAGE_CATEGORY);
		}

		public ImageFetchRequest(int type, String downloadUrl, String category) {
			this(type, downloadUrl, category, null);
		}

		public ImageFetchRequest(int type, String downloadUrl, String category, BitmapOperationListener l) {
			if (TextUtils.isEmpty(downloadUrl) || TextUtils.isEmpty(category)) {
				throw new IllegalArgumentException("download Image url can't be empty");
			}

			mFetchBtUrl = downloadUrl;
			mType = type;
			mStatus = STATUS_NORMAL;
			mCategory = category;
			mBitmapOperationListener = l;
			mUrlHashCode = mFetchBtUrl.hashCode();
		}
		
		public void cancelDownload() {
		    mStatus = STATUS_CANCEL;
		}

		@Override
		public String toString() {
			return "ImageFetchRequest [mFetchBtUrl=" + mFetchBtUrl + ", mUrlHashCode=" + mUrlHashCode + ", mType="
					+ mType + ", mStatus=" + mStatus + ", mCategory=" + mCategory + ", mBitmapOperationListener="
					+ mBitmapOperationListener + ", requestIsOperating=" + requestIsOperating + "]";
		}

	}

	public static final class ImageFetchResponse {
	    /**
	     * 下载的图片的Bitmap对象，可以直接用于显示.
	     */
		private Bitmap mBt;
		/**
		 * 下载的图片的URL
		 */
		private String mBtUrl;
		/**
		 * 下载的图片的cache的路径，此路径中指向的图片是缓存库的一份磁盘镜像，不要使用此路径对文件进行操作。
		 */
		private String mLocalCachePath;
		/**
		 * 下载的图片的本地存储路径，如果文件下载成功，那么此路劲指向的就是真正的本地图片储存路径，如果文件下载
		 * 失败，或是没有下载完成，那么为空。
		 */
		private String mLocalRawPath;
		/**
		 * 下载请求的Request对象
		 */
		private ImageFetchRequest mRequest;

		private ImageFetchResponse() {
		}

        public Bitmap getmBt() {
            return mBt;
        }

        public String getmBtUrl() {
            return mBtUrl;
        }

        public String getmLocalCachePath() {
            return mLocalCachePath;
        }

        public String getmLocalRawPath() {
            return mLocalRawPath;
        }

        public ImageFetchRequest getmRequest() {
            return mRequest;
        }

        @Override
        public String toString() {
            return "ImageFetchResponse [mBt=" + mBt + ", mBtUrl=" + mBtUrl + ", mLocalCachePath=" + mLocalCachePath
                    + ", mLocalRawPath=" + mLocalRawPath + ", mRequest=" + mRequest + "]";
        }

	}

	public static interface WorkListener {
		void onProcessWork(Runnable r);
	}

	public static final int IMAGE_FETCH_SUCCESS = -20000;
	public static final int IMAGE_FETCH_FAILED = -40000;
//	public static final int IMAGE_DOWNLOAD_PROCESS = -20001;

	private static final int DEFAULT_KEEPALIVE = 5 * 1000;

	private final NotifyHandlerObserver mSuccessHandler = new NotifyHandlerObserver(IMAGE_FETCH_SUCCESS);
	private final NotifyHandlerObserver mFailedHandler = new NotifyHandlerObserver(IMAGE_FETCH_FAILED);
//	private final NotifyHandlerObserver mDownloadProcessHandler = new NotifyHandlerObserver(IMAGE_DOWNLOAD_PROCESS);
	private Object objLock = new Object();
	boolean bIsStop = true;
	
//	boolean bIsRunning = false;
	boolean bIsWaiting = false;
	private ArrayList<ImageFetchRequest> mRequestList;
	private Context mContext;
	private long mKeepAlive;
	private ICacheManager<Bitmap> mCacheManager;
	
	private WorkListener mWorkListener = new WorkListener() {
		@Override
		public void onProcessWork(Runnable r) {
			if (r != null) {
				CustomThreadPool.getInstance().excuteWithSpecialThread(ImageDownloader.class.getSimpleName(), new TaskWrapper(r));
			}
		}
	};

	public static ImageDownloader getInstance(Context context) {
		return SingleInstanceBase.getInstance(ImageDownloader.class);
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

	protected ImageDownloader() {
		super();
	}

	@Override
	protected void init(Context context) {
		mContext = context.getApplicationContext();
		mRequestList = new ArrayList<ImageFetchRequest>();
		bIsStop = false;
		mKeepAlive = DEFAULT_KEEPALIVE;
		mCacheManager = CacheFactory.getCacheManager(CacheFactory.TYPE_CACHE.TYPE_IMAGE);
		mListenerList = Collections.synchronizedList(new LinkedList<DownloadListenerObj>());
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
	
	public boolean postRequest(ImageFetchRequest request, DownloadListener l) {
		if (mRequestList == null || request == null || TextUtils.isEmpty(request.mFetchBtUrl) || l == null) {
			return false;
		}
		
		DownloadListenerObj downloadObj = new DownloadListenerObj(request.mFetchBtUrl, l);
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
	
	public boolean postRequest(ImageFetchRequest request) {
		if (mRequestList == null || request == null || TextUtils.isEmpty(request.mFetchBtUrl)) {
			return false;
		}

		if (DEBUG) {
			UtilsConfig.LOGD_WITH_TIME("<<<<< [[postRequest]] >>>>> ::::::::: " + request.toString());
		}
		synchronized (mRequestList) {
			boolean contain = false;
			for (ImageFetchRequest r : mRequestList) {
				if (r.mUrlHashCode == request.mUrlHashCode) {
					contain = true;
					break;
				}
			}
			if (!contain) {
				mRequestList.add(0, request);

				if (DEBUG) {
					UtilsConfig.LOGD("postRequest, add request : " + request.toString() + " into download list");
				}
			}
			bIsStop = false;
			
			ThreadPoolSnapShot tss = CustomThreadPool.getInstance().getSpecialThreadSnapShot(ImageDownloader.class.getSimpleName());
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

	public void run() {
		InternetUtils.setHttpReturnListener(mContext, SingleInstanceBase.getInstance(ImageDownloader.class));

		while (!bIsStop) {
			waitforUrl();

			if (DEBUG) {
				UtilsConfig.LOGD_WITH_TIME("<<<<< [[run]] >>>>>");
			}
			synchronized (mRequestList) {
				if (mRequestList.size() == 0) {
//					bIsRunning = false;
					bIsStop = true;
					break;
				}
			}
			
			if (DEBUG) {
				UtilsConfig.LOGD_WITH_TIME("<<<<< [[run]]  end synchronized (mRequestList) >>>>>");
			}

			ImageFetchRequest request = null;
			try {
				request = findRequestCanOperate(mRequestList);
				if (request == null) {
					bIsStop = true;
				}
//				synchronized (mRequestList) {
//					if (mRequestList.size() > 0) {
//						// request = mRequestList.remove(0);
//						request = mRequestList.get(0);
//					} else {
//						bIsStop = true;
//					}
//				}
				if (request != null && request.mStatus != ImageFetchRequest.STATUS_CANCEL) {
					if (DEBUG) {
						UtilsConfig.LOGD("================ <<" + Thread.currentThread().getName() + ">> working on : ");
						UtilsConfig.LOGD("begin operate one request : " + request.toString());
						UtilsConfig.LOGD("============================================");
					}

					String cacheFile = InternetUtils.requestBigResourceWithCache(mContext, request.mFetchBtUrl);
					if (DEBUG) {
						UtilsConfig.LOGD("----- after get the cache file : " + cacheFile + " =======");
					}
					if (!TextUtils.isEmpty(cacheFile)) {
						Bitmap bt = null;
						String localPath = null;
						if (android.os.Environment.getExternalStorageState().equals(
								android.os.Environment.MEDIA_MOUNTED)) {
							try {
								if (DEBUG) {
									UtilsConfig.LOGD("return is cache file path : " + cacheFile);
								}

								if (request.mBitmapOperationListener != null) {
                                    mCacheManager.putResource(DEFAULT_RAW_IMAGE_CATEGORY, request.mFetchBtUrl,
                                            cacheFile);
									Bitmap downloadBt = mCacheManager.getResource(DEFAULT_RAW_IMAGE_CATEGORY,
											request.mFetchBtUrl);
									if (downloadBt != null) {
										bt = request.mBitmapOperationListener.onAfterBitmapDownload(downloadBt);
										if (bt != null) {
											mCacheManager.putResource(request.mCategory, request.mFetchBtUrl, bt);
										}

										mCacheManager.releaseResource(DEFAULT_RAW_IMAGE_CATEGORY, request.mFetchBtUrl);
										if (downloadBt != null && !downloadBt.isRecycled()) {
											downloadBt.recycle();
											downloadBt = null;
										}

										if (bt != null) {
											BitmapUtils.makeThumbnail(bt, request.mCategory, request.mFetchBtUrl);
										}
									}
								} else {
									localPath = mCacheManager.putResource(request.mCategory, request.mFetchBtUrl,
											cacheFile);
									bt = mCacheManager.getResource(request.mCategory, request.mFetchBtUrl);

									BitmapUtils.makeThumbnail(bt, request.mCategory, request.mFetchBtUrl);
								}
							} catch (Exception e) {
								e.printStackTrace();
								if (DEBUG) {
									UtilsConfig.LOGD("Excption : ", e);
								}
							}
						} else {
							try {
								bt = ImageUtils.loadBitmapWithSizeOrientation(cacheFile);
							} catch (Exception e) {
								e.printStackTrace();
								if (DEBUG) {
									UtilsConfig.LOGD("Excption : ", e);
								}
							}
						}

						if (bt != null) {
							ImageFetchResponse response = new ImageFetchResponse();
							response.mBt = bt;
							response.mLocalCachePath = localPath;
							response.mBtUrl = request.mFetchBtUrl;
							response.mRequest = request;
							response.mLocalRawPath = cacheFile;
							mSuccessHandler.notifyAll(-1, -1, response);
							
							handleResponseByListener(DOWNLOAD_SUCCESS, request.mFetchBtUrl, response);
							removeRequest(request);

							dumpRequestList();

							continue;
						}
					}
					
					handleResponseByListener(DOWNLOAD_FAILED, request.mFetchBtUrl, request);
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
				handleResponseByListener(DOWNLOAD_FAILED, request.mFetchBtUrl, request);
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
	
	private ImageFetchRequest findRequestCanOperate(ArrayList<ImageFetchRequest> requestList) {
		if (DEBUG) {
			UtilsConfig.LOGD_WITH_TIME("<<<<< [[findRequestCanOperate]] >>>>>");
		}
		
		synchronized (requestList) {
			for (ImageFetchRequest r : requestList) {
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
	
	private void removeRequest(ImageFetchRequest r) {
		synchronized (mRequestList) {
			mRequestList.remove(r);
		}
	}

	public synchronized Boolean isStopped() {
		return bIsStop;
	}

	public void dumpRequestList() {
//		if (DEBUG) {
//			synchronized (mRequestList) {
//				Config.LOGD("-------------- begin current Request List --------------");
//				Config.LOGD("");
//				Config.LOGD("");
//				for (ImageFetchRequest request : mRequestList) {
//					Config.LOGD("request : " + request.toString());
//				}
//				Config.LOGD("");
//				Config.LOGD("");
//				Config.LOGD("-------------- end current Request List --------------");
//			}
//		}
	}

//	private String pathFilter(String src) {
//		return src.replace(":", "+").replace("/", "_").replace(".", "-");
//	}
	
	private void handleProcess(String requestUrl, int fileSize, int downloadSize) {
	    int hashCode = requestUrl.hashCode();
	    for (DownloadListenerObj l : mListenerList) {
	        if (l.code == hashCode && l.mDownloadListener != null) {
	            l.mDownloadListener.onDownloadProcess(fileSize, downloadSize);
	        }
	    }
	}
	
	private ImageFetchRequest findCacelRequest(String requestUrl) {
	    int hashCode = requestUrl.hashCode();
        synchronized (mRequestList) {
            for (ImageFetchRequest r : mRequestList) {
                if (r.mUrlHashCode == hashCode) {
                    return r;
                }
            }
        }
        
        return null;
	}
	
	@Override
	public void onCheckRequestHeaders(String requestUrl, HttpRequestBase request) {
		if (request == null) {
			throw new IllegalArgumentException("Http Request is null");
		}

		if (SUPPORT_RANGED) {
			// 目前只有大文件下载才会做此接口回调，在此回调中可以增加断点续传
			if (request instanceof HttpGet) {
				String saveFile = mDownloadFilenameCreateListener.onFilenameCreateWithDownloadUrl(requestUrl);
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
		// Config.LOGD("entry into >>>>>");
		if (!Environment.isSDCardReady()) {
			UtilsConfig.LOGD("return because unmount the sdcard");
			return null;
		}

		if (is != null) {
			String saveUrl = mDownloadFilenameCreateListener.onFilenameCreateWithDownloadUrl(requestUrl);
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
	                
	                ImageFetchRequest r = findCacelRequest(requestUrl);
	                if (r != null && r.mStatus == ImageFetchRequest.STATUS_CANCEL) {
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
			
//			String savePath = FileOperatorHelper.saveFileByISSupportAppend(INPUT_STREAM_CACHE_PATH + saveUrl, is);
			try {
				is.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (!TextUtils.isEmpty(savePath)) {
				if (ImageUtils.isBitmapData(savePath)) {
					if (DEBUG) {
						long successTime = System.currentTimeMillis();
						UtilsConfig.LOGD("[[onInputStreamReturn]] save Request url : " + saveUrl
								+ " success ||||||| and the saved image size : "
								+ FileUtil.convertStorage(new File(savePath).length()) + ", save cost time = "
								+ (successTime - curTime) + "ms");
					}

					// Config.LOGD("leave <<<<<");
					return savePath;
				} else {
					FileOperatorHelper.DeleteFile(FileUtil.getFileInfo(savePath));
					return null;
				}
			} else {
				// 遗留文件，用于下次的断点下载
				if (DEBUG) {
					UtilsConfig.LOGD("===== failed to downlaod requestUrl : " + requestUrl + " beacuse the debug 断点 =====");
				}
				return null;
			}
		} else {
			if (DEBUG) {
				UtilsConfig.LOGD("===== failed to downlaod requestUrl : " + requestUrl + " beacuse requestUrl is NULL =====");
			}
		}

		return null;
	}

}
