package com.cartoononline;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicBoolean;

public class CRuntime {

    public static AtomicBoolean IS_INIT = new AtomicBoolean(false);
 
    private static final String DATE_FORMAT = "yyyyMMdd";
    private static SimpleDateFormat gDateFormat = new SimpleDateFormat(DATE_FORMAT);
    private static Calendar gCalendar = Calendar.getInstance();
    
    public static String CUR_FORMAT_TIME;
    
    public static AccountPointInfo ACCOUNT_POINT_INFO = new AccountPointInfo();
    
    public static String composeTime() {
        gCalendar.setTimeInMillis(System.currentTimeMillis());
        return gDateFormat.format(gCalendar.getTime());
    }
}
