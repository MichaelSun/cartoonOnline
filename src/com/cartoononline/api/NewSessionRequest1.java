package com.cartoononline.api;

import com.plugin.internet.core.RequestBase;
import com.plugin.internet.core.annotations.NoNeedTicket;
import com.plugin.internet.core.annotations.OptionalParam;
import com.plugin.internet.core.annotations.RequiredParam;
import com.plugin.internet.core.annotations.RestMethodName;

@NoNeedTicket()
@RestMethodName("showbooks")
public class NewSessionRequest1 extends RequestBase<NewSessionResponse> {

    @OptionalParam("page")
    private int mPageNo;
    
    @OptionalParam("pageSize")
    private int mPageSize;
    
    @RequiredParam("category")
    private String mCategory;
    
    public NewSessionRequest1(int pageNo, int pageSize, String category) {
        mPageNo = pageNo;
        mPageSize = pageSize;
        mCategory = category;
    }
    
}
