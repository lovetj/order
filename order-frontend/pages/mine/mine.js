// pages/mine/mine.js —— 顾客端我的
const app = getApp()
const api = require('../../utils/api')

Page({
  data: {
    userInfo: {},
    // 头像是否为真实图片 URL（否则按 emoji/文字渲染）
    isAvatarUrl: false,
    tableNo: '',
    stats: [
      { label: '待接单', value: 0 },
      { label: '制作中', value: 0 },
      { label: '已完成', value: 0 }
    ],
    menus: [
      { icon: '📋', name: '我的订单', key: 'order' },
      { icon: '👑', name: '会员中心', key: 'member' },
      { icon: '🎁', name: '积分商城', key: 'points' },
      { icon: '🎫', name: '我的优惠券', key: 'coupon' },
      { icon: '🏷️', name: '领券中心', key: 'couponCenter' },
      { icon: '📍', name: '收货地址', key: 'address' },
      { icon: '💬', name: '联系客服', key: 'service' }
    ],
    couponCount: 0,
    points: 0
  },

  onShow() {
    if (this.getTabBar && this.getTabBar()) {
      this.getTabBar().setData({ selected: 3 })
    }
    if (app.globalData.role !== 'customer') {
      wx.reLaunch({ url: '/pages/role/role' })
      return
    }
    if (!app.isLogin()) {
      wx.reLaunch({ url: '/pages/login/login?role=customer' })
      return
    }
    const userInfo = app.globalData.userInfo || {}
    this.setData({
      userInfo,
      isAvatarUrl: /^https?:\/\//.test(userInfo.avatar || ''),
      tableNo: app.globalData.tableNo || '未获取'
    })
    this.loadUserInfo()
    this.loadCounts()
    this.loadMember()
  },

  // 会员信息（积分 / 优惠券数量 / 等级）
  loadMember() {
    if (!app.isLogin()) return
    api.getMemberInfo().then((data) => {
      if (!data) return
      this.setData({
        couponCount: data.couponCount || 0,
        points: data.points || 0
      })
    }).catch(() => {})
  },

  // 进入会员中心
  goMember() {
    wx.navigateTo({ url: '/pages/member/member' })
  },

  // 拉取最新用户信息
  loadUserInfo() {
    if (!app.isLogin()) return
    api.getUserInfo().then((user) => {
      if (!user) return
      const userInfo = {
        nickName: user.nickname || '微信用户',
        avatar: user.avatar || '🙋',
        memberLevel: user.memberLevel || '普通会员'
      }
      app.globalData.userInfo = userInfo
      this.setData({ userInfo, isAvatarUrl: /^https?:\/\//.test(userInfo.avatar || '') })
    }).catch(() => {})
  },

  // 订单各状态数量
  loadCounts() {
    if (!app.isLogin()) return
    api.getOrderCounts().then((counts) => {
      if (!counts) return
      this.setData({
        stats: [
          { label: '待接单', value: counts.pending || 0 },
          { label: '制作中', value: counts.cooking || 0 },
          { label: '已完成', value: counts.done || 0 }
        ]
      })
    }).catch(() => {})
  },

  handleMenu(e) {
    const key = e.currentTarget.dataset.key
    if (key === 'order') {
      wx.switchTab({ url: '/pages/order/order' })
      return
    }
    if (key === 'coupon') {
      wx.navigateTo({ url: '/pages/coupon/coupon' })
      return
    }
    if (key === 'couponCenter') {
      wx.navigateTo({ url: '/pages/coupon-center/coupon-center' })
      return
    }
    if (key === 'member') {
      wx.navigateTo({ url: '/pages/member/member' })
      return
    }
    if (key === 'points') {
      wx.navigateTo({ url: '/pages/points/points' })
      return
    }
    if (key === 'address') {
      wx.chooseAddress({
        fail: () => wx.showToast({ title: '未授权或暂不可用', icon: 'none' })
      })
      return
    }
    wx.showToast({ title: '功能开发中', icon: 'none' })
  },

  switchRole() {
    wx.showModal({
      title: '切换身份',
      content: '切换到「我是店家」需要重新以店家账号登录',
      confirmText: '去登录',
      confirmColor: '#2f80ed',
      success: (res) => {
        if (!res.confirm) return
        app.logout()
        wx.reLaunch({ url: '/pages/login/login?role=merchant' })
      }
    })
  },

  logout() {
    wx.showModal({
      title: '提示',
      content: '确定退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          app.logout()
          wx.reLaunch({ url: '/pages/role/role' })
        }
      }
    })
  }
})
