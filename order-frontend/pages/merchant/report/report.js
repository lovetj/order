// pages/merchant/report/report.js —— 店家端经营报表
const app = getApp()
const api = require('../../../utils/api')

// 快捷日期范围
const RANGES = [
  { key: 'today', label: '今日' },
  { key: 'week', label: '近7天' },
  { key: 'month', label: '近30天' }
]

function formatDate(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

Page({
  data: {
    ranges: RANGES,
    activeRange: 'week',
    startDate: '',
    endDate: '',
    summary: {},
    dailyList: [],
    topDishes: [],
    categoryList: [],
    loading: false
  },

  onLoad() {
    this.setRange('week')
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
    this.loadReport()
  },

  // 选择快捷范围
  switchRange(e) {
    const { key } = e.currentTarget.dataset
    this.setRange(key)
    this.loadReport()
  },

  setRange(key) {
    const today = new Date()
    let start = new Date()
    if (key === 'today') {
      start = today
    } else if (key === 'week') {
      start = new Date(today.getTime() - 6 * 24 * 3600 * 1000)
    } else if (key === 'month') {
      start = new Date(today.getTime() - 29 * 24 * 3600 * 1000)
    }
    this.setData({
      activeRange: key,
      startDate: formatDate(start),
      endDate: formatDate(today)
    })
  },

  // 手动选择日期
  onStartChange(e) {
    this.setData({ startDate: e.detail.value, activeRange: '' })
    this.loadReport()
  },

  onEndChange(e) {
    this.setData({ endDate: e.detail.value, activeRange: '' })
    this.loadReport()
  },

  loadReport() {
    const { startDate, endDate } = this.data
    if (startDate > endDate) {
      wx.showToast({ title: '开始日期不能晚于结束日期', icon: 'none' })
      return
    }
    this.setData({ loading: true })
    api.getReport({ startDate, endDate }).then((data) => {
      if (!data) return
      this.setData({
        summary: data.summary || {},
        dailyList: data.dailyList || [],
        topDishes: data.topDishes || [],
        categoryList: data.categoryList || []
      })
    }).catch(() => {
      this.setData({ summary: {}, dailyList: [], topDishes: [], categoryList: [] })
    }).then(() => this.setData({ loading: false }))
  },

  // 分类占比进度条宽度（按金额）
  getBarWidth(amount, list) {
    const max = Math.max.apply(null, list.map((c) => Number(c.amount) || 0))
    if (!max) return 0
    return Math.round((Number(amount) / max) * 100)
  },

  // 导出经营报表 CSV
  exportReport() {
    const { startDate, endDate } = this.data
    const url = api.exportReportUrl(startDate, endDate)
    this.downloadAndOpen(url, `经营报表_${startDate}_${endDate}.csv`)
  },

  // 导出订单明细 CSV
  exportOrders() {
    const { startDate, endDate } = this.data
    const url = api.exportOrdersUrl(startDate, endDate, null)
    this.downloadAndOpen(url, `订单明细_${startDate}_${endDate}.csv`)
  },

  /**
   * 下载文件并打开
   * 小程序无直接下载能力，用 wx.downloadFile 拉取后通过 openDocument 打开
   */
  downloadAndOpen(url, fileName) {
    const token = require('../../../utils/request').getToken()
    wx.showLoading({ title: '导出中', mask: true })
    wx.downloadFile({
      url,
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: (res) => {
        if (res.statusCode !== 200) {
          wx.showToast({ title: '导出失败', icon: 'none' })
          return
        }
        wx.openDocument({
          filePath: res.tempFilePath,
          fileType: 'csv',
          showMenu: true,
          success: () => {},
          fail: () => {
            // 部分环境不支持 csv 预览，改为复制路径提示
            wx.showModal({
              title: '导出成功',
              content: `文件已下载：${fileName}\n可点击右上角「…」转发或保存。`,
              showCancel: false,
              confirmText: '知道了',
              confirmColor: '#2f80ed'
            })
          }
        })
      },
      fail: () => wx.showToast({ title: '网络异常，导出失败', icon: 'none' }),
      complete: () => wx.hideLoading()
    })
  }
})
