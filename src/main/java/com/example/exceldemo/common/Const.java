package com.example.exceldemo.common;



/**
 * 常量类
 *
 * @date 2018/8/4
 */
public class Const {
    public static final String ATTACHMENT = "工作汇报";
    public static final String TRUE = "true";
    public static final String FALSE = "false";
    public static final String COUNT = "序号";

    public interface RowType {
        String TABLE_NAME = "TABLE_NAME";
        String TABLE_HEADER = "TABLE_HEADER";
        String TABLE_CONTENT = "TABLE_CONTENT";
    }

    public interface TimeOfDay {
        String HOUR_MINUTE_SECOND_START = " 00:00:00";
        String HOUR_MINUTE_SECOND_END = " 23:59:59";
    }
}