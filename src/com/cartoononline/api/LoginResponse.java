package com.cartoononline.api;

import com.plugin.internet.core.ResponseBase;
import com.plugin.internet.core.json.JsonCreator;
import com.plugin.internet.core.json.JsonProperty;

public class LoginResponse extends ResponseBase {

    public static final int CODE_SUCCESS = 1;
    public static final int CODE_UNKNOWN = 0;
    public static final int CODE_USER_EXIST = -1;
    public static final int CODE_PASSWORD_EMPTY = -2;
    public static final int CODE_USER_NOT_EXIST = -3;
    public static final int CODE_PASSWORD_ERROR = -4;

    public int code;

    public String data;

    @JsonCreator
    public LoginResponse(@JsonProperty("code") int code, 
            @JsonProperty("data") String data) {
        this.code = code;
        this.data = data;
    }

    @Override
    public String toString() {
        return "LoginResponse [code=" + code + ", data=" + data + "]";
    }
    
}
