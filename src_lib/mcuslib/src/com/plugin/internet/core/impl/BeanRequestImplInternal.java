package com.plugin.internet.core.impl;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.plugin.common.utils.UtilsConfig;
import com.plugin.internet.core.BeanRequestInterface;
import com.plugin.internet.core.HttpClientInterface;
import com.plugin.internet.core.HttpConnectHookListener;
import com.plugin.internet.core.InternetConfig;
import com.plugin.internet.core.InternetStringUtils;
import com.plugin.internet.core.JsonUtils;
import com.plugin.internet.core.MultipartHttpEntity;
import com.plugin.internet.core.NetWorkException;
import com.plugin.internet.core.RequestBase;
import com.plugin.internet.core.RequestEntity;

class BeanRequestImplInternal implements BeanRequestInterface {

	private static final String TAG = "BeanRequestImpl";
	private static final boolean DEBUG = InternetConfig.DEBUG;
	private static final boolean DEBUG_SERVER_CODE = false;

	private static final String KEY_TICKET = "t";
	private static final String KEY_APPID = "app_id";
	private static final String KEY_APP_SECRETKEY = "app_secret_key";
	private static final String KEY_USER_SECRETKEY = "user_secret_key";
	private static final String KEY_METHOD = "method";
	private static final String KEY_VERSION = "v";
	private static final String KEY_GZ = "gz";
	private static final String KEY_SIG = "sig";
	private static final String KEY_CLIENT_INFO = "client_info";
	private static final String KEY_CALL_ID = "call_id";

	private static final String BASE_URL = "http://booklink.sinaapp.com/";

	private static BeanRequestImplInternal mInstance;

	private HashMap<String, String> mBaseApiInfo;

	private HttpClientInterface mHttpClientInterface;

	private HttpConnectHookListener mHttpHookListener;

	private static Object lockObject = new Object();

	public static BeanRequestImplInternal getInstance(Context context) {
		if (mInstance == null) {
			synchronized (lockObject) {
				if (mInstance == null) {
					mInstance = new BeanRequestImplInternal(context);
				}
			}
		}
		return mInstance;
	}

	private BeanRequestImplInternal(Context context) {
		mHttpClientInterface = HttpClientFactory.createHttpClientInterface(context);
		mBaseApiInfo = new HashMap<String, String>();
//		mBaseApiInfo.put(KEY_APPID, AppConfig.APP_ID);
//		mBaseApiInfo.put(KEY_APP_SECRETKEY, AppConfig.SECRET_KEY);
//		mBaseApiInfo.put(KEY_VERSION, "1.0");
//		mBaseApiInfo.put(KEY_GZ, "compression");
//		mBaseApiInfo.put(KEY_CLIENT_INFO, Runtime.getClientInfo(context));
	}

	@Override
	public void setRequestAdditionalKVInfo(Map<String, String> kvInfo) {
		if (kvInfo != null) {
			for (String k : kvInfo.keySet()) {
				if (!TextUtils.isEmpty(kvInfo.get(k))) {
					mBaseApiInfo.put(k, kvInfo.get(k));
				} else {
					throw new IllegalArgumentException("Additional info value can't be empty");
				}
			}
		}
	}

	// @Override
	// public void setUserInfo(String ticket, String secret_key) {
	// if (TextUtils.isEmpty(ticket) || TextUtils.isEmpty(secret_key)) {
	// throw new
	// IllegalArgumentException("ticket or secret_key can't be empty");
	// }
	//
	// mBaseApiInfo.put(KEY_TICKET, ticket);
	// mBaseApiInfo.put(KEY_USER_SECRETKEY, secret_key);
	// }

