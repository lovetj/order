// pages/merchant/dish-edit/dish-edit.js —— 菜品新增/编辑
const api = require('../../../utils/api')
const { baseUrl } = require('../../../utils/config')
const { getToken } = require('../../../utils/request')

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
    // 规格分组：[{ groupName, selectType, required, options:[{id,name,extraPrice,isDefault}] }]
    specGroups: [],
    form: {
      categoryId: '',
      name: '',
      description: '',
      image: '🍽️',
      price: '',
      stock: '999',
      isHot: 0,
      status: 1,
      sort: 0
    },
    submitting: false,
    uploading: false
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
            this.setData({
              'form.image': body.data.url,
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
    this.setData({ 'form.image': '🍽️', isImageUrl: false })
  },

  previewImage() {
    if (!this.data.isImageUrl || !this.data.form.image) return
    wx.previewImage({ urls: [this.data.form.image] })
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
      this.setData({
        categoryIndex: index < 0 ? 0 : index,
        // 图片为 http 开头说明是真实图片 URL
        isImageUrl: /^https?:\/\//.test(image),
        form: {
          categoryId: dish.categoryId,
          name: dish.name || '',
          description: dish.desc || '',
          image,
          price: dish.price != null ? String(dish.price) : '',
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

  // 删除规格分组
  removeSpecGroup(e) {
    const { gi } = e.currentTarget.dataset
    const specGroups = [...this.data.specGroups]
    specGroups.splice(gi, 1)
    this.setData({ specGroups })
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

  // 删除选项
  removeSpecOption(e) {
    const { gi, oi } = e.currentTarget.dataset
    const specGroups = [...this.data.specGroups]
    specGroups[gi].options.splice(oi, 1)
    this.setData({ specGroups })
  },

  // 设置选项为默认选中
  setDefaultOption(e) {
    const { gi, oi } = e.currentTarget.dataset
    const specGroups = [...this.data.specGroups]
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
    if (!form.name || !form.name.trim()) {
      wx.showToast({ title: '请输入菜品名称', icon: 'none' })
      return
    }
    const price = Number(form.price)
    if (!price || price <= 0) {
      wx.showToast({ title: '请输入有效价格', icon: 'none' })
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
      description: form.description,
      image: form.image,
      price,
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
    wx.showModal({
      title: '删除确认',
      content: '确定删除该菜品吗？',
      confirmText: '删除',
      confirmColor: '#ff3b30',
      success: (res) => {
        if (!res.confirm) return
        api.deleteDish(this.data.id).then(() => {
          wx.showToast({ title: '已删除', icon: 'none' })
          setTimeout(() => wx.navigateBack(), 700)
        }).catch(() => {})
      }
    })
  }
})
