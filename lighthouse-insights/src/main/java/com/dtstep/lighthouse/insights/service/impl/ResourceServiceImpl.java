package com.dtstep.lighthouse.insights.service.impl;

import com.dtstep.lighthouse.common.exception.RoleDefendException;
import com.dtstep.lighthouse.insights.dto.RolePair;
import com.dtstep.lighthouse.insights.enums.OwnerTypeEnum;
import com.dtstep.lighthouse.insights.enums.ResourceTypeEnum;
import com.dtstep.lighthouse.insights.enums.RoleTypeEnum;
import com.dtstep.lighthouse.insights.modal.Resource;
import com.dtstep.lighthouse.insights.modal.Role;
import com.dtstep.lighthouse.insights.service.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResourceServiceImpl implements ResourceService {

    @Autowired
    private RoleService roleService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private ProjectServiceImpl projectService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private StatService statService;

    @Transactional
    @Override
    public RolePair addResourceCallback(Resource resource) {
        List<Role> roleList = new ArrayList<>();
        Role manageRole = null;
        Role accessRole = null;
        String name = null;
        if(resource.getResourceType() == ResourceTypeEnum.Department){
            RoleTypeEnum parentManageRoleType = resource.getResourcePid().intValue() == 0 ? RoleTypeEnum.FULL_MANAGE_PERMISSION : RoleTypeEnum.DEPARTMENT_MANAGE_PERMISSION;
            Role parentManageRole = roleService.queryRole(parentManageRoleType,resource.getResourcePid());
            Integer manageRolePid = parentManageRole.getId();
            RoleTypeEnum parentAccessRoleType = resource.getResourcePid().intValue() == 0 ? RoleTypeEnum.FULL_ACCESS_PERMISSION : RoleTypeEnum.DEPARTMENT_ACCESS_PERMISSION;
            Role parentAccessRole = roleService.queryRole(parentAccessRoleType,resource.getResourcePid());
            Integer accessRolePid = parentAccessRole.getId();
            manageRole = new Role(RoleTypeEnum.DEPARTMENT_MANAGE_PERMISSION,resource.getResourceId(),manageRolePid);
            accessRole = new Role(RoleTypeEnum.DEPARTMENT_ACCESS_PERMISSION,resource.getResourceId(),accessRolePid);
            name = departmentService.queryById(resource.getResourceId()).getName();
        }else if(resource.getResourceType() == ResourceTypeEnum.Project){
            Role parentManageRole = roleService.queryRole(RoleTypeEnum.DEPARTMENT_MANAGE_PERMISSION,resource.getResourcePid());
            Integer manageRolePid = parentManageRole.getId();
            Role parentAccessRole = roleService.queryRole(RoleTypeEnum.DEPARTMENT_ACCESS_PERMISSION,resource.getResourcePid());
            Integer accessRolePid = parentAccessRole.getId();
            manageRole = new Role(RoleTypeEnum.PROJECT_MANAGE_PERMISSION,resource.getResourceId(),manageRolePid);
            accessRole = new Role(RoleTypeEnum.PROJECT_ACCESS_PERMISSION,resource.getResourceId(),accessRolePid);
            name = projectService.queryById(resource.getResourceId()).getTitle();
        }else if(resource.getResourceType() == ResourceTypeEnum.Group){
            Role parentManageRole = roleService.queryRole(RoleTypeEnum.PROJECT_MANAGE_PERMISSION,resource.getResourcePid());
            Integer manageRolePid = parentManageRole.getId();
            Role parentAccessRole = roleService.queryRole(RoleTypeEnum.PROJECT_ACCESS_PERMISSION,resource.getResourcePid());
            Integer accessRolePid = parentAccessRole.getId();
            manageRole = new Role(RoleTypeEnum.GROUP_MANAGE_PERMISSION,resource.getResourceId(),manageRolePid);
            accessRole = new Role(RoleTypeEnum.GROUP_ACCESS_PERMISSION,resource.getResourceId(),accessRolePid);
            name = groupService.queryById(resource.getResourceId()).getToken();
        }else if(resource.getResourceType() == ResourceTypeEnum.Stat){
            Role parentManageRole = roleService.queryRole(RoleTypeEnum.GROUP_MANAGE_PERMISSION,resource.getResourcePid());
            Integer manageRolePid = parentManageRole.getId();
            Role parentAccessRole = roleService.queryRole(RoleTypeEnum.GROUP_ACCESS_PERMISSION,resource.getResourcePid());
            Integer accessRolePid = parentAccessRole.getId();
            manageRole = new Role(RoleTypeEnum.STAT_MANAGE_PERMISSION,resource.getResourceId(),manageRolePid);
            accessRole = new Role(RoleTypeEnum.STAT_ACCESS_PERMISSION,resource.getResourceId(),accessRolePid);
        }else if(resource.getResourceType() == ResourceTypeEnum.Metric){
            Role parentManageRole = roleService.queryRole(RoleTypeEnum.DEPARTMENT_MANAGE_PERMISSION,resource.getResourcePid());
            Integer manageRolePid = parentManageRole.getId();
            Role parentAccessRole = roleService.queryRole(RoleTypeEnum.DEPARTMENT_ACCESS_PERMISSION,resource.getResourcePid());
            Integer accessRolePid = parentAccessRole.getId();
            manageRole = new Role(RoleTypeEnum.METRIC_MANAGE_PERMISSION,resource.getResourceId(),manageRolePid);
            accessRole = new Role(RoleTypeEnum.METRIC_ACCESS_PERMISSION,resource.getResourceId(),accessRolePid);
        }
        Validate.notNull(manageRole);
        Validate.notNull(accessRole);
        manageRole.setDesc(manageRole.getRoleType().name() + "(" + name + ")");
        accessRole.setDesc(accessRole.getRoleType().name() + "(" + name + ")");
        roleService.create(manageRole);
        roleService.create(accessRole);
        return new RolePair(manageRole.getId(),accessRole.getId());
    }

    @Transactional
    @Override
    public void updateResourcePidCallback(Resource resource) {
        List<Role> roleList = new ArrayList<>();
        Role manageRole = null;
        Role accessRole = null;
        String name = null;
        if(resource.getResourceType() == ResourceTypeEnum.Department){
            RoleTypeEnum parentManageRoleType = resource.getResourcePid().intValue() == 0 ? RoleTypeEnum.FULL_MANAGE_PERMISSION : RoleTypeEnum.DEPARTMENT_MANAGE_PERMISSION;
            Role parentManageRole = roleService.queryRole(parentManageRoleType,resource.getResourcePid());
            Integer manageRolePid = parentManageRole.getId();
            RoleTypeEnum parentAccessRoleType = resource.getResourcePid().intValue() == 0 ? RoleTypeEnum.FULL_ACCESS_PERMISSION : RoleTypeEnum.DEPARTMENT_ACCESS_PERMISSION;
            Role parentAccessRole = roleService.queryRole(parentAccessRoleType,resource.getResourcePid());
            Integer accessRolePid = parentAccessRole.getId();
            manageRole = new Role(RoleTypeEnum.DEPARTMENT_MANAGE_PERMISSION,resource.getResourceId(),manageRolePid);
            accessRole = new Role(RoleTypeEnum.DEPARTMENT_ACCESS_PERMISSION,resource.getResourceId(),accessRolePid);
            name = departmentService.queryById(resource.getResourceId()).getName();
        }else if(resource.getResourceType() == ResourceTypeEnum.Project){
            Role parentManageRole = roleService.queryRole(RoleTypeEnum.DEPARTMENT_MANAGE_PERMISSION,resource.getResourcePid());
            Integer manageRolePid = parentManageRole.getId();
            Role parentAccessRole = roleService.queryRole(RoleTypeEnum.DEPARTMENT_ACCESS_PERMISSION,resource.getResourcePid());
            Integer accessRolePid = parentAccessRole.getId();
            manageRole = new Role(RoleTypeEnum.PROJECT_MANAGE_PERMISSION,resource.getResourceId(),manageRolePid);
            accessRole = new Role(RoleTypeEnum.PROJECT_ACCESS_PERMISSION,resource.getResourceId(),accessRolePid);
            name = projectService.queryById(resource.getResourceId()).getTitle();
        }else if(resource.getResourceType() == ResourceTypeEnum.Group){
            Role parentManageRole = roleService.queryRole(RoleTypeEnum.PROJECT_MANAGE_PERMISSION,resource.getResourcePid());
            Integer manageRolePid = parentManageRole.getId();
            Role parentAccessRole = roleService.queryRole(RoleTypeEnum.PROJECT_ACCESS_PERMISSION,resource.getResourcePid());
            Integer accessRolePid = parentAccessRole.getId();
            manageRole = new Role(RoleTypeEnum.GROUP_MANAGE_PERMISSION,resource.getResourceId(),manageRolePid);
            accessRole = new Role(RoleTypeEnum.GROUP_ACCESS_PERMISSION,resource.getResourceId(),accessRolePid);
            name = groupService.queryById(resource.getResourceId()).getToken();
        }else if(resource.getResourceType() == ResourceTypeEnum.Stat){
            Role parentManageRole = roleService.queryRole(RoleTypeEnum.GROUP_MANAGE_PERMISSION,resource.getResourcePid());
            Integer manageRolePid = parentManageRole.getId();
            Role parentAccessRole = roleService.queryRole(RoleTypeEnum.GROUP_ACCESS_PERMISSION,resource.getResourcePid());
            Integer accessRolePid = parentAccessRole.getId();
            manageRole = new Role(RoleTypeEnum.STAT_MANAGE_PERMISSION,resource.getResourceId(),manageRolePid);
            accessRole = new Role(RoleTypeEnum.STAT_ACCESS_PERMISSION,resource.getResourceId(),accessRolePid);
        }else if(resource.getResourceType() == ResourceTypeEnum.Metric){
            Role parentManageRole = roleService.queryRole(RoleTypeEnum.DEPARTMENT_MANAGE_PERMISSION,resource.getResourcePid());
            Integer manageRolePid = parentManageRole.getId();
            Role parentAccessRole = roleService.queryRole(RoleTypeEnum.DEPARTMENT_ACCESS_PERMISSION,resource.getResourcePid());
            Integer accessRolePid = parentAccessRole.getId();
            manageRole = new Role(RoleTypeEnum.METRIC_MANAGE_PERMISSION,resource.getResourceId(),manageRolePid);
            accessRole = new Role(RoleTypeEnum.METRIC_ACCESS_PERMISSION,resource.getResourceId(),accessRolePid);
        }
        Validate.notNull(manageRole);
        Validate.notNull(accessRole);
        manageRole.setDesc(manageRole.getRoleType().name() + "(" + name + ")");
        accessRole.setDesc(accessRole.getRoleType().name() + "(" + name + ")");
        roleService.update(manageRole);
        roleService.update(accessRole);
    }

    @Transactional
    @Override
    public void deleteResourceCallback(Resource resource){
        List<Role> roleList = new ArrayList<>();
        Role manageRole = null;
        Role accessRole = null;
        if(resource.getResourceType() == ResourceTypeEnum.Department){
            manageRole = roleService.queryRole(RoleTypeEnum.DEPARTMENT_MANAGE_PERMISSION,resource.getResourceId());
            accessRole = roleService.queryRole(RoleTypeEnum.DEPARTMENT_ACCESS_PERMISSION,resource.getResourceId());
        }else if(resource.getResourceType() == ResourceTypeEnum.Project){
            manageRole = roleService.queryRole(RoleTypeEnum.PROJECT_MANAGE_PERMISSION,resource.getResourceId());
            accessRole = roleService.queryRole(RoleTypeEnum.PROJECT_ACCESS_PERMISSION,resource.getResourceId());
        }else if(resource.getResourceType() == ResourceTypeEnum.Group){
            manageRole = roleService.queryRole(RoleTypeEnum.GROUP_MANAGE_PERMISSION,resource.getResourceId());
            accessRole = roleService.queryRole(RoleTypeEnum.GROUP_ACCESS_PERMISSION,resource.getResourceId());
        }else if(resource.getResourceType() == ResourceTypeEnum.Stat){
            manageRole = roleService.queryRole(RoleTypeEnum.STAT_MANAGE_PERMISSION,resource.getResourceId());
            accessRole = roleService.queryRole(RoleTypeEnum.STAT_ACCESS_PERMISSION,resource.getResourceId());
        }else if(resource.getResourceType() == ResourceTypeEnum.Metric){
            manageRole = roleService.queryRole(RoleTypeEnum.METRIC_MANAGE_PERMISSION,resource.getResourceId());
            accessRole = roleService.queryRole(RoleTypeEnum.METRIC_ACCESS_PERMISSION,resource.getResourceId());
        }
        Validate.notNull(manageRole);
        Validate.notNull(accessRole);
        List<Role> childManageRoles = roleService.queryListByPid(manageRole.getId(),1,20);
        if(CollectionUtils.isNotEmpty(childManageRoles)){
            String childRoles = childManageRoles.stream().map(z -> String.valueOf(z.getId())).collect(Collectors.joining(","));
            throw new RoleDefendException("can't delete manage role [resourceId:" + resource.getResourceId() + ",roleType:"+resource.getResourceType().name()+"],child roles ["+childRoles+"] exists!" );
        }
        List<Role> childAccessRoles = roleService.queryListByPid(manageRole.getId(),1,20);
        if(CollectionUtils.isNotEmpty(childAccessRoles)){
            String childRoles = childAccessRoles.stream().map(z -> String.valueOf(z.getId())).collect(Collectors.joining(","));
            throw new RoleDefendException("can't delete access role [resourceId:" + resource.getResourceId() + ",roleType:"+resource.getResourceType().name()+"],child roles ["+childRoles+"] exists!" );
        }
        roleService.deleteById(manageRole.getId());
        roleService.deleteById(accessRole.getId());
    }
}