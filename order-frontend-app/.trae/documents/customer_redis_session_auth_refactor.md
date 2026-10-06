# 顾客登录态重构：用户+商家+桌号 三要素 Redis 会话

## Context（背景）
当前顾客登录用的是「永不过期」的 JWT，只包含 userId，登录态未与店铺/桌位绑定：
- 顾客端 token 是 JWT，`customer.session` 不存在，`user.points` 等一切以全局 userId 关联。
- shopId 通过扫码写入 storage 并作为 `X-Shop-Id` 请求头传递；tableId 目前只用于查桌位，**不参与鉴权**。
- 结果：顾客在 A 店拿到的登录态在 B 店同样有效，登录态与「在哪家店哪张桌就餐」无关。

目标：顾客扫码进入后统一登录态；登录态由 `(userId, shopId, tableId)` 三要素共同校验；登录字符串存 Redis；TTL 走 yml 配置（默认 1 天）；扫码/登录时未登录则重新登录；所有要求登录的顾客端后端校验都必须按三要素作用。商家端 JWT 登录保持不变。

## 设计决策（默认，未收到用户异议）
1. **会话模型**：Redis `key = login:{userId}:{shopId}:{tableId}`，`value = 登录随机串(token)`，TTL = 配置的过期秒数。登录字符串即存于 Redis，返回给前端作为承载凭据。
2. **校验范围**：增强顾客业务接口（下单/订单/我的/优惠券/积分商城/用户资料）必须登录且校验三要素；浏览类接口（菜单列表、菜品详情、分类、桌位查询、店铺信息）保持公开可先浏览。
3. **商家端**：继续使用现有 JWT + MerchantAuthInterceptor，不回归影响。

## 后端改动（order-backend）

### 1. 配置
`src\main\java\com\order\...\application.yml` 新增：
```yaml
customer:
  session:
    expire-seconds: 86400   # 顾客登录态有效期（秒），默认 1 天
    key-prefix: login:      # 可配置前缀，默认 login:
```
（`jwt.*` 保留，仍用于商家端。）

### 2. Redis 会话服务（新增）
`src\main\java\com\order\service\CustomerSessionService.java` + `impl`，用 `StringRedisTemplate`（redis 已在 application.yml 配置好）：
- `String createLogin(String userId, String shopId, String tableId)`：生成 UUID token，写 `login:{userId}:{shopId}:{tableId}` → token，TTL=配置值，返回 token。
- `boolean validateLogin(String userId, String shopId, String tableId, String token)`：读 key，比对 value.equals(token)。
- `void removeLogin(...)`（供退本桌/换桌用，可选）。
- 任一维度为空则校验/创建失败（调用方给出友好提示）。

### 3. 顾客登录绑定三要素
- `UserServiceImpl.phoneLogin / wxLogin`：在返回 `{ token, user, isNewUser }` 前，若入参带 `shopId`、`tableId`（扫码上下文），调用 `CustomerSessionService.createLogin(...)`，将返回的 token 作为登录字符串（自建会话后返回的是 session token）。
- `PhoneLoginDTO / WxLoginDTO` 增加可选字段 `shopId`、`tableId`（前端扫码后登录时携带）。
- 兼容：未传 shopId/tableId（无桌位上下文）时仍返回 token，但不建三要素会话（浏览仍可用，业务接口会被拦截器要求重新绑定）。

### 4. 扫码/换桌统一登录态（新增）
`src\main\java\com\order\controller\CustomerSessionController.java`：
- `POST /api/customer/session/bind`，body `{ shopId, tableId }`（已登录 token 鉴权）：为当前用户重建/刷新三要素会话，返回 `{ token, expireSeconds }`。用于「已登录用户换桌、扫码时未登录则重新登录」。
- `GET /api/customer/session/check`：校验当前三要素登录态，返回 ok/401（前端可据此触发重登）。

