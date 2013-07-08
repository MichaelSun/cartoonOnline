package com.cartoononline.api;

import com.plugin.internet.core.RequestBase;
import com.plugin.internet.core.annotations.NoNeedTicket;
import com.plugin.internet.core.annotations.RequiredParam;
import com.plugin.internet.core.annotations.RestMethodName;

@NoNeedTicket()
@RestMethodName("user/point")
public class UploadPointRequest extends RequestBase<UploadPointResponse> {

    @RequiredParam("u")
    private String mUserName;

    @RequiredParam("point")
    private String mPoint;

    public UploadPointRequest(String userName, String point) {
        mUserName = userName;
        mPoint = point;
    }
}
