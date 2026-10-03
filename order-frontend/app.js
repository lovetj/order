// app.js
const { setToken, getToken, clearToken } = require('./utils/request')

App({
  globalData: {
    // 'customer' | 'merchant'
    role: '',
    // 当前店铺ID（扫码得到，多店铺隔离的核心标识）
    shopId: '',
    // 扫码进入时携带的桌号
    tableNo: '',
    // 购物车：[{ id, name, price, image, count }]
    cart: [],
    // 登录用户信息
    userInfo: {
      nickName: '微信用户',
      avatar: '🙋',
      memberLevel: '普通会员'
    },
    // 店家账号信息（登录后写入，含所属 shopId）
    admin: null
  },

  onLaunch() {
    // 恢复上次选择的身份、店铺与桌号
    this.globalData.role = wx.getStorageSync('role') || ''
    this.globalData.shopId = wx.getStorageSync('shopId') || ''
    this.globalData.tableNo = wx.getStorageSync('tableNo') || ''

    this.handleLaunchQuery(wx.getLaunchOptionsSync().query || {})
  },

  onShow(options) {
    // 从扫码/分享等场景再次进入时，同样解析 query
    this.handleLaunchQuery((options && options.query) || {})
  },

  /**
   * 解析启动/切入参数，提取店铺ID与桌号
   * 支持两种二维码格式：
   *   1) 小程序页面路径：pages/role/role?shopId=1&tableNo=A01
   *   2) 普通 URL：https://xxx?shopId=1&tableNo=A01
   */
  handleLaunchQuery(query) {
    const shopId = query.shopId || ''
    const tableNo = query.tableNo || ''
    if (shopId) {
      this.setShopId(shopId)
    }
    if (tableNo) {
      this.setTableNo(tableNo)
    }
  },

  // 设置身份
  setRole(role) {
    this.globalData.role = role
    wx.setStorageSync('role', role)
  },

  // 清空身份（切换身份 / 退出）
  clearRole() {
    this.globalData.role = ''
    this.globalData.cart = []
    this.globalData.admin = null
    wx.removeStorageSync('role')
  },

  // 设置当前店铺
  setShopId(shopId) {
    this.globalData.shopId = shopId || ''
    if (shopId) {
      wx.setStorageSync('shopId', shopId)
    } else {
      wx.removeStorageSync('shopId')
    }
  },

  // 设置桌号
  setTableNo(tableNo) {
    this.globalData.tableNo = tableNo || ''
    if (tableNo) {
      wx.setStorageSync('tableNo', tableNo)
    } else {
      wx.removeStorageSync('tableNo')
    }
  },

  // 是否已登录（有 token）
  isLogin() {
    return !!getToken()
  },

  // 退出登录
  logout() {
    clearToken()
    this.clearRole()
  }
})
