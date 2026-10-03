// pages/points-record/points-record.js —— 我的兑换记录
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
    api.getMyExchanges().then((list) => {
      // 标记是否为真实图片 URL，用于 WXML 区分 image / emoji 渲染
      const records = (list || []).map((r) => ({
        ...r,
        imageUrl: /^https?:\/\//.test(r.goodsImage || '')
      }))
      this.setData({ list: records })
    }).catch(() => {
      this.setData({ list: [] })
    }).then(() => this.setData({ loading: false }))
  },

  showCode(e) {
    const { code, name } = e.currentTarget.dataset
    wx.showModal({
      title: '核销码',
      content: `商品：${name}\n核销码：${code}\n\n请到店出示核销码领取`,
      showCancel: false,
      confirmText: '知道了',
      confirmColor: '#d4a24a'
    })
  },

  goPoints() {
    wx.navigateTo({ url: '/pages/points/points' })
  }
})
