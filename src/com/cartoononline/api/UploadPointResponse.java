package com.cartoononline.api;

import com.plugin.internet.core.ResponseBase;
import com.plugin.internet.core.json.JsonCreator;
import com.plugin.internet.core.json.JsonProperty;

public class UploadPointResponse extends ResponseBase {

    public int code;

    public String data;
    
    @JsonCreator
    public UploadPointResponse(@JsonProperty("code") int code, 
            @JsonProperty("data") String data) {
        this.code = code;
        this.data = data;
    }

    @Override
    public String toString() {
        return "UploadPointResponse [code=" + code + ", data=" + data + "]";
    }

}