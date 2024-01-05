package com.dtstep.lighthouse.insights.service;

import com.dtstep.lighthouse.commonv2.insights.ListData;
import com.dtstep.lighthouse.insights.dto.ChangePasswordParam;
import com.dtstep.lighthouse.insights.dto.ChangeUserStateParam;
import com.dtstep.lighthouse.insights.dto.UserQueryParam;
import com.dtstep.lighthouse.insights.dto.UserUpdateParam;
import com.dtstep.lighthouse.insights.modal.User;

import java.util.List;

public interface UserService {

    void initAdmin();

    int create(User user,boolean needApprove);

    int update(UserUpdateParam user);

    int changePassword(ChangePasswordParam updateParam);

    int changeState(ChangeUserStateParam updateParam);

    List<User> termQuery(String search);

    boolean isUserNameExist(String username);

    User queryBasicInfoById(int id);

    User cacheQueryById(int id);

    User queryAllInfoById(int id);

    User queryByUserName(String userName);

    int deleteById(int userId);

    ListData<User> queryList(UserQueryParam queryParam,Integer pageNum,Integer pageSize);
}
