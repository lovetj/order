package com.order.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.order.config.FileConfigProperties;
import com.order.dto.LoginDTO;
import com.order.entity.Admin;
import com.order.mapper.AdminMapper;
import com.order.service.AdminService;
import com.order.util.FileUrlUtil;
import com.order.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private FileConfigProperties fileConfigProperties;

    /**
     * 店家账号密码登录
     *
     * 店家账号由数据库/超管预先写入，不提供注册功能。
     */
    @Override
    public Map<String, Object> login(LoginDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getUsername()) || !StringUtils.hasText(dto.getPassword())) {
            throw new RuntimeException("用户名和密码不能为空");
        }
        Admin admin = getByUsername(dto.getUsername());
        if (admin == null || !StringUtils.hasText(admin.getPassword())
                || !BCrypt.checkpw(dto.getPassword(), admin.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        if (admin.getStatus() != null && admin.getStatus() != 1) {
            throw new RuntimeException("账号已被禁用");
        }
        if (!StringUtils.hasText(admin.getShopId())) {
            throw new RuntimeException("该店家账号未绑定店铺，请联系平台管理员");
        }
        // 店铺ID写入 token，后续所有店家端接口据此做数据隔离
        String token = jwtUtil.generateToken(admin.getId(), admin.getUsername(), "merchant", admin.getShopId());

        // 不下发密码哈希
        admin.setPassword(null);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("admin", admin);
        return result;
    }

    @Override
    public Admin getByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, username.trim()));
    }

    @Override
    public Admin getProfile(String adminId) {
        if (!StringUtils.hasText(adminId)) {
            return null;
        }
        Admin admin = getById(adminId);
        if (admin == null) {
            return null;
        }
        admin.setPassword(null);
        admin.setAvatar(toAbsoluteAvatar(admin.getAvatar()));
        return admin;
    }

    @Override
    public Admin updateProfile(String adminId, Admin form) {
        if (!StringUtils.hasText(adminId)) {
            throw new RuntimeException("无法识别账号信息，请重新登录");
        }
        Admin admin = getById(adminId);
        if (admin == null) {
            throw new RuntimeException("账号不存在");
        }
        // 目前仅开放头像；头像统一按相对路径入库
        if (form != null && form.getAvatar() != null) {
            admin.setAvatar(FileUrlUtil.toRelative(form.getAvatar()));
        }
        updateById(admin);
        return getProfile(adminId);
    }

    /** 头像输出完整地址，前端可直接用于 image src */
    private String toAbsoluteAvatar(String avatar) {
        return FileUrlUtil.toAbsolute(avatar, fileConfigProperties.getBaseServer());
    }
}
