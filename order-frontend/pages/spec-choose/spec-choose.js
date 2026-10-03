// pages/spec-choose/spec-choose.js —— 菜品规格/口味选择
const api = require('../../utils/api')

Page({
  data: {
    dish: {},
    specGroups: [],
    // 已选项：{ groupName: [optionId, ...] }
    selected: {},
    extraPrice: 0,
    totalPrice: 0,
    quantity: 1
  },

  onLoad(options) {
    const dishId = options && options.id ? options.id : ''
    if (!dishId) {
      wx.showToast({ title: '参数错误', icon: 'none' })
      setTimeout(() => wx.navigateBack(), 800)
      return
    }
    this.setData({ dishId })
    this.loadDetail(dishId)
  },

  loadDetail(dishId) {
    api.getDishDetail(dishId).then((dish) => {
      if (!dish) return
      const specGroups = (dish.specs || []).map((group) => ({
        ...group,
        options: (group.options || []).map((o) => ({
          ...o,
          selected: !!o.isDefault
        }))
      }))
      this.setData({
        dish: {
          ...dish,
          imageUrl: /^https?:\/\//.test(dish.image || '')
        },
        specGroups
      })
      this.calcPrice()
    }).catch(() => {})
  },

  // 选择规格：WXML 不支持 indexOf，直接用 selected 布尔标记
  toggleOption(e) {
    const { gi, oi, type } = e.currentTarget.dataset
    const specGroups = this.data.specGroups
    const group = specGroups[gi]
    if (!group) return
    const option = group.options[oi]
    if (!option) return

    if (type === 2) {
      // 多选：切换
      option.selected = !option.selected
    } else {
      // 单选：先清除本组，再选中当前
      group.options.forEach((o) => { o.selected = false })
      option.selected = true
    }
    this.setData({ specGroups: [...specGroups] })
    this.calcPrice()
  },

  // 计算加价与总价
  calcPrice() {
    const { specGroups, dish, quantity } = this.data
    let extra = 0
    specGroups.forEach((group) => {
      group.options.forEach((opt) => {
        if (opt.selected) {
          extra += Number(opt.extraPrice) || 0
        }
      })
    })
    const unit = Number(dish.price || 0) + extra
    this.setData({
      extraPrice: extra,
      totalPrice: (unit * quantity).toFixed(2)
    })
  },

  // 生成规格文案，如「微辣, 加蛋」
  buildSpecText() {
    const names = []
    this.data.specGroups.forEach((group) => {
      group.options.forEach((opt) => {
        if (opt.selected) {
          names.push(opt.name)
        }
      })
    })
    return names.join(', ')
  },

  changeQty(e) {
    const delta = Number(e.currentTarget.dataset.delta)
    let quantity = this.data.quantity + delta
    if (quantity < 1) quantity = 1
    this.setData({ quantity })
    this.calcPrice()
  },

  // 无规格菜品：直接加入购物车（保留默认数量 1）
  confirmNoSpec() {
    const { dish } = this.data
    const result = {
      dishId: dish.id,
      quantity: this.data.quantity,
      specIds: [],
      specText: '',
      extraPrice: 0,
      unitPrice: Number(dish.price || 0)
    }
    const pages = getCurrentPages()
    const prevPage = pages[pages.length - 2]
    if (prevPage && prevPage.onSpecChosen) {
      prevPage.onSpecChosen(result)
    }
    wx.navigateBack()
  },

  confirm() {
    const { specGroups, dish, quantity } = this.data

    if (specGroups.length === 0) {
      this.confirmNoSpec()
      return
    }

    // 校验必选组
    for (const group of specGroups) {
      if (group.required === 1) {
        const hasChosen = group.options.some((o) => o.selected)
        if (!hasChosen) {
          wx.showToast({ title: `请选择${group.groupName}`, icon: 'none' })
          return
        }
      }
    }

    // 收集所有选中选项ID
    const specIds = []
    specGroups.forEach((group) => {
      group.options.forEach((opt) => {
        if (opt.selected) {
          specIds.push(opt.id)
        }
      })
    })

    const result = {
      dishId: dish.id,
      quantity,
      specIds,
      specText: this.buildSpecText(),
      extraPrice: this.data.extraPrice,
      unitPrice: Number(dish.price || 0) + this.data.extraPrice
    }

    // 通过事件通道返回给上一页
    const pages = getCurrentPages()
    const prevPage = pages[pages.length - 2]
    if (prevPage && prevPage.onSpecChosen) {
      prevPage.onSpecChosen(result)
    }
    wx.navigateBack()
  }
})