	@Override
	public <T> T request(RequestBase<T> request) throws NetWorkException {
		long entryTime = System.currentTimeMillis();
		if (DEBUG) {
			UtilsConfig.LOGD("Entery Internet request, current time = " + entryTime + "ms from 1970");
		}

		if (request == null) {
			if (mHttpHookListener != null) {
				mHttpHookListener.onHttpConnectError(NetWorkException.REQUEST_NULL, "Request can't be NUll", request);
			}

			throw new NetWorkException(NetWorkException.REQUEST_NULL, "Request can't be NUll", null);
		}

		boolean ignore = request.canIgnoreResult();
		if (!mHttpClientInterface.isNetworkAvailable()) {
			if (!ignore && mHttpHookListener != null) {
				mHttpHookListener.onHttpConnectError(NetWorkException.NETWORK_NOT_AVILABLE, "网络连接错误，请检查您的网络", request);
			}

			throw new NetWorkException(NetWorkException.NETWORK_NOT_AVILABLE, "网络连接错误，请检查您的网络", null);
		}

		RequestEntity requestEntity = request.getRequestEntity();
		Bundle baseParams = requestEntity.getBasicParams();

		if (baseParams == null) {
			if (!ignore && mHttpHookListener != null) {
				mHttpHookListener.onHttpConnectError(NetWorkException.PARAM_EMPTY, "网络请求参数列表不能为空", request);
			}

			throw new NetWorkException(NetWorkException.PARAM_EMPTY, "网络请求参数列表不能为空", null);
		}

		int sessionConfig = request.getSessinConfig();
		if (sessionConfig == RequestBase.NEED_TICKET) {
			if (!mBaseApiInfo.containsKey(KEY_TICKET) || !mBaseApiInfo.containsKey(KEY_USER_SECRETKEY)) {
				if (!ignore && mHttpHookListener != null) {
					mHttpHookListener.onHttpConnectError(NetWorkException.USER_NOT_LOGIN, "用户没有登陆", request);
				}

				throw new NetWorkException(NetWorkException.USER_NOT_LOGIN, "用户没有登陆", null);
			}
		}

		String api_url = BASE_URL;
		String method = null;
		if (baseParams.containsKey("method")) {
			method = baseParams.getString(KEY_METHOD);
			baseParams.remove(KEY_METHOD);
			api_url = api_url + method;
		} else {
			if (!ignore && mHttpHookListener != null) {
				mHttpHookListener.onHttpConnectError(NetWorkException.MISS_API_NAME, "缺少Rest API Method的名字", request);
			}

			throw new NetWorkException(NetWorkException.MISS_API_NAME, "缺少Rest API Method的名字", null);
		}

//		if (!baseParams.containsKey(KEY_CLIENT_INFO)) {
//			if (mBaseApiInfo.containsKey(KEY_CLIENT_INFO)) {
//				baseParams.putString(KEY_CLIENT_INFO, mBaseApiInfo.get(KEY_CLIENT_INFO));
//			}
//		}

//		baseParams.putString(KEY_APPID, mBaseApiInfo.get(KEY_APPID));
//		baseParams.putString(KEY_VERSION, mBaseApiInfo.get(KEY_VERSION));
//		baseParams.putString(KEY_GZ, mBaseApiInfo.get(KEY_GZ));
//		baseParams.putString(KEY_CALL_ID, String.valueOf(System.currentTimeMillis()));
		// baseParams.putString(KEY_GZ, mBaseApiInfo.get(KEY_GZ));
//		String secret_key = "";
//		if (sessionConfig == RequestBase.NEED_TICKET) {
//			secret_key = mBaseApiInfo.get(KEY_USER_SECRETKEY);
//			baseParams.putString(KEY_TICKET, mBaseApiInfo.get(KEY_TICKET));
//		}
//		baseParams.putString(KEY_SIG, getSig(baseParams, secret_key));

		// send the http info to hook listener
		if (mHttpHookListener != null) {
			mHttpHookListener.onPreHttpConnect(BASE_URL, method, baseParams);
		}

		String contentType = requestEntity.getContentType();
		if (contentType == null) {
			if (!ignore && mHttpHookListener != null) {
				mHttpHookListener.onHttpConnectError(NetWorkException.MISS_CONTENT, "Content Type MUST be specified",
						request);
			}

			throw new NetWorkException(NetWorkException.MISS_CONTENT, "Content Type MUST be specified", null);
		}

		if (DEBUG) {
			StringBuilder param = new StringBuilder();
			if (baseParams != null) {
				for (String key : baseParams.keySet()) {
					param.append("|    ").append(key).append(" : ").append(baseParams.get(key)).append("\n");
				}
			}

            UtilsConfig.LOGD("\n\n//***\n| [[request::" + request + "]] \n" + "| RestAPI URL = " + api_url
                    + "\n| after getSig bundle params is = \n" + param + " \n\\\\***\n");
		}

		int size = 0;
		HttpEntity entity = null;
		if (contentType.equals(RequestEntity.REQUEST_CONTENT_TYPE_TEXT_PLAIN)) {
			if (UtilsConfig.DEBUG_NETWORK_ST) {
				if (baseParams != null) {
					for (String key : baseParams.keySet()) {
						size += key.getBytes().length;
						size += baseParams.getString(key).getBytes().length;
					}
				}
			}

			List<NameValuePair> paramList = convertBundleToNVPair(baseParams);
			if (paramList != null) {
				try {
					entity = new UrlEncodedFormEntity(paramList, HTTP.UTF_8);
				} catch (UnsupportedEncodingException e) {
					if (!ignore && mHttpHookListener != null) {
						mHttpHookListener.onHttpConnectError(NetWorkException.ENCODE_HTTP_PARAMS_ERROR,
								"Unable to encode http parameters", request);
					}

					throw new NetWorkException(NetWorkException.ENCODE_HTTP_PARAMS_ERROR,
							"Unable to encode http parameters", null);
				}
			}
		} else if (contentType.equals(RequestEntity.REQUEST_CONTENT_TYPE_MUTIPART)) {
			requestEntity.setBasicParams(baseParams);
			entity = new MultipartHttpEntity(requestEntity);

			if (UtilsConfig.DEBUG_NETWORK_ST) {
				size += ((MultipartHttpEntity) entity).getRequestSize();
			}
		}

		if (DEBUG) {
			UtilsConfig.LOGD("before get internet data from server, time cost from entry = "
					+ (System.currentTimeMillis() - entryTime) + "ms");
		}

		String response = mHttpClientInterface.getResource(String.class, api_url, "POST", entity);
		if (DEBUG_SERVER_CODE) {
			response = "{code:7,data:\"测试code7\"}";
		}

		// if (Config.DEBUG_NETWORK_ST) {
		// String value =
		// DataBaseOperator.getInstance().queryCacheValue(Config.NETWORK_STATISTICS_TYPE,
		// method,
		// Config.NETWORK_STATISTICS_UP);
		// int oldSize = 0;
		// if (!TextUtils.isEmpty(value)) {
		// oldSize = Integer.valueOf(value);
		// }
		// oldSize += size;
		// DataBaseOperator.getInstance().addCacheValue(Config.NETWORK_STATISTICS_TYPE,
		// method,
		// Config.NETWORK_STATISTICS_UP, String.valueOf(oldSize));
		// }

		if (DEBUG) {
			long endTime = System.currentTimeMillis();
			StringBuilder sb = new StringBuilder(1024);
			sb.append("\n\n")
					.append("//***\n")
					.append("| ------------- begin response ------------\n")
					.append("|\n")
					.append("| [[request::" + request + "]] " + " cost time from entry : "
							+ (endTime - entryTime) + "ms. " + "raw response String = \n");
			UtilsConfig.LOGD(sb.toString());
			sb.setLength(0);
			sb.append("| " + response + "\n");
			int step = 1024;
			int index = 0;
			do {
				if (index >= sb.length()) {
					break;
				} else {
					if ((index + step) < sb.length()) {
						UtilsConfig.LOGD(sb.substring(index, index + step), false);
					} else {
						UtilsConfig.LOGD(sb.substring(index, sb.length()), false);
					}
				}
				index = index + step;
			} while (index < sb.length());
			sb.setLength(0);
			sb.append("|\n|\n").append("| ------------- end response ------------\n").append("\\\\***");
			UtilsConfig.LOGD(sb.toString());

			// Config.LOGD("\n\n");
			// Config.LOGD("//***");
			// Config.LOGD("| ------------- begin response ------------");
			// Config.LOGD("|");
			// Config.LOGD("| [[RRConnect::request::" + request + "]] " +
			// " cost time from entry : " + (endTime - entryTime)
			// + "ms. " + "raw response String = ");
			// Config.LOGD("| " + response);
			// Config.LOGD("|");
			// Config.LOGD("| ------------- end response ------------\n|\n|");
			// Config.LOGD("\\\\***");
		}

		if (mHttpHookListener != null) {
			mHttpHookListener.onPostHttpConnect(response, 200);
		}

		if (response == null) {
			if (!ignore && mHttpHookListener != null) {
				mHttpHookListener.onHttpConnectError(NetWorkException.SERVER_ERROR, "服务器错误，请稍后重试", request);
			}

			throw new NetWorkException(NetWorkException.SERVER_ERROR, "服务器错误，请稍后重试", null);
		} else {
			// 调试网络流量
			// if (Config.DEBUG_NETWORK_ST) {
			// String value =
			// DataBaseOperator.getInstance().queryCacheValue(Config.NETWORK_STATISTICS_TYPE,
			// method,
			// Config.NETWORK_STATISTICS_DOWN);
			// int oldSize = 0;
			// if (!TextUtils.isEmpty(value)) {
			// oldSize = Integer.valueOf(value);
			// }
			// oldSize += response.getBytes().length;
			// DataBaseOperator.getInstance().addCacheValue(Config.NETWORK_STATISTICS_TYPE,
			// method,
			// Config.NETWORK_STATISTICS_DOWN, String.valueOf(oldSize));
			// }
		}

//		JsonErrorResponse failureResponse = JsonUtils.parseError(response);
//		if (failureResponse == null) {
//			if (!TextUtils.isEmpty(method) && method.equals("batch.batchRun")) {
//				// 特殊处理batch.batchRun
//				BatchRunResponse responeObj = new BatchRunResponse();
//				BatchRunRequest reqeustObj = (BatchRunRequest) request;
//
//				if (reqeustObj != null && reqeustObj.requestList != null) {
//					responeObj.responseList = new ResponseBase[reqeustObj.requestList.length];
//				}
//
//				try {
//					JSONObject jsonObj = new JSONObject(response);
//					if (reqeustObj.requestList != null) {
//						for (int index = 0; index < reqeustObj.requestList.length; ++index) {
//							String api_name = reqeustObj.requestList[index].getMethodName();
//							if (!TextUtils.isEmpty(api_name)) {
//								String apiData = jsonObj.optString(api_name);
//								if (!TextUtils.isEmpty(apiData)) {
//									JsonErrorResponse fResponse = JsonUtils.parseError(apiData);
//									if (fResponse == null) {
//										ResponseBase oneResponse = (ResponseBase) JsonUtils.parse(apiData,
//												reqeustObj.requestList[index].getGenericType());
//										responeObj.responseList[index] = oneResponse;
//									} else {
//										responeObj.responseList[index] = null;
//									}
//								} else {
//									responeObj.responseList[index] = null;
//								}
//							}
//						}
//					}
//				} catch (Exception e) {
//					e.printStackTrace();
//					return null;
//				}
//
//				return (T) responeObj;
//			} else {
				T ret = JsonUtils.parse(response, request.getGenericType());
				if (DEBUG) {
					UtilsConfig.LOGD("Before return, after success get the data from server, parse cost time from entry = "
							+ (System.currentTimeMillis() - entryTime) + "ms");
				}
				return ret;
//			}
//		} else {
//			if (!ignore && mHttpHookListener != null) {
//				mHttpHookListener.onHttpConnectError(failureResponse.errorCode, failureResponse.errorMsg, request);
//			}
//
//			throw new NetWorkException(failureResponse.errorCode, failureResponse.errorMsg, response);
//		}
	}

