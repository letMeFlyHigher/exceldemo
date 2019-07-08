package com.example.exceldemo.model;

import com.example.exceldemo.annota.ExcelAnnotation;

import java.util.Date;

@ExcelAnnotation(sheetName = "煤业公司采煤工作面抽采达标进尺情况", datePattern = "yyyy-MM-dd", counted = true)
public class ExtractionStandardFootage {

    private Integer id;

    @ExcelAnnotation(headerName = "矿井名称",
            index = 0, level = 0, parentIndex = -1)
    private String mineName;

    @ExcelAnnotation(headerName = "综采工作面",
            index = 1, level = 0, parentIndex = -1)
    private String workFaceName;

    @ExcelAnnotation(headerName = "回风巷（m）",
            index = 2, level = 0, parentIndex = -1)
    private String returnAirWay;

    @ExcelAnnotation(headerName = "防突评价允许进尺",
            index = 3, level = 1, parentIndex = 2)
    private Integer permissionFootageReturn;

    @ExcelAnnotation(headerName = "当日进尺",
            index = 4, level = 1, parentIndex = 2)
    private Integer currentFootageReturn;

    @ExcelAnnotation(headerName = "本循环累计进尺",
            index = 5, level = 1, parentIndex = 2)
    private Integer totalFootageReturn;

    @ExcelAnnotation(headerName = "剩余允许进尺",
            index = 6, level = 1, parentIndex = 2)
    private Integer leftPermissionFootageReturn;

    @ExcelAnnotation(headerName = "进风巷（m）",
            index = 7, level = 0, parentIndex = -1)
    private String intakeAiyWay;

    @ExcelAnnotation(headerName = "防突评价允许进尺",
            index = 8, level = 1, parentIndex = 7)
    private Integer permissionFootageIntake;

    @ExcelAnnotation(headerName = "当日进尺",
            index = 9, level = 1, parentIndex = 7)
    private Integer currentFootageIntake;

    @ExcelAnnotation(headerName = "本循环累计进尺",
            index = 10, level = 1, parentIndex = 7)
    private Integer totalFootageIntake;

    @ExcelAnnotation(headerName = "剩余允许进尺",
            index = 11, level = 1, parentIndex = 7)
    private Integer leftPermissionFootageIntake;

    @ExcelAnnotation(headerName = "备注",
            index = 12, level = 0, parentIndex = -1)
    private String remark;

    private Date createTime;

    private Date updateTime;

    private Integer sysdeptId;

    private static final long serialVersionUID = 1L;

    // 后续构造器及setter和getter省略

}
