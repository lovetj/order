package com.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.order.dto.LoginDTO;
import com.order.dto.PhoneLoginDTO;
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

    /**
     * 顾客手机号登录
     *
     * 小程序端：getPhoneNumber 授权 code -> 微信接口换取真实手机号 -> 按手机号查用户/建档
     * H5 端：直接提交手机号 -> 按手机号查用户/建档
     *
     * @return { token, user, isNewUser }
     */
    Map<String, Object> phoneLogin(PhoneLoginDTO dto);

    User getByUsername(String username);

    User getByOpenid(String openid);

    /**
     * 按手机号查用户
     */
    User getByPhone(String phone);

    /**
     * 更新用户资料（昵称、头像）
     */
    void updateProfile(String userId, String nickname, String avatar);
}
