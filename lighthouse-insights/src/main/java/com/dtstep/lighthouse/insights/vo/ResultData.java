package com.dtstep.lighthouse.insights.vo;

import com.dtstep.lighthouse.common.entity.ResultCode;
import com.dtstep.lighthouse.common.entity.ServiceResult;
import com.dtstep.lighthouse.insights.util.SpringUtil;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

public class ResultData<T> {

    private String code;

    private String message;

    private T data;

    private static MessageSource messageSource;

    public ResultData(){}

    public ResultData(ResultCode resultCode,T data){
        messageSource = SpringUtil.getBean(MessageSource.class);
        String message = messageSource.getMessage(resultCode.getI18nLabel(), null, LocaleContextHolder.getLocale());
        this.code = resultCode.getCode();
        this.message = message;
        this.data = data;
    }

    public ResultData(String customMessage){}

    public static<T> ResultData<T> success(T data){
        return new ResultData<T>(ResultCode.success,data);
    }

    public static<T> ResultData<T> success(){
        return new ResultData<T>(ResultCode.success,null);
    }

    public static<T> ResultData<T> result(ResultCode resultCode){
        messageSource = SpringUtil.getBean(MessageSource.class);
        String[] params = resultCode.getParams();
        String message = messageSource.getMessage(resultCode.getI18nLabel(),null,LocaleContextHolder.getLocale());
        if(params != null){
            message = String.format(message,params);
        }
        ResultData resultData = new ResultData();
        resultData.setCode(resultCode.getCode());
        resultData.setMessage(message);
        resultData.setData(null);
        return resultData;
    }

    public static<T> ResultData<T> result(ServiceResult serviceResult){
        ResultCode resultCode = serviceResult.getResultCode();
        messageSource = SpringUtil.getBean(MessageSource.class);
        String[] params = resultCode.getParams();
        String message = messageSource.getMessage(resultCode.getI18nLabel(),null,LocaleContextHolder.getLocale());
        if(params != null){
            message = String.format(message,params);
        }
        ResultData resultData = new ResultData();
        resultData.setCode(resultCode.getCode());
        resultData.setMessage(message);
        resultData.setData(serviceResult.getData());
        return resultData;
    }


    public static<T> ResultData<T> result(ResultCode resultCode, String ...params){
        messageSource = SpringUtil.getBean(MessageSource.class);
        String message = messageSource.getMessage(resultCode.getI18nLabel(),null,LocaleContextHolder.getLocale());
        message = String.format(message,params);
        ResultData resultData = new ResultData();
        resultData.setCode("-1");
        resultData.setMessage(message);
        return resultData;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
