// pages/login/login.js —— 登录页
// 顾客端：微信授权登录（wx.login 取 code + 头像昵称授权）
// 店家端：账号密码登录（不提供注册）
const app = getApp()
const api = require('../../utils/api')
const { setToken } = require('../../utils/request')

Page({
  data: {
    // customer | merchant
    role: 'customer',
    tableNo: '',
    logging: false,
    // 顾客端：微信授权信息
    avatarUrl: '',
    nickName: '',
    // 店家端：账号密码
    username: '',
    password: '',
    // 密码是否明文显示（默认密文）
    showPassword: false
  },

  onLoad(options) {
    const role = (options && options.role) === 'merchant' ? 'merchant' : 'customer'
    this.setData({
      role,
      tableNo: app.globalData.tableNo || ''
    })
  },

  goBack() {
    wx.reLaunch({ url: '/pages/role/role' })
  },

  onChooseAvatar(e) {
    this.setData({ avatarUrl: e.detail.avatarUrl || '' })
  },

  onNicknameInput(e) {
    this.setData({ nickName: (e.detail.value || '').trim() })
  },

  onUsernameInput(e) {
    this.setData({ username: (e.detail.value || '').trim() })
  },

  onPasswordInput(e) {
    this.setData({ password: (e.detail.value || '').trim() })
  },

  // 切换密码明文/密文显示
  togglePassword() {
    this.setData({ showPassword: !this.data.showPassword })
  },

  // ==================== 顾客端：微信授权登录 ====================
  customerLogin() {
    if (this.data.logging) return
    this.setData({ logging: true })

    wx.login({
      success: (res) => {
        if (!res.code) {
          this.setData({ logging: false })
          wx.showToast({ title: '微信登录失败：未获取到 code', icon: 'none' })
          return
        }
        api.wxLogin({
          code: res.code,
          nickname: this.data.nickName || '微信用户',
          avatar: '',
          role: 'customer'
        }).then((data) => {
          // 先写入 token，后续头像上传需要鉴权
          setToken(data.token)
          const user = data.user || {}

          // 微信 chooseAvatar 返回的是本地临时路径，需上传到后端持久化
          return this.uploadAvatarIfNeeded().then((avatarUrl) => {
            if (avatarUrl) {
              user.avatar = avatarUrl
              // 同步更新后端用户资料
              api.updateProfile({ nickname: user.nickname, avatar: avatarUrl }).catch(() => {})
            }
            app.globalData.userInfo = {
              nickName: user.nickname || this.data.nickName || '微信用户',
              avatar: avatarUrl || user.avatar || '🙋',
              memberLevel: user.memberLevel || '普通会员'
            }
            app.globalData.admin = null
            app.setRole('customer')
            this.setData({ logging: false })
            wx.showToast({ title: '登录成功', icon: 'success', duration: 700 })
            setTimeout(() => {
              wx.switchTab({ url: '/pages/index/index' })
              // 未识别店铺时提示扫码，避免点餐提交被后端拒绝
              if (!app.globalData.shopId) {
                setTimeout(() => {
                  wx.showToast({ title: '请扫描桌位二维码以识别店铺', icon: 'none', duration: 2000 })
                }, 800)
              }
            }, 700)
          })
        }).catch((err) => {
          this.setData({ logging: false })
          wx.showToast({ title: (err && err.message) || '登录失败', icon: 'none' })
        })
      },
      fail: () => {
        this.setData({ logging: false })
        wx.showToast({ title: '微信登录失败', icon: 'none' })
      }
    })
  },

  /**
   * 上传微信授权头像到后端（本地临时路径无法长期使用）
   * @returns {Promise<string>} 后端返回的持久化头像地址，失败返回空串
   */
  uploadAvatarIfNeeded() {
    const avatarUrl = this.data.avatarUrl
    if (!avatarUrl) {
      return Promise.resolve('')
    }
    const isTemp = avatarUrl.startsWith('wxfile://')
      || avatarUrl.startsWith('http://tmp')
      || avatarUrl.startsWith('tmp/')
    if (!isTemp) {
      return Promise.resolve(avatarUrl)
    }
    return api.uploadFile(avatarUrl, 'avatar')
      .then((data) => (data && (data.url || data.relativePath)) || '')
      .catch(() => '')
  },

  // ==================== 店家端：账号密码登录 ====================
  merchantLogin() {
    if (this.data.logging) return
    const { username, password } = this.data
    if (!username) {
      wx.showToast({ title: '请输入账号', icon: 'none' })
      return
    }
    if (!password) {
      wx.showToast({ title: '请输入密码', icon: 'none' })
      return
    }

    this.setData({ logging: true })
    api.adminLogin({ username, password }).then((data) => {
      setToken(data.token)
      const admin = data.admin || {}
      app.globalData.admin = admin
      // 店家登录后绑定其所属店铺，所有管理接口据此隔离
      app.setShopId(admin.shopId || '')
      app.setRole('merchant')
      this.setData({ logging: false })
      wx.showToast({ title: '登录成功', icon: 'success', duration: 700 })
      setTimeout(() => wx.switchTab({ url: '/pages/merchant/dashboard/dashboard' }), 700)
    }).catch((err) => {
      this.setData({ logging: false })
      wx.showToast({ title: (err && err.message) || '账号或密码错误', icon: 'none' })
    })
  }
})
