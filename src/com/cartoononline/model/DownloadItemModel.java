package com.cartoononline.model;

import android.graphics.Bitmap;

import com.plugin.database.dao.annotations.Ignore;
import com.plugin.database.dao.annotations.PrimaryKey;

public final class DownloadItemModel {

    public String downloadUrl;
    
    public String sessionName;
    
    public String time;
    
    public String coverUrl;
    
    public String description;
    
    @PrimaryKey()
    public int downloadUrlHashCode;

    public int size;
    
    @Ignore()
    public Bitmap coverBt;

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getDownloadUrlHashCode() {
        return downloadUrlHashCode;
    }

    public void setDownloadUrlHashCode(int downloadUrlHashCode) {
        this.downloadUrlHashCode = downloadUrlHashCode;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public Bitmap getCoverBt() {
        return coverBt;
    }

    public void setCoverBt(Bitmap coverBt) {
        this.coverBt = coverBt;
    }

    @Override
    public String toString() {
        return "DownloadItemModel [downloadUrl=" + downloadUrl + ", sessionName=" + sessionName + ", time=" + time
                + ", coverUrl=" + coverUrl + ", description=" + description + ", downloadUrlHashCode="
                + downloadUrlHashCode + ", size=" + size + ", coverBt=" + coverBt + "]";
    }
    
}
