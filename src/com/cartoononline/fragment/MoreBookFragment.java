package com.cartoononline.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.album.mmall.R;
import com.cartoononline.Config;
import com.plugin.common.utils.UtilsConfig;

public class MoreBookFragment extends Fragment {
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LOGD("[[MoreBookFragment::onCreateView]]");
        
        return inflater.inflate(R.layout.more_book, null);
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LOGD("[[MoreBookFragment::onDestroyView]]");
    }
    
    private static void LOGD(String msg) {
        if (Config.DEBUG) {
            UtilsConfig.LOGD(msg);
        }
    }
}
