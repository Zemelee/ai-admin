package com.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("company")
public class Company extends BaseEntity {
    private String name;
    private String socialCode;
    private String address;
    private String industry;
    private String contactPerson;
    private String contactPhone;
    private Integer isBlacklist;
    private String blacklistReason;
    private String remark;
}
