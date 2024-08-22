package com.dtstep.lighthouse.insights.service.impl;

import com.dtstep.lighthouse.common.entity.stat.StatExtEntity;
import com.dtstep.lighthouse.common.enums.PrivateTypeEnum;
import com.dtstep.lighthouse.common.enums.RoleTypeEnum;
import com.dtstep.lighthouse.common.modal.*;
import com.dtstep.lighthouse.core.wrapper.StatDBWrapper;
import com.dtstep.lighthouse.insights.dao.ViewDao;
import com.dtstep.lighthouse.insights.dto.StatQueryParam;
import com.dtstep.lighthouse.insights.service.*;
import com.dtstep.lighthouse.insights.vo.StatVO;
import com.dtstep.lighthouse.insights.vo.ViewVO;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ViewServiceImpl implements ViewService {

    @Autowired
    private ViewDao viewDao;

    @Autowired
    private BaseService baseService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private UserService userService;

    @Override
    public List<ViewVO> queryByIds(List<Integer> ids) {
        ViewQueryParam queryParam = new ViewQueryParam();
        queryParam.setIds(ids);
        List<View> viewList = viewDao.queryList(queryParam);
        List<ViewVO> voList = new ArrayList<>();
        for(View view : viewList){
            ViewVO viewVO = translate(view);
            voList.add(viewVO);
        }
        return voList;
    }

    @Override
    public ViewVO queryById(Integer id) {
        View view = viewDao.queryById(id);
        ViewVO viewVO = null;
        if(view != null){
            viewVO = translate(view);
        }
        return viewVO;
    }

    private ViewVO translate(View view){
        ViewVO viewVO = new ViewVO(view);
        int currentUserId = baseService.getCurrentUserId();
        Role manageRole = roleService.cacheQueryRole(RoleTypeEnum.VIEW_MANAGE_PERMISSION, view.getId());
        Role accessRole = roleService.cacheQueryRole(RoleTypeEnum.VIEW_ACCESS_PERMISSION, view.getId());
        List<Integer> adminIds = permissionService.queryUserPermissionsByRoleId(manageRole.getId(),3);
        if(CollectionUtils.isNotEmpty(adminIds)){
            List<User> admins = adminIds.stream().map(z -> userService.cacheQueryById(z)).collect(Collectors.toList());
            viewVO.setAdmins(admins);
        }
        if(permissionService.checkUserPermission(currentUserId, manageRole.getId())){
            viewVO.addPermission(PermissionEnum.ManageAble);
            viewVO.addPermission(PermissionEnum.AccessAble);
        }else if(view.getPrivateType() == PrivateTypeEnum.Public
                || permissionService.checkUserPermission(currentUserId, accessRole.getId())){
            viewVO.addPermission(PermissionEnum.AccessAble);
        }
        return viewVO;
    }
}