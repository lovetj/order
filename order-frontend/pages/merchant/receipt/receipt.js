// pages/merchant/receipt/receipt.js —— 小票打印预览
const app = getApp()
const api = require('../../../utils/api')

Page({
  data: {
    orderId: '',
    receipt: {},
    loading: false
  },

  onLoad(options) {
    if (app.globalData.role !== 'merchant' || !app.isLogin()) {
      wx.reLaunch({ url: '/pages/role/role' })
      return
    }
    const orderId = options && options.id ? options.id : ''
    this.setData({ orderId })
    if (orderId) {
      this.loadReceipt()
    }
  },

  loadReceipt() {
    this.setData({ loading: true })
    api.getReceipt(this.data.orderId).then((data) => {
      this.setData({ receipt: data || {} })
    }).catch(() => {}).then(() => this.setData({ loading: false }))
  },

  /**
   * 打印小票
   * 说明：小程序无法直接驱动热敏打印机，常见方案：
   *  1) 云打印机（如易联云/飞鹅）：后端调用其开放接口下发打印任务
   *  2) 蓝牙打印机：通过 wx.openBluetoothAdapter + writeBLECharacteristicValue 透传 ESC/POS 指令
   * 此处演示蓝牙方案：获取 Base64 指令后按字节下发
   */
  print() {
    wx.showModal({
      title: '打印方式',
      content: '选择打印方式：\n· 云打印：由后端对接云打印机下发任务\n· 蓝牙打印：连接蓝牙热敏打印机透传指令',
      confirmText: '云打印',
      cancelText: '蓝牙打印',
      success: (res) => {
        if (res.confirm) {
          this.cloudPrint()
        } else if (res.cancel) {
          this.bluetoothPrint()
        }
      }
    })
  },

  // 云打印：由后端下发（此处仅请求指令，实际由后端对接打印机）
  cloudPrint() {
    api.getReceiptEscPos(this.data.orderId).then((data) => {
      if (!data || !data.command) {
        wx.showToast({ title: '获取打印指令失败', icon: 'none' })
        return
      }
      wx.showModal({
        title: '云打印指令已生成',
        content: `指令长度：${data.command.length} 字符（Base64，${data.charset} 编码）\n\n实际项目请在后端对接云打印机（易联云/飞鹅）开放接口下发该指令。`,
        showCancel: false,
        confirmText: '知道了',
        confirmColor: '#2f80ed'
      })
    }).catch(() => {})
  },

  // 蓝牙打印：透传 ESC/POS 指令
  bluetoothPrint() {
    api.getReceiptEscPos(this.data.orderId).then((data) => {
      if (!data || !data.command) {
        wx.showToast({ title: '获取打印指令失败', icon: 'none' })
        return
      }
      wx.showModal({
        title: '蓝牙打印',
        content: '请在真机上连接蓝牙热敏打印机后，通过 wx.openBluetoothAdapter 与 writeBLECharacteristicValue 下发该 Base64 指令（解码为字节后发送）。',
        showCancel: false,
        confirmText: '知道了',
        confirmColor: '#2f80ed'
      })
    }).catch(() => {})
  },

  copyText() {
    const text = this.data.receipt.text || ''
    wx.setClipboardData({
      data: text,
      success: () => wx.showToast({ title: '小票内容已复制', icon: 'none' })
    })
  }
})
