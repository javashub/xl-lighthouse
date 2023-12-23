package com.dtstep.lighthouse.insights.dao;

import com.dtstep.lighthouse.common.enums.user.UserStateEnum;
import com.dtstep.lighthouse.insights.dto.ChangePasswordParam;
import com.dtstep.lighthouse.insights.dto.UserQueryParam;
import com.dtstep.lighthouse.insights.dto.UserUpdateParam;
import com.dtstep.lighthouse.insights.modal.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserDao {

    boolean isUserNameExist(String username);

    int insert(User user);

    User queryById(int id);

    User queryByUserName(String username);

    List<User> queryList(UserQueryParam queryParam, Integer pageNum,Integer pageSize);

    Integer count(@Param("queryParam")UserQueryParam queryParam);

    int update(UserUpdateParam user);

    int changePassword(ChangePasswordParam updateParam);

    int changePasswd(Integer id,String password);

    int changeState(Integer id, UserStateEnum userStateEnum);
}