	@Override
	public String getSig(Bundle params, String secret_key) {
		if (params == null || params.size() == 0) {
			return null;
		}

		TreeMap<String, String> sortParams = new TreeMap<String, String>();
		for (String key : params.keySet()) {
			sortParams.put(key, params.getString(key));
		}

		Vector<String> vecSig = new Vector<String>();
		for (String key : sortParams.keySet()) {
			String value = sortParams.get(key);
			if (value.length() > InternetConfig.SIG_PARAM_MAX_LENGTH) {
				value = value.substring(0, InternetConfig.SIG_PARAM_MAX_LENGTH);
			}
			vecSig.add(key + "=" + value);
		}
		// LOGD("[[getSig]] after operate, the params is : " + vecSig);

		String[] nameValuePairs = new String[vecSig.size()];
		vecSig.toArray(nameValuePairs);

		for (int i = 0; i < nameValuePairs.length; i++) {
			for (int j = nameValuePairs.length - 1; j > i; j--) {
				if (nameValuePairs[j].compareTo(nameValuePairs[j - 1]) < 0) {
					String temp = nameValuePairs[j];
					nameValuePairs[j] = nameValuePairs[j - 1];
					nameValuePairs[j - 1] = temp;
				}
			}
		}
		StringBuffer nameValueStringBuffer = new StringBuffer();
		for (int i = 0; i < nameValuePairs.length; i++) {
			nameValueStringBuffer.append(nameValuePairs[i]);
		}

		nameValueStringBuffer.append(secret_key);
		String sig = InternetStringUtils.MD5Encode(nameValueStringBuffer.toString());
		return sig;
	}

	private void checkException(int exceptionCode) {
		// switch (exceptionCode) {
		// case RRException.API_EC_INVALID_SESSION_KEY:
		// case RRException.API_EC_USER_AUDIT:
		// case RRException.API_EC_USER_BAND:
		// case RRException.API_EC_USER_SUICIDE:
		// LOGD("[[checkException]] should clean the user info in local");
		// //
		// mAccessTokenManager.clearUserLoginInfoByUid(mAccessTokenManager.getUID());
		// break;
		// default:
		// return;
		// }
	}

	private List<NameValuePair> convertBundleToNVPair(Bundle bundle) {
		if (bundle == null) {
			return null;
		}
		ArrayList<NameValuePair> list = new ArrayList<NameValuePair>();
		Set<String> keySet = bundle.keySet();
		for (String key : keySet) {
			list.add(new BasicNameValuePair(key, bundle.getString(key)));
		}

		return list;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.plugin.internet.core.BeanRequestInterface#setHttpHookListener()
	 */
	@Override
	public void setHttpHookListener(HttpConnectHookListener l) {
		mHttpHookListener = l;
	}

}
