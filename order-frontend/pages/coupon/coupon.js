// pages/coupon/coupon.js —— 我的优惠券
const app = getApp()
const api = require('../../utils/api')

const TABS = [
  { key: 'unused', name: '未使用', status: 0 },
  { key: 'used', name: '已使用', status: 1 },
  { key: 'expired', name: '已过期', status: 2 }
]

Page({
  data: {
    tabs: TABS,
    current: 0,
    list: [],
    loading: false
  },

  onShow() {
    if (!app.globalData.role) {
      wx.reLaunch({ url: '/pages/role/role' })
      return
    }
    this.loadList()
  },

  switchTab(e) {
    const { status } = e.currentTarget.dataset
    this.setData({ current: Number(status) }, () => this.loadList())
  },

  loadList() {
    this.setData({ loading: true })
    api.getMyCoupons(this.data.current).then((list) => {
      // 预计算折扣文案，避免 WXML 中浮点运算精度问题
      const coupons = (list || []).map((c) => ({
        ...c,
        discountText: c.type === 2 && c.discount != null
          ? String(Math.round(Number(c.discount) * 100) / 10)
          : ''
      }))
      this.setData({ list: coupons })
    }).catch(() => {
      this.setData({ list: [] })
    }).then(() => this.setData({ loading: false }))
  },

  // 去领券中心
  goCenter() {
    wx.navigateTo({ url: '/pages/coupon-center/coupon-center' })
  },

  // 去点餐
  goMenu() {
    wx.switchTab({ url: '/pages/menu/menu' })
  }
})
