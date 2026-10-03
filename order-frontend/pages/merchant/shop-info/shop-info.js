// pages/merchant/shop-info/shop-info.js —— 店家端店铺信息编辑
const api = require('../../../utils/api')
const { formatImageUrl, formatImageUrls, toRelativePath } = require('../../../utils/util')

const MAX_IMAGES = 9

Page({
  data: {
    saving: false,
    uploading: false,
    // 是否存在未保存的改动：用于离页提醒与「未保存」提示
    hasUnsaved: false,
    // 图片完整展示地址（用于 image 渲染）
    logoUrl: '',
    imageUrls: [],
    // 图片是否被用户改动过：未改动的字段不提交，避免空值覆盖数据库已有图片
    logoDirty: false,
    imagesDirty: false,
    form: {
      name: '',
      realName: '',
      logo: '',
      images: [],
      slogan: '',
      notice: '',
      address: '',
      phone: '',
      latitude: null,
      longitude: null
    },
    // 地图选点标记
    mapMarkers: []
  },

  onLoad() {
    this.loadShop()
  },

  onUnload() {
    // 关闭系统级离页拦截，避免已保存后仍弹窗
    if (this._alertEnabled && wx.disableAlertBeforeUnload) {
      wx.disableAlertBeforeUnload()
      this._alertEnabled = false
    }
  },

  /**
   * 离页守护：有未保存改动时，阻止直接返回并二次确认。
   * 尤其是「已上传图片但没点保存」的情况，此时图片文件已在服务器上，
   * 但路径尚未入库，必须提醒用户保存。
   */
  enableLeaveGuard() {
    if (wx.enableAlertBeforeUnload && !this._alertEnabled) {
      wx.enableAlertBeforeUnload({
        message: this.data.logoDirty || this.data.imagesDirty
          ? '图片已上传但尚未保存，离开将不会生效，确定离开吗？'
          : '店铺信息尚未保存，确定离开吗？'
      })
      this._alertEnabled = true
    }
  },

  // 标记有未保存改动（图片上传/删除、文本输入都会触发）
  markDirty() {
    if (!this.data.hasUnsaved) {
      this.setData({ hasUnsaved: true })
    }
    this.enableLeaveGuard()
  },

  loadShop() {
    // 店家端接口：从登录 token 解析店铺，保证读到的是本店真实数据
    api.getMerchantShopInfo().then((shop) => {
      if (!shop) return
      // 后端可能返回完整 URL 或相对路径，统一归一化为相对路径存入 form，
      // 展示时再用 formatImageUrl 拼完整地址，保证「表单 -> 提交」始终是相对路径
      const logo = toRelativePath(shop.logo)
      const images = this.parseImages(shop.images).map((p) => toRelativePath(p))

      // 关键点：只更新「图片」相关字段，且用合并方式写回 form。
      // 早先直接整体替换 form，若用户已先上传完图片、本请求才返回，
      // 会把刚上传的路径覆盖成空值，导致最终保存时写入空字符串。
      const latitude = shop.latitude != null ? Number(shop.latitude) : null
      const longitude = shop.longitude != null ? Number(shop.longitude) : null

      this.setData({
        form: {
          ...this.data.form,
          name: shop.name || '',
          realName: shop.realName || '',
          logo: this.data.logoDirty ? this.data.form.logo : logo,
          images: this.data.imagesDirty ? this.data.form.images : images,
          slogan: shop.slogan || '',
          notice: shop.notice || '',
          address: shop.address || '',
          phone: shop.phone || '',
          latitude,
          longitude
        },
        logoUrl: formatImageUrl(this.data.logoDirty ? this.data.form.logo : logo),
        imageUrls: formatImageUrls(this.data.imagesDirty ? this.data.form.images : images),
        mapMarkers: latitude && longitude
          ? [{ id: 1, latitude, longitude, title: shop.address || '店铺位置', width: 24, height: 24 }]
          : []
      })
    }).catch(() => {})
  },

  // 解析图片字段：支持逗号分隔 / JSON 数组，返回相对路径数组
  parseImages(images) {
    if (!images) return []
    let str = String(images).trim()
    if (str.startsWith('[') && str.endsWith(']')) {
      str = str.slice(1, -1)
    }
    return str.split(',')
      .map((s) => s.trim().replace(/^["']|["']$/g, ''))
      .filter(Boolean)
  },

  onInput(e) {
    const { field } = e.currentTarget.dataset
    this.setData({ [`form.${field}`]: e.detail.value })
    this.markDirty()
  },

  // ==================== 店铺地址（站内地图定位选点） ====================

  /**
   * 微信地图选点 / 实时定位：
   * 先获取当前实时定位作为地图中心，再拉起 wx.chooseLocation 选点，
   * 选点结果写回地址文本与经纬度，并生成地图预览标记。
   * 说明：chooseLocation 是微信原生页面，自带搜索、附近地点与「取消/确定」，
   * 其左上角返回键由微信渲染，无法去除。
   */
  onChooseLocation() {
    // 防重入：选点页正在打开/已经打开时，忽略重复触发，
    // 避免 wx.getLocation 的异步回调把选点页二次拉起，造成「点了 < 页面没关闭」的假象
    if (this._picking) return
    this._picking = true

    const that = this
    const done = () => { that._picking = false }

    const openMapChooser = (latitude, longitude) => {
      const chooseParams = {}
      if (latitude && longitude) {
        chooseParams.latitude = latitude
        chooseParams.longitude = longitude
      }
      wx.chooseLocation({
        ...chooseParams,
        success: (res) => {
          // res: { name, address, latitude, longitude }
          const address = res.address || res.name || ''
          that.setData({
            'form.latitude': res.latitude,
            'form.longitude': res.longitude,
            'form.address': address,
            mapMarkers: [{
              id: 1,
              latitude: res.latitude,
              longitude: res.longitude,
              title: res.name || '店铺位置',
              width: 24,
              height: 24
            }]
          })
          that.markDirty()
          wx.showToast({ title: '已成功定位选点', icon: 'success' })
        },
        fail: (err) => {
          const msg = (err && err.errMsg) || ''
          // 用户点 < 、取消 或系统返回：属于正常关闭，不做任何提示与处理
          if (msg.indexOf('cancel') > -1 || msg.indexOf('fail cancel') > -1) {
            return
          }
          // 仅位置权限被拒绝时才引导去设置
          if (msg.indexOf('auth') > -1 || msg.indexOf('deny') > -1) {
            wx.showModal({
              title: '提示',
              content: '需要获取您的地理位置权限以在地图上选点，请前往设置开启',
              confirmText: '去开启',
              success: (modalRes) => {
                if (modalRes.confirm) wx.openSetting()
              }
            })
          }
        },
        complete: done
      })
    }

    wx.getLocation({
      type: 'gcj02',
      success: (locRes) => {
        openMapChooser(locRes.latitude, locRes.longitude)
      },
      fail: (err) => {
        const msg = (err && err.errMsg) || ''
        // 权限相关才引导设置；其余情况（如定位超时）直接打开地图选点
        if (msg.indexOf('auth') > -1 || msg.indexOf('deny') > -1) {
          this._picking = false
          wx.showModal({
            title: '提示',
            content: '需要获取您的地理位置权限以在地图上选点，请前往设置开启',
            confirmText: '去开启',
            success: (modalRes) => {
              if (modalRes.confirm) wx.openSetting()
            }
          })
          return
        }
        // 获取当前位置失败时直接打开地图选点
        openMapChooser()
      }
    })
  },

  // ==================== 店铺 Logo（单张） ====================

  /**
   * 点击 Logo：与店铺图片交互保持一致
   * - 已上传：预览大图
   * - 未上传：唤起选择（此时方框显示 ＋）
   */
  onLogoTap() {
    if (this.data.logoUrl) {
      this.previewLogo()
    } else {
      this.chooseLogo()
    }
  },

  chooseLogo() {
    if (this.data.uploading) return
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      sizeType: ['compressed'],
      success: (res) => {
        const filePath = res.tempFiles[0].tempFilePath
        this.setData({ uploading: true })
        wx.showLoading({ title: '上传中', mask: true })
        api.uploadFile(filePath, 'shop').then((data) => {
          // 统一存相对路径入库，展示时再拼完整地址
          const relativePath = toRelativePath(data.url, data.relativePath)
          if (!relativePath) {
            wx.showToast({ title: '上传结果异常：未获取到路径', icon: 'none' })
            return
          }
          this.setData({
            'form.logo': relativePath,
            logoUrl: formatImageUrl(relativePath),
            logoDirty: true
          })
          this.markDirty()
          wx.showToast({ title: '上传成功，记得点保存', icon: 'none' })
        }).catch((err) => {
          wx.showToast({ title: err.message || '上传失败', icon: 'none' })
        }).then(() => {
          wx.hideLoading()
          this.setData({ uploading: false })
        })
      }
    })
  },

  previewLogo() {
    if (!this.data.logoUrl) return
    wx.previewImage({ urls: [this.data.logoUrl] })
  },

  removeLogo() {
    const logo = this.data.form.logo
    if (!logo) return
    wx.showModal({
      title: '删除确认',
      content: '确定删除店铺 Logo 吗？删除后服务器上的图片也会被移除。',
      confirmColor: '#ff3b30',
      success: (res) => {
        if (!res.confirm) return
        // 先更新本地表单，再真实删除服务器文件；删除失败不影响本地已移除的结果
        this.setData({ 'form.logo': '', logoUrl: '', logoDirty: true })
        this.markDirty()
        api.deleteFile(logo).catch((err) => {
          console.warn('[shop-info] 删除 Logo 文件失败', logo, err)
        })
      }
    })
  },

  // ==================== 店铺图片（多图上传） ====================

  chooseImages() {
    if (this.data.uploading) return
    const remain = MAX_IMAGES - this.data.form.images.length
    if (remain <= 0) {
      wx.showToast({ title: `最多上传 ${MAX_IMAGES} 张`, icon: 'none' })
      return
    }
    wx.chooseMedia({
      count: remain,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      sizeType: ['compressed'],
      success: (res) => {
        const paths = (res.tempFiles || []).map((f) => f.tempFilePath)
        if (!paths.length) return
        this.setData({ uploading: true })
        wx.showLoading({ title: `上传中 0/${paths.length}`, mask: true })
        // 多文件逐个上传；后端原图保存不做压缩，仅校验单张不超过 10MB
        api.uploadFiles(paths, 'shop').then((list) => {
          const added = (list || [])
            .map((item) => toRelativePath(item.url, item.relativePath))
            .filter(Boolean)
          if (!added.length) {
            wx.showToast({ title: '上传结果异常：未获取到路径', icon: 'none' })
            return
          }
          const images = this.data.form.images.concat(added)
          this.setData({
            'form.images': images,
            imageUrls: formatImageUrls(images),
            imagesDirty: true
          })
          this.markDirty()
          wx.showToast({ title: `成功上传 ${added.length} 张，记得点保存`, icon: 'none' })
        }).catch((err) => {
          wx.showToast({ title: err.message || '上传失败', icon: 'none' })
        }).then(() => {
          wx.hideLoading()
          this.setData({ uploading: false })
        })
      }
    })
  },

  previewImage(e) {
    const { index } = e.currentTarget.dataset
    wx.previewImage({
      current: this.data.imageUrls[index],
      urls: this.data.imageUrls
    })
  },

  removeImage(e) {
    const { index } = e.currentTarget.dataset
    const removed = this.data.form.images[index]
    wx.showModal({
      title: '删除确认',
      content: '确定删除这张店铺图片吗？删除后服务器上的图片也会被移除。',
      confirmColor: '#ff3b30',
      success: (res) => {
        if (!res.confirm) return
        const images = [...this.data.form.images]
        images.splice(index, 1)
        this.setData({
          'form.images': images,
          imageUrls: formatImageUrls(images),
          imagesDirty: true
        })
        this.markDirty()
        // 真实删除服务器文件；删除失败不影响本地已移除的结果
        if (removed) {
          api.deleteFile(removed).catch((err) => {
            console.warn('[shop-info] 删除店铺图片文件失败', removed, err)
          })
        }
      }
    })
  },

  save() {
    if (this.data.saving) return
    const { form } = this.data
    if (!form.name || !form.name.trim()) {
      wx.showToast({ title: '请输入店铺名称', icon: 'none' })
      return
    }
    if (!form.realName || !form.realName.trim()) {
      wx.showToast({ title: '请输入真实姓名', icon: 'none' })
      return
    }
    if (!form.phone || !form.phone.trim()) {
      wx.showToast({ title: '请输入联系电话', icon: 'none' })
      return
    }
    if (!/^\d{6,20}$/.test(form.phone.trim())) {
      wx.showToast({ title: '请输入有效的联系电话', icon: 'none' })
      return
    }
    if (!form.address || !form.address.trim()) {
      wx.showToast({ title: '请输入店铺地址', icon: 'none' })
      return
    }
    if (!form.logo) {
      wx.showToast({ title: '请上传店铺 Logo', icon: 'none' })
      return
    }

    const list = Array.isArray(form.images) ? form.images : this.parseImages(form.images)
    if (!list.length) {
      wx.showToast({ title: '请至少上传一张店铺图片', icon: 'none' })
      return
    }

    const payload = {
      name: form.name.trim(),
      realName: form.realName.trim(),
      slogan: form.slogan,
      notice: form.notice,
      address: form.address.trim(),
      phone: form.phone.trim(),
      latitude: form.latitude != null ? Number(form.latitude) : null,
      longitude: form.longitude != null ? Number(form.longitude) : null
    }

    // 只有用户改动过图片时才提交这两个字段。
    // 未改动时字段为 undefined（序列化后会被丢弃），后端收到 null 即「不修改」，
    // 从根本上避免空值把数据库中已有的图片路径覆盖成空字符串。
    if (this.data.logoDirty) {
      payload.logo = form.logo || ''
    }
    if (this.data.imagesDirty) {
      payload.images = list.join(',')
    }

    this.setData({ saving: true })
    api.updateShop(payload).then(() => {
      // 保存成功：关闭离页拦截，避免返回时再弹确认
      if (this._alertEnabled && wx.disableAlertBeforeUnload) {
        wx.disableAlertBeforeUnload()
        this._alertEnabled = false
      }
      this.setData({ hasUnsaved: false, logoDirty: false, imagesDirty: false })
      wx.showToast({ title: '保存成功', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 700)
    }).catch(() => {
      this.setData({ saving: false })
    })
  },

  // 取消编辑：有未保存改动时二次确认后返回
  cancel() {
    if (this.data.saving) return
    const quit = () => {
      if (this._alertEnabled && wx.disableAlertBeforeUnload) {
        wx.disableAlertBeforeUnload()
        this._alertEnabled = false
      }
      wx.navigateBack()
    }
    if (!this.data.hasUnsaved) {
      quit()
      return
    }
    wx.showModal({
      title: '放弃编辑？',
      content: '当前修改尚未保存，确定要离开吗？',
      confirmText: '放弃',
      confirmColor: '#ff3b30',
      success: (res) => {
        if (res.confirm) quit()
      }
    })
  }
})
