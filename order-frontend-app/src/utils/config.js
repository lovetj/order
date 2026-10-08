// utils/config.js
// 后端接口基础地址配置
//
// 开发环境：
//   1) 启动 order-backend（默认端口 8082）
//   2) H5 本地开发直接访问 http://localhost:8082（如遇跨域请在后端配置 CORS）
//   3) App/真机调试时 localhost 需替换为电脑局域网 IP，如 http://192.168.0.114:8082
//
// 生产环境：
//   改为 https 正式域名

export const ENV = 'prod'

export const CONFIG = {
  dev: {
    baseUrl: 'http://localhost:8082/order_backend'
  },
  prod: {
    baseUrl: 'https://2uu4401930iw.vicp.fun/order_backend'
  }
}

export const baseUrl = CONFIG[ENV].baseUrl

export default {
  ENV,
  CONFIG,
  baseUrl
}
