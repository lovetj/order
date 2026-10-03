package com.order.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.common.Result;
import com.order.util.AuthUtil;
import com.order.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

/**
 * 店家端统一鉴权拦截器
 *
 * 设计目标：把「哪些接口属于店家管理端」的判定从各 Controller 中抽离，
 * 统一在此处拦截，避免遗漏导致越权（例如顾客 token 调用商品/订单/报表接口）。
 *
 * 规则来源：{@link #MERCHANT_RULES} 中声明「路径 + HTTP 方法」白名单组合，
 * 命中任意一条即要求：
 *   1) 携带合法 token（Authorization: Bearer {token}）
 *   2) JWT 中的 role 必须为 merchant
 * 否则返回统一 Result 结构：401 未登录 / 403 无权限。
 */
@Component
public class MerchantAuthInterceptor implements HandlerInterceptor {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 店家端接口规则表。
     * 每行：[urlPattern, method...]，method 传空表示匹配所有方法。
     */
    private static final String[][] MERCHANT_RULES = {
            // ---------- 门店（店家端管理） ----------
            { "/api/shop/merchant" },
            // ---------- 店家账号资料（头像等） ----------
            { "/api/admin/profile" },
            { "/api/shop/dashboard" },
            { "/api/shop/toggle-business" },
            { "/api/shop", HttpMethod.PUT.name() },

            // ---------- 分类（新增/编辑/删除 + 全量列表含禁用项） ----------
            { "/api/category/all" },
            { "/api/category", HttpMethod.POST.name(), HttpMethod.PUT.name() },
            { "/api/category/*", HttpMethod.PUT.name(), HttpMethod.DELETE.name() },

            // ---------- 菜品（管理端） ----------
            { "/api/dish/page" },
            { "/api/dish/admin/**" },
            { "/api/dish", HttpMethod.POST.name(), HttpMethod.PUT.name() },
            { "/api/dish/*/status" },
            { "/api/dish/*/price" },
            { "/api/dish/batch", HttpMethod.DELETE.name() },
            { "/api/dish/*", HttpMethod.DELETE.name() },

            // ---------- 菜品规格（店家维护） ----------
            { "/api/dish-spec/*/detail" },
            { "/api/dish-spec/*", HttpMethod.POST.name() },

            // ---------- 订单（店家端处理） ----------
            { "/api/order/admin/**" },
            { "/api/order/*/accept" },
            { "/api/order/*/finish" },
            { "/api/order/*/reject" },

            // ---------- 桌位管理 ----------
            { "/api/table", HttpMethod.POST.name(), HttpMethod.PUT.name() },
            { "/api/table/**" },

            // ---------- 优惠券管理 ----------
            { "/api/coupon/admin/**" },

            // ---------- 积分商城管理 + 核销 ----------
            { "/api/points/admin/**" },
            { "/api/points/verify" },

            // ---------- 经营报表 / 导出 / 小票 ----------
            { "/api/report/**" },
            { "/api/export/**" },
            { "/api/receipt/**" },
    };

    /**
     * 需要「已登录」但不需要店家角色的接口（顾客也可访问）。
     * 例如：顾客上传微信头像 /api/file/upload?bizType=avatar
     */
    private static final String[][] LOGIN_ONLY_RULES = {
            { "/api/file/upload" },
            { "/api/file/delete" },
    };

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 跨域预检请求直接放行
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI();
        String method = request.getMethod();
        String authorization = request.getHeader("Authorization");
        boolean loggedIn = StringUtils.hasText(authorization) && jwtUtil.validateToken(authorization);

        // 1) 店家管理端接口：必须已登录且为店家角色
        if (matches(MERCHANT_RULES, uri, method)) {
            if (!loggedIn) {
                writeError(response, Result.error(401, "请先登录店家账号"));
                return false;
            }
            if (!AuthUtil.isMerchant(jwtUtil, authorization)) {
                writeError(response, Result.error(403, "无权限操作，请以店家身份登录"));
                return false;
            }
            return true;
        }

        // 2) 仅需登录的接口（任意角色）：如顾客上传头像
        if (matches(LOGIN_ONLY_RULES, uri, method)) {
            if (!loggedIn) {
                writeError(response, Result.error(401, "请先登录"));
                return false;
            }
            return true;
        }

        return true;
    }

    /** 判断请求是否命中指定规则表 */
    private boolean matches(String[][] rules, String uri, String method) {
        for (String[] rule : rules) {
            String pattern = rule[0];
            if (!PATH_MATCHER.match(pattern, uri)) {
                continue;
            }
            // 规则未指定方法 => 匹配所有方法
            if (rule.length == 1) {
                return true;
            }
            for (int i = 1; i < rule.length; i++) {
                if (rule[i].equalsIgnoreCase(method)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 输出统一 Result JSON（HTTP 状态保持 200，由业务码区分，前端 request.js 已适配） */
    private void writeError(HttpServletResponse response, Result<?> result) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.write(OBJECT_MAPPER.writeValueAsString(result));
            writer.flush();
        }
    }
}
