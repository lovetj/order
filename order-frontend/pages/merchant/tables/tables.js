// pages/merchant/tables/tables.js —— 店家端桌位与二维码管理
const app = getApp()
const api = require('../../../utils/api')

Page({
  data: {
    shopId: '',
    list: [],
    loading: false
  },

  onShow() {
    if (app.globalData.role !== 'merchant') {
      wx.reLaunch({ url: '/pages/role/role' })
      return
    }
    // 门店ID：从店家登录信息中获取（由后端 token 决定，不硬编码）
    const admin = app.globalData.admin || {}
    const shopId = admin.shopId || app.globalData.shopId || ''
    if (!shopId) {
      wx.showToast({ title: '未获取到店铺信息，请重新登录', icon: 'none' })
      return
    }
    this.setData({ shopId })
    this.loadList()
  },

  loadList() {
    this.setData({ loading: true })
    api.getTables(this.data.shopId).then((list) => {
      this.setData({ list: list || [] })
    }).catch(() => {
      this.setData({ list: [] })
    }).then(() => this.setData({ loading: false }))
  },

  // 新增桌位
  addTable() {
    wx.showModal({
      title: '新增桌位',
      editable: true,
      placeholderText: '请输入桌号，如 A01',
      confirmColor: '#2f80ed',
      success: (res) => {
        if (!res.confirm) return
        const tableNo = (res.content || '').trim().toUpperCase()
        if (!tableNo) {
          wx.showToast({ title: '桌号不能为空', icon: 'none' })
          return
        }
        // 店铺ID由后端从登录态注入，前端不再传，避免越权
        api.addTable({
          tableNo,
          capacity: 4,
          status: 1
        }).then(() => {
          wx.showToast({ title: '新增成功', icon: 'success' })
          this.loadList()
        }).catch(() => {})
      }
    })
  },

  // 启用/停用
  toggleStatus(e) {
    const { id } = e.currentTarget.dataset
    api.toggleTable(id).then(() => {
      this.loadList()
      wx.showToast({ title: '已更新状态', icon: 'none' })
    }).catch(() => {})
  },

  // 生成/查看桌位二维码（提示扫码路径）
  showQrcode(e) {
    const { id } = e.currentTarget.dataset
    const table = this.data.list.find((t) => t.id === id)
    if (!table) return
    // 二维码必须携带 shopId，顾客扫码后才能识别所属店铺
    const path = `pages/role/role?shopId=${this.data.shopId}&tableNo=${table.tableNo}`
    wx.showModal({
      title: `桌位 ${table.tableNo} 二维码`,
      content: `顾客扫码后进入小程序的路径：\n${path}\n\n请在微信公众平台「工具-生成小程序码」中，用该路径生成带参数的小程序码，打印后贴在餐桌上。`,
      showCancel: false,
      confirmText: '知道了',
      confirmColor: '#2f80ed'
    })
  },

  // 修改桌号
  editTable(e) {
    const { id } = e.currentTarget.dataset
    const table = this.data.list.find((t) => t.id === id)
    if (!table) return
    wx.showModal({
      title: '修改桌号',
      editable: true,
      placeholderText: `当前 ${table.tableNo}`,
      confirmColor: '#2f80ed',
      success: (res) => {
        if (!res.confirm) return
        const tableNo = (res.content || '').trim().toUpperCase()
        if (!tableNo) {
          wx.showToast({ title: '桌号不能为空', icon: 'none' })
          return
        }
        api.updateTable({ id, tableNo }).then(() => {
          wx.showToast({ title: '已修改', icon: 'none' })
          this.loadList()
        }).catch(() => {})
      }
    })
  },

  deleteTable(e) {
    const { id } = e.currentTarget.dataset
    wx.showModal({
      title: '删除确认',
      content: '确定删除该桌位吗？',
      confirmText: '删除',
      confirmColor: '#ff3b30',
      success: (res) => {
        if (!res.confirm) return
        api.deleteTable(id).then(() => {
          wx.showToast({ title: '已删除', icon: 'none' })
          this.loadList()
        }).catch(() => {})
      }
    })
  },

  // 长按操作菜单
  onTableLongPress(e) {
    const { id } = e.currentTarget.dataset
    wx.showActionSheet({
      itemList: ['修改桌号', '删除桌位'],
      success: (res) => {
        if (res.tapIndex === 0) {
          this.editTable({ currentTarget: { dataset: { id } } })
        } else if (res.tapIndex === 1) {
          this.deleteTable({ currentTarget: { dataset: { id } } })
        }
      }
    })
  }
})
