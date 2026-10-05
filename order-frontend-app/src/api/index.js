// api/index.js
// 接口统一管理，与 order-backend 的接口一一对应
import { http, uploadFile, uploadFiles } from '../utils/request'
import { baseUrl } from '../utils/config'

export default {
  // ==================== 登录 / 用户 ====================
  // 顾客端：昵称+头像快速登录（后端接口沿用 /api/user/wx-login，未配置 appId 时走 Mock 模式，任意非空 code 均可）
  login: (data) => http.post('/api/user/wx-login', data),
  // 当前用户信息
  getUserInfo: () => http.get('/api/user/info'),
  // 更新用户资料
  updateProfile: (data) => http.put('/api/user/profile', data),

  // ==================== 店家登录 ====================
  adminLogin: (data) => http.post('/api/admin/login', data),
  // 店家账号资料（含头像，头像返回完整地址）
  getMerchantProfile: () => http.get('/api/admin/profile'),
  // 更新店家资料（目前仅头像，传相对路径）
  updateMerchantProfile: (data) => http.put('/api/admin/profile', data),

  // ==================== 门店 ====================
  getShopInfo: () => http.get('/api/shop/info'),
  // 店家端：获取本店信息（走 token 解析店铺，保证拿到真实数据）
  getMerchantShopInfo: () => http.get('/api/shop/merchant'),
  getDashboard: () => http.get('/api/shop/dashboard'),
  toggleBusiness: () => http.put('/api/shop/toggle-business'),
  // 店家端：更新店铺信息（含店铺图片）
  updateShop: (data) => http.put('/api/shop', data),

  // ==================== 分类 ====================
  getCategories: () => http.get('/api/category/list'),
  getAllCategories: () => http.get('/api/category/all'),
  addCategory: (data) => http.post('/api/category', data),
  updateCategory: (data) => http.put('/api/category', data),
  deleteCategory: (id) => http.del(`/api/category/${id}`),

  // ==================== 菜品（顾客端） ====================
  getDishes: (params) => http.get('/api/dish/list', params || {}),
  getRecommend: (limit = 4) => http.get('/api/dish/recommend', { limit }),
  getDishDetail: (id) => http.get(`/api/dish/${id}`),

  // ==================== 菜品（店家端） ====================
  getAdminDishes: (params) => http.get('/api/dish/admin/list', params || {}),
  pageAdminDishes: (data) => http.post('/api/dish/page', data),
  addDish: (data) => http.post('/api/dish', data),
  updateDish: (data) => http.put('/api/dish', data),
  updateDishStatus: (id, status) => http.put(`/api/dish/${id}/status?status=${status}`),
  updateDishPrice: (id, price) => http.put(`/api/dish/${id}/price?price=${price}`),
  deleteDish: (id) => http.del(`/api/dish/${id}`),

  // ==================== 订单（顾客端） ====================
  createOrder: (data) => http.post('/api/order/create', data, { showLoading: true, loadingText: '下单中' }),
  getOrders: (params) => http.get('/api/order/list', params || {}),
  getOrderCounts: () => http.get('/api/order/counts'),
  getOrderDetail: (id) => http.get(`/api/order/${id}`),
  cancelOrder: (id, reason) => http.put(`/api/order/${id}/cancel${reason ? `?reason=${encodeURIComponent(reason)}` : ''}`),

  // ==================== 订单（店家端） ====================
  getAdminOrders: (params) => http.get('/api/order/admin/list', params || {}),
  getAdminOrderCounts: () => http.get('/api/order/admin/counts'),
  acceptOrder: (id) => http.put(`/api/order/${id}/accept`),
  finishOrder: (id) => http.put(`/api/order/${id}/finish`),
  rejectOrder: (id, reason) => http.put(`/api/order/${id}/reject${reason ? `?reason=${encodeURIComponent(reason)}` : ''}`),

  // ==================== 桌位管理（店家端） ====================
  getTables: () => http.get('/api/table/list'),
  addTable: (data) => http.post('/api/table', data),
  updateTable: (data) => http.put('/api/table', data),
  toggleTable: (id) => http.put(`/api/table/${id}/toggle`),
  deleteTable: (id) => http.del(`/api/table/${id}`),

  // ==================== 经营报表（店家端） ====================
  getReport: (params) => http.get('/api/report/summary', params || {}),

  // ==================== 会员中心（顾客端） ====================
  getMemberInfo: () => http.get('/api/member/info'),

  // ==================== 优惠券（顾客端） ====================
  getAvailableCoupons: () => http.get('/api/coupon/available'),
  receiveCoupon: (couponId) => http.post(`/api/coupon/receive/${couponId}`),
  getMyCoupons: (status) => http.get('/api/coupon/mine', status != null ? { status } : {}),
  getCouponCounts: () => http.get('/api/coupon/counts'),
  getBestCoupon: (amount) => http.get('/api/coupon/best', { amount }),

  // ==================== 优惠券管理（店家端） ====================
  getAdminCoupons: () => http.get('/api/coupon/admin/list'),
  addCoupon: (data) => http.post('/api/coupon/admin', data),
  updateCoupon: (data) => http.put('/api/coupon/admin', data),
  updateCouponStatus: (id, status) => http.put(`/api/coupon/admin/${id}/status?status=${status}`),
  deleteCoupon: (id) => http.del(`/api/coupon/admin/${id}`),

  // ==================== 文件上传 ====================
  uploadUrl: (bizType = 'dish') => `${baseUrl}/api/file/upload?bizType=${bizType}`,
  uploadFile: (filePath, bizType = 'other') => uploadFile(filePath, bizType),
  uploadFiles: (filePaths, bizType = 'other') => uploadFiles(filePaths, bizType),
  deleteFile: (path) => http.del(`/api/file/delete?path=${encodeURIComponent(path || '')}`),

  // ==================== 小票打印（店家端） ====================
  getReceipt: (orderId) => http.get(`/api/receipt/${orderId}`),
  getReceiptEscPos: (orderId) => http.get(`/api/receipt/${orderId}/escpos`),

  // ==================== 菜品规格 ====================
  getDishSpecs: (dishId) => http.get(`/api/dish-spec/${dishId}`),
  getDishSpecDetail: (dishId) => http.get(`/api/dish-spec/${dishId}/detail`),
  saveDishSpecs: (dishId, specs) => http.post(`/api/dish-spec/${dishId}`, specs),

  // ==================== 积分商城 ====================
  getPointsGoods: () => http.get('/api/points/goods'),
  exchangePoints: (goodsId, quantity = 1) => http.post('/api/points/exchange', { goodsId, quantity }),
  getMyExchanges: () => http.get('/api/points/mine'),
  verifyPoints: (verifyCode) => http.post('/api/points/verify', { verifyCode }),

  // ==================== 积分商城管理（店家端） ====================
  getAdminPointsGoods: () => http.get('/api/points/admin/list'),
  addPointsGoods: (data) => http.post('/api/points/admin', data),
  updatePointsGoods: (data) => http.put('/api/points/admin', data),
  updatePointsGoodsStatus: (id, status) => http.put(`/api/points/admin/${id}/status?status=${status}`),
  deletePointsGoods: (id) => http.del(`/api/points/admin/${id}`),

  // ==================== 报表导出（店家端） ====================
  exportReportUrl: (startDate, endDate) =>
    `${baseUrl}/api/export/report?startDate=${startDate || ''}&endDate=${endDate || ''}`,
  exportOrdersUrl: (startDate, endDate, status) =>
    `${baseUrl}/api/export/orders?startDate=${startDate || ''}&endDate=${endDate || ''}${status != null ? `&status=${status}` : ''}`
}
