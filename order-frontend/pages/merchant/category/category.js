// pages/merchant/category/category.js —— 店家端分类管理
const app = getApp()
const api = require('../../../utils/api')

// 常用图标
const ICONS = ['🍽️', '🔥', '🍚', '🍜', '🍲', '🥤', '🍟', '🥗', '🍤', '🍱', '🍢', '🍰']

Page({
  data: {
    list: [],
    icons: ICONS,
    loading: false
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
    api.getAllCategories().then((list) => {
      this.setData({ list: list || [] })
    }).catch(() => {
      this.setData({ list: [] })
    }).then(() => this.setData({ loading: false }))
  },

  // 新增分类
  addCategory() {
    wx.showModal({
      title: '新增分类',
      editable: true,
      placeholderText: '请输入分类名称，如：凉菜',
      confirmColor: '#2f80ed',
      success: (res) => {
        if (!res.confirm) return
        const name = (res.content || '').trim()
        if (!name) {
          wx.showToast({ title: '分类名称不能为空', icon: 'none' })
          return
        }
        api.addCategory({
          name,
          code: 'c_' + Date.now(),
          icon: '🍽️',
          sort: this.data.list.length + 1,
          status: 1
        }).then(() => {
          wx.showToast({ title: '新增成功', icon: 'success' })
          this.loadList()
        }).catch(() => {})
      }
    })
  },

  // 编辑分类名称
  editCategory(e) {
    const { id } = e.currentTarget.dataset
    const target = this.data.list.find((c) => c.id === id)
    if (!target) return
    wx.showModal({
      title: '修改分类名称',
      editable: true,
      placeholderText: `当前：${target.name}`,
      confirmColor: '#2f80ed',
      success: (res) => {
        if (!res.confirm) return
        const name = (res.content || '').trim()
        if (!name) {
          wx.showToast({ title: '分类名称不能为空', icon: 'none' })
          return
        }
        api.updateCategory({ id, name }).then(() => {
          wx.showToast({ title: '已修改', icon: 'none' })
          this.loadList()
        }).catch(() => {})
      }
    })
  },

  // 切换启用/禁用
  toggleStatus(e) {
    const { id } = e.currentTarget.dataset
    const target = this.data.list.find((c) => c.id === id)
    if (!target) return
    const nextStatus = target.status === 1 ? 0 : 1
    api.updateCategory({ id, status: nextStatus }).then(() => {
      this.loadList()
      wx.showToast({ title: nextStatus === 1 ? '已启用' : '已禁用', icon: 'none' })
    }).catch(() => {})
  },

  // 删除分类
  deleteCategory(e) {
    const { id } = e.currentTarget.dataset
    const target = this.data.list.find((c) => c.id === id)
    if (!target) return
    wx.showModal({
      title: '删除确认',
      content: `确定删除分类「${target.name}」吗？\n（分类下若仍有菜品将无法删除）`,
      confirmText: '删除',
      confirmColor: '#ff3b30',
      success: (res) => {
        if (!res.confirm) return
        api.deleteCategory(id).then(() => {
          wx.showToast({ title: '已删除', icon: 'none' })
          this.loadList()
        }).catch(() => {})
      }
    })
  },

  // 排序调整（上移）
  moveUp(e) {
    const { id } = e.currentTarget.dataset
    const list = [...this.data.list]
    const index = list.findIndex((c) => c.id === id)
    if (index <= 0) return
    const prev = list[index - 1]
    // 交换 sort
    const curSort = list[index].sort
    const prevSort = prev.sort
    Promise.all([
      api.updateCategory({ id: list[index].id, sort: prevSort }),
      api.updateCategory({ id: prev.id, sort: curSort })
    ]).then(() => this.loadList()).catch(() => {})
  }
})
