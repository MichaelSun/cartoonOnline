package com.cartoononline;


public final class SessionInfo {

    @Override
    public String toString() {
        return "SessionInfo [name=" + name + ", time=" + time + ", cover=" + cover + ", description=" + description
                + "]";
    }

    public String name;
    
    public String time;
    
    public String cover;
    
    public String description;
    
    public String path;
    
    public String sessionName;
    
}
