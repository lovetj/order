// pages/merchant/goods/goods.js —— 店家端商品管理
const app = getApp()
const api = require('../../../utils/api')
const { formatImageUrl } = require('../../../utils/util')

Page({
  data: {
    categories: [],
    activeCategory: 'all',
    goods: [],
    list: [],
    keyword: '',
    onShelfCount: 0,
    loading: false,
    // 上下架操作的全局加载蒙版
    shelfLoading: false,
    loadingText: '处理中…',
    // 自绘细滚动条：左侧分类栏
    sideBarTop: 0,
    sideBarH: 0,
    // 自绘细滚动条：右侧商品栏
    mainBarTop: 0,
    mainBarH: 0,
    // 右侧列表滚动位置（切换分类/搜索时重置到顶部）
    mainScrollTop: 0,
    // 分类统计弹框
    statVisible: false,
    statTitle: '',
    statList: [],
    statTotal: 0
  },

  onShow() {
    if (this.getTabBar && this.getTabBar()) {
      this.getTabBar().setData({ selected: 1 })
    }
    if (app.globalData.role !== 'merchant') {
      wx.reLaunch({ url: '/pages/role/role' })
      return
    }
    if (!app.isLogin()) {
      wx.reLaunch({ url: '/pages/login/login?role=merchant' })
      return
    }
    // 每次展示重置定位标记：编辑返回时由 editGoods 重新设置
    this._focusDone = false
    this.loadData()
  },

  // 底部页签切换时刷新当前页面内容（由 custom-tab-bar 调用）
  onTabRefresh() {
    if (!app.isLogin()) return
    this._focusDone = false
    this.loadData()
  },

  // 加载分类 + 全部商品（含下架）
  loadData() {
    this.setData({ loading: true })
    Promise.all([
      api.getAllCategories(),
      api.getAdminDishes({ categoryId: 'all' })
    ]).then(([categories, goods]) => {
      const list = goods || []
      // 从编辑页返回：若商品分类变更，先切到其新分类，保证能被定位到
      const focusId = this._focusId
      let activeCategory = this.data.activeCategory
      if (focusId) {
        const target = list.find((g) => String(g.id) === String(focusId))
        // 目标被删除则不定位
        if (!target) {
          this._focusId = null
        } else if (activeCategory !== 'all' && target.categoryId !== activeCategory) {
          activeCategory = target.categoryId
        }
      }
      this.setData({
        categories: categories || [],
        goods: list,
        activeCategory
      })
      this._restoreTimer && clearTimeout(this._restoreTimer)
      this._restoreTimer = setTimeout(() => { this._restoreTimer = null }, 5000)
      this.filter(activeCategory)
    }).catch(() => {
      this.setData({ categories: [], goods: [], list: [] })
    }).then(() => {
      this.setData({ loading: false })
      // 数据就绪后测量视口高度，初始化细滚动条
      wx.nextTick(() => this.updateBars())
    })
  },

  switchCategory(e) {
    const { id } = e.currentTarget.dataset
    this.setData({ activeCategory: id })
    this.filter(id)
    // 切换分类后内容高度变化，重置右侧细滚动条并回到顶部
    wx.nextTick(() => this.updateBars())
  },

  // ==================== 自绘细滚动条 ====================

  // 记录当前滚动位置（scroll-view bindscroll 会不断触发，detail 含 scrollTop/scrollHeight）
  onSideScroll(e) {
    this._sideScrollTop = e.detail.scrollTop
    this._sideScrollHeight = e.detail.scrollHeight
    this.refreshBar('side')
  },

  onMainScroll(e) {
    this._mainScrollTop = e.detail.scrollTop
    this._mainScrollHeight = e.detail.scrollHeight
    this.refreshBar('main')
  },

  /**
   * 根据内容高度 / 视口高度更新滚动条长度与位置。
   * 内容高度来自 bindscroll 的 detail.scrollHeight（触发后即缓存），
   * 视口高度来自 boundingClientRect。
   */
  refreshBar(which) {
    const isSide = which === 'side'
    const scrollTop = (isSide ? this._sideScrollTop : this._mainScrollTop) || 0
    const scrollHeight = (isSide ? this._sideScrollHeight : this._mainScrollHeight) || 0
    const viewHeight = (isSide ? this._sideViewHeight : this._mainViewHeight) || 0
    if (viewHeight <= 0 || scrollHeight <= viewHeight) {
      // 内容未超出视口：隐藏滚动条
      this.setData(isSide ? { sideBarH: 0 } : { mainBarH: 0 })
      return
    }
    const barH = Math.max(60, (viewHeight / scrollHeight) * viewHeight)
    const maxTop = viewHeight - barH
    const ratio = scrollTop / (scrollHeight - viewHeight)
    const barTop = Math.min(maxTop, Math.max(0, ratio * maxTop))
    if (isSide) {
      this.setData({ sideBarH: barH, sideBarTop: barTop })
    } else {
      this.setData({ mainBarH: barH, mainBarTop: barTop })
    }
  },

  // 测量两栏视口高度与内容高度（数据/分类变化后调用），并刷新滚动条
  updateBars() {
    const query = wx.createSelectorQuery().in(this)
    query.select('.side').boundingClientRect()
    query.select('.main').boundingClientRect()
    query.select('.side .side-item').boundingClientRect()
    query.select('.goods-item').boundingClientRect()
    query.exec((res) => {
      if (!res || !res[0] || !res[1]) return
      this._sideViewHeight = res[0].height
      this._mainViewHeight = res[1].height

      // 用「单个子项高度 × 数量」估算内容高度（拿不到精确 scrollHeight 时的兜底），
      // 待首次 scroll 事件带回真实 scrollHeight 后会被修正。
      const sideItemH = res[2] ? res[2].height : 0
      const sideCount = this.data.categories.length + 1 // 含「全部」
      const mainItemH = res[3] ? res[3].height : 0
      const mainCount = this.data.list.length
      this._mainItemHeight = mainItemH
      this._sideScrollHeight = this._sideScrollHeight || sideItemH * sideCount
      this._mainScrollHeight = this._mainScrollHeight || mainItemH * mainCount

      this.refreshBar('side')
      this.refreshBar('main')

      // 从编辑页返回：数据渲染完成后定位到目标商品
      if (this._focusId && !this._focusDone) {
        this._focusDone = true
        const fid = this._focusId
        this._focusId = null
        wx.nextTick(() => this.scrollToGoods(fid))
      }
    })
  },

  // 搜索输入：仅按名称过滤，实时刷新
  onSearchInput(e) {
    this.setData({ keyword: e.detail.value || '' })
    this.filter(this.data.activeCategory)
  },

  // 清空搜索
  clearSearch() {
    this.setData({ keyword: '' })
    this.filter(this.data.activeCategory)
  },

  filter(categoryId) {
    const keyword = (this.data.keyword || '').trim().toLowerCase()
    let list = categoryId === 'all'
      ? this.data.goods
      : this.data.goods.filter((g) => g.categoryId === categoryId)
    // 按名称关键字过滤
    if (keyword) {
      list = list.filter((g) => (g.name || '').toLowerCase().indexOf(keyword) > -1)
    }
    const mapped = list
      // 数据库存相对路径（或以 http 开头为历史数据），拼成完整地址用于渲染；
      // hasImage 标记是否为真实图片（用于 WXML 区分 image / emoji 渲染）
      .map((g) => {
        const img = g.image || ''
        const hasImage = /^https?:\/\//.test(img) || img.startsWith('/')
        return { ...g, hasImage, imageUrl: hasImage ? formatImageUrl(img) : '' }
      })
    this.setData({
      list: mapped,
      onShelfCount: this.data.goods.filter((g) => g.status).length
    })
    // 编辑返回定位场景：保留滚动位置，待 updateBars 完成后精确定位
    if (this._focusId) {
      wx.nextTick(() => this.updateBars())
      return
    }
    // 列表内容变化后，重置右侧滚动条（回到顶部并重新计算长度）
    this._mainScrollTop = 0
    this._mainScrollHeight = 0
    // scroll-top 需从非 0 值变化才会触发，先置 0 再复位
    this.setData({ mainScrollTop: this.data.mainScrollTop === 0 ? 0.1 : 0 })
    wx.nextTick(() => this.updateBars())
  },

  // ==================== 分类统计弹框 ====================

  // 空操作：阻止弹框内部点击冒泡到遮罩
  noop() {},

  /**
   * 点击「商品总数 / 在售中」：
   * 按分类统计对应口径下的商品数量，弹框展示
   * @param {string} type all=全部商品 / onShelf=仅上架商品
   */
  showStatDialog(e) {
    const type = e.currentTarget.dataset.type
    const all = this.data.goods || []
    // 口径过滤：在售中只统计上架商品
    const scoped = type === 'onShelf' ? all.filter((g) => g.status) : all
    // 按分类聚合数量（未匹配到分类的归入「未分类」）
    const categories = this.data.categories || []
    const countMap = {}
    scoped.forEach((g) => {
      const key = g.categoryId || '__none__'
      countMap[key] = (countMap[key] || 0) + 1
    })
    const statList = categories
      .map((c) => ({ id: c.id, name: c.name, count: countMap[c.id] || 0 }))
      // 保留有商品的分类，数量为 0 的也展示更直观；此处全部展示
      .filter((c) => c.count > 0)
    // 未分类兜底
    if (countMap.__none__) {
      statList.push({ id: '__none__', name: '未分类', count: countMap.__none__ })
    }
    this.setData({
      statVisible: true,
      statTitle: type === 'onShelf' ? '在售中各分类数量' : '商品总数各分类数量',
      statList,
      statTotal: scoped.length
    })
  },

  closeStatDialog() {
    this.setData({ statVisible: false })
  },

  // 上下架：status 前端是布尔值，接口传 1/0
  toggleShelf(e) {
    const { id } = e.currentTarget.dataset
    const target = this.data.goods.find((g) => g.id === id)
    if (!target) return
    // 已有操作进行中，忽略重复点击
    if (this.data.shelfLoading) return

    const nextStatus = target.status ? 0 : 1
    this.setData({
      shelfLoading: true,
      loadingText: nextStatus === 1 ? '正在上架…' : '正在下架…'
    })

    api.updateDishStatus(id, nextStatus).then(() => {
      const goods = this.data.goods.map((g) =>
        g.id === id ? { ...g, status: !!nextStatus } : g
      )
      this.setData({ goods })
      // 操作后保持在原商品位置
      this.focusGoods(id)
      this.filter(this.data.activeCategory)
      wx.showToast({ title: nextStatus === 1 ? '已上架' : '已下架', icon: 'none' })
    }).catch(() => {}).then(() => {
      this.setData({ shelfLoading: false })
    })
  },

  // 标记需要定位的商品，使列表刷新后回到该商品位置
  focusGoods(id) {
    this._focusId = id
    this._focusDone = false
  },

  // 改价
  editPrice(e) {
    const { id } = e.currentTarget.dataset
    const target = this.data.goods.find((g) => g.id === id)
    if (!target) return
    wx.showModal({
      title: '修改价格',
      editable: true,
      placeholderText: `当前 ¥${target.price}`,
      confirmColor: '#2f80ed',
      success: (res) => {
        if (!res.confirm) return
        const price = Number(res.content)
        if (!price || price <= 0) {
          wx.showToast({ title: '请输入有效价格', icon: 'none' })
          return
        }
        // 展示改价加载动画（复用全局蒙版）
        this.setData({ shelfLoading: true, loadingText: '正在改价…' })
        api.updateDishPrice(id, price).then(() => {
          const goods = this.data.goods.map((g) => (g.id === id ? { ...g, price } : g))
          this.setData({ goods })
          // 操作后保持在原商品位置
          this.focusGoods(id)
          this.filter(this.data.activeCategory)
          wx.showToast({ title: '价格已更新', icon: 'none' })
        }).catch(() => {}).then(() => {
          this.setData({ shelfLoading: false })
        })
      }
    })
  },

  // 新增商品：跳转表单页
  addGoods() {
    wx.navigateTo({ url: '/pages/merchant/dish-edit/dish-edit' })
  },

  // 编辑商品：跳转表单页（带 id），并记录，返回列表时定位到该商品
  editGoods(e) {
    const { id } = e.currentTarget.dataset
    this.focusGoods(id)
    wx.navigateTo({ url: `/pages/merchant/dish-edit/dish-edit?id=${id}` })
  },

  // 返回列表后，滚动定位到指定商品（尽量置中显示）
  scrollToGoods(id) {
    const query = wx.createSelectorQuery().in(this)
    query.select('.main').boundingClientRect()
    query.select(`#goods-${id}`).boundingClientRect()
    query.exec((res) => {
      const view = res[0]
      if (!view) return
      const viewHeight = view.height
      const listH = this._mainItemHeight || 0

      if (res[1]) {
        // 精确命中：目标节点已渲染，按其在视口中的相对位置计算滚动距离
        const delta = res[1].top - view.top - (viewHeight - res[1].height) / 2
        this.applyScroll((this._mainScrollTop || 0) + delta)
        return
      }
      // 兜底：节点未渲染（目标在可视区外），按等距估算其偏移
      const index = this.data.list.findIndex((g) => String(g.id) === String(id))
      if (index < 0 || !listH) return
      this.applyScroll(index * listH - viewHeight / 2 + listH / 2)
    })
  },

  // 统一设置滚动位置（scroll-top 需数值变化才会触发滚动）
  applyScroll(scrollTop) {
    const next = Math.max(0, Math.round(scrollTop))
    const current = this._mainScrollTop || 0
    if (Math.abs(next - current) < 2) {
      this.setData({ mainScrollTop: next === 0 ? 0.1 : 0 })
      return
    }
    this._mainScrollTop = next
    this.setData({ mainScrollTop: next })
  },

  // 删除商品
  deleteGoods(e) {
    const { id } = e.currentTarget.dataset
    wx.showModal({
      title: '删除确认',
      content: '确定删除该商品吗？',
      confirmText: '删除',
      confirmColor: '#ff3b30',
      success: (res) => {
        if (!res.confirm) return
        api.deleteDish(id).then(() => {
          wx.showToast({ title: '已删除', icon: 'none' })
          this.loadData()
        }).catch(() => {})
      }
    })
  },

  // 长按商品弹出操作菜单
  onGoodsLongPress(e) {
    const { id } = e.currentTarget.dataset
    wx.showActionSheet({
      itemList: ['编辑商品', '修改价格', '删除商品'],
      success: (res) => {
        if (res.tapIndex === 0) {
          this.editGoods({ currentTarget: { dataset: { id } } })
        } else if (res.tapIndex === 1) {
          this.editPrice({ currentTarget: { dataset: { id } } })
        } else if (res.tapIndex === 2) {
          this.deleteGoods({ currentTarget: { dataset: { id } } })
        }
      }
    })
  }
})
