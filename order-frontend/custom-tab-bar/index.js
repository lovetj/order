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
      // dataset 取出的 index 为字符串，需转数值后再与 selected 比较
      // 点击当前页签：直接刷新当前页面内容（不重复跳转）
      if (Number(index) === Number(this.data.selected)) {
        this.refreshCurrentPage()
        return
      }
      // 跨页签切换：由目标页 onShow 自行刷新，避免重复请求
      wx.switchTab({ url: path })
    },

    // 刷新当前页面内容
    refreshCurrentPage() {
      const pages = getCurrentPages()
      const current = pages.length ? pages[pages.length - 1] : null
      if (!current) return
      // 优先调用页面统一刷新入口，其次兜底 onShow
      if (typeof current.onTabRefresh === 'function') {
        current.onTabRefresh()
      } else if (typeof current.onShow === 'function') {
        current.onShow()
      }
    }
  }
})
