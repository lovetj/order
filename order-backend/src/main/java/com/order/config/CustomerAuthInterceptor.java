package com.order.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.common.Result;
import com.order.service.CustomerSessionService;
import com.order.util.JwtUtil;
import com.order.util.ShopContext;
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
 * 顾客登录态统一鉴权拦截器（三要素：user + shop + table）
 *
 * 只对 {@link #CUSTOMER_RULES} 中声明的顾客业务接口强制校验：
 *   1) 携带合法登录态（Authorization: Bearer {token}）
 *   2) 同时携带 X-Shop-Id、X-Table-Id 请求头
 *   3) Redis 中以 login:{userId}:{shopId}:{tableId} 存在且 value==token
 * 三者缺一即按 401 拦截，前端据此重新登录/重新扫码。
 *
 * 设计原则：只增强声明内接口，其它一律放行，商家端接口仍由 MerchantAuthInterceptor
 * 先行校验（该拦截器 order = 2，仅在其后补充顾客三要素校验），互不干扰。
 */
@Component
public class CustomerAuthInterceptor implements HandlerInterceptor {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 顾客端需三要素登录态的业务接口 */
    private static final String[][] CUSTOMER_RULES = {
            // ---------- 订单（顾客端查看/下单/取消） ----------
            { "/api/order/create", HttpMethod.POST.name() },
            { "/api/order/list", HttpMethod.GET.name() },
            { "/api/order/counts", HttpMethod.GET.name() },
            { "/api/order/*", HttpMethod.GET.name() },
            { "/api/order/*/cancel", HttpMethod.PUT.name() },
            // ---------- 会员中心 ----------
            { "/api/member/info", HttpMethod.GET.name() },
            // ---------- 优惠券（顾客端） ----------
            { "/api/coupon/available", HttpMethod.GET.name() },
            { "/api/coupon/receive/*", HttpMethod.POST.name() },
            { "/api/coupon/mine", HttpMethod.GET.name() },
            { "/api/coupon/counts", HttpMethod.GET.name() },
            { "/api/coupon/best", HttpMethod.GET.name() },
            // ---------- 积分商城（顾客端兑换/记录） ----------
            { "/api/points/goods", HttpMethod.GET.name() },
            { "/api/points/exchange", HttpMethod.POST.name() },
            { "/api/points/mine", HttpMethod.GET.name() },
    };

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomerSessionService customerSessionService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 跨域预检直接放行
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 仅拦截声明的顾客业务接口，其余一律放行
        if (!matches(CUSTOMER_RULES, uri, method)) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization) || !jwtUtil.validateToken(authorization)) {
            writeError(response, Result.error(401, "登录已过期，请重新登录"));
            return false;
        }
        String userId = jwtUtil.getUserId(authorization);
        String shopId = request.getHeader(ShopContext.SHOP_ID_HEADER);
        String tableId = request.getHeader("X-Table-Id");
        if (!StringUtils.hasText(shopId) || !StringUtils.hasText(tableId)) {
            writeError(response, Result.error(401, "未识别桌位信息，请重新扫描桌位二维码"));
            return false;
        }
        String token = authorization.startsWith("Bearer ") ? authorization.substring(7).trim() : authorization.trim();
        if (!customerSessionService.validateLogin(userId, shopId, tableId, token)) {
            writeError(response, Result.error(401, "登录已过期，请重新登录"));
            return false;
        }
        return true;
    }

    private boolean matches(String[][] rules, String uri, String method) {
        for (String[] rule : rules) {
            if (!PATH_MATCHER.match(rule[0], uri)) {
                continue;
            }
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