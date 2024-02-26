package com.dtstep.lighthouse.insights.controller;

import com.dtstep.lighthouse.common.constant.SysConst;
import com.dtstep.lighthouse.commonv2.insights.ResultCode;
import com.dtstep.lighthouse.insights.controller.annotation.AuthPermission;
import com.dtstep.lighthouse.common.modal.IDParam;
import com.dtstep.lighthouse.insights.vo.ResultData;
import com.dtstep.lighthouse.insights.dto.UserQueryParam;
import com.dtstep.lighthouse.common.enums.RoleTypeEnum;
import com.dtstep.lighthouse.common.modal.TreeNode;
import com.dtstep.lighthouse.common.modal.Department;
import com.dtstep.lighthouse.insights.service.DepartmentService;
import com.dtstep.lighthouse.insights.service.ProjectService;
import com.dtstep.lighthouse.insights.service.UserService;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@ControllerAdvice
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserService userService;

    @RequestMapping("/department/structure")
    public ResultData<List<TreeNode>> all() {
        List<TreeNode> list = departmentService.getStructure();
        return ResultData.success(list);
    }

    @AuthPermission(roleTypeEnum = RoleTypeEnum.OPT_MANAGE_PERMISSION)
    @RequestMapping("/department/create")
    public ResultData<Integer> create(@Validated @RequestBody Department createParam) {
        int level = departmentService.getLevel(createParam.getPid());
        if(level >= SysConst.DEPARTMENT_MAX_LEVEL){
            return ResultData.result(ResultCode.departCreateErrorLevelLimit);
        }
        int result = departmentService.create(createParam);
        return ResultData.success(result);
    }

    @AuthPermission(roleTypeEnum = RoleTypeEnum.OPT_MANAGE_PERMISSION)
    @RequestMapping("/department/updateById")
    public ResultData<Integer> updateById(@Validated @RequestBody Department updateParam) {
        int pidLevel = departmentService.getLevel(updateParam.getPid());
        int childLevel = departmentService.getChildLevel(updateParam.getId());
        if(pidLevel + childLevel >= SysConst.DEPARTMENT_MAX_LEVEL){
            return ResultData.result(ResultCode.departCreateErrorLevelLimit);
        }
        int id = departmentService.update(updateParam);
        if(id > 0){
            return ResultData.success(id);
        }else{
            return ResultData.result(ResultCode.systemError);
        }
    }

    @AuthPermission(roleTypeEnum = RoleTypeEnum.OPT_MANAGE_PERMISSION)
    @RequestMapping("/department/deleteById")
    public ResultData<Integer> deleteById(@Validated @RequestBody IDParam idParam) {
        Integer id = idParam.getId();
        Department department = departmentService.queryById(id);
        Validate.notNull(department);
        int childSize = departmentService.countChildByPid(id);
        if(childSize > 0){
            return ResultData.result(ResultCode.departDelErrorChildExist);
        }
        UserQueryParam userQueryParam = new UserQueryParam();
        userQueryParam.setDepartmentIds(List.of(department.getId()));
        int userSize = userService.count(userQueryParam);
        if(userSize > 0){
            return ResultData.result(ResultCode.departDelErrorUserExist);
        }
        int result = departmentService.delete(department);
        if(result == 1){
            return ResultData.success(result);
        }else{
            return ResultData.result(ResultCode.systemError);
        }
    }

}
