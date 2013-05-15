package com.plugin.internet;

import java.io.InputStream;
import java.util.Map;

import android.content.Context;

import com.plugin.internet.core.BeanRequestInterface;
import com.plugin.internet.core.HttpConnectHookListener;
import com.plugin.internet.core.HttpRequestHookListener;
import com.plugin.internet.core.NetWorkException;
import com.plugin.internet.core.RequestBase;
import com.plugin.internet.core.impl.BeanRequestFactory;
import com.plugin.internet.core.impl.HttpClientFactory;

public class InternetUtils {

	/**
	 * 同步接口 发送REST请求
	 * 
	 * @param <T>
	 * @param request
	 *            REST请求
	 * @return REST返回
	 * @throws NetWorkException
	 */
	public static <T> T request(Context context, RequestBase<T> request) throws NetWorkException {
		if (context != null && BeanRequestFactory.createBeanRequestInterface(context.getApplicationContext()) != null) {
			return BeanRequestFactory.createBeanRequestInterface(context.getApplicationContext()).request(request);
		}

		return null;
	}

	public static byte[] requestImageByte(Context context, String imageUrl) throws NetWorkException {
		if (context != null && HttpClientFactory.createHttpClientInterface(context.getApplicationContext()) != null) {
			return HttpClientFactory.createHttpClientInterface(context.getApplicationContext()).getResource(
					byte[].class, imageUrl, "GET", null);
		}

		return null;
	}

	// public static InputStream requestInputStream(Context context, String
	// imageUrl) throws NetWorkException {
	// if
	// (HttpClientFactory.createHttpClientInterface(context.getApplicationContext())
	// != null) {
	// return
	// HttpClientFactory.createHttpClientInterface(context.getApplicationContext())
	// .getResource(InputStream.class, imageUrl, "GET", null);
	// }
	//
	// return null;
	// }

	public static String requestBigResourceWithCache(Context context, String imageUrl) throws NetWorkException {
		if (context != null && HttpClientFactory.createHttpClientInterface(context.getApplicationContext()) != null) {
			return HttpClientFactory.createHttpClientInterface(context.getApplicationContext()).getResource(
					InputStream.class, String.class, imageUrl, "GET", null);
		}

		return null;
	}

	public static void setHttpAdditionalInfo(Context context, Map<String, String> AdditionalInfo) {
		if (context != null && BeanRequestFactory.createBeanRequestInterface(context.getApplicationContext()) != null) {
			BeanRequestFactory.createBeanRequestInterface(context.getApplicationContext()).setRequestAdditionalKVInfo(
					AdditionalInfo);
		}
	}

	public static void setHttpBeanRequestImpl(BeanRequestInterface impl) {
		BeanRequestFactory.setgBeanRequestInterfaceImpl(impl);
	}

	public static void setHttpHookListener(Context context, HttpConnectHookListener l) {
		if (context != null && BeanRequestFactory.createBeanRequestInterface(context.getApplicationContext()) != null) {
			BeanRequestFactory.createBeanRequestInterface(context.getApplicationContext()).setHttpHookListener(l);
		}
	}

	public static void setHttpReturnListener(Context context, HttpRequestHookListener l) {
		if (context != null && HttpClientFactory.createHttpClientInterface(context.getApplicationContext()) != null) {
			HttpClientFactory.createHttpClientInterface(context.getApplicationContext()).setHttpReturnListener(l);
		}
	}

}
