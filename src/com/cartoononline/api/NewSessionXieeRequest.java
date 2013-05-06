package com.cartoononline.api;

import com.plugin.internet.core.RequestBase;
import com.plugin.internet.core.annotations.NoNeedTicket;
import com.plugin.internet.core.annotations.OptionalParam;
import com.plugin.internet.core.annotations.RestMethodName;

@NoNeedTicket()
@RestMethodName("showbooks/xiee")
public class NewSessionXieeRequest extends RequestBase<NewSessionResponse> {

    @OptionalParam("page")
    private int mPageNo;
    
    @OptionalParam("pageSize")
    private int mPageSize;
    
    public NewSessionXieeRequest(int pageNo, int pageSize) {
        mPageNo = pageNo;
        mPageSize = pageSize;
    }
    
}
