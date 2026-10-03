// pages/merchant/coupon/coupon.js —— 店家端优惠券管理
const app = getApp()
const api = require('../../../utils/api')

Page({
  data: {
    list: [],
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
    api.getAdminCoupons().then((list) => {
      this.setData({ list: list || [] })
    }).catch(() => {
      this.setData({ list: [] })
    }).then(() => this.setData({ loading: false }))
  },

  // 新增满减券
  addCoupon() {
    wx.showModal({
      title: '新增优惠券',
      editable: true,
      placeholderText: '请输入券名称，如：满 80 减 15',
      confirmColor: '#2f80ed',
      success: (res) => {
        if (!res.confirm) return
        const name = (res.content || '').trim()
        if (!name) {
          wx.showToast({ title: '券名称不能为空', icon: 'none' })
          return
        }
        // 简化流程：默认生成一张满 50 减 10 的券，可在后续详情页调整
        api.addCoupon({
          name,
          type: 1,
          threshold: 50,
          amount: 10,
          totalCount: -1,
          perLimit: 1,
          validDays: 30,
          status: 1,
          description: '扫码点餐专享'
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
    const target = this.data.list.find((c) => c.id === id)
    if (!target) return
    const nextStatus = target.status === 1 ? 0 : 1
    api.updateCouponStatus(id, nextStatus).then(() => {
      this.loadList()
      wx.showToast({ title: nextStatus === 1 ? '已启用' : '已停用', icon: 'none' })
    }).catch(() => {})
  },

  deleteCoupon(e) {
    const { id } = e.currentTarget.dataset
    const target = this.data.list.find((c) => c.id === id)
    if (!target) return
    wx.showModal({
      title: '删除确认',
      content: `确定删除「${target.name}」吗？`,
      confirmText: '删除',
      confirmColor: '#ff3b30',
      success: (res) => {
        if (!res.confirm) return
        api.deleteCoupon(id).then(() => {
          wx.showToast({ title: '已删除', icon: 'none' })
          this.loadList()
        }).catch(() => {})
      }
    })
  },

  // 券面文案（折扣保留 1 位小数，避免浮点误差）
  couponDesc(item) {
    if (item.type === 2 && item.discount != null) {
      const zhe = Math.round(Number(item.discount) * 100) / 10
      return `${item.threshold > 0 ? '满' + item.threshold + '元享' : ''}${zhe} 折`
    }
    return `${item.threshold > 0 ? '满' + item.threshold + '减' : '立减'}${item.amount} 元`
  }
})
