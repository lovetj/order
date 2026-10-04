// pages/menu/menu.js —— 顾客端点餐
const app = getApp()
const api = require('../../utils/api')
const { formatImageUrl } = require('../../utils/util')

Page({
  data: {
    categories: [],
    goods: [],
    currentCategory: '',
    goodsList: [],
    cart: {},          // { dishId: count }
    cartCount: 0,
    cartAmount: 0,
    showCart: false
  },

  onShow() {
    if (this.getTabBar && this.getTabBar()) {
      this.getTabBar().setData({ selected: 1 })
    }
    if (app.globalData.role !== 'customer') {
      wx.reLaunch({ url: '/pages/role/role' })
      return
    }
    if (!app.isLogin()) {
      wx.reLaunch({ url: '/pages/login/login?role=customer' })
      return
    }
    if (!this.data.categories.length) {
      this.loadData()
    } else {
      // 回到页面时同步后端最新菜品（价格/库存可能变化）
      this.loadGoods()
    }
  },

  // 底部页签切换时刷新当前页面内容（由 custom-tab-bar 调用）
  onTabRefresh() {
    if (!app.isLogin()) return
    if (!this.data.categories.length) {
      this.loadData()
    } else {
      this.loadGoods()
    }
  },

  // 加载分类 + 全部菜品 + 规格标记
  loadData() {
    Promise.all([api.getCategories(), api.getDishes({ categoryId: 'all' })]).then(([categories, goods]) => {
      const firstId = categories && categories.length ? categories[0].id : 'all'
      this.setData({
        categories: categories || [],
        goods: (goods || []).map((g) => this.decorateImage(g)),
        currentCategory: firstId
      }, () => {
        this.buildList()
        this.markSpecDishes()
      })
    }).catch(() => {
      this.setData({ categories: [], goods: [] })
    })
  },

  /**
   * 标记哪些菜品带规格（带规格的点击 + 需弹窗选择）
   * 后端列表接口不返回 specs，这里逐个查询详情较重，
   * 优化方案：由后端在列表接口返回 hasSpec 字段；当前先按需懒加载
   */
  markSpecDishes() {
    // 简洁做法：列表接口暂不含 specs，用户点 + 时统一走规格页，
    // 规格页无规格时直接返回并加入购物车。
    this.setData({ dishHasSpec: {} })
  },

  /**
   * 统一处理菜品图片：数据库存相对路径（历史数据可能是完整 URL），
   * hasImage 标记是否为真实图片，imageUrl 为拼好的完整展示地址。
   */
  decorateImage(g) {
    const img = (g && g.image) || ''
    const hasImage = /^https?:\/\//.test(img) || img.startsWith('/')
    return { ...g, hasImage, imageUrl: hasImage ? formatImageUrl(img) : '' }
  },

  // 仅刷新菜品（保留分类与购物车）
  loadGoods() {
    api.getDishes({ categoryId: 'all' }).then((goods) => {
      this.setData({
        goods: (goods || []).map((g) => this.decorateImage(g))
      }, () => this.buildList())
    }).catch(() => {})
  },

  buildList() {
    const { goods, currentCategory, cart } = this.data
    const list = (currentCategory === 'all'
      ? [...goods].sort((a, b) => (b.sales || 0) - (a.sales || 0))
      : goods.filter((g) => g.categoryId === currentCategory))
      // 只展示上架菜品
      .filter((g) => g.status)

    // 购物车 key 为「dishId|specText」，此处按 dishId 汇总每种菜品的总份数
    const countByDish = {}
    Object.keys(cart).forEach((key) => {
      const entry = cart[key]
      if (!entry) return
      countByDish[entry.id] = (countByDish[entry.id] || 0) + entry.count
    })

    this.setData({
      goodsList: list.map((g) => ({
        ...g,
        count: countByDish[g.id] || 0
      }))
    })
  },

  switchCategory(e) {
    this.setData({ currentCategory: e.currentTarget.dataset.id }, () => this.buildList())
  },

  /**
   * 点击 + ：跳转规格选择页
   * 规格页会拉取菜品规格；若无规格，返回时直接加入购物车
   * 注：起购份数(minBuy) 由规格页的步进器保证首次加入时不少于 minBuy
   */
  addItem(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/spec-choose/spec-choose?id=${id}` })
  },

  /**
   * 规格选择页回调：加入带规格的购物车项
   * 无规格菜品返回时 specIds 为空、extraPrice 为 0
   * @param {Object} result { dishId, quantity, specIds, specText, extraPrice, unitPrice }
   */
  onSpecChosen(result) {
    this.addToCart(result.dishId, result.quantity, result.specIds, result.specText, result.extraPrice)
  },

  /**
   * 加入购物车
   * 购物车 key = dishId + '|' + specText（同菜品不同规格视为不同条目）
   */
  addToCart(dishId, quantity, specIds, specText, extraPrice) {
    const dish = this.data.goods.find((g) => g.id === dishId)
    if (!dish) return
    const unitPrice = Number(dish.price) + (Number(extraPrice) || 0)
    const key = dishId + '|' + (specText || '')
    const cart = this.data.cart
    if (cart[key]) {
      cart[key].count += quantity
    } else {
      cart[key] = {
        id: dishId,
        key,
        name: dish.name,
        price: unitPrice,
        image: dish.image,
        hasImage: dish.hasImage,
        imageUrl: dish.imageUrl,
        specIds: specIds || [],
        specText: specText || '',
        count: quantity
      }
    }
    this.updateCart(cart)
  },

  minusItem(e) {
    const key = e.currentTarget.dataset.key
    const cart = { ...this.data.cart }
    if (!cart[key]) return
    cart[key].count -= 1
    if (cart[key].count <= 0) delete cart[key]
    this.updateCart(cart)
  },

  updateCart(cart) {
    let cartCount = 0
    let cartAmount = 0
    const cartList = []
    // 汇总每个菜品的总数量（用于列表角标）
    const countByDish = {}
    Object.keys(cart).forEach((key) => {
      const entry = cart[key]
      cartCount += entry.count
      cartAmount += entry.count * entry.price
      countByDish[entry.id] = (countByDish[entry.id] || 0) + entry.count
      cartList.push(entry)
    })
    const goodsList = this.data.goodsList.map((g) => ({
      ...g,
      count: countByDish[g.id] || 0
    }))
    app.globalData.cart = cart
    this.setData({
      cart,
      cartList,
      cartCount,
      cartAmount: Number(cartAmount.toFixed(2)),
      goodsList
    })
  },

  toggleCart() {
    if (!this.data.cartCount) return
    this.setData({ showCart: !this.data.showCart })
  },

  closeCart() {
    this.setData({ showCart: false })
  },

  clearCart() {
    this.setData({ showCart: false })
    this.updateCart({})
  },

  submitOrder() {
    if (!this.data.cartCount) {
      wx.showToast({ title: '请先选择菜品', icon: 'none' })
      return
    }
    const tableNo = app.globalData.tableNo
    if (!tableNo) {
      wx.showToast({ title: '请先扫码获取桌号', icon: 'none' })
      return
    }
    const { cart, cartAmount, cartCount } = this.data
    // 自动匹配最优优惠券并展示
    api.getBestCoupon(cartAmount).then((coupon) => {
      let content = `共 ${cartCount} 件，合计 ¥${cartAmount}，桌号 ${tableNo}`
      let userCouponId = ''
      if (coupon) {
        userCouponId = coupon.id
        content += `\n已使用：${coupon.label}`
      }
      wx.showModal({
        title: '确认下单',
        content,
        confirmText: '确认下单',
        confirmColor: '#ff6b35',
        success: (res) => {
          if (!res.confirm) return
          // 组装后端需要的 items: [{ dishId, quantity, specIds, specText }]
          const items = Object.keys(cart).map((key) => {
            const entry = cart[key]
            return {
              dishId: entry.id,
              quantity: entry.count,
              specIds: entry.specIds || [],
              specText: entry.specText || ''
            }
          })
          const payload = { tableNo, items, peopleCount: null }
          if (userCouponId) {
            payload.userCouponId = userCouponId
          }
          api.createOrder(payload)
            .then(() => {
              this.setData({ showCart: false })
              this.updateCart({})
              wx.showToast({ title: '下单成功', icon: 'success' })
              setTimeout(() => wx.switchTab({ url: '/pages/order/order' }), 800)
            })
            .catch(() => {})
        }
      })
    }).catch(() => {
      // 优惠券查询失败不影响下单
      wx.showModal({
        title: '确认下单',
        content: `共 ${cartCount} 件，合计 ¥${cartAmount}，桌号 ${tableNo}`,
        confirmText: '确认下单',
        confirmColor: '#ff6b35',
        success: (res) => {
          if (!res.confirm) return
          const items = Object.keys(cart).map((key) => {
            const entry = cart[key]
            return {
              dishId: entry.id,
              quantity: entry.count,
              specIds: entry.specIds || [],
              specText: entry.specText || ''
            }
          })
          api.createOrder({ tableNo, items, peopleCount: null })
            .then(() => {
              this.setData({ showCart: false })
              this.updateCart({})
              wx.showToast({ title: '下单成功', icon: 'success' })
              setTimeout(() => wx.switchTab({ url: '/pages/order/order' }), 800)
            })
            .catch(() => {})
        }
      })
    })
  }
})
