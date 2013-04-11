package com.cartoononline;

import java.util.List;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.cartoononline.model.DownloadItemModel;

public class DownloadItemAdapter extends BaseAdapter {

    private List<DownloadItemModel> mDownloadItemModelList;
    
    private LayoutInflater mLayoutInflater;
    
    public DownloadItemAdapter(List<DownloadItemModel> data, LayoutInflater lf) {
        mDownloadItemModelList = data;
        mLayoutInflater = lf;
    }
    
    public void setData(List<DownloadItemModel> data) {
        mDownloadItemModelList = data;
        this.notifyDataSetChanged();
    }
    
    @Override
    public int getCount() {
        if (mDownloadItemModelList != null) {
            return mDownloadItemModelList.size();
        }
        
        return 0;
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
        ViewHolder holder = null;
        if (ret == null) {
            ret = mLayoutInflater.inflate(R.layout.download_item, null);
            holder = new ViewHolder();
            holder.icon = (ImageView) ret.findViewById(R.id.item_icon);
            holder.name = (TextView) ret.findViewById(R.id.name);
            holder.description = (TextView) ret.findViewById(R.id.description);
            holder.size = (TextView) ret.findViewById(R.id.size);
            holder.status = (TextView) ret.findViewById(R.id.status);
            ret.setTag(holder);
        } else {
            holder = (ViewHolder) ret.getTag();
        }
        
        DownloadItemModel item = mDownloadItemModelList.get(position);
        holder.name.setText(item.sessionName);
        holder.description.setText(item.description);
        holder.size.setText(item.size);
        holder.status.setText("undownalod");
        holder.status.setBackgroundResource(R.color.download_status);
        
        return ret;
    }

    private class ViewHolder {
        ImageView icon;
        TextView name;
        TextView description;
        TextView size;
        TextView status;
    }
    
}
