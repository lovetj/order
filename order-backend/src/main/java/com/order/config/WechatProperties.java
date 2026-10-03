package com.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "wechat")
public class WechatProperties {

    private Miniapp miniapp = new Miniapp();

    public Miniapp getMiniapp() {
        return miniapp;
    }

    public void setMiniapp(Miniapp miniapp) {
        this.miniapp = miniapp;
    }

    public static class Miniapp {
        private String appId;
        private String secret;
        /**
         * 是否开启 Mock 登录（未配置密钥或本地联调时使用）
         */
        private boolean mock = false;

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public boolean isMock() {
            return mock;
        }

        public void setMock(boolean mock) {
            this.mock = mock;
        }
    }
}
