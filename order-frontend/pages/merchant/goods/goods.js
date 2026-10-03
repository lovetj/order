// pages/merchant/goods/goods.js —— 店家端商品管理
const app = getApp()
const api = require('../../../utils/api')

Page({
  data: {
    categories: [],
    activeCategory: 'all',
    goods: [],
    list: [],
    onShelfCount: 0,
    loading: false
  },

  onShow() {
    if (this.getTabBar && this.getTabBar()) {
      this.getTabBar().setData({ selected: 1 })
    }
    if (app.globalData.role !== 'merchant') {
      wx.reLaunch({ url: '/pages/role/role' })
      return
    }
    if (!app.isLogin()) {
      wx.reLaunch({ url: '/pages/login/login?role=merchant' })
      return
    }
    this.loadData()
  },

  // 加载分类 + 全部商品（含下架）
  loadData() {
    this.setData({ loading: true })
    Promise.all([
      api.getAllCategories(),
      api.getAdminDishes({ categoryId: 'all' })
    ]).then(([categories, goods]) => {
      this.setData({
        categories: categories || [],
        goods: goods || []
      })
      this.filter(this.data.activeCategory)
    }).catch(() => {
      this.setData({ categories: [], goods: [], list: [] })
    }).then(() => this.setData({ loading: false }))
  },

  switchCategory(e) {
    const { id } = e.currentTarget.dataset
    this.setData({ activeCategory: id })
    this.filter(id)
  },

  filter(categoryId) {
    const list = (categoryId === 'all'
      ? this.data.goods
      : this.data.goods.filter((g) => g.categoryId === categoryId))
      // 标记是否为真实图片 URL，用于 WXML 区分 image / emoji 渲染
      .map((g) => ({ ...g, imageUrl: /^https?:\/\//.test(g.image || '') }))
    this.setData({
      list,
      onShelfCount: this.data.goods.filter((g) => g.status).length
    })
  },

  // 上下架：status 前端是布尔值，接口传 1/0
  toggleShelf(e) {
    const { id } = e.currentTarget.dataset
    const target = this.data.goods.find((g) => g.id === id)
    if (!target) return
    const nextStatus = target.status ? 0 : 1
    api.updateDishStatus(id, nextStatus).then(() => {
      const goods = this.data.goods.map((g) =>
        g.id === id ? { ...g, status: !!nextStatus } : g
      )
      this.setData({ goods })
      this.filter(this.data.activeCategory)
      wx.showToast({ title: nextStatus === 1 ? '已上架' : '已下架', icon: 'none' })
    }).catch(() => {})
  },

  // 改价
  editPrice(e) {
    const { id } = e.currentTarget.dataset
    const target = this.data.goods.find((g) => g.id === id)
    if (!target) return
    wx.showModal({
      title: '修改价格',
      editable: true,
      placeholderText: `当前 ¥${target.price}`,
      confirmColor: '#2f80ed',
      success: (res) => {
        if (!res.confirm) return
        const price = Number(res.content)
        if (!price || price <= 0) {
          wx.showToast({ title: '请输入有效价格', icon: 'none' })
          return
        }
        api.updateDishPrice(id, price).then(() => {
          const goods = this.data.goods.map((g) => (g.id === id ? { ...g, price } : g))
          this.setData({ goods })
          this.filter(this.data.activeCategory)
          wx.showToast({ title: '价格已更新', icon: 'none' })
        }).catch(() => {})
      }
    })
  },

  // 新增商品：跳转表单页
  addGoods() {
    wx.navigateTo({ url: '/pages/merchant/dish-edit/dish-edit' })
  },

  // 编辑商品：跳转表单页（带 id）
  editGoods(e) {
    const { id } = e.currentTarget.dataset
    wx.navigateTo({ url: `/pages/merchant/dish-edit/dish-edit?id=${id}` })
  },

  // 删除商品
  deleteGoods(e) {
    const { id } = e.currentTarget.dataset
    wx.showModal({
      title: '删除确认',
      content: '确定删除该商品吗？',
      confirmText: '删除',
      confirmColor: '#ff3b30',
      success: (res) => {
        if (!res.confirm) return
        api.deleteDish(id).then(() => {
          wx.showToast({ title: '已删除', icon: 'none' })
          this.loadData()
        }).catch(() => {})
      }
    })
  },

  // 长按商品弹出操作菜单
  onGoodsLongPress(e) {
    const { id } = e.currentTarget.dataset
    wx.showActionSheet({
      itemList: ['编辑商品', '修改价格', '删除商品'],
      success: (res) => {
        if (res.tapIndex === 0) {
          this.editGoods({ currentTarget: { dataset: { id } } })
        } else if (res.tapIndex === 1) {
          this.editPrice({ currentTarget: { dataset: { id } } })
        } else if (res.tapIndex === 2) {
          this.deleteGoods({ currentTarget: { dataset: { id } } })
        }
      }
    })
  }
})
