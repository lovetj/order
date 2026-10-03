// pages/merchant/points-goods/points-goods.js —— 店家端积分商品管理
const app = getApp()
const api = require('../../../utils/api')

Page({
  data: {
    list: [],
    loading: false,
    verifyCode: ''
  },

  onShow() {
    if (app.globalData.role !== 'merchant') {
      wx.reLaunch({ url: '/pages/role/role' })
      return
    }
    if (!app.isLogin()) {
      wx.reLaunch({ url: '/pages/login/login?role=merchant' })
      return
    }
    this.loadList()
  },

  loadList() {
    this.setData({ loading: true })
    api.getAdminPointsGoods().then((list) => {
      // 标记是否为真实图片 URL，用于 WXML 区分 image / emoji 渲染
      const goods = (list || []).map((g) => ({
        ...g,
        imageUrl: /^https?:\/\//.test(g.image || '')
      }))
      this.setData({ list: goods })
    }).catch(() => {
      this.setData({ list: [] })
    }).then(() => this.setData({ loading: false }))
  },

  // 新增积分商品
  addGoods() {
    wx.showModal({
      title: '新增积分商品',
      editable: true,
      placeholderText: '请输入商品名称',
      confirmColor: '#d4a24a',
      success: (res) => {
        if (!res.confirm) return
        const name = (res.content || '').trim()
        if (!name) {
          wx.showToast({ title: '名称不能为空', icon: 'none' })
          return
        }
        this.inputPoints(name)
      }
    })
  },

  inputPoints(name) {
    wx.showModal({
      title: `新增（${name}）`,
      editable: true,
      placeholderText: '请输入兑换所需积分，如 100',
      confirmColor: '#d4a24a',
      success: (res) => {
        if (!res.confirm) return
        const points = Number(res.content)
        if (!points || points <= 0) {
          wx.showToast({ title: '请输入有效积分', icon: 'none' })
          return
        }
        api.addPointsGoods({
          name,
          description: '凭核销码到店兑换',
          image: '🎁',
          points,
          stock: -1,
          perLimit: 1,
          sort: 0,
          status: 1
        }).then(() => {
          wx.showToast({ title: '新增成功', icon: 'success' })
          this.loadList()
        }).catch(() => {})
      }
    })
  },

  // 上下架
  toggleStatus(e) {
    const { id } = e.currentTarget.dataset
    const target = this.data.list.find((g) => g.id === id)
    if (!target) return
    const nextStatus = target.status === 1 ? 0 : 1
    api.updatePointsGoodsStatus(id, nextStatus).then(() => {
      this.loadList()
      wx.showToast({ title: nextStatus === 1 ? '已上架' : '已下架', icon: 'none' })
    }).catch(() => {})
  },

  deleteGoods(e) {
    const { id } = e.currentTarget.dataset
    const target = this.data.list.find((g) => g.id === id)
    if (!target) return
    wx.showModal({
      title: '删除确认',
      content: `确定删除「${target.name}」吗？`,
      confirmText: '删除',
      confirmColor: '#ff3b30',
      success: (res) => {
        if (!res.confirm) return
        api.deletePointsGoods(id).then(() => {
          wx.showToast({ title: '已删除', icon: 'none' })
          this.loadList()
        }).catch(() => {})
      }
    })
  },

  // 核销
  onVerifyInput(e) {
    this.setData({ verifyCode: e.detail.value })
  },

  doVerify() {
    const code = (this.data.verifyCode || '').trim()
    if (!code) {
      wx.showToast({ title: '请输入核销码', icon: 'none' })
      return
    }
    api.verifyPoints(code).then(() => {
      wx.showToast({ title: '核销成功', icon: 'success' })
      this.setData({ verifyCode: '' })
    }).catch(() => {})
  }
})
