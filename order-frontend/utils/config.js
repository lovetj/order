// utils/config.js
// 后端接口基础地址配置
//
// 开发环境：
//   1) 启动 order-backend（默认端口 8082）
//   2) 微信开发者工具 -> 详情 -> 本地设置 -> 勾选「不校验合法域名、web-view、TLS 版本以及 HTTPS 证书」
//   3) 模拟器可直接访问 http://localhost:8082
//   4) 真机预览时 localhost 需替换为电脑局域网 IP，如 http://192.168.0.114:8082
//
// 生产环境：
//   改为 https 正式域名，并在小程序后台「开发管理 -> 服务器域名」配置 request 合法域名

const ENV = 'dev'

const CONFIG = {
  dev: {
    baseUrl: 'http://localhost:8082'
  },
  prod: {
    baseUrl: 'https://your-domain.com'
  }
}

module.exports = {
  ENV,
  baseUrl: CONFIG[ENV].baseUrl
}
