package com.cartoononline.model;

import android.graphics.Bitmap;

import com.plugin.database.dao.annotations.Ignore;
import com.plugin.database.dao.annotations.OrderBy;
import com.plugin.database.dao.annotations.PrimaryKey;

public class DownloadItemModel {
    
    @Ignore()
    public static final int UNDOWNLOAD = 0;
    @Ignore()
    public static final int UNREAD = 0;
    @Ignore()
    public static final int DOWNLOADED = 1;
    @Ignore()
    public static final int UNZIPED = 2;
    @Ignore()
    public static final int DOWNLOAD_READ = 3;

    public String localFullPath;
    
    public String downloadUrl;
    
    public String sessionName;
    
    public String time;
    
    public String coverUrl;
    
    public String description;
    
    @OrderBy(order = "ASC")
    public long downloadTime;
    
    @PrimaryKey()
    public int downloadUrlHashCode;

    public String size;
    
    public int downloadCount;
    
    public int readStatus;
    
    @Ignore()
    public Bitmap coverBt;
    
    @Ignore()
    public int downloadStatus;
    
    public int getReadStatus() {
        return readStatus;
    }

    public void setReadStatus(int readStatus) {
        this.readStatus = readStatus;
    }

    public int getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(int downloadCount) {
        this.downloadCount = downloadCount;
    }

    public long getDownloadTime() {
        return downloadTime;
    }

    public void setDownloadTime(long downloadTime) {
        this.downloadTime = downloadTime;
    }

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
        return "DownloadItemModel [localFullPath=" + localFullPath + ", downloadUrl=" + downloadUrl + ", sessionName="
                + sessionName + ", time=" + time + ", coverUrl=" + coverUrl + ", description=" + description
                + ", downloadUrlHashCode=" + downloadUrlHashCode + ", size=" + size + ", coverBt=" + coverBt
                + ", status=" + downloadStatus + "]";
    }
    
}
