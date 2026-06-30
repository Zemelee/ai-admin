package com.dto.export;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class WarningExportRow {
    @ExcelProperty("预警级别")
    private String level;

    @ExcelProperty("规则描述")
    private String ruleDesc;

    @ExcelProperty("学生姓名")
    private String studentName;

    @ExcelProperty("学号")
    private String studentNo;

    @ExcelProperty("班级")
    private String className;

    @ExcelProperty("专业")
    private String major;

    @ExcelProperty("详情")
    private String detail;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("生成时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm")
    private LocalDateTime createTime;
}