### 5. 顾客统一鉴权拦截器（新增）
`src\main\java\com\order\config\CustomerAuthInterceptor.java`，仿照现成 `MerchantAuthInterceptor`：
- 表驱动 `CUSTOMER_RULES`（需三要素的顾客业务路径）：
  - `/api/order/**`（顾客 list/detail/create/counts/cancel）
  - `/api/member/info`
  - `/api/user/info`、`/api/user/profile`（PUT）
  - `/api/coupon/available`、`/api/coupon/receive/*`、`/api/coupon/mine`、`/api/coupon/counts`、`/api/coupon/best`
  - `/api/points/goods`、`/api/points/exchange`、`/api/points/mine`
- `publicRules`（放行，无需登录）：`/api/user/phone-login`、`/api/user/wx-login`、`/api/customer/session/bind`、`/api/dining-table/**`、`/api/dish/**`（list/customer/page/recommend/{id}）、`/api/category/list`、`/api/shop/info`、`/api/file/**`。
- `preHandle`：OPTIONS 放行；命中 CUSTOMER_RULES 时读 `Authorization`、`X-User-Id`、`X-Shop-Id`、`X-Table-Id` 四个头，任一缺失或 `CustomerSessionService.validateLogin` 校验不过 → `writeError(401, {code:401, message:'登录已过期/未扫码，请重新登录'})`。商品/桌号不匹配提示细化。
- `WebConfig.addInterceptors` 追加注册该拦截器 `.addPathPatterns("/api/**").order(2)`（商家拦截器 order=1，顾客拦截器 order=2；商家路径已由商家拦截器处理，顾客拦截器用规则表排除，两者按需互斥）。

## 前端改动（order-frontend-app）

### 1. 请求层
`src\utils\request.js`：
- 增加发送 `X-User-Id`（取 `uni.getStorageSync('userId')`）、`X-Table-Id`（取 `uni.getStorageSync('tableId')`）头；`Authorization` 仍带登录字符串。
- 现有 401 处理已自动清 token 并 `reLaunch` 到登录页，保持不变，即为「未登录则重新登录」。

### 2. 登录页
`src\pages\login\login.vue` `handleLoginSuccess`：
- 将 `data.user.id` 写入 storage `userId`。
- 调用 `api.login({ phone, role, shopId, tableId })`（读 `app.globalData.shopId/tableId`）让后端建/刷新三要素会话。

### 3. 扫码/换桌绑定
`src\App.vue` 及扫码入口（index.vue/role.vue 设置 shopId/tableId 处）：
- 在记录到 `shopId/tableId` 后，若已登录，调用 `/api/customer/session/bind` 重建会话并回写新 token，保证「扫码即统一登录态、换桌即重新绑定」。

### 4. 前端 XML/接口
`src\api\index.js` 增加 `bindCustomerSession({shopId,tableId})`、`checkCustomerSession()`。

## 验证
1. 后端：`mvn -o compile`（order-backend 目录，需 shell 与 dangerouslyDisableSandbox），BUILD SUCCESS。
2. 前端：`npm run build:h5`（order-frontend-app，ENV=dev）。
3. 端到端（需 Redis 运行、执行既有 order.sql/本次不涉及新表）：
   - 扫码进入 → 未登录时浏览菜单正常；点“我是顾客”→ 手机号登录 → 前端携带 shopId/tableId 建三要素会话。
   - 下单/查看订单/积分商城：带三要素头可成功；手工把 token 改成随机串或改 `X-Table-Id` 为另一桌 → 返回 401 → 前端回到登录页。
   - Redis 查看 key `login:{userId}:{shopId}:{tableId}` 存在且 TTL 为 86400；改 yml `expire-seconds` 后重启观察 TTL 变化。
   - 换另一桌扫码 → 触发 bind → 旧三要素 key 失效、新 key 建立。
4. 回归：商家端登录/管理接口不受影响（JWT 拦截器 order=1 先行校验）。

## 关键改动文件
- 后端：`application.yml`、`PhoneLoginDTO`、`WxLoginDTO`、`UserServiceImpl`、新增 `CustomerSessionService(+impl)`、`CustomerSessionController`、`CustomerAuthInterceptor`、`WebConfig`。
- 前端：`utils/request.js`、`pages/login/login.vue`、`App.vue` 及扫码入口、`api/index.js`。

> 注：后端目录不在 IDE 工作目录内，Write/Edit 受限 → 后端文件改动统一走 Shell(powershell) + `dangerouslyDisableSandbox`，与上一轮积分重构一致。