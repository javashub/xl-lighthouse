package com.dtstep.lighthouse.common.modal;

import com.dtstep.lighthouse.common.enums.ApproveStateEnum;
import com.dtstep.lighthouse.common.enums.RoleTypeEnum;

import java.io.Serializable;
import java.time.LocalDateTime;

public class OrderDetail implements Serializable {

    private Integer id;

    private Integer orderId;

    private ApproveStateEnum state;

    private Integer roleId;

    private RoleTypeEnum roleType;

    private Integer userId;

    private String reply;

    private LocalDateTime createTime;

    private LocalDateTime processTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public ApproveStateEnum getState() {
        return state;
    }

    public void setState(ApproveStateEnum state) {
        this.state = state;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getProcessTime() {
        return processTime;
    }

    public void setProcessTime(LocalDateTime processTime) {
        this.processTime = processTime;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public RoleTypeEnum getRoleType() {
        return roleType;
    }

    public void setRoleType(RoleTypeEnum roleType) {
        this.roleType = roleType;
    }
}