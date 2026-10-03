// custom-tab-bar/index.js
// 自定义 tabBar：根据角色（顾客 / 店家）渲染不同的导航项

const CUSTOMER_TABS = [
  { pagePath: '/pages/index/index', text: '首页', icon: '🏠' },
  { pagePath: '/pages/menu/menu', text: '点餐', icon: '🍽️' },
  { pagePath: '/pages/order/order', text: '订单', icon: '🧾' },
  { pagePath: '/pages/mine/mine', text: '我的', icon: '👤' }
]

const MERCHANT_TABS = [
  { pagePath: '/pages/merchant/dashboard/dashboard', text: '管理后台', icon: '📊' },
  { pagePath: '/pages/merchant/goods/goods', text: '商品', icon: '📦' },
  { pagePath: '/pages/merchant/orders/orders', text: '订单', icon: '🧾' },
  { pagePath: '/pages/merchant/mine/mine', text: '我的', icon: '🏪' }
]

Component({
  data: {
    selected: 0,
    list: []
  },

  lifetimes: {
    attached() {
      this.refresh()
    }
  },

  pageLifetimes: {
    show() {
      this.refresh()
    }
  },

  methods: {
    refresh() {
      const app = getApp()
      const role = app.globalData.role || wx.getStorageSync('role') || 'customer'
      const list = role === 'merchant' ? MERCHANT_TABS : CUSTOMER_TABS
      const pages = getCurrentPages()
      const current = pages.length ? `/${pages[pages.length - 1].route}` : ''
      const index = list.findIndex((item) => item.pagePath === current)
      this.setData({ list, selected: index < 0 ? 0 : index })
    },

    switchTab(e) {
      const { index, path } = e.currentTarget.dataset
      if (index === this.data.selected) return
      wx.switchTab({ url: path })
    }
  }
})
