package com.cartoononline.api;

import com.plugin.internet.core.RequestBase;
import com.plugin.internet.core.annotations.NoNeedTicket;
import com.plugin.internet.core.annotations.RequiredParam;
import com.plugin.internet.core.annotations.RestMethodName;

@NoNeedTicket()
@RestMethodName("album/download")
public class DownloadAlbumRequest extends RequestBase<DownloadAlbumResponse> {

    @RequiredParam("domain")
    public String domain;
    
    @RequiredParam("id")
    public String id;
    
    public DownloadAlbumRequest(String domain, String fileIndex) {
        this.domain = domain;
        this.id = fileIndex;
    }
}
