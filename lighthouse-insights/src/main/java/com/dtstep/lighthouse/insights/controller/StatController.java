package com.dtstep.lighthouse.insights.controller;

import com.dtstep.lighthouse.common.util.JsonUtil;
import com.dtstep.lighthouse.commonv2.insights.ListData;
import com.dtstep.lighthouse.commonv2.insights.ResultCode;
import com.dtstep.lighthouse.insights.controller.annotation.AuthPermission;
import com.dtstep.lighthouse.insights.dto.*;
import com.dtstep.lighthouse.insights.enums.RoleTypeEnum;
import com.dtstep.lighthouse.insights.modal.Stat;
import com.dtstep.lighthouse.insights.service.StatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ControllerAdvice
public class StatController {

    @Autowired
    private StatService statService;

    @RequestMapping("/stat/list")
    public ResultData<ListData<StatDto>> list(@Validated @RequestBody ListSearchObject<StatQueryParam> searchObject) {
        ListData<StatDto> listData = statService.queryList(searchObject.getQueryParams(),searchObject.getPagination().getPageNum(),searchObject.getPagination().getPageSize());
        System.out.println("listData:" + JsonUtil.toJSONString(listData));
        return ResultData.success(listData);
    }

    @AuthPermission(roleTypeEnum = RoleTypeEnum.PROJECT_MANAGE_PERMISSION,relationParam = "projectId")
    @RequestMapping("/stat/create")
    public ResultData<Integer> create(@Validated @RequestBody Stat createParam) {
        int id = statService.create(createParam);
        if(id > 0){
            return ResultData.success(id);
        }else{
            return ResultData.failed(ResultCode.systemError);
        }
    }

    @AuthPermission(roleTypeEnum = RoleTypeEnum.PROJECT_MANAGE_PERMISSION,relationParam = "projectId")
    @RequestMapping("/stat/update")
    public ResultData<Integer> update(@Validated @RequestBody Stat createParam) {
        int id = statService.update(createParam);
        if(id > 0){
            return ResultData.success(id);
        }else{
            return ResultData.failed(ResultCode.systemError);
        }
    }

    @AuthPermission(roleTypeEnum = RoleTypeEnum.PROJECT_MANAGE_PERMISSION,relationParam = "projectId")
    @AuthPermission(roleTypeEnum = RoleTypeEnum.OPT_MANAGE_PERMISSION)
    @RequestMapping("/stat/changeState")
    public ResultData<Integer> changeState(@Validated @RequestBody ChangeStatStateParam changeParam) {
        Stat stat = new Stat();
        stat.setState(changeParam.getState());
        stat.setId(changeParam.getId());
        int id = statService.update(stat);
        if(id > 0){
            return ResultData.success(id);
        }else{
            return ResultData.failed(ResultCode.systemError);
        }
    }
}
