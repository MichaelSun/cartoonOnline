package com.cartoononline;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.cartoononline.model.SessionReadModel;

public class ReaderListAdapter extends BaseAdapter {

    private List<SessionReadModel> mReaderItems;
    
    private LayoutInflater mLayoutInflater;
    
    private Context mContext;
    
    public ReaderListAdapter(List<SessionReadModel> items, LayoutInflater lf, Context context) {
        mReaderItems = items;
        mLayoutInflater = lf;
        mContext = context;
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
            ret = mLayoutInflater.inflate(R.layout.list_item, null);
        }
        
        SessionReadModel item = mReaderItems.get(position);
        
        ((ImageView) ret.findViewById(R.id.item_icon)).setImageBitmap(item.coverBt);
        ((TextView) ret.findViewById(R.id.name)).setText(item.name);
        ((TextView) ret.findViewById(R.id.description)).setText(item.description);
        if (item.isRead != 0) {
            ((TextView) ret.findViewById(R.id.readstatus)).setText(R.string.readed);
            ((TextView) ret.findViewById(R.id.readstatus)).setBackgroundResource(R.color.green);
        } else {
            ((TextView) ret.findViewById(R.id.readstatus)).setText(R.string.unreaded);
            ((TextView) ret.findViewById(R.id.readstatus)).setBackgroundResource(R.color.red);
        }
        
        return ret;
    }

}
