package com.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "file")
public class FileConfigProperties {
    /**
     * 文件访问基础地址，如 http://localhost:8081/order_file
     */
    private String baseServer;
    /**
     * 文件物理存储目录
     */
    private String basePath;

    public String getBaseServer() {
        return baseServer;
    }

    public void setBaseServer(String baseServer) {
        this.baseServer = baseServer;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }
}
