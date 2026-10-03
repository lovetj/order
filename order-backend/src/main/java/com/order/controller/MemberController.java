package com.order.controller;

import com.order.common.Result;
import com.order.dto.MemberVO;
import com.order.service.MemberService;
import com.order.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 会员中心 —— 顾客端「我的」
 */
@RestController
@RequestMapping("/api/member")
public class MemberController {

    @Autowired
    private MemberService memberService;

    @Autowired
    private JwtUtil jwtUtil;

    /** 会员信息（等级/积分/成长进度/优惠券数） */
    @GetMapping("/info")
    public Result<MemberVO> info(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @RequestHeader(value = "userId", required = false) String headerUserId) {
        String userId = resolveUserId(authorization, headerUserId);
        if (userId == null) {
            return Result.error(401, "用户未登录，请先登录");
        }
        return Result.success(memberService.getMemberInfo(userId));
    }

    private String resolveUserId(String authorization, String headerUserId) {
        if (authorization != null && !authorization.trim().isEmpty() && jwtUtil.validateToken(authorization)) {
            return jwtUtil.getUserId(authorization);
        }
        if (headerUserId != null && !headerUserId.trim().isEmpty()) {
            return headerUserId.trim();
        }
        return null;
    }
}
