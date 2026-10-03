package com.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.order.dto.LoginDTO;
import com.order.entity.Admin;

import java.util.Map;

public interface AdminService extends IService<Admin> {

    /**
     * 店家账号密码登录
     *
     * @return { token, admin }
     */
    Map<String, Object> login(LoginDTO dto);

    Admin getByUsername(String username);

    /**
     * 当前登录店家账号信息（不下发密码，头像输出完整地址）
     */
    Admin getProfile(String adminId);

    /**
     * 更新当前登录店家资料（目前仅头像；库里存相对路径）
     *
     * @return 更新后的账号信息（头像为完整地址）
     */
    Admin updateProfile(String adminId, Admin form);
}
