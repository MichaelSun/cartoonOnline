package com.cartoononline.model;

import android.graphics.Bitmap;

import com.plugin.database.dao.annotations.Ignore;
import com.plugin.database.dao.annotations.PrimaryKey;

public final class DownloadItemModel {
    
    @Ignore()
    public static final int UNDOWNLOAD = 1;
    @Ignore()
    public static final int DOWNLOADED = 2;
    @Ignore()
    public static final int UNZIPED = 3;

    public String localFullPath;
    
    public String downloadUrl;
    
    public String sessionName;
    
    public String time;
    
    public String coverUrl;
    
    public String description;
    
    @PrimaryKey()
    public int downloadUrlHashCode;

    public String size;
    
    @Ignore()
    public Bitmap coverBt;
    
    @Ignore()
    public int status;
    
    public String getLocalFullPath() {
        return localFullPath;
    }

    public void setLocalFullPath(String localFullPath) {
        this.localFullPath = localFullPath;
    }

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

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
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
