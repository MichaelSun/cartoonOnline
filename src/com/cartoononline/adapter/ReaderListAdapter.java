package com.cartoononline.adapter;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.album.disi.R;
import com.cartoononline.model.SessionReadModel;
import com.plugin.common.cache.CacheFactory;
import com.plugin.common.cache.ICacheManager;
import com.plugin.common.utils.UtilsConfig;

public class ReaderListAdapter extends BaseAdapter {

    private List<SessionReadModel> mReaderItems;
    
    private LayoutInflater mLayoutInflater;
    
    private Context mContext;
    
    private ICacheManager<Bitmap> mCacheManager;
    
    public ReaderListAdapter(List<SessionReadModel> items, LayoutInflater lf, Context context) {
        mReaderItems = items;
        mLayoutInflater = lf;
        mContext = context;
        mCacheManager = CacheFactory.getCacheManager(CacheFactory.TYPE_CACHE.TYPE_IMAGE);
    }
    
    public void setReadItems(List<SessionReadModel> items) {
        mReaderItems = items;
        this.notifyDataSetChanged();
    }
    
    @Override
    public int getCount() {
        if (mReaderItems == null) {
            return 0;
        }
        
        return mReaderItems.size();
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
        if (ret == null) {
            ret = mLayoutInflater.inflate(R.layout.reader_item, null);
        }
        
        SessionReadModel item = mReaderItems.get(position);
        
        Bitmap bt = mCacheManager.getResource(UtilsConfig.IMAGE_CACHE_CATEGORY_RAW, item.coverPath);
        ((ImageView) ret.findViewById(R.id.item_icon)).setImageBitmap(bt);
        ((TextView) ret.findViewById(R.id.description)).setText(item.description);
        if (item.isRead != 0) {
            ((TextView) ret.findViewById(R.id.readstatus)).setText(R.string.readed);
            ((TextView) ret.findViewById(R.id.readstatus)).setBackgroundResource(R.drawable.read_bg);
        } else {
            ((TextView) ret.findViewById(R.id.readstatus)).setText(R.string.unreaded);
            ((TextView) ret.findViewById(R.id.readstatus)).setBackgroundResource(R.drawable.unread_bg);
        }
        
        return ret;
    }
    
}
