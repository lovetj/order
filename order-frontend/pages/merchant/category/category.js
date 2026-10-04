// pages/merchant/category/category.js —— 店家端分类管理
const app = getApp()
const api = require('../../../utils/api')

// 常用图标
const ICONS = ['🍽️', '🔥', '🍚', '🍜', '🍲', '🥤', '🍟', '🥗', '🍤', '🍱', '🍢', '🍰']

Page({
  data: {
    list: [],
    icons: ICONS,
    loading: false,
    // 自定义输入弹窗状态
    dialogVisible: false,
    dialogMode: 'add', // add | edit
    dialogValue: '',
    dialogFocus: false,
    // 编辑时的目标分类ID
    dialogEditId: ''
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

  // ==================== 自定义输入弹窗（新增 / 编辑） ====================

  // 空操作：用于阻止弹窗内部点击冒泡到遮罩
  noop() {},

  // 打开新增弹窗：每次打开都清空输入框，避免残留上次内容
  openAdd() {
    this.setData({
      dialogVisible: true,
      dialogMode: 'add',
      dialogValue: '',
      dialogEditId: '',
      dialogFocus: true
    })
  },

  // 打开编辑弹窗：预填当前名称
  editCategory(e) {
    const { id } = e.currentTarget.dataset
    const target = this.data.list.find((c) => c.id === id)
    if (!target) return
    this.setData({
      dialogVisible: true,
      dialogMode: 'edit',
      dialogValue: target.name || '',
      dialogEditId: id,
      dialogFocus: true
    })
  },

  onDialogInput(e) {
    this.setData({ dialogValue: e.detail.value })
  },

  closeDialog() {
    this.setData({ dialogVisible: false, dialogFocus: false })
  },

  // 确定：按模式执行新增 / 编辑
  confirmDialog() {
    const name = (this.data.dialogValue || '').trim()
    if (!name) {
      wx.showToast({ title: '分类名称不能为空', icon: 'none' })
      return
    }
    if (this.data.dialogMode === 'edit') {
      this.updateCategoryName(this.data.dialogEditId, name)
    } else {
      this.addCategory(name)
    }
  },

  addCategory(name) {
    api.addCategory({
      name,
      code: 'c_' + Date.now(),
      icon: '🍽️',
      sort: this.data.list.length + 1,
      status: 1
    }).then(() => {
      this.closeDialog()
      wx.showToast({ title: '新增成功', icon: 'success' })
      this.loadList()
    }).catch(() => {})
  },

  updateCategoryName(id, name) {
    api.updateCategory({ id, name }).then(() => {
      this.closeDialog()
      wx.showToast({ title: '已修改', icon: 'none' })
      this.loadList()
    }).catch(() => {})
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
    this.swapSort(list, index, index - 1)
  },

  // 排序调整（下移）
  moveDown(e) {
    const { id } = e.currentTarget.dataset
    const list = [...this.data.list]
    const index = list.findIndex((c) => c.id === id)
    if (index < 0 || index >= list.length - 1) return
    this.swapSort(list, index, index + 1)
  },

  // 交换两个分类的 sort 值（升序排列，数值小的在前）
  swapSort(list, i, j) {
    const a = list[i]
    const b = list[j]
    // 若两者 sort 相同（历史数据），按索引兜底生成可交换的值，避免顺序不变
    let aSort = a.sort
    let bSort = b.sort
    if (aSort === bSort) {
      aSort = i
      bSort = j
    }
    Promise.all([
      api.updateCategory({ id: a.id, sort: bSort }),
      api.updateCategory({ id: b.id, sort: aSort })
    ]).then(() => this.loadList()).catch(() => {})
  }
})
