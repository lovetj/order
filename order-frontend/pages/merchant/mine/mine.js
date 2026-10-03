// pages/merchant/mine/mine.js —— 店家端我的
const app = getApp()
const api = require('../../../utils/api')
const { formatImageUrl, toRelativePath } = require('../../../utils/util')

Page({
  data: {
    shop: {
      name: '扫码点餐',
      owner: '',
      // 头像完整展示地址，空串表示未设置（展示灰色默认头像）
      avatarUrl: '',
      phone: '',
      status: '营业中'
    },
    uploadingAvatar: false,
    menus: [
      { icon: '🏬', name: '店铺信息', key: 'info' },
      { icon: '🏷️', name: '分类管理', key: 'category' },
      { icon: '🎫', name: '优惠券管理', key: 'coupon' },
      { icon: '🎁', name: '积分商品管理', key: 'pointsGoods' },
      { icon: '🪑', name: '桌位与二维码管理', key: 'table' },
      { icon: '📊', name: '经营报表', key: 'report' },
      { icon: '🕙', name: '营业时间设置', key: 'time' },
      { icon: '💬', name: '联系平台客服', key: 'service' }
    ]
  },

  onShow() {
    if (this.getTabBar && this.getTabBar()) {
      this.getTabBar().setData({ selected: 3 })
    }
    if (app.globalData.role !== 'merchant') {
      wx.reLaunch({ url: '/pages/role/role' })
      return
    }
    if (!app.isLogin()) {
      wx.reLaunch({ url: '/pages/login/login?role=merchant' })
      return
    }
    this.loadShop()
    this.loadProfile()
  },

  loadShop() {
    // 确保店铺ID与登录账号一致（切换账号后刷新）
    const admin = app.globalData.admin || {}
    if (admin.shopId) {
      app.setShopId(admin.shopId)
    }
    api.getMerchantShopInfo().then((shop) => {
      if (!shop) return
      this.setData({
        shop: {
          ...this.data.shop,
          name: shop.name || '扫码点餐',
          // 展示店家真实姓名，数据源为 shop.real_name
          owner: shop.realName || '',
          phone: shop.phone || admin.phone || '',
          status: shop.businessStatus === 1 ? '营业中' : '休息中'
        }
      })
    }).catch(() => {})
  },

  // 加载店家账号头像（后端返回完整地址，未设置则为空）
  loadProfile() {
    api.getMerchantProfile().then((profile) => {
      const avatarUrl = formatImageUrl((profile && profile.avatar) || '')
      this.setData({ 'shop.avatarUrl': avatarUrl })
    }).catch(() => {})
  },

  // ==================== 头像上传 ====================

  /** 点击头像：未设置则选择上传，已设置则预览 */
  onAvatarTap() {
    if (this.data.uploadingAvatar) return
    if (this.data.shop.avatarUrl) {
      this.showAvatarActions()
    } else {
      this.chooseAvatar()
    }
  },

  /** 已设置头像时：提供预览 / 更换 / 删除 */
  showAvatarActions() {
    wx.showActionSheet({
      itemList: ['预览头像', '更换头像', '删除头像'],
      success: (res) => {
        if (res.tapIndex === 0) {
          wx.previewImage({ urls: [this.data.shop.avatarUrl] })
        } else if (res.tapIndex === 1) {
          this.chooseAvatar()
        } else if (res.tapIndex === 2) {
          this.removeAvatar()
        }
      }
    })
  },

  /** 选择本地图片并上传，成功后写入后端并刷新展示 */
  chooseAvatar() {
    if (this.data.uploadingAvatar) return
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      sizeType: ['compressed'],
      success: (res) => {
        const filePath = res.tempFiles[0].tempFilePath
        if (!filePath) return
        this.setData({ uploadingAvatar: true })
        wx.showLoading({ title: '上传中', mask: true })
        api.uploadFile(filePath, 'avatar').then((data) => {
          // 统一用相对路径入库，展示时再拼完整地址
          const relativePath = toRelativePath(data.url, data.relativePath)
          if (!relativePath) {
            wx.showToast({ title: '上传结果异常：未获取到路径', icon: 'none' })
            return
          }
          return api.updateMerchantProfile({ avatar: relativePath }).then((profile) => {
            const avatarUrl = formatImageUrl((profile && profile.avatar) || relativePath)
            this.setData({ 'shop.avatarUrl': avatarUrl })
            // 同步到全局，便于其它页面复用
            if (app.globalData.admin) {
              app.globalData.admin.avatar = avatarUrl
            }
            wx.showToast({ title: '头像已更新', icon: 'success' })
          })
        }).catch((err) => {
          wx.showToast({ title: (err && err.message) || '头像上传失败', icon: 'none' })
        }).then(() => {
          wx.hideLoading()
          this.setData({ uploadingAvatar: false })
        })
      }
    })
  },

  /** 删除头像：清空后端头像字段，展示灰色默认头像 */
  removeAvatar() {
    wx.showModal({
      title: '删除头像',
      content: '确定删除当前头像吗？',
      confirmColor: '#ff3b30',
      success: (res) => {
        if (!res.confirm) return
        api.updateMerchantProfile({ avatar: '' }).then(() => {
          this.setData({ 'shop.avatarUrl': '' })
          if (app.globalData.admin) {
            app.globalData.admin.avatar = ''
          }
          wx.showToast({ title: '已删除头像', icon: 'success' })
        }).catch((err) => {
          wx.showToast({ title: (err && err.message) || '删除失败', icon: 'none' })
        })
      }
    })
  },

  handleMenu(e) {
    const key = e.currentTarget.dataset.key
    if (key === 'table') {
      wx.navigateTo({ url: '/pages/merchant/tables/tables' })
      return
    }
    if (key === 'report') {
      wx.navigateTo({ url: '/pages/merchant/report/report' })
      return
    }
    if (key === 'category') {
      wx.navigateTo({ url: '/pages/merchant/category/category' })
      return
    }
    if (key === 'coupon') {
      wx.navigateTo({ url: '/pages/merchant/coupon/coupon' })
      return
    }
    if (key === 'pointsGoods') {
      wx.navigateTo({ url: '/pages/merchant/points-goods/points-goods' })
      return
    }
    if (key === 'info') {
      wx.navigateTo({ url: '/pages/merchant/shop-info/shop-info' })
      return
    }
    wx.showToast({ title: '功能开发中', icon: 'none' })
  },

  // 营业状态切换（走接口）
  toggleShop() {
    api.toggleBusiness().then(() => {
      const next = this.data.shop.status === '营业中' ? '休息中' : '营业中'
      this.setData({ 'shop.status': next })
      wx.showToast({ title: `已切换为${next}`, icon: 'none' })
    }).catch(() => {})
  },

  switchRole() {
    wx.showModal({
      title: '切换身份',
      content: '切换到「我是顾客」需要重新微信授权登录',
      confirmText: '去登录',
      confirmColor: '#ff6b35',
      success: (res) => {
        if (!res.confirm) return
        app.logout()
        wx.reLaunch({ url: '/pages/login/login?role=customer' })
      }
    })
  },

  backToRoleSelect() {
    app.logout()
    wx.reLaunch({ url: '/pages/role/role' })
  }
})
