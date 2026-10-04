// pages/order/order.js —— 顾客端订单
const app = getApp()
const api = require('../../utils/api')

const TABS = [
  { key: 'all', name: '全部' },
  { key: 'pending', name: '待接单' },
  { key: 'cooking', name: '制作中' },
  { key: 'done', name: '已完成' }
]

Page({
  data: {
    tabs: TABS,
    current: 'all',
    list: [],
    loading: false
  },

  onShow() {
    if (this.getTabBar && this.getTabBar()) {
      this.getTabBar().setData({ selected: 2 })
    }
    if (app.globalData.role !== 'customer') {
      wx.reLaunch({ url: '/pages/role/role' })
      return
    }
    if (!app.isLogin()) {
      wx.reLaunch({ url: '/pages/login/login?role=customer' })
      return
    }
    this.loadList()
  },

  // 底部页签切换时刷新当前页面内容（由 custom-tab-bar 调用）
  onTabRefresh() {
    if (!app.isLogin()) return
    this.loadList()
  },

  loadList() {
    this.setData({ loading: true })
    api.getOrders({ pageNum: 1, pageSize: 50, status: this.data.current })
      .then((page) => {
        this.setData({ list: (page && page.records) || [] })
      })
      .catch(() => {
        this.setData({ list: [] })
      })
      .then(() => this.setData({ loading: false }))
  },

  switchTab(e) {
    this.setData({ current: e.currentTarget.dataset.key }, () => this.loadList())
  },

  goMenu() {
    wx.switchTab({ url: '/pages/menu/menu' })
  },

  showDetail(e) {
    const { id } = e.currentTarget.dataset
    const order = this.data.list.find((o) => o.id === id)
    if (!order) return
    const items = (order.items || []).map((i) => {
      const spec = i.specText ? `（${i.specText}）` : ''
      return `${i.name}${spec} x${i.count}`
    }).join('\n')
    wx.showModal({
      title: `订单 ${order.id}`,
      content: `${items}\n\n合计：¥${order.amount}\n桌号：${order.table}\n状态：${order.statusText}`,
      showCancel: false,
      confirmText: '知道了',
      confirmColor: '#ff6b35'
    })
  },

  // 取消订单（仅待接单/制作中可取消）
  cancelOrder(e) {
    const { id } = e.currentTarget.dataset
    wx.showModal({
      title: '取消订单',
      content: '确定取消该订单吗？',
      confirmText: '确定取消',
      confirmColor: '#ff3b30',
      success: (res) => {
        if (!res.confirm) return
        api.cancelOrder(id, '顾客取消').then(() => {
          wx.showToast({ title: '已取消', icon: 'none' })
          this.loadList()
        }).catch(() => {})
      }
    })
  }
})
