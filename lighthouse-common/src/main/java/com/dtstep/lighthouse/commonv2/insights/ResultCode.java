package com.dtstep.lighthouse.commonv2.insights;

public enum ResultCode {

    success("0","success"),

    paramValidateFailed("1","paramValidateFailed"),

    unauthorized("401","unauthorized"),

    accessDenied("403","accessDenied"),

    systemError("500","systemError"),

    loginCheckFailed("1001","loginCheckFailed"),

    authRenewalFailed("1002","authRenewalFailed"),

    registerUserNameExist("1003","registerUserNameExist"),

    userPendApprove("1004","userPendApprove"),

    userStateUnAvailable("1005","userStateUnAvailable"),

    departDelErrorProjectExist("2001","departDelErrorProjectExist"),

    departDelErrorChildExist("2002","departDelErrorChildExist"),

    departCreateErrorLevelLimit("2003","departCreateErrorLevelLimit"),

    ;

    ResultCode(String code , String i18nLabel){
        this.code = code;
        this.i18nLabel = i18nLabel;
    }

    private String code;

    private String i18nLabel;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getI18nLabel() {
        return i18nLabel;
    }

    public void setI18nLabel(String i18nLabel) {
        this.i18nLabel = i18nLabel;
    }
}
