package com.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.order.dto.LoginDTO;
import com.order.dto.WxLoginDTO;
import com.order.entity.User;

import java.util.Map;

public interface UserService extends IService<User> {

    /**
     * 账号密码登录
     */
    String login(LoginDTO dto);

    /**
     * 微信小程序登录（code 换取 openid）
     *
     * @return { token, user }
     */
    Map<String, Object> wxLogin(WxLoginDTO dto);

    User getByUsername(String username);

    User getByOpenid(String openid);

    /**
     * 更新用户资料（昵称、头像）
     */
    void updateProfile(String userId, String nickname, String avatar);
}
