package com.cartoononline.api;

import com.plugin.internet.core.RequestBase;
import com.plugin.internet.core.annotations.NoNeedTicket;
import com.plugin.internet.core.annotations.OptionalParam;
import com.plugin.internet.core.annotations.RestMethodName;

@NoNeedTicket()
@RestMethodName("showbooksparams/")
public class NewSessionRequest extends RequestBase<NewSessionResponse> {

    @OptionalParam("page")
    private int mPageNo;
    
    @OptionalParam("pageSize")
    private int mPageSize;
    
    public NewSessionRequest(int pageNo, int pageSize) {
        mPageNo = pageNo;
        mPageSize = pageSize;
    }
    
}
