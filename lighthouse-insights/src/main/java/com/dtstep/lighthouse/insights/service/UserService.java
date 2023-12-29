package com.dtstep.lighthouse.insights.service;

import com.dtstep.lighthouse.commonv2.insights.ListData;
import com.dtstep.lighthouse.insights.dto.ChangePasswordParam;
import com.dtstep.lighthouse.insights.dto.UserQueryParam;
import com.dtstep.lighthouse.insights.dto.UserUpdateParam;
import com.dtstep.lighthouse.insights.modal.User;

public interface UserService {

    int create(User user);

    int update(UserUpdateParam user);

    int changePassword(ChangePasswordParam updateParam);

    User queryById(int id);

    User queryAllInfoById(int id);

    User queryByUserName(String userName);

    ListData<User> queryList(UserQueryParam queryParam,Integer pageNum,Integer pageSize);
}
