package com.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("student")
public class Student extends BaseEntity {
    private Long userId;
    private String studentNo;
    private String className;
    private String major;
    private String grade;
    private String idCard;
    private Integer gender;
    private Long teacherId;
    private Long companyId;
    private Long mentorId;
    private LocalDate internStart;
    private LocalDate internEnd;
    private String internStatus;
    private String parentPhone;
}
