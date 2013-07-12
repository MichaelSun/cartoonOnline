package com.cartoononline.api;

import com.plugin.internet.core.RequestBase;
import com.plugin.internet.core.annotations.NoNeedTicket;
import com.plugin.internet.core.annotations.RequiredParam;
import com.plugin.internet.core.annotations.RestMethodName;

@NoNeedTicket()
@RestMethodName("user/registe")
public class RegisteRequest extends RequestBase<RegisteResponse> {

    @RequiredParam("u")
    private String mUserName;

    @RequiredParam("p")
    private String mPassword;

    public RegisteRequest(String userName, String password) {
        mUserName = userName;
        mPassword = password;
    }

}
