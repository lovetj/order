# 扫码点餐小程序后端 order-backend

基于 Spring Boot 2.7.18 + MyBatis-Plus 3.5.3.1 + MySQL 8 的扫码点餐小程序后端，与前端 `order-frontend` 数据结构严格对齐。

## 技术栈

| 组件 | 版本 |
|---|---|
| Spring Boot | 2.7.18 |
| MyBatis-Plus | 3.5.3.1 |
| MySQL | 8.0.33 (驱动) |
| Redis | Spring Boot Starter Data Redis |
| JWT | jjwt 0.11.5 |
| Hutool | 5.8.18 |
| Java | 1.8 |

## 快速开始

1. 执行 `sql/order.sql` 建库建表并初始化数据
2. 修改 `src/main/resources/application.yml` 中的数据库连接
3. 启动 `com.order.OrderApplication`，默认端口 `8082`

## 默认账号

| 端 | 用户名 | 密码 |
|---|---|---|
| 店家端 | admin | admin123 |
| 顾客端 | user | user123 |

> 微信登录默认开启 Mock 模式（`wechat.miniapp.mock: true`），上线前请填写 app-id / secret 并关闭 mock。

## 前端对接说明

前端 `order-frontend` 已完成真实接口对接，本地模拟数据文件 `utils/data.js` 已移除。

前端请求层：
- `utils/config.js` —— 后端基础地址（开发环境 `http://localhost:8082`）
- `utils/request.js` —— 统一请求封装，自动携带 `Authorization: Bearer {token}`，统一解包 `Result`，401 自动回登录页
- `utils/api.js` —— 全部接口方法统一管理

前端启动前置条件：
1. 先启动 `order-backend`（端口 8082）
2. 微信开发者工具勾选「不校验合法域名」
3. 真机预览请把 `utils/config.js` 的 `localhost` 改为电脑局域网 IP

### 顾客端

| 前端位置 | 接口 | 说明 |
|---|---|---|
| 首页门店信息 | `GET /api/shop/info` | 门店名称/标语/公告/评分 |
| 首页推荐菜品 | `GET /api/dish/recommend?limit=4` | 热销推荐 |
| 点餐-分类 | `GET /api/category/list` | 左侧分类栏 |
| 点餐-菜品 | `GET /api/dish/list?categoryId=xx` | `categoryId=all` 返回全部 |
| 提交订单 | `POST /api/order/create` | 传 tableNo + items[{dishId, quantity}] |
| 订单列表 | `GET /api/order/list?status=pending` | status 为字符串 |
| 订单角标 | `GET /api/order/counts` | 各状态数量 |
| 订单详情 | `GET /api/order/{id}` | id 传订单号 |
| 取消订单 | `PUT /api/order/{id}/cancel` | |

### 店家端

| 前端位置 | 接口 | 说明 |
|---|---|---|
| 管理后台看板 | `GET /api/shop/dashboard` | 概览+7日柱状图+待处理订单 |
| 营业状态切换 | `PUT /api/shop/toggle-business` | |
| 商品列表 | `GET /api/dish/admin/list?categoryId=all` | 含下架商品 |
| 商品分页 | `POST /api/dish/page` | |
| 上下架 | `PUT /api/dish/{id}/status?status=1` | 1上架 0下架 |
| 改价 | `PUT /api/dish/{id}/price?price=38` | |
| 新增/编辑菜品 | `POST/PUT /api/dish` | |
| 删除菜品 | `DELETE /api/dish/{id}` | 逻辑删除 |
| 订单列表 | `GET /api/order/admin/list?status=pending` | |
| 订单角标 | `GET /api/order/admin/counts` | |
| 接单 | `PUT /api/order/{id}/accept` | 待接单 -> 制作中 |
| 出餐 | `PUT /api/order/{id}/finish` | 制作中 -> 已完成 |
| 拒单 | `PUT /api/order/{id}/reject?reason=xx` | -> 已取消 |
| 桌位列表 | `GET /api/table/list?shopId=1` | |
| 新增桌位 | `POST /api/table` | body: `{shopId, tableNo, capacity, status}` |
| 编辑桌位 | `PUT /api/table` | |
| 启用/停用 | `PUT /api/table/{id}/toggle` | |
| 删除桌位 | `DELETE /api/table/{id}` | |
| 经营报表 | `GET /api/report/summary?startDate=&endDate=` | 不传日期默认近 7 天 |
| 分类管理 | `GET /api/category/all` `POST /api/category` `PUT /api/category` `DELETE /api/category/{id}` | 分类下有菜品时禁止删除 |
| 优惠券管理 | `GET /api/coupon/admin/list` `POST /api/coupon/admin` `PUT /api/coupon/admin` `PUT /api/coupon/admin/{id}/status` `DELETE /api/coupon/admin/{id}` | |
| 小票数据 | `GET /api/receipt/{orderId}` | 含纯文本小票，供前端预览 |
| 小票打印指令 | `GET /api/receipt/{orderId}/escpos` | 返回 Base64 的 ESC/POS 指令 |
| 报表导出 | `GET /api/export/report?startDate=&endDate=` | 下载 CSV（UTF-8 BOM） |
| 订单导出 | `GET /api/export/orders?startDate=&endDate=&status=` | 下载订单明细 CSV |

