package com.dto.export;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ScoreExportRow {
    @ExcelProperty("排名")
    private Integer rank;

    @ExcelProperty("学号")
    private String studentNo;

    @ExcelProperty("姓名")
    private String studentName;

    @ExcelProperty("班级")
    private String className;

    @ExcelProperty("专业")
    private String major;

    @ExcelProperty("实习企业")
    private String companyName;

    @ExcelProperty("实习状态")
    private String internStatus;

    @ExcelProperty("日志得分(25%)")
    private BigDecimal logScore;

    @ExcelProperty("周记得分(25%)")
    private BigDecimal weeklyScore;

    @ExcelProperty("报告得分(20%)")
    private BigDecimal reportScore;

    @ExcelProperty("企业鉴定(30%)")
    private BigDecimal evalScore;

    @ExcelProperty("综合成绩")
    private BigDecimal totalScore;

    @ExcelProperty("等级")
    private String grade;
}