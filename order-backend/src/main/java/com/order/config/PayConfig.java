package com.order.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 支付全局配置(Mock模式 + 微信支付V3 + 支付宝配置)
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "pay")
public class PayConfig {

    /** Mock模式(开发/测试用，默认开启) */
    private Mock mock = new Mock();

    /** 真实微信支付V3配置 */
    private Wechat wechat = new Wechat();

    /** 支付宝配置 */
    private Alipay alipay = new Alipay();

    @Data
    public static class Mock {
        /** 是否开启Mock模式(默认true) */
        private boolean enabled = true;
        /** Mock 支付成功概率 0~100 */
        private int successRate = 100;
        /** Mock 支付延迟ms */
        private int delayMs = 1200;
    }

    @Data
    public static class Wechat {
        private String mchId = "";
        private String appId = "";
        private String apiV3Key = "";
        private String privateKeyPath = "";
        private String certSerialNo = "";
        private String publicKeyId = "";
        private String platformCertPath = "";
        private String platformCertSerialNo = "";
        private String notifyUrl = "";
        private String refundNotifyUrl = "";
        private String tradeType = "JSAPI";
    }

    @Data
    public static class Alipay {
        private String appId = "";
        private String privateKey = "";
        private String alipayPublicKey = "";
        private String serverUrl = "https://openapi.alipay.com/gateway.do";
        private String notifyUrl = "";
        private String returnUrl = "";
    }
}
