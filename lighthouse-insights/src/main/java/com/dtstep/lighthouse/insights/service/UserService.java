package com.dtstep.lighthouse.insights.service;

import com.dtstep.lighthouse.commonv2.insights.ListData;
import com.dtstep.lighthouse.insights.dto.ChangePasswordParam;
import com.dtstep.lighthouse.insights.dto.UserDto;
import com.dtstep.lighthouse.insights.dto.UserQueryParam;
import com.dtstep.lighthouse.insights.dto.UserUpdateParam;
import com.dtstep.lighthouse.insights.modal.User;

public interface UserService {

    void initAdmin();

    int create(User user,boolean needApprove);

    int update(UserUpdateParam user);

    int changePassword(ChangePasswordParam updateParam);

    boolean isUserNameExist(String username);

    UserDto queryById(int id);

    UserDto queryAllInfoById(int id);

    UserDto queryByUserName(String userName);

    ListData<User> queryList(UserQueryParam queryParam,Integer pageNum,Integer pageSize);
}
