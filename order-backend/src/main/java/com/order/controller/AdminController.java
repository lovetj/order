package com.order.controller;

import com.order.common.Result;
import com.order.dto.LoginDTO;
import com.order.entity.Admin;
import com.order.service.AdminService;
import com.order.util.AuthUtil;
import com.order.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

/**
 * 店家端 —— 登录 / 账号信息
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 店家账号密码登录（不提供注册入口，账号来自数据库）
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(adminService.login(dto));
    }

    /** 店家账号信息 */
    @GetMapping("/info")
    public Result<Admin> info(@RequestParam String username) {
        Admin admin = adminService.getByUsername(username);
        if (admin != null) {
            admin.setPassword(null);
        }
        return Result.success(admin);
    }

    /** 当前登录店家的账号信息（头像返回完整地址，可直接渲染） */
    @GetMapping("/profile")
    public Result<Admin> profile(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String adminId = AuthUtil.resolveUserId(jwtUtil, authorization, null);
        if (adminId == null) {
            return Result.error(401, "请先登录");
        }
        Admin admin = adminService.getProfile(adminId);
        if (admin == null) {
            return Result.error(404, "账号不存在");
        }
        return Result.success(admin);
    }

    /** 更新当前登录店家资料（目前仅头像） */
    @PutMapping("/profile")
    public Result<Admin> updateProfile(@RequestBody(required = false) Admin form,
                                       @RequestHeader(value = "Authorization", required = false) String authorization) {
        String adminId = AuthUtil.resolveUserId(jwtUtil, authorization, null);
        if (adminId == null) {
            return Result.error(401, "请先登录");
        }
        return Result.success(adminService.updateProfile(adminId, form));
    }
}
