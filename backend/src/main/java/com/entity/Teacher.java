package com.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("teacher")
public class Teacher extends BaseEntity {
    private Long userId;
    private String teacherNo;
    private String department;
    private String title;
    private String officePhone;
}
