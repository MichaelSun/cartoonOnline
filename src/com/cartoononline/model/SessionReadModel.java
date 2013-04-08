package com.cartoononline.model;

import android.graphics.Bitmap;

import com.plugin.database.dao.annotations.Ignore;
import com.plugin.database.dao.annotations.OrderBy;
import com.plugin.database.dao.annotations.PrimaryKey;

public final class SessionReadModel {

    public String name;
    
    public String coverPath;
    
    public String description;
    
    public String sessionName;
    
    public int isRead;
    
    @PrimaryKey()
    public int localFullPathHashCode;
    
    public String localFullPath;
    
    public String sessionMakeTime;
    
    @OrderBy(order = "DESC")
    public long unzipTime;
    
    public String srcURI;
    
    @Ignore()
    public Bitmap coverBt;

    public SessionReadModel() {
    }
    
    public int getLocalFullPathHashCode() {
        return localFullPathHashCode;
    }

    public void setLocalFullPathHashCode(int localFullPathHashCode) {
        this.localFullPathHashCode = localFullPathHashCode;
    }

    public String getSessionMakeTime() {
        return sessionMakeTime;
    }

    public void setSessionMakeTime(String sessionMakeTime) {
        this.sessionMakeTime = sessionMakeTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCoverPath() {
        return coverPath;
    }

    public void setCoverPath(String coverPath) {
        this.coverPath = coverPath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public int getIsRead() {
        return isRead;
    }

    public void setIsRead(int isRead) {
        this.isRead = isRead;
    }

    public String getLocalFullPath() {
        return localFullPath;
    }

    public void setLocalFullPath(String localFullPath) {
        this.localFullPath = localFullPath;
    }

    public long getUnzipTime() {
        return unzipTime;
    }

    public void setUnzipTime(long unzipTime) {
        this.unzipTime = unzipTime;
    }

    public String getSrcURI() {
        return srcURI;
    }

    public void setSrcURI(String srcURI) {
        this.srcURI = srcURI;
    }

    public Bitmap getCoverBt() {
        return coverBt;
    }

    public void setCoverBt(Bitmap coverBt) {
        this.coverBt = coverBt;
    }

    @Override
    public String toString() {
        return "SessionReadModel [name=" + name + ", coverPath=" + coverPath + ", description=" + description
                + ", sessionName=" + sessionName + ", isRead=" + isRead + ", localFullPath=" + localFullPath
                + ", unzipTime=" + unzipTime + ", srcURI=" + srcURI + ", coverBt=" + coverBt + "]";
    }

}
