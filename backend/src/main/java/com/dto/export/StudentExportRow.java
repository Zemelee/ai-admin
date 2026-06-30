package com.dto.export;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StudentExportRow {
    @ExcelProperty("学号")
    private String studentNo;

    @ExcelProperty("姓名")
    private String realName;

    @ExcelProperty("账号")
    private String username;

    @ExcelProperty("班级")
    private String className;

    @ExcelProperty("专业")
    private String major;

    @ExcelProperty("年级")
    private String grade;

    @ExcelProperty("手机号")
    private String phone;

    @ExcelProperty("指导教师")
    private String teacherName;

    @ExcelProperty("实习企业")
    private String companyName;

    @ExcelProperty("企业指导")
    private String mentorName;

    @ExcelProperty("实习状态")
    private String internStatus;

    @ExcelProperty("账号状态")
    private String accountStatus;
}