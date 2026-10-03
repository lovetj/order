// pages/points/points.js —— 积分商城
const app = getApp()
const api = require('../../utils/api')

Page({
  data: {
    goods: [],
    myPoints: 0,
    loading: false
  },

  onShow() {
    if (!app.globalData.role) {
      wx.reLaunch({ url: '/pages/role/role' })
      return
    }
    this.loadPoints()
    this.loadGoods()
  },

  loadPoints() {
    api.getMemberInfo().then((data) => {
      if (data) {
        this.setData({ myPoints: data.points || 0 })
      }
    }).catch(() => {})
  },

  loadGoods() {
    this.setData({ loading: true })
    api.getPointsGoods().then((list) => {
      // 标记是否为真实图片 URL，用于 WXML 区分 image / emoji 渲染
      const goods = (list || []).map((g) => ({
        ...g,
        imageUrl: /^https?:\/\//.test(g.image || '')
      }))
      this.setData({ goods })
    }).catch(() => {
      this.setData({ goods: [] })
    }).then(() => this.setData({ loading: false }))
  },

  exchange(e) {
    const { id } = e.currentTarget.dataset
    const item = this.data.goods.find((g) => g.id === id)
    if (!item) return

    if (this.data.myPoints < item.points) {
      wx.showModal({
        title: '积分不足',
        content: `兑换「${item.name}」需要 ${item.points} 积分，当前仅 ${this.data.myPoints} 积分。\n消费即可累积积分（1 元 = 1 积分）。`,
        showCancel: false,
        confirmText: '知道了',
        confirmColor: '#ff6b35'
      })
      return
    }

    wx.showModal({
      title: '确认兑换',
      content: `使用 ${item.points} 积分兑换「${item.name}」？`,
      confirmText: '确认兑换',
      confirmColor: '#d4a24a',
      success: (res) => {
        if (!res.confirm) return
        api.exchangePoints(id, 1).then((record) => {
          wx.showModal({
            title: '兑换成功',
            content: `核销码：${record.verifyCode}\n\n请到店出示核销码领取`,
            showCancel: false,
            confirmText: '知道了',
            confirmColor: '#d4a24a',
            complete: () => {
              this.loadPoints()
              this.loadGoods()
            }
          })
        }).catch(() => {})
      }
    })
  },

  goMyExchanges() {
    wx.navigateTo({ url: '/pages/points-record/points-record' })
  }
})
