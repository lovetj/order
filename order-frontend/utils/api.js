// utils/api.js
// 接口统一管理，与 order-backend 的接口一一对应
const { http } = require('./request')

module.exports = {
  // ==================== 登录 / 用户 ====================
  // 顾客端：微信小程序授权登录（唯一登录方式）
  wxLogin: (data) => http.post('/api/user/wx-login', data),
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
  // 店铺由后端从登录 token 解析，前端无需传 shopId
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
  /**
   * 上传图片。使用 wx.uploadFile，需单独处理（不走 request 封装）
   * @param {string} filePath 本地临时路径
   * @param {string} bizType  dish / shop / avatar
   */
  uploadUrl: (bizType = 'dish') => `${require('./config').baseUrl}/api/file/upload?bizType=${bizType}`,

  /**
   * 上传文件并返回后端 data（{ url, relativePath, fileName }）
   * 会自动携带 Authorization token
   * @param {string} filePath 本地临时路径
   * @param {string} bizType  dish / shop / avatar
   */
  uploadFile: (filePath, bizType = 'other') => {
    const { baseUrl } = require('./config')
    const { getToken } = require('./request')
    const token = getToken()
    return new Promise((resolve, reject) => {
      wx.uploadFile({
        url: `${baseUrl}/api/file/upload?bizType=${bizType}`,
        filePath,
        name: 'file',
        header: token ? { Authorization: `Bearer ${token}` } : {},
        success: (res) => {
          try {
            const body = JSON.parse(res.data)
            if (body && body.code === 200) {
              resolve(body.data)
            } else {
              reject(new Error((body && body.message) || '上传失败'))
            }
          } catch (e) {
            reject(new Error('上传失败'))
          }
        },
        fail: () => reject(new Error('上传失败'))
      })
    })
  },

  /**
   * 多文件上传（后端原图保存，不做压缩，仅校验单张不超过 10MB）
   *
   * 注意：wx.uploadFile 只支持单个 filePath，不支持 files 数组，
   * 因此这里并发调用单文件上传接口，全部成功后按顺序汇总返回。
   *
   * @param {string[]} filePaths 本地临时路径数组
   * @param {string} bizType dish / shop / avatar
   * @returns {Promise<Array<{url:string, relativePath:string, fileName:string}>>}
   */
  uploadFiles: (filePaths, bizType = 'other') => {
    const paths = (filePaths || []).filter(Boolean)
    if (!paths.length) return Promise.resolve([])
    const uploadOne = (filePath) => {
      const { baseUrl } = require('./config')
      const { getToken } = require('./request')
      const token = getToken()
      return new Promise((resolve, reject) => {
        wx.uploadFile({
          url: `${baseUrl}/api/file/upload?bizType=${bizType}`,
          filePath,
          name: 'file',
          header: token ? { Authorization: `Bearer ${token}` } : {},
          success: (res) => {
            try {
              const body = JSON.parse(res.data)
              if (body && body.code === 200) {
                resolve(body.data)
              } else {
                reject(new Error((body && body.message) || '上传失败'))
              }
            } catch (e) {
              reject(new Error('上传失败'))
            }
          },
          fail: () => reject(new Error('上传失败'))
        })
      })
    }
    // 保持与传入顺序一致的结果
    return Promise.all(paths.map(uploadOne))
  },

  /**
   * 删除已上传的图片（后端真实删除磁盘文件）
   * @param {string} path 相对路径，如 /shop/20261003/xxx.jpg
   */
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
    `${require('./config').baseUrl}/api/export/report?startDate=${startDate || ''}&endDate=${endDate || ''}`,
  exportOrdersUrl: (startDate, endDate, status) =>
    `${require('./config').baseUrl}/api/export/orders?startDate=${startDate || ''}&endDate=${endDate || ''}${status != null ? `&status=${status}` : ''}`,
}
