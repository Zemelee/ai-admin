package com.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mentor")
public class Mentor extends BaseEntity {
    private Long userId;
    private Long companyId;
    private String position;
    private String dept;
}
