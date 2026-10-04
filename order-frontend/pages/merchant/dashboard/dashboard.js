// pages/merchant/dashboard/dashboard.js —— 店家端管理后台
const app = getApp()
const api = require('../../../utils/api')

Page({
  data: {
    shop: { name: '扫码点餐', status: '营业中' },
    overview: [],
    weekSales: []
  },

  onShow() {
    if (this.getTabBar && this.getTabBar()) {
      this.getTabBar().setData({ selected: 0 })
    }
    if (app.globalData.role !== 'merchant') {
      wx.reLaunch({ url: '/pages/role/role' })
      return
    }
    // 店家未登录（token 丢失/过期）时回到店家登录页
    if (!app.isLogin()) {
      wx.reLaunch({ url: '/pages/login/login?role=merchant' })
      return
    }
    this.loadShop()
    this.loadDashboard()
  },

  // 底部页签切换时刷新当前页面内容（由 custom-tab-bar 调用）
  onTabRefresh() {
    if (!app.isLogin()) return
    this.loadShop()
    this.loadDashboard()
  },

  loadShop() {
    api.getMerchantShopInfo().then((shop) => {
      if (!shop) return
      this.setData({
        shop: {
          name: shop.name || '扫码点餐',
          status: shop.businessStatus === 1 ? '营业中' : '休息中'
        }
      })
    }).catch(() => {})
  },

  loadDashboard() {
    api.getDashboard().then((data) => {
      if (!data) return
      this.setData({
        overview: data.overview || [],
        weekSales: data.weekSales || []
      })
    }).catch(() => {})
  },

  toggleShop() {
    api.toggleBusiness().then(() => {
      const next = this.data.shop.status === '营业中' ? '休息中' : '营业中'
      this.setData({ 'shop.status': next })
      wx.showToast({ title: `已切换为${next}`, icon: 'none' })
    }).catch(() => {})
  }
})
