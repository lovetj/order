// pages/merchant/orders/orders.js —— 店家端订单管理
const app = getApp()
const api = require('../../../utils/api')

const TABS = [
  { key: 'pending', label: '待接单' },
  { key: 'cooking', label: '制作中' },
  { key: 'done', label: '已完成' },
  { key: 'all', label: '全部' }
]

Page({
  data: {
    tabs: TABS,
    activeTab: 'pending',
    list: [],
    counts: {},
    loading: false
  },

  onShow() {
    if (this.getTabBar && this.getTabBar()) {
      this.getTabBar().setData({ selected: 2 })
    }
    if (app.globalData.role !== 'merchant') {
      wx.reLaunch({ url: '/pages/role/role' })
      return
    }
    if (!app.isLogin()) {
      wx.reLaunch({ url: '/pages/login/login?role=merchant' })
      return
    }
    this.loadCounts()
    this.loadList()
  },

  // 各状态数量（Tab 角标）
  loadCounts() {
    api.getAdminOrderCounts().then((counts) => {
      this.setData({ counts: counts || {} })
    }).catch(() => {})
  },

  // 当前 Tab 订单列表
  loadList() {
    this.setData({ loading: true })
    api.getAdminOrders({ pageNum: 1, pageSize: 50, status: this.data.activeTab })
      .then((page) => {
        this.setData({ list: (page && page.records) || [] })
      })
      .catch(() => {
        this.setData({ list: [] })
      })
      .then(() => this.setData({ loading: false }))
  },

  switchTab(e) {
    const { key } = e.currentTarget.dataset
    this.setData({ activeTab: key }, () => this.loadList())
  },

  // 接单 / 出餐：根据服务端返回的 action.next 决定
  handleNext(e) {
    const { id, next } = e.currentTarget.dataset
    if (next === 'cooking') {
      api.acceptOrder(id).then(() => {
        wx.showToast({ title: '接单成功', icon: 'none' })
        this.afterAction()
      }).catch(() => {})
    } else if (next === 'done') {
      api.finishOrder(id).then(() => {
        wx.showToast({ title: '出餐成功', icon: 'none' })
        this.afterAction()
      }).catch(() => {})
    }
  },

  rejectOrder(e) {
    const { id } = e.currentTarget.dataset
    wx.showModal({
      title: '拒单确认',
      content: '确定拒绝该订单吗？',
      confirmText: '拒单',
      confirmColor: '#ff3b30',
      success: (res) => {
        if (!res.confirm) return
        api.rejectOrder(id, '店家拒单').then(() => {
          wx.showToast({ title: '已拒单', icon: 'none' })
          this.afterAction()
        }).catch(() => {})
      }
    })
  },

  // 操作后刷新列表与角标
  afterAction() {
    this.loadCounts()
    this.loadList()
  },

  // 查看/打印小票
  printReceipt(e) {
    const { id } = e.currentTarget.dataset
    wx.navigateTo({ url: `/pages/merchant/receipt/receipt?id=${id}` })
  }
})
