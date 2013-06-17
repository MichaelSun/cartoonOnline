package com.cartoononline.api;

import com.plugin.internet.core.RequestBase;
import com.plugin.internet.core.annotations.NoNeedTicket;
import com.plugin.internet.core.annotations.RequiredParam;
import com.plugin.internet.core.annotations.RestMethodName;

@NoNeedTicket()
@RestMethodName("db/list/HotAlbum")
public class HotSessionRequest extends RequestBase<NewSessionResponse> {
    
    @RequiredParam("category")
    private String mCategory;

    public HotSessionRequest(String category) {
        mCategory = category;
    }
}
