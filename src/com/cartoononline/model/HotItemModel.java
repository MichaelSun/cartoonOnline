package com.cartoononline.model;

public class HotItemModel extends DownloadItemModel {

    @Override
    public String toString() {
        return "HotItemModel [localFullPath=" + localFullPath + ", downloadUrl=" + downloadUrl + ", sessionName="
                + sessionName + ", time=" + time + ", coverUrl=" + coverUrl + ", description=" + description
                + ", downloadTime=" + downloadTime + ", downloadUrlHashCode=" + downloadUrlHashCode + ", size=" + size
                + ", downloadCount=" + downloadCount + ", coverBt=" + coverBt + ", status=" + downloadStatus + "]";
    }

}
