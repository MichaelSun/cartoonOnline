package com.cartoononline.api;

import com.plugin.internet.core.ResponseBase;
import com.plugin.internet.core.json.JsonCreator;
import com.plugin.internet.core.json.JsonProperty;

public class DownloadAlbumResponse extends ResponseBase {

    public static final int RESULT_OK = 1;
    public static final int RESULT_ERROR = 0;
    
    public int result;
    
    @JsonCreator
    public DownloadAlbumResponse(@JsonProperty("result") int result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "DownloadAlbumResponse [result=" + result + "]";
    }
    
}
