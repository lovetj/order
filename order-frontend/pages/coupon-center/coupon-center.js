// pages/coupon-center/coupon-center.js —— 领券中心
const app = getApp()
const api = require('../../utils/api')

Page({
  data: {
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

  loadList() {
    this.setData({ loading: true })
    api.getAvailableCoupons().then((list) => {
      // 预计算折扣文案，避免 WXML 中浮点运算精度问题（如 0.88*10 = 8.799999）
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

  receive(e) {
    const { id } = e.currentTarget.dataset
    api.receiveCoupon(id).then(() => {
      wx.showToast({ title: '领取成功', icon: 'success' })
      this.loadList()
    }).catch(() => {})
  },

  // 券面文案
  labelOf(item) {
    if (item.type === 2) {
      return `${item.discount * 10} 折`
    }
    return `减 ${item.amount} 元`
  },

  goMyCoupons() {
    wx.navigateTo({ url: '/pages/coupon/coupon' })
  }
})
