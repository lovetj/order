// pages/member/member.js —— 会员中心
const app = getApp()
const api = require('../../utils/api')

Page({
  data: {
    member: {
      nickName: '微信用户',
      avatar: '🙋',
      memberLevel: '普通会员',
      points: 0,
      totalConsume: 0,
      orderCount: 0,
      nextLevelName: '',
      nextLevelNeed: 0,
      progress: 0,
      couponCount: 0
    },
    loading: false
  },

  onShow() {
    if (!app.globalData.role) {
      wx.reLaunch({ url: '/pages/role/role' })
      return
    }
    this.loadMember()
  },

  loadMember() {
    this.setData({ loading: true })
    api.getMemberInfo().then((data) => {
      if (!data) return
      this.setData({
        member: {
          nickName: data.nickName || '微信用户',
          avatar: data.avatar || '🙋',
          memberLevel: data.memberLevel || '普通会员',
          points: data.points || 0,
          totalConsume: data.totalConsume || 0,
          orderCount: data.orderCount || 0,
          nextLevelName: data.nextLevelName || '',
          nextLevelNeed: data.nextLevelNeed || 0,
          progress: data.progress || 0,
          couponCount: data.couponCount || 0
        }
      })
    }).catch(() => {}).then(() => this.setData({ loading: false }))
  },

  goCoupon() {
    wx.navigateTo({ url: '/pages/coupon/coupon' })
  },

  goCouponCenter() {
    wx.navigateTo({ url: '/pages/coupon-center/coupon-center' })
  },

  goPointsMall() {
    wx.navigateTo({ url: '/pages/points/points' })
  },

  goOrders() {
    wx.switchTab({ url: '/pages/order/order' })
  }
})
