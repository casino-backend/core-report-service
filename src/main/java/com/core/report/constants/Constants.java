package com.core.report.constants;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;

public class Constants {
    public static final String DATE_TIME_LAYOUT = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_LAYOUT = "yyyy-MM-dd";
    public static final String TIME_LAYOUT = "HH:mm:ss";
    public static final String HOUR_MINUTE_LAYOUT = "HH:mm";

    // SimpleDateFormat objects for parsing and formatting
    public static final DateTimeFormatter DATE_TIME_FORMAT =  DateTimeFormatter.ofPattern(DATE_TIME_LAYOUT);
    public static final DateTimeFormatter DATE_FORMAT =  DateTimeFormatter.ofPattern(DATE_LAYOUT);
    public static final DateTimeFormatter TIME_FORMAT =  DateTimeFormatter.ofPattern(TIME_LAYOUT);
    public static final DateTimeFormatter HOUR_MINUTE_FORMAT =  DateTimeFormatter.ofPattern(HOUR_MINUTE_LAYOUT);
}
