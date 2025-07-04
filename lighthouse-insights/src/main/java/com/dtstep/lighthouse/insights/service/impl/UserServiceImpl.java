package com.dtstep.lighthouse.insights.service.impl;
/*
 * Copyright (C) 2022-2025 XueLing.雪灵
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.dtstep.lighthouse.common.enums.*;
import com.dtstep.lighthouse.common.modal.PermissionEnum;
import com.dtstep.lighthouse.common.modal.Role;
import com.dtstep.lighthouse.common.modal.User;
import com.dtstep.lighthouse.common.entity.ListData;
import com.dtstep.lighthouse.insights.dao.DepartmentDao;
import com.dtstep.lighthouse.insights.dao.UserDao;
import com.dtstep.lighthouse.insights.dto.PermissionQueryParam;
import com.dtstep.lighthouse.insights.dto.UserQueryParam;
import com.dtstep.lighthouse.insights.service.*;
import com.dtstep.lighthouse.insights.vo.UserVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private DepartmentDao departmentDao;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    private RoleService roleService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private BaseService baseService;

    @Autowired
    private ResourceService resourceService;

    @Transactional
    @Override
    public int create(User user, boolean needApprove) throws Exception{
        user.setState(needApprove?UserStateEnum.USER_PEND:UserStateEnum.USER_NORMAL);
        LocalDateTime localDateTime = LocalDateTime.now();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreateTime(localDateTime);
        user.setUpdateTime(localDateTime);
        user.setLastTime(localDateTime);
        userDao.insert(user);
        int userId = user.getId();
        if(needApprove){
            orderService.submit(user,OrderTypeEnum.USER_PEND_APPROVE,null,null);
        }
        return userId;
    }

    @Override
    public int update(User user) {
        user.setUpdateTime(LocalDateTime.now());
        return userDao.update(user);
    }


    @Override
    @Cacheable(value = "NormalPeriod",key = "#targetClass + '_' + 'queryById' + '_' + #id",cacheManager = "caffeineCacheManager",unless = "#result == null")
    public User cacheQueryById(int id) {
        return userDao.queryById(id);
    }

    @Override
    public boolean isUserNameExist(String username) {
        return userDao.isUserNameExist(username);
    }

    @Override
    public User queryById(int id) {
        return userDao.queryById(id);
    }

    @Override
    public UserStateEnum queryUserState(Integer id) {
        return UserStateEnum.forValue(userDao.queryUserState(id));
    }

    @Override
    public User queryAllInfoByUserName(String userName) {
        return userDao.queryAllInfoByUserName(userName);
    }

    @Override
    public ListData<UserVO> queryList(UserQueryParam queryParam, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum,pageSize);
        PageInfo<User> pageInfo = null;
        List<UserVO> dtoList = new ArrayList<>();
        try{
            List<User> userList = userDao.queryList(queryParam);
            pageInfo = new PageInfo<>(userList);
        }finally {
            PageHelper.clearPage();
        }
        for(User user : pageInfo.getList()){
            UserVO vo = new UserVO(user);
            vo.addPermission(PermissionEnum.ManageAble);
            dtoList.add(vo);
        }
        return ListData.newInstance(dtoList,pageInfo.getTotal(),pageNum,pageSize);
    }

    @Override
    public List<User> termQuery(String search) {
        return userDao.termQuery(search);
    }

    @Override
    public List<User> termQueryByIds(List<Integer> ids) {
        return userDao.termQueryByIds(ids);
    }

    @Transactional
    @Override
    public int deleteById(int id) {
        PermissionQueryParam queryParam = new PermissionQueryParam();
        queryParam.setOwnerId(id);
        queryParam.setOwnerType(OwnerTypeEnum.USER);
        permissionService.delete(queryParam);
        return userDao.deleteById(id);
    }

    @Override
    public int count(UserQueryParam queryParam) {
        return userDao.count(queryParam);
    }

    @Override
    public String queryUserPassword(Integer id) {
        return userDao.queryUserPassword(id);
    }

    @Override
    @Cacheable(value = "NormalPeriod",key = "#targetClass + '_' + 'getUserPermissions' + '_' + #id",cacheManager = "caffeineCacheManager",unless = "#result == null")
    public Set<PermissionEnum> getUserPermissions(Integer id) {
        Role optManageRole = roleService.cacheQueryRole(RoleTypeEnum.OPT_MANAGE_PERMISSION,0);
        boolean hasOptManageRole = permissionService.checkUserPermission(id,optManageRole.getId());
        Role sysManageRole = roleService.cacheQueryRole(RoleTypeEnum.FULL_MANAGE_PERMISSION,0);
        boolean hasSysManageRole = permissionService.checkUserPermission(id,sysManageRole.getId());
        Set<PermissionEnum> sets = new HashSet<>();
        if(hasOptManageRole){
            sets.add(PermissionEnum.OperationManageAble);
        }
        if(hasSysManageRole){
            sets.add(PermissionEnum.SystemManageAble);
        }
        return sets;
    }
}
