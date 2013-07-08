package com.cartoononline.api;

import com.plugin.internet.core.RequestBase;
import com.plugin.internet.core.annotations.NoNeedTicket;
import com.plugin.internet.core.annotations.RequiredParam;
import com.plugin.internet.core.annotations.RestMethodName;

@NoNeedTicket()
@RestMethodName("user/login")
public class LoginRequest extends RequestBase<LoginResponse> {

    @RequiredParam("u")
    private String mUserName;
    
    @RequiredParam("p")
    private String mPassword;
    
    public LoginRequest(String userName, String password) {
        mUserName = userName;
        mPassword = password;
    }
    
}
