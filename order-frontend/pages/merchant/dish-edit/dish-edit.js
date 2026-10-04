// pages/merchant/dish-edit/dish-edit.js —— 菜品新增/编辑
const api = require('../../../utils/api')
const { baseUrl } = require('../../../utils/config')
const { getToken } = require('../../../utils/request')
const { formatImageUrl, toRelativePath } = require('../../../utils/util')

// 常用 emoji 作为图片兜底（未上传图片时使用）
const EMOJIS = ['🍽️', '🥘', '🍗', '🐟', '🍚', '🥟', '🍜', '🍲', '🥣', '🧃', '🥤', '🍟', '🍖', '🥗', '🍤', '🍱']

Page({
  data: {
    id: '',
    isEdit: false,
    categories: [],
    categoryIndex: 0,
    emojis: EMOJIS,
    // 是否为真实图片 URL（而非 emoji）
    isImageUrl: false,
    // 图片完整展示地址（form.image 存相对路径，此处存拼好的完整地址用于渲染）
    imageUrl: '',
    // 规格分组：[{ groupName, selectType, required, options:[{id,name,extraPrice,isDefault}] }]
    specGroups: [],
    form: {
      categoryId: '',
      name: '',
      description: '',
      image: '🍽️',
      price: '',
      minBuy: '1',
      stock: '999',
      isHot: 0,
      status: 1,
      sort: 0
    },
    submitting: false,
    deleting: false,
    uploading: false,
    // 当前激活的选项（显示左右移动按钮）
    activeGi: -1,
    activeOi: -1,
    // 当前激活的分组（显示左右移动按钮）
    activeGroupGi: -1
  },

  // 选择并上传图片
  chooseImage() {
    if (this.data.uploading) return
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      sizeType: ['compressed'],
      success: (res) => {
        const filePath = res.tempFiles[0].tempFilePath
        this.uploadImage(filePath)
      }
    })
  },

  uploadImage(filePath) {
    this.setData({ uploading: true })
    wx.showLoading({ title: '上传中', mask: true })
    const token = getToken()
    wx.uploadFile({
      url: `${baseUrl}/api/file/upload?bizType=dish`,
      filePath,
      name: 'file',
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: (res) => {
        try {
          const body = JSON.parse(res.data)
          if (body.code === 200 && body.data && body.data.url) {
            // 数据库只存相对路径（不拼 baseUrl），展示时再拼完整地址
            const relativePath = toRelativePath(body.data.url, body.data.relativePath)
            if (!relativePath) {
              wx.showToast({ title: '上传结果异常：未获取到路径', icon: 'none' })
              return
            }
            this.setData({
              'form.image': relativePath,
              imageUrl: formatImageUrl(relativePath),
              isImageUrl: true
            })
            wx.showToast({ title: '上传成功', icon: 'success' })
          } else {
            wx.showToast({ title: body.message || '上传失败', icon: 'none' })
          }
        } catch (e) {
          wx.showToast({ title: '上传失败', icon: 'none' })
        }
      },
      fail: () => wx.showToast({ title: '上传失败', icon: 'none' }),
      complete: () => {
        wx.hideLoading()
        this.setData({ uploading: false })
      }
    })
  },

  // 移除图片，回退到 emoji
  removeImage() {
    this.setData({ 'form.image': '🍽️', imageUrl: '', isImageUrl: false })
  },

  previewImage() {
    if (!this.data.isImageUrl || !this.data.imageUrl) return
    wx.previewImage({ urls: [this.data.imageUrl] })
  },

  onLoad(options) {
    const id = options && options.id ? options.id : ''
    this.setData({ id, isEdit: !!id })
    wx.setNavigationBarTitle({ title: id ? '编辑菜品' : '新增菜品' })
    this.loadCategories(id)
  },

  loadCategories(id) {
    api.getAllCategories().then((categories) => {
      const list = categories || []
      this.setData({ categories: list })
      if (!id && list.length) {
        this.setData({ 'form.categoryId': list[0].id, categoryIndex: 0 })
      }
      if (id) {
        this.loadDish(id)
      }
    }).catch(() => {})
  },

  loadDish(id) {
    api.getDishDetail(id).then((dish) => {
      if (!dish) return
      const index = this.data.categories.findIndex((c) => c.id === dish.categoryId)
      const image = dish.image || '🍽️'
      // 图片路径形如 /dish/xxx.jpg（或以 http 开头的历史数据），其余视为 emoji
      const isImageUrl = /^https?:\/\//.test(image) || image.startsWith('/')
      this.setData({
        categoryIndex: index < 0 ? 0 : index,
        isImageUrl,
        imageUrl: isImageUrl ? formatImageUrl(image) : '',
        form: {
          categoryId: dish.categoryId,
          name: dish.name || '',
          description: dish.desc || '',
          image,
          price: dish.price != null ? String(dish.price) : '',
          minBuy: dish.minBuy != null && dish.minBuy > 0 ? String(dish.minBuy) : '1',
          stock: dish.stock != null ? String(dish.stock) : '999',
          isHot: dish.isHot || 0,
          status: dish.status ? 1 : 0,
          sort: dish.sort || 0
        },
        // 回填已有规格分组
        specGroups: (dish.specs || []).map((group) => ({
          groupName: group.groupName,
          selectType: group.selectType == null ? 1 : group.selectType,
          required: group.required == null ? 0 : group.required,
          options: (group.options || []).map((o) => ({
            id: o.id,
            name: o.name,
            extraPrice: Number(o.extraPrice) || 0,
            isDefault: !!o.isDefault
          }))
        }))
      })
    }).catch(() => {})
  },

  onInput(e) {
    const { field } = e.currentTarget.dataset
    this.setData({ [`form.${field}`]: e.detail.value })
  },

  // 切换分类
  onCategoryChange(e) {
    const index = Number(e.detail.value)
    const category = this.data.categories[index]
    this.setData({
      categoryIndex: index,
      'form.categoryId': category ? category.id : ''
    })
  },

  // 选择 emoji 图标（作为图片兜底）
  chooseEmoji(e) {
    this.setData({
      'form.image': e.currentTarget.dataset.emoji,
      imageUrl: '',
      isImageUrl: false
    })
  },

  // 是否热销
  onHotChange(e) {
    this.setData({ 'form.isHot': e.detail.value ? 1 : 0 })
  },

  // 是否上架
  onStatusChange(e) {
    this.setData({ 'form.status': e.detail.value ? 1 : 0 })
  },

  // ==================== 规格管理 ====================

  // 新增一个规格分组（如「辣度」「加料」）
  addSpecGroup() {
    wx.showModal({
      title: '新增规格分组',
      editable: true,
      placeholderText: '如：辣度 / 加料',
      confirmColor: '#2f80ed',
      success: (res) => {
        if (!res.confirm) return
        const name = (res.content || '').trim()
        if (!name) {
          wx.showToast({ title: '分组名称不能为空', icon: 'none' })
          return
        }
        const specGroups = [...this.data.specGroups]
        if (specGroups.some((g) => g.groupName === name)) {
          wx.showToast({ title: '分组已存在', icon: 'none' })
          return
        }
        specGroups.push({
          groupName: name,
          selectType: 1,
          required: 1,
          options: []
        })
        this.setData({ specGroups })
      }
    })
  },

  // 删除规格分组（带确认提示）
  removeSpecGroup(e) {
    const gi = Number(e.currentTarget.dataset.gi)
    const group = this.data.specGroups[gi]
    if (!group) return
    wx.showModal({
      title: '删除确认',
      content: `确定删除分组「${group.groupName}」及其全部选项吗？`,
      confirmText: '删除',
      confirmColor: '#ff3b30',
      success: (res) => {
        if (!res.confirm) return
        const specGroups = [...this.data.specGroups]
        specGroups.splice(gi, 1)
        // 分组被删除，重置激活态
        this.setData({ specGroups, activeGi: -1, activeOi: -1, activeGroupGi: -1 })
        wx.showToast({ title: '已删除', icon: 'none' })
      }
    })
  },

  // 切换分组选择类型：单选 / 多选
  toggleSelectType(e) {
    const { gi } = e.currentTarget.dataset
    const specGroups = [...this.data.specGroups]
    specGroups[gi].selectType = specGroups[gi].selectType === 1 ? 2 : 1
    this.setData({ specGroups })
  },

  // 切换分组是否必选
  toggleRequired(e) {
    const { gi } = e.currentTarget.dataset
    const specGroups = [...this.data.specGroups]
    specGroups[gi].required = specGroups[gi].required === 1 ? 0 : 1
    this.setData({ specGroups })
  },

  // 新增分组下的选项
  addSpecOption(e) {
    const { gi } = e.currentTarget.dataset
    const specGroups = [...this.data.specGroups]
    const group = specGroups[gi]
    wx.showModal({
      title: `新增选项（${group.groupName}）`,
      editable: true,
      placeholderText: '如：微辣 / 加蛋',
      confirmColor: '#2f80ed',
      success: (res) => {
        if (!res.confirm) return
        const name = (res.content || '').trim()
        if (!name) {
          wx.showToast({ title: '选项名称不能为空', icon: 'none' })
          return
        }
        // 输入加价
        wx.showModal({
          title: `选项「${name}」加价`,
          editable: true,
          placeholderText: '加价金额，不需要加价填 0',
          confirmColor: '#2f80ed',
          success: (r2) => {
            if (!r2.confirm) return
            const extra = Number(r2.content) || 0
            group.options.push({
              id: '',
              name,
              extraPrice: extra,
              isDefault: false
            })
            this.setData({ specGroups })
          }
        })
      }
    })
  },

  // 删除选项（带确认提示）
  removeSpecOption(e) {
    const { gi, oi } = e.currentTarget.dataset
    const option = this.data.specGroups[gi].options[oi]
    wx.showModal({
      title: '删除确认',
      content: `确定删除选项「${option.name}」吗？`,
      confirmText: '删除',
      confirmColor: '#ff3b30',
      success: (res) => {
        if (!res.confirm) return
        const specGroups = this.data.specGroups.map((g) => ({
          ...g,
          options: [...g.options]
        }))
        specGroups[gi].options.splice(oi, 1)
        // 选项被删除，重置激活态，避免移动按钮残留
        this.setData({ specGroups, activeGi: -1, activeOi: -1 })
        wx.showToast({ title: '已删除', icon: 'none' })
      }
    })
  },

  // ==================== 选项排序（左右移动按钮） ====================

  /**
   * 点击选项：
   *  - 第一次点击：仅激活该项（左右显示移动按钮），不改变默认状态
   *  - 再次点击已激活项：切换其「默认」状态（单选互斥 / 多选可多选）
   */
  toggleOption(e) {
    const gi = Number(e.currentTarget.dataset.gi)
    const oi = Number(e.currentTarget.dataset.oi)
    const isActive = this.data.activeGi === gi && this.data.activeOi === oi

    // 未激活：仅激活，不切换默认
    if (!isActive) {
      this.setData({ activeGi: gi, activeOi: oi, activeGroupGi: -1 })
      return
    }

    // 已激活：再次点击切换默认状态
    const specGroups = this.data.specGroups.map((g) => ({
      ...g,
      options: [...g.options]
    }))
    const group = specGroups[gi]
    group.options.forEach((o, idx) => {
      if (group.selectType === 1) {
        // 单选：互斥
        o.isDefault = idx === oi ? !o.isDefault : false
      } else {
        // 多选：可多默认
        if (idx === oi) o.isDefault = !o.isDefault
      }
    })
    this.setData({ specGroups })
  },

  // 左移 / 右移选项（dir: -1 左移，1 右移）
  moveOption(e) {
    const gi = Number(e.currentTarget.dataset.gi)
    const from = Number(e.currentTarget.dataset.oi)
    const target = from + Number(e.currentTarget.dataset.dir)
    const specGroups = this.data.specGroups.map((g) => ({
      ...g,
      options: [...g.options]
    }))
    const options = specGroups[gi].options
    if (target < 0 || target >= options.length) return
    const [moved] = options.splice(from, 1)
    options.splice(target, 0, moved)
    // 激活项跟随移动后的新位置
    this.setData({ specGroups, activeGi: gi, activeOi: target })
  },

  // 取消激活（点击空白处收起所有移动按钮）
  clearActive() {
    if (this.data.activeGi === -1 && this.data.activeGroupGi === -1) return
    this.setData({ activeGi: -1, activeOi: -1, activeGroupGi: -1 })
  },

  // 空操作：用于阻止子元素点击冒泡到根节点（避免误收起）
  noop() {},

  // ==================== 分组排序（左右移动按钮） ====================

  // 点击分组名：激活该分组
  toggleGroup(e) {
    const gi = Number(e.currentTarget.dataset.gi)
    const activeGroupGi = this.data.activeGroupGi === gi ? -1 : gi
    this.setData({ activeGroupGi, activeGi: -1, activeOi: -1 })
  },

  // 左移 / 右移分组（dir: -1 左移，1 右移）
  moveGroup(e) {
    const { gi, dir } = e.currentTarget.dataset
    const from = Number(gi)
    const target = from + Number(dir)
    const specGroups = [...this.data.specGroups]
    if (target < 0 || target >= specGroups.length) return
    const [moved] = specGroups.splice(from, 1)
    specGroups.splice(target, 0, moved)
    // 激活分组跟随移动到新位置
    this.setData({ specGroups, activeGroupGi: target })
  },

  // 保存规格（新增菜品时需先保存菜品拿到 id）
  saveSpecs(dishId) {
    const specs = []
    this.data.specGroups.forEach((group) => {
      group.options.forEach((opt) => {
        specs.push({
          groupName: group.groupName,
          name: opt.name,
          extraPrice: Number(opt.extraPrice) || 0,
          isDefault: opt.isDefault ? 1 : 0,
          selectType: group.selectType,
          required: group.required,
          status: 1
        })
      })
    })
    return api.saveDishSpecs(dishId, specs)
  },

  submit() {
    if (this.data.submitting) return
    const { form, isEdit, id } = this.data

    if (!form.categoryId) {
      wx.showToast({ title: '请选择分类', icon: 'none' })
      return
    }
    // 菜品图片必填：需为真实上传的图片（emoji 兜底不算）
    if (!this.data.isImageUrl || !form.image || !form.image.startsWith('/')) {
      wx.showToast({ title: '请上传菜品图片', icon: 'none' })
      return
    }
    if (!form.name || !form.name.trim()) {
      wx.showToast({ title: '请输入菜品名称', icon: 'none' })
      return
    }
    if (!form.description || !form.description.trim()) {
      wx.showToast({ title: '请输入菜品描述', icon: 'none' })
      return
    }
    const price = Number(form.price)
    if (!price || price <= 0) {
      wx.showToast({ title: '请输入有效价格', icon: 'none' })
      return
    }
    const minBuy = form.minBuy === '' ? 1 : Number(form.minBuy)
    if (isNaN(minBuy) || minBuy < 1 || !Number.isInteger(minBuy)) {
      wx.showToast({ title: '起购份数需为不小于1的整数', icon: 'none' })
      return
    }
    const stock = form.stock === '' ? 999 : Number(form.stock)
    if (isNaN(stock) || stock < 0) {
      wx.showToast({ title: '请输入有效库存', icon: 'none' })
      return
    }

    const payload = {
      categoryId: form.categoryId,
      name: form.name.trim(),
      description: form.description.trim(),
      image: form.image,
      price,
      minBuy,
      stock,
      isHot: form.isHot,
      status: form.status,
      sort: Number(form.sort) || 0
    }

    this.setData({ submitting: true })
    const request = isEdit
      ? api.updateDish({ ...payload, id })
      : api.addDish(payload)

    request.then((result) => {
      // 新增接口返回新菜品ID；编辑沿用当前 id
      const dishId = isEdit ? id : result
      // 菜品保存成功后再保存规格（规格需 dishId）
      return this.saveSpecs(dishId).then(() => {
        wx.showToast({ title: isEdit ? '保存成功' : '新增成功', icon: 'success' })
        setTimeout(() => wx.navigateBack(), 700)
      })
    }).catch(() => {
      this.setData({ submitting: false })
    })
  },

  // 删除（仅编辑模式）
  remove() {
    if (this.data.deleting || this.data.submitting) return
    wx.showModal({
      title: '删除确认',
      content: '确定删除该菜品吗？',
      confirmText: '删除',
      confirmColor: '#ff3b30',
      success: (res) => {
        if (!res.confirm) return
        this.setData({ deleting: true })
        api.deleteDish(this.data.id).then(() => {
          wx.showToast({ title: '已删除', icon: 'none' })
          setTimeout(() => wx.navigateBack(), 700)
        }).catch(() => {}).then(() => {
          this.setData({ deleting: false })
        })
      }
    })
  }
})
