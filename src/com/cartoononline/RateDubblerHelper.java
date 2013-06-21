/**
 * RateDubblerHelper.java
 */
package com.cartoononline;

import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.text.TextUtils;

import com.plugin.common.utils.DebugLog;

public class RateDubblerHelper {

	private static final String TAG = "RateDubblerHelper";

	private static final String RATE_DUBBLER_URI = "market://details?id=";
	private static final String RATE_DUBBLER_BROWSER_URI = "http://play.google.com/store/apps/details?id=";
	
	private static RateDubblerHelper mRateDubblerHelper;
	private Context mContext;

	public synchronized static RateDubblerHelper getInstance(Context context) {
		if (mRateDubblerHelper == null) {
			mRateDubblerHelper = new RateDubblerHelper(context.getApplicationContext());
		}
		return mRateDubblerHelper;
	}

	/**
	 * @param context
	 */
	public RateDubblerHelper(Context context) {
		mContext = context;
	}


	public void OpenApp(String packageName) {

		if (mContext == null || TextUtils.isEmpty(packageName)) {
			return;
		}

		// 如果安装了google play则跳转到google play上此应用的地址
		if (isAvilible(mContext, "com.android.vending")) {
			DebugLog.d(" has installed google play------------------>", "true");
			// 跳转google play
			try {
				Uri downloadUri = Uri.parse(RATE_DUBBLER_URI + packageName);
				Intent it = new Intent(Intent.ACTION_VIEW, downloadUri);
				it.setClassName("com.android.vending", "com.google.android.finsky.activities.LaunchUrlHandlerActivity");
				it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				mContext.startActivity(it);
				return;
			} catch (Exception e) {
				e.printStackTrace();
			}
			// 失败则跳转google Market
			try {
				Uri downloadUri = Uri.parse(RATE_DUBBLER_URI + packageName);
				Intent it = new Intent(Intent.ACTION_VIEW, downloadUri);
				it.setClassName("com.android.vending", "com.android.vending.SearchAssetListActivity");
				it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				mContext.startActivity(it);
				return;
			} catch (Exception e) {
				e.printStackTrace();
			}
			// 仍失败则跳转浏览器
			try {
				Uri downloadUri = Uri.parse(RATE_DUBBLER_BROWSER_URI + packageName);
				Intent it = new Intent(Intent.ACTION_VIEW, downloadUri);
				it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				PackageManager packageManager = mContext.getPackageManager();
				List<ResolveInfo> activities = packageManager.queryIntentActivities(it, 0);
				boolean isIntentSafe = activities.size() > 0;

				// Start an activity if it's safe
				if (isIntentSafe) {
					mContext.startActivity(it);
					DebugLog.d("AboutActivity  ", "Browser is available!");
				} else {
					DebugLog.d("AboutActivity  ", "There is no browser!");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		} else {
			// 没安装google play时，跳google play浏览器
			DebugLog.d(" has installed google play----------------->", "false");
			Uri downloadUri = Uri.parse(RATE_DUBBLER_BROWSER_URI + packageName);
			Intent it = new Intent(Intent.ACTION_VIEW, downloadUri);
			it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			PackageManager packageManager = mContext.getPackageManager();
			List<ResolveInfo> activities = packageManager.queryIntentActivities(it, 0);
			boolean isIntentSafe = activities.size() > 0;

			// Start an activity if it's safe
			if (isIntentSafe) {
				mContext.startActivity(it);
				DebugLog.d("AboutActivity  ", "Browser is available!");
			} else {
				DebugLog.d("AboutActivity  ", "There is no browser!");
			}
		}
	}

	/**
	 * 判断是否安装某程序
	 */
	private boolean isAvilible(Context context, String packageName) {
		final PackageManager packageManager = context.getPackageManager();// 获取packagemanager
		List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);// 获取所有已安装程序的包信息
		List<String> pName = new ArrayList<String>();// 用于存储所有已安装程序的包名
		// 从pinfo中将包名字逐一取出，压入pName list中
		if (pinfo != null) {
			for (int i = 0; i < pinfo.size(); i++) {
				String pn = pinfo.get(i).packageName;
				pName.add(pn);
			}
		}
		return pName.contains(packageName);// 判断pName中是否有目标程序的包名，有TRUE，没有FALSE
	}

}
