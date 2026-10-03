// pages/index/index.js —— 顾客端首页
const app = getApp()
const api = require('../../utils/api')

Page({
  data: {
    shop: {
      name: '扫码点餐',
      slogan: '现点现做 · 用心出餐',
      score: 5.0,
      monthSales: 0
    },
    tableNo: '',
    banners: [
      { id: 1, emoji: '🎁', title: '新客立减 15 元', desc: '扫码点餐专享' },
      { id: 2, emoji: '💰', title: '满 100 减 20', desc: '堂食全场通用' },
      { id: 3, emoji: '🔥', title: '招牌菜品 8 折', desc: '每日限量供应' }
    ],
    recommend: [],
    notices: []
  },

  onShow() {
    if (this.getTabBar && this.getTabBar()) {
      this.getTabBar().setData({ selected: 0 })
    }
    if (!app.globalData.role) {
      wx.reLaunch({ url: '/pages/role/role' })
      return
    }
    // 顾客端未登录（如退出后返回）时引导重新微信登录
    if (!app.isLogin()) {
      wx.reLaunch({ url: '/pages/login/login?role=customer' })
      return
    }
    this.setData({ tableNo: app.globalData.tableNo || '未获取' })
    this.loadShop()
    this.loadRecommend()
  },

  // 加载门店信息
  loadShop() {
    api.getShopInfo().then((shop) => {
      if (!shop) return
      // 公告：后端用 | 分隔多条
      const notices = shop.notice
        ? String(shop.notice).split('|').filter((n) => n && n.trim())
        : []
      this.setData({
        shop: {
          name: shop.name || '扫码点餐',
          slogan: shop.slogan || '',
          score: shop.score || 5.0,
          monthSales: shop.monthSales || 0
        },
        notices
      })
    }).catch(() => {})
  },

  // 加载店长推荐
  loadRecommend() {
    api.getRecommend(4).then((list) => {
      const recommend = (list || []).map((g) => ({
        ...g,
        imageUrl: /^https?:\/\//.test(g.image || '')
      }))
      this.setData({ recommend })
    }).catch(() => {})
  },

  scanTable() {
    wx.scanCode({
      success: (res) => {
        const parsed = this.parseQrContent(res.result || '')
        if (parsed.shopId) {
          app.setShopId(parsed.shopId)
        }
        if (parsed.tableNo) {
          app.setTableNo(parsed.tableNo)
        }
        this.setData({ tableNo: parsed.tableNo || app.globalData.tableNo || '未获取' })
        wx.showToast({
          title: parsed.tableNo ? `已识别桌号 ${parsed.tableNo}` : '未识别到桌号',
          icon: 'none'
        })
        // 切换店铺后重新加载本店菜单
        this.loadShop()
        this.loadRecommend()
      },
      fail: () => wx.showToast({ title: '扫码已取消', icon: 'none' })
    })
  },

  /**
   * 解析二维码内容，支持：
   *   1) 小程序路径 pages/role/role?shopId=1&tableNo=A01
   *   2) 普通 URL https://xxx?shopId=1&tableNo=A01
   *   3) 纯桌号 A01（兼容旧码）
   */
  parseQrContent(content) {
    const text = String(content || '').trim()
    const result = { shopId: '', tableNo: '' }
    if (!text) return result

    const queryIndex = text.indexOf('?')
    if (queryIndex >= 0) {
      const query = text.substring(queryIndex + 1)
      query.split('&').forEach((pair) => {
        const [k, v] = pair.split('=')
        if (!k || v === undefined) return
        const key = decodeURIComponent(k).trim()
        const value = decodeURIComponent(v).trim()
        if (key === 'shopId') result.shopId = value
        if (key === 'tableNo') result.tableNo = value
      })
      return result
    }

    // 无 query：按纯桌号处理
    const plain = text.replace(/[^\w-]/g, '')
    if (plain && plain.length <= 10) {
      result.tableNo = plain
    }
    return result
  },

  goMenu() {
    wx.switchTab({ url: '/pages/menu/menu' })
  },

  goOrders() {
    wx.switchTab({ url: '/pages/order/order' })
  }
})