### 顾客端 · 会员与优惠券

| 前端位置 | 接口 | 说明 |
|---|---|---|
| 会员中心 | `GET /api/member/info` | 等级/积分/成长进度/券数量 |
| 领券中心 | `GET /api/coupon/available` | 可领取的券（已过滤领完/过期） |
| 领取优惠券 | `POST /api/coupon/receive/{couponId}` | 校验每人限领 |
| 我的优惠券 | `GET /api/coupon/mine?status=0` | 0未使用 1已使用 2已过期（自动标记过期） |
| 券数量统计 | `GET /api/coupon/counts` | `{unused, used, expired}` |
| 结算试算 | `GET /api/coupon/best?amount=88` | 返回优惠力度最大的可用券 |

### 文件上传

| 接口 | 说明 |
|---|---|
| `POST /api/file/upload?bizType=dish` | form-data，字段名 `file`，返回 `{url, relativePath, fileName}` |

> 支持 jpg/jpeg/png/gif/webp/bmp，按 `bizType/日期` 分目录存储到 `file.base-path`，访问地址为 `file.base-server + relativePath`。

### 菜品规格 / 口味选项

| 接口 | 说明 |
|---|---|
| `GET /api/dish-spec/{dishId}` | 顾客端：规格分组（辣度/加料/温度等） |
| `GET /api/dish-spec/{dishId}/detail` | 店家端：规格明细 |
| `POST /api/dish-spec/{dishId}` | 店家端：整体保存规格（先删后插） |
| `GET /api/dish/{id}` | 菜品详情已自动附带 `specs` 分组 |

> 规格支持单选/多选、必选、加价、默认选中。下单时传 `items[].specIds` 与 `specText`，后端会校验合法性、计算加价并写入订单明细快照。

### 积分商城

| 接口 | 说明 |
|---|---|
| `GET /api/points/goods` | 积分商品列表（上架中） |
| `POST /api/points/exchange` | 积分兑换，body `{goodsId, quantity}`，返回含核销码的记录 |
| `GET /api/points/mine` | 我的兑换记录 |
| `POST /api/points/verify` | 店家端核销，body `{verifyCode}` |
| `GET /api/points/admin/list` `POST /api/points/admin` `PUT /api/points/admin` `PUT /api/points/admin/{id}/status` `DELETE /api/points/admin/{id}` | 店家端积分商品管理 |

> 兑换会校验积分余额、库存与每人限兑，成功后扣积分并生成 8 位核销码。

### 支付说明

本项目**不含在线支付功能**，采用堂食线下结算模式：顾客下单后订单直接进入「待接单」，由店家接单、出餐，顾客到店/离店时线下付款。

### 登录

| 接口 | 说明 |
|---|---|
| `POST /api/user/wx-login` | 微信小程序登录，body: `{code, nickname, avatar, role}` |
| `POST /api/admin/login` | 店家账号登录，body: `{username, password}` |
| `GET /api/user/info` | 当前用户信息 |

请求头统一携带：`Authorization: Bearer {token}`，也兼容 `userId: {userId}` 头（便于本地调试）。

### 字段映射（重点）

| 前端字段 | 数据库字段 | 说明 |
|---|---|---|
| `dish.desc` | `dish.description` | |
| `dish.status` (boolean) | `dish.status` (tinyint) | 1 -> true |
| `order.table` | `order.table_no` | |
| `order.amount` | `order.pay_amount` | |
| `order.status` (字符串) | `order.status` (tinyint) | 0待接单/1制作中/2已完成/3已取消 |
| `order.statusText` | 服务端推导 | 待接单/制作中/已完成/已取消 |
| `order.items[].name` | `order_item.dish_name` | |
| `order.items[].count` | `order_item.quantity` | |
| `order.items[].price` | `order_item.price` | |
| `order.action` | 服务端推导 | `{text, next, nextText}` 用于店家端操作按钮 |

## 接口返回格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

## 项目结构

```
src/main/java/com/order/
├── OrderApplication.java          启动类
├── common/                        Result / PageResult 统一响应
├── config/                        MyBatis-Plus / CORS / 文件 / 微信配置
├── controller/                    REST 接口
├── dto/                           入参与出参对象
├── entity/                        数据库实体
├── exception/                     全局异常处理
├── mapper/                        MyBatis-Plus Mapper
├── service/ + service/impl/       业务逻辑
└── util/                          JwtUtil
```
