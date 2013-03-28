package com.cartoononline;

import java.util.List;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ReaderListAdapter extends BaseAdapter {

    public static final class ReaderItem {
        public Bitmap image;
        public String description;
    }
    
    private List<ReaderItem> mReaderItems;
    
    private LayoutInflater mLayoutInflater;
    
    public ReaderListAdapter(List<ReaderItem> items, LayoutInflater lf) {
        mReaderItems = items;
        mLayoutInflater = lf;
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
            ret = mLayoutInflater.inflate(R.layout.list_item, null);
        }
        
        ReaderItem item = mReaderItems.get(position);
        
        ((ImageView) ret.findViewById(R.id.item_icon)).setImageBitmap(item.image);
        ((TextView) ret.findViewById(R.id.name)).setText(item.description);
        
        return ret;
    }

}
