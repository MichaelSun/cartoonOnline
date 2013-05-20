package com.cartoononline.api;

import java.util.Arrays;

import com.plugin.internet.core.ResponseBase;
import com.plugin.internet.core.json.JsonCreator;
import com.plugin.internet.core.json.JsonProperty;

public class NewSessionResponse extends ResponseBase {

    public static class SessionItem {
        public String downloadUrl;
        
        public String imageUrl;
        
        public String description;
        
        public String name;
        
        public String size;
        
        public String time;
        
        public int count;
        
        @JsonCreator
        public SessionItem(
                @JsonProperty("downloadUrl") String downloadUrl,
                @JsonProperty("imageUrl") String imageUrl,
                @JsonProperty("description") String description,
                @JsonProperty("name") String name,
                @JsonProperty("time") String time,
                @JsonProperty("size") String size,
                @JsonProperty("downloadCount") int count) {
            this.downloadUrl = downloadUrl;
            this.imageUrl = imageUrl;
            this.description = description;
            this.name = name;
            this.time = time;
            this.size = size;
            this.count = count;
        }

        @Override
        public String toString() {
            return "SessionItem [downloadUrl=" + downloadUrl + ", imageUrl=" + imageUrl + ", description="
                    + description + ", name=" + name + ", size=" + size + ", time=" + time + "]";
        }
        
    }
    
    public boolean hasmore;
    
    public SessionItem[] items;
    
    @JsonCreator
    public NewSessionResponse(
            @JsonProperty("hasmore") int more,
            @JsonProperty("data") SessionItem[] items) {
        this.hasmore = more == 0 ? false : true;
        this.items = items;
    }

    @Override
    public String toString() {
        return "NewSessionResponse [hasmore=" + hasmore + ", items=" + Arrays.toString(items) + "]";
    }
}
