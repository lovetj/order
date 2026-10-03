// pages/role/role.js —— 角色选择（启动页）
const app = getApp()

Page({
  data: {
    tableNo: ''
  },

  onLoad() {
    this.setData({ tableNo: app.globalData.tableNo || '' })
  },

  chooseRole(e) {
    const { role } = e.currentTarget.dataset
    // 顾客 -> 微信授权登录；店家 -> 账号密码登录
    wx.navigateTo({ url: `/pages/login/login?role=${role}` })
  }
})
