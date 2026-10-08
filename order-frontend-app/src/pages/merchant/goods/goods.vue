<template>
  <view class="page layout-page">
    <bottom-nav />
    
    <!-- 顶部统计与搜索筛选（固定在顶部） -->
    <view class="header">
      <!-- 统计卡片 -->
      <view class="summary-card">
        <view class="summary-item" @click="showStatDialog('all')">
          <view class="summary-value">{{totalCount}}</view>
          <view class="summary-label">
            <text>商品总数</text>
            <text class="stat-arrow-icon">›</text>
          </view>
        </view>
        <view class="summary-divider"></view>
        <view class="summary-item" @click="showStatDialog('onShelf')">
          <view class="summary-value active-color">{{onShelfCount}}</view>
          <view class="summary-label">
            <text>在售中</text>
            <text class="stat-arrow-icon">›</text>
          </view>
        </view>
        <view class="summary-divider"></view>
        <view class="summary-item">
          <view class="summary-value off-color">{{offShelfCount}}</view>
          <view class="summary-label">
            <text>已下架</text>
          </view>
        </view>
      </view>

      <!-- 搜索栏 -->
      <view class="search-container">
        <view class="search-bar">
          <text class="search-icon">🔍</text>
          <input
            class="search-input"
            :value="keyword"
            placeholder="搜索商品名称、描述"
            confirm-type="search"
            @input="onSearchInput"
          />
          <view v-if="keyword" class="search-clear-btn" @click="clearSearch">
            <text class="search-clear-text">×</text>
          </view>
        </view>
      </view>

      <!-- 筛选栏：状态 + 标签 -->
      <view class="filter-bar">
        <!-- 状态筛选 -->
        <view class="filter-group">
          <view
            class="filter-pill"
            :class="{ active: statusFilter === 0 }"
            @click="switchStatus(0)"
          >全部</view>
          <view
            class="filter-pill on"
            :class="{ active: statusFilter === 1 }"
            @click="switchStatus(1)"
          >
            <text class="pill-dot"></text>在售
          </view>
          <view
            class="filter-pill off"
            :class="{ active: statusFilter === 2 }"
            @click="switchStatus(2)"
          >
            <text class="pill-dot"></text>下架
          </view>
        </view>

        <!-- 热销筛选 -->
        <view class="filter-group">
          <view
            class="filter-pill hot-pill"
            :class="{ active: hotFilter === 1 }"
            @click="switchHot(hotFilter === 1 ? 0 : 1)"
          >
            <text class="hot-fire">🔥</text>热销推荐
          </view>
        </view>
      </view>
    </view>

    <!-- 主体双栏内容区域：左侧分类 / 右侧商品列表 -->
    <view class="body">
      <!-- 左侧分类侧边栏 -->
      <view class="side-wrap">
        <scroll-view
          class="side"
          scroll-y
          enhanced
          :show-scrollbar="false"
          @scroll="onSideScroll"
        >
          <view
            class="side-item"
            :class="{ active: activeCategory === 'all' }"
            @click="switchCategory('all')"
          >
            <view class="side-indicator"></view>
            <text class="side-name">全部商品</text>
          </view>
          <view
            v-for="item in categories"
            :key="item.id"
            class="side-item"
            :class="{ active: activeCategory === item.id }"
            @click="switchCategory(item.id)"
          >
            <view class="side-indicator"></view>
            <text class="side-name">{{item.name}}</text>
          </view>
          <view class="side-bottom-space"></view>
        </scroll-view>
        <!-- 自绘滚动条 -->
        <view class="thin-bar side-bar" :style="{ height: sideBarH + 'rpx', top: sideBarTop + 'rpx' }"></view>
      </view>

      <!-- 右侧商品列表 -->
      <view class="main-wrap">
        <scroll-view
          class="main"
          scroll-y
          enhanced
          :show-scrollbar="false"
          :scroll-top="mainScrollTop"
          @scroll="onMainScroll"
        >
          <!-- 商品卡片列表 -->
          <view
            class="goods-item"
            v-for="item in list"
            :key="item.id"
            :id="'goods-' + item.id"
            @longpress="onGoodsLongPress(item.id)"
          >
            <!-- 商品缩略图 -->
            <view class="thumb-box">
              <image v-if="item.hasImage" class="thumb-img" :src="item.imageUrl" mode="aspectFill"></image>
              <view v-else class="thumb-placeholder">
                <text class="thumb-icon">{{item.image || '🍽️'}}</text>
              </view>
              <!-- 热销标签浮标 -->
              <view v-if="item.isHot" class="badge-hot">
                <text class="badge-hot-icon">🔥</text>
                <text class="badge-hot-text">热销</text>
              </view>
              <!-- 下架遮罩 -->
              <view v-if="!item.status" class="thumb-off-mask">
                <text class="thumb-off-text">已下架</text>
              </view>
            </view>

            <!-- 商品信息与操作 -->
            <view class="info">
              <!-- 标题行与上下架快捷切换 -->
              <view class="name-row">
                <text class="name">{{item.name}}</text>
                <view
                  class="status-badge"
                  :class="item.status ? 'status-on' : 'status-off'"
                  @click.stop="toggleShelf(item.id)"
                >
                  <view class="status-dot"></view>
                  <text class="status-text">{{item.status ? '在售' : '下架'}}</text>
                </view>
              </view>

              <!-- 商品描述 -->
              <view class="desc" v-if="item.desc">
                {{item.desc}}
              </view>

              <!-- 价格与库存信息 -->
              <view class="meta-row">
                <view class="price-box">
                  <text class="price-symbol">¥</text>
                  <text class="price-value">{{item.price}}</text>
                </view>
                <view class="tags-box">
                  <text v-if="item.minBuy > 1" class="tag-minbuy">{{item.minBuy}}份起</text>
                  <text class="tag-stock" :class="{ 'stock-low': item.stock <= 5 }">
                    库存 {{item.stock}}
                  </text>
                </view>
              </view>

              <!-- 操作按钮组 -->
              <view class="action-row">
                <view class="action-btn btn-more" @click.stop="deleteGoods(item.id)">
                  <text class="btn-text">删除</text>
                </view>
                <view class="action-btn btn-price" @click.stop="editPrice(item.id)">
                  <text class="btn-text">改价</text>
                </view>
                <view class="action-btn btn-edit" @click.stop="editGoods(item.id)">
                  <text class="btn-text">编辑</text>
                </view>
              </view>
            </view>
          </view>

          <!-- 首屏加载动画 -->
          <view v-if="listLoading" class="list-loading">
            <view class="mini-spinner"></view>
            <text class="list-loading-text">正在加载商品...</text>
          </view>

          <!-- 空状态 -->
          <view v-if="!listLoading && list.length === 0" class="empty-wrap">
            <view class="empty-icon-box">📦</view>
            <view class="empty-title">{{keyword ? '未找到相关商品' : '暂无商品数据'}}</view>
            <view class="empty-sub">{{keyword ? '请尝试更换搜索关键词或筛选条件' : '快去添加本店的第一个美味菜品吧'}}</view>
            <view v-if="!keyword" class="empty-action-btn" @click="addGoods">
              <text>+ 添加商品</text>
            </view>
          </view>

          <!-- 下拉追加加载动画 -->
          <view v-if="loadingMore" class="list-loading list-loading-more">
            <view class="mini-spinner"></view>
            <text class="list-loading-text">加载更多中...</text>
          </view>

          <!-- 到底了提示 -->
          <view v-if="!listLoading && !loadingMore && !hasMore && list.length > 0" class="list-end">
            <view class="end-line"></view>
            <text class="end-text">已展示全部商品</text>
            <view class="end-line"></view>
          </view>

          <!-- 底部占位 -->
          <view class="list-bottom-space"></view>
        </scroll-view>

        <!-- 自绘滚动条 -->
        <view class="thin-bar main-bar" :style="{ height: mainBarH + 'rpx', top: mainBarTop + 'rpx' }"></view>
      </view>
    </view>

    <!-- 悬浮新增按钮 -->
    <view class="fab-btn" @click="addGoods">
      <text class="fab-icon">＋</text>
      <text class="fab-label">新增商品</text>
    </view>

    <!-- 全局加载蒙版 -->
    <view class="loading-mask" v-if="shelfLoading">
      <view class="loading-box">
        <view class="loading-spinner"></view>
        <text class="loading-text">{{loadingText}}</text>
      </view>
    </view>

    <!-- 分类统计弹框 -->
    <view v-if="statVisible" class="stat-mask" @click="closeStatDialog">
      <view class="stat-dialog" @click.stop="noop">
        <view class="stat-head">
          <view class="stat-title-wrap">
            <text class="stat-title">{{statTitle}}</text>
            <text class="stat-subtitle">点击列表可快捷查看各分类商品分布</text>
          </view>
          <view class="stat-close-btn" @click="closeStatDialog">
            <text class="stat-close-text">✕</text>
          </view>
        </view>

        <scroll-view scroll-y class="stat-body">
          <view
            class="stat-card-row"
            v-for="item in statList"
            :key="item.id"
            @click="onStatCategorySelect(item.id)"
          >
            <view class="stat-row-left">
              <text class="stat-name">{{item.name}}</text>
              <view class="stat-progress-bg">
                <view
                  class="stat-progress-bar"
                  :style="{ width: getProgressPercent(item.count) + '%' }"
                ></view>
              </view>
            </view>
            <view class="stat-row-right">
              <text class="stat-count">{{item.count}}</text>
              <text class="stat-unit">件</text>
            </view>
          </view>
          <view v-if="statList.length === 0" class="stat-empty">暂无统计数据</view>
        </scroll-view>

        <view class="stat-foot">
          <view class="stat-total-label">当前口径合计</view>
          <view class="stat-total-val-box">
            <text class="stat-total-value">{{statTotal}}</text>
            <text class="stat-total-unit">件</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
const app = getApp()
import api from '@/api/index'
import { formatImageUrl } from '@/utils/util'

export default {
  data() {
    return {
      categories: [],
      activeCategory: 'all',
      list: [],
      keyword: '',
      // 筛选：statusFilter 0=全部 1=上架 2=下架；hotFilter 0=全部 1=热销 2=非热销
      statusFilter: 0,
      hotFilter: 0,
      totalCount: 0,
      onShelfCount: 0,
      // 分页加载状态
      currentPage: 1,
      pageSize: 10,
      total: 0,
      hasMore: true,
      listLoading: false,   // 首屏 / 切换分类 / 搜索加载
      loadingMore: false,   // 下拉到底追加加载
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
    }
  },
  computed: {
    offShelfCount() {
      const count = (this.totalCount || 0) - (this.onShelfCount || 0)
      return count > 0 ? count : 0
    }
  },
  onShow() {
    if (app.globalData.role !== 'merchant') {
      uni.reLaunch({ url: '/pages/role/role' })
      return
    }
    if (!app.globalData.isLogin()) {
      uni.reLaunch({ url: '/pages/login/login?role=merchant' })
      return
    }
    // 每次展示重置定位标记：编辑返回时由 editGoods 重新设置
    this._focusDone = false
    this.loadData()
  },
  // 底部导航切换时刷新当前页面数据
  onTabRefresh() {
    if (!app.globalData.isLogin()) return
    this._focusDone = false
    this.loadData()
  },
  methods: {
    // 加载分类 + 统计 + 第一页商品
    loadData() {
      this.loadCategories()
      this.loadStats()
      this.loadList(true)
    },

    // 加载全部商品分类
    loadCategories() {
      api.getAllCategories().then((categories) => {
        this.categories = categories || []
        this.$nextTick(() => this.updateBars())
      }).catch(() => {
        this.categories = []
      })
    },

    // 加载统计数量（总数 / 在售 / 各分类），来自独立接口
    loadStats() {
      api.getDishStats().then((stats) => {
        this._statsData = stats || {}
        this.totalCount = (stats && stats.total) || 0
        this.onShelfCount = (stats && stats.onShelf) || 0
      }).catch(() => {
        this._statsData = {}
        this.totalCount = 0
        this.onShelfCount = 0
      })
    },

    // 分页查询：reset=true 重新加载第一页，否则在原列表基础上追加一页
    loadList(reset) {
      if (reset) {
        if (this.listLoading) return
        this.listLoading = true
        this.currentPage = 1
        this.hasMore = true
      } else {
        if (this.listLoading || this.loadingMore) return
        this.loadingMore = true
      }
      const params = {
        pageNum: this.currentPage,
        pageSize: this.pageSize,
        categoryId: this.activeCategory === 'all' ? null : this.activeCategory,
        keyword: (this.keyword || '').trim() || null,
        status: this.statusFilter === 0 ? null : (this.statusFilter === 1 ? 1 : 0),
        isHot: this.hotFilter === 0 ? null : (this.hotFilter === 1 ? 1 : 0)
      }
      api.pageAdminDishes(params).then((page) => {
        const records = ((page && page.records) || []).map((g) => this.mapItem(g))
        this.list = reset ? records : this.list.concat(records)
        this.total = (page && page.total) || 0
        this.hasMore = this.currentPage < ((page && page.pages) || 0)
        this.currentPage += 1
        this.listLoading = false
        this.loadingMore = false
        // 数据就绪后测量视口高度，初始化/更新细滚动条
        this.$nextTick(() => this.updateBars())
      }).catch(() => {
        this.listLoading = false
        this.loadingMore = false
      })
    },

    // 数据项规整：补全图片地址与 hasImage 标记
    mapItem(g) {
      const img = g.image || ''
      const hasImage = /^https?:\/\//.test(img) || img.startsWith('/')
      return { ...g, hasImage, imageUrl: hasImage ? formatImageUrl(img) : '' }
    },

    // 切换分类：重置到顶部并按新分类查询第一页
    switchCategory(id) {
      if (this.activeCategory === id) return
      this.activeCategory = id
      this.rollTopMain()
      this.loadList(true)
    },

    // 列表重置到顶部
    rollTopMain() {
      this._mainScrollTop = 0
      this._mainScrollHeight = 0
      // scroll-top 需从非 0 值变化才会触发，先置 0 再复位
      this.mainScrollTop = this.mainScrollTop === 0 ? 0.1 : 0
    },

    // ==================== 自绘细滚动条 ====================
    onSideScroll(e) {
      this._sideScrollTop = e.detail.scrollTop
      this._sideScrollHeight = e.detail.scrollHeight
      this.refreshBar('side')
    },

    onMainScroll(e) {
      this._mainScrollTop = e.detail.scrollTop
      this._mainScrollHeight = e.detail.scrollHeight
      this.refreshBar('main')
      this.maybeLoadMore(e.detail)
    },

    // 触底时请求下一页追加渲染
    maybeLoadMore(detail) {
      if (this.listLoading || this.loadingMore || !this.hasMore) return
      const viewHeight = this._mainViewHeight || 0
      if (!viewHeight) return
      const { scrollTop, scrollHeight } = detail || {}
      if (scrollHeight - scrollTop - viewHeight < 30) {
        this.loadList(false)
      }
    },

    refreshBar(which) {
      const isSide = which === 'side'
      const scrollTop = (isSide ? this._sideScrollTop : this._mainScrollTop) || 0
      const scrollHeight = (isSide ? this._sideScrollHeight : this._mainScrollHeight) || 0
      const viewHeight = (isSide ? this._sideViewHeight : this._mainViewHeight) || 0
      if (viewHeight <= 0 || scrollHeight <= viewHeight) {
        if (isSide) {
          this.sideBarH = 0
        } else {
          this.mainBarH = 0
        }
        return
      }
      const barH = Math.max(60, (viewHeight / scrollHeight) * viewHeight)
      const maxTop = viewHeight - barH
      const ratio = scrollTop / (scrollHeight - viewHeight)
      const barTop = Math.min(maxTop, Math.max(0, ratio * maxTop))
      if (isSide) {
        this.sideBarH = barH
        this.sideBarTop = barTop
      } else {
        this.mainBarH = barH
        this.mainBarTop = barTop
      }
    },

    // 测量两栏视口高度与内容高度
    updateBars() {
      const query = uni.createSelectorQuery().in(this)
      query.select('.side').boundingClientRect()
      query.select('.main').boundingClientRect()
      query.select('.side .side-item').boundingClientRect()
      query.select('.goods-item').boundingClientRect()
      query.exec((res) => {
        if (!res || !res[0] || !res[1]) return
        this._sideViewHeight = res[0].height
        this._mainViewHeight = res[1].height

        const sideItemH = res[2] ? res[2].height : 0
        const sideCount = this.categories.length + 1
        const mainItemH = res[3] ? res[3].height : 0
        const mainCount = this.list.length
        this._mainItemHeight = mainItemH
        this._sideScrollHeight = this._sideScrollHeight || sideItemH * sideCount
        this._mainScrollHeight = this._mainScrollHeight || mainItemH * mainCount

        this.refreshBar('side')
        this.refreshBar('main')

        if (this._focusId && !this._focusDone) {
          this._focusDone = true
          const fid = this._focusId
          this._focusId = null
          this.$nextTick(() => this.scrollToGoods(fid))
        }
      })
    },

    // 搜索输入（防抖）
    onSearchInput(e) {
      this.keyword = e.detail.value || ''
      clearTimeout(this._searchTimer)
      this._searchTimer = setTimeout(() => {
        this.rollTopMain()
        this.loadList(true)
      }, 300)
    },

    // 清空搜索
    clearSearch() {
      this.keyword = ''
      this.rollTopMain()
      this.loadList(true)
    },

    // 切换上下架筛选
    switchStatus(v) {
      if (this.statusFilter === v) return
      this.statusFilter = v
      this.rollTopMain()
      this.loadList(true)
    },

    // 切换热销筛选
    switchHot(v) {
      if (this.hotFilter === v) return
      this.hotFilter = v
      this.rollTopMain()
      this.loadList(true)
    },

    // ==================== 分类统计弹框 ====================
    noop() {},

    showStatDialog(type) {
      const stats = this._statsData || {}
      const items = stats.categories || []
      const list = items
        .filter((c) => (type === 'onShelf' ? (c.onShelf || 0) > 0 : (c.total || 0) > 0))
        .map((c) => ({ id: c.id, name: c.name || '未分类', count: type === 'onShelf' ? c.onShelf : c.total }))
      this.statVisible = true
      this.statTitle = type === 'onShelf' ? '在售商品分类分布' : '全部商品分类分布'
      this.statList = list
      this.statTotal = type === 'onShelf' ? (stats.onShelf || 0) : (stats.total || 0)
    },

    closeStatDialog() {
      this.statVisible = false
    },

    getProgressPercent(count) {
      if (!this.statTotal || this.statTotal <= 0) return 0
      return Math.min(100, Math.round(((count || 0) / this.statTotal) * 100))
    },

    onStatCategorySelect(categoryId) {
      this.closeStatDialog()
      this.switchCategory(categoryId)
    },

    // 上下架操作
    toggleShelf(id) {
      const target = this.list.find((g) => g.id === id)
      if (!target) return
      if (this.shelfLoading) return

      const nextStatus = target.status ? 0 : 1
      this.shelfLoading = true
      this.loadingText = nextStatus === 1 ? '正在上架…' : '正在下架…'

      api.updateDishStatus(id, nextStatus).then(() => {
        this.list = this.list.map((g) =>
          g.id === id ? { ...g, status: !!nextStatus } : g
        )
        this.loadStats()
        uni.showToast({ title: nextStatus === 1 ? '已成功上架' : '已成功下架', icon: 'none' })
      }).catch(() => {}).then(() => {
        this.shelfLoading = false
      })
    },

    // 标记需要定位的商品
    focusGoods(id) {
      this._focusId = id
      this._focusDone = false
    },

    // 改价
    editPrice(id) {
      const target = this.list.find((g) => g.id === id)
      if (!target) return
      uni.showModal({
        title: '修改商品价格',
        editable: true,
        placeholderText: `当前价格 ¥${target.price}`,
        confirmColor: '#2f80ed',
        success: (res) => {
          if (!res.confirm) return
          const price = Number(res.content)
          if (isNaN(price) || price < 0) {
            uni.showToast({ title: '请输入有效价格', icon: 'none' })
            return
          }
          this.shelfLoading = true
          this.loadingText = '正在修改价格…'
          api.updateDishPrice(id, price).then(() => {
            this.list = this.list.map((g) => (g.id === id ? { ...g, price } : g))
            uni.showToast({ title: '价格已更新', icon: 'none' })
          }).catch(() => {}).then(() => {
            this.shelfLoading = false
          })
        }
      })
    },

    // 新增商品
    addGoods() {
      uni.navigateTo({ url: '/pages/merchant/dish-edit/dish-edit' })
    },

    // 编辑商品
    editGoods(id) {
      this.focusGoods(id)
      uni.navigateTo({ url: `/pages/merchant/dish-edit/dish-edit?id=${id}` })
    },

    // 返回列表后滚动定位
    scrollToGoods(id) {
      const query = uni.createSelectorQuery().in(this)
      query.select('.main').boundingClientRect()
      query.select(`#goods-${id}`).boundingClientRect()
      query.exec((res) => {
        const view = res[0]
        if (!view) return
        const viewHeight = view.height
        const listH = this._mainItemHeight || 0

        if (res[1]) {
          const delta = res[1].top - view.top - (viewHeight - res[1].height) / 2
          this.applyScroll((this._mainScrollTop || 0) + delta)
          return
        }
        const index = this.list.findIndex((g) => String(g.id) === String(id))
        if (index < 0 || !listH) return
        this.applyScroll(index * listH - viewHeight / 2 + listH / 2)
      })
    },

    applyScroll(scrollTop) {
      const next = Math.max(0, Math.round(scrollTop))
      const current = this._mainScrollTop || 0
      if (Math.abs(next - current) < 2) {
        this.mainScrollTop = next === 0 ? 0.1 : 0
        return
      }
      this._mainScrollTop = next
      this.mainScrollTop = next
    },

    // 删除商品
    deleteGoods(id) {
      uni.showModal({
        title: '删除确认',
        content: '确定要删除该商品吗？删除后不可恢复。',
        confirmText: '确认删除',
        confirmColor: '#ef4444',
        success: (res) => {
          if (!res.confirm) return
          api.deleteDish(id).then(() => {
            uni.showToast({ title: '已删除', icon: 'none' })
            this.loadStats()
            this.loadList(true)
          }).catch(() => {})
        }
      })
    },

    // 长按操作菜单
    onGoodsLongPress(id) {
      uni.showActionSheet({
        itemList: ['编辑商品', '修改价格', '删除商品'],
        success: (res) => {
          if (res.tapIndex === 0) {
            this.editGoods(id)
          } else if (res.tapIndex === 1) {
            this.editPrice(id)
          } else if (res.tapIndex === 2) {
            this.deleteGoods(id)
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  box-sizing: border-box;
  background: #f4f6fa;
  overflow: hidden;
  padding-bottom: 0;
  min-height: 0;
  /* #ifdef H5 */
  height: calc(100vh - 44px - env(safe-area-inset-top));
  /* #endif */
}

/* ==================== 顶部 Header ==================== */
.header {
  flex-shrink: 0;
  background: #ffffff;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.03);
  z-index: 10;
}

/* 统计卡片：深邃渐变 + 磨砂质感 */
.summary-card {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-around;
  margin: 16rpx 20rpx 12rpx;
  padding: 24rpx 0;
  background: linear-gradient(135deg, #2468f2 0%, #1a56d6 100%);
  border-radius: 20rpx;
  color: #ffffff;
  box-shadow: 0 8rpx 24rpx rgba(26, 86, 214, 0.22);
}

.summary-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  transition: opacity 0.15s;
}

.summary-item:active {
  opacity: 0.75;
}

.summary-value {
  font-size: 44rpx;
  font-weight: 700;
  line-height: 1.1;
  letter-spacing: -0.5rpx;
}

.summary-value.active-color {
  color: #55f0b5;
}

.summary-value.off-color {
  color: #cbd5e1;
}

.summary-label {
  display: flex;
  align-items: center;
  font-size: 22rpx;
  opacity: 0.88;
  margin-top: 8rpx;
}

.stat-arrow-icon {
  font-size: 20rpx;
  margin-left: 6rpx;
  opacity: 0.8;
}

.summary-divider {
  width: 1rpx;
  height: 44rpx;
  background: rgba(255, 255, 255, 0.18);
}

/* 搜索容器 */
.search-container {
  padding: 4rpx 20rpx 12rpx;
}

.search-bar {
  display: flex;
  align-items: center;
  background: #f1f3f7;
  height: 72rpx;
  padding: 0 24rpx;
  border-radius: 36rpx;
  box-sizing: border-box;
}

.search-icon {
  font-size: 26rpx;
  margin-right: 12rpx;
  color: #8c93a0;
}

.search-input {
  flex: 1;
  font-size: 26rpx;
  color: #1e293b;
}

.search-clear-btn {
  width: 36rpx;
  height: 36rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #cbd5e1;
}

.search-clear-text {
  font-size: 28rpx;
  color: #ffffff;
  line-height: 1;
}

/* 筛选工具栏 */
.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20rpx 16rpx;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.filter-pill {
  display: flex;
  align-items: center;
  padding: 8rpx 22rpx;
  border-radius: 30rpx;
  font-size: 23rpx;
  color: #64748b;
  background: #f1f5f9;
  font-weight: 500;
  transition: all 0.2s ease;
}

.filter-pill .pill-dot {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: #94a3b8;
  margin-right: 8rpx;
}

.filter-pill.active {
  color: #ffffff;
  background: #2468f2;
}

.filter-pill.active .pill-dot {
  background: #ffffff;
}

.filter-pill.on.active {
  background: #10b981;
}

.filter-pill.off.active {
  background: #64748b;
}

.hot-pill {
  color: #64748b;
}

.hot-pill .hot-fire {
  font-size: 22rpx;
  margin-right: 6rpx;
}

.hot-pill.active {
  background: linear-gradient(135deg, #ff7a45, #ff5722);
  color: #ffffff;
}

/* ==================== 主体双栏区域 ==================== */
.body {
  flex: 1;
  display: flex;
  min-height: 0;
  background: #f4f6fa;
}

/* 左侧分类侧边栏 */
.side-wrap {
  position: relative;
  width: 172rpx;
  flex-shrink: 0;
  height: 100%;
}

.side {
  width: 100%;
  height: 100%;
  background: #ebedf2;
}

.side-item {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 30rpx 14rpx;
  font-size: 26rpx;
  color: #64748b;
  font-weight: 400;
  transition: all 0.15s ease;
}

.side-item .side-indicator {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 8rpx;
  height: 32rpx;
  border-radius: 0 6rpx 6rpx 0;
  background: transparent;
  transition: all 0.2s ease;
}

.side-name {
  text-align: center;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  word-break: break-all;
}

.side-item.active {
  background: #ffffff;
  color: #1e293b;
  font-weight: 600;
}

.side-item.active .side-indicator {
  background: #2468f2;
  height: 38rpx;
}

.side-bottom-space {
  height: 180rpx;
}

/* 右侧商品列表 */
.main-wrap {
  position: relative;
  flex: 1;
  height: 100%;
  min-width: 0;
}

.main {
  width: 100%;
  height: 100%;
  padding: 0 16rpx 0 16rpx;
  box-sizing: border-box;
}

/* 自绘滚动条 */
.thin-bar {
  position: absolute;
  right: 4rpx;
  width: 6rpx;
  border-radius: 6rpx;
  background: rgba(0, 0, 0, 0.18);
  pointer-events: none;
  transition: top 0.05s linear;
}

/* 商品卡片 */
.goods-item {
  display: flex;
  align-items: stretch;
  background: #ffffff;
  padding: 20rpx 18rpx;
  margin: 16rpx 0;
  border-radius: 18rpx;
  box-shadow: 0 4rpx 16rpx rgba(15, 23, 42, 0.04);
  transition: transform 0.1s ease;
}

.goods-item:active {
  transform: scale(0.995);
}

/* 缩略图 */
.thumb-box {
  position: relative;
  width: 144rpx;
  height: 144rpx;
  border-radius: 14rpx;
  background: #f8fafc;
  flex-shrink: 0;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.thumb-img {
  width: 100%;
  height: 100%;
}

.thumb-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: #f1f5f9;
}

.thumb-icon {
  font-size: 56rpx;
}

.badge-hot {
  position: absolute;
  left: 0;
  top: 0;
  background: linear-gradient(135deg, #ff5722, #ff7a45);
  color: #ffffff;
  padding: 2rpx 10rpx 4rpx 8rpx;
  border-bottom-right-radius: 12rpx;
  display: flex;
  align-items: center;
  font-size: 18rpx;
  font-weight: 600;
  line-height: 1;
}

.badge-hot-icon {
  font-size: 16rpx;
  margin-right: 2rpx;
}

.thumb-off-mask {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.52);
  display: flex;
  align-items: center;
  justify-content: center;
}

.thumb-off-text {
  color: #ffffff;
  font-size: 20rpx;
  font-weight: 600;
  padding: 4rpx 10rpx;
  background: rgba(0, 0, 0, 0.4);
  border-radius: 6rpx;
}

/* 信息区 */
.info {
  flex: 1;
  min-width: 0;
  margin-left: 18rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.name-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12rpx;
}

.name {
  flex: 1;
  font-size: 28rpx;
  font-weight: 600;
  color: #1e293b;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  word-break: break-all;
}

/* 上下架微标签开关 */
.status-badge {
  display: flex;
  align-items: center;
  padding: 4rpx 14rpx;
  border-radius: 20rpx;
  font-size: 20rpx;
  font-weight: 500;
  flex-shrink: 0;
  transition: all 0.2s ease;
}

.status-badge .status-dot {
  width: 8rpx;
  height: 8rpx;
  border-radius: 50%;
  margin-right: 6rpx;
}

.status-badge.status-on {
  background: #ecfdf5;
  color: #059669;
}

.status-badge.status-on .status-dot {
  background: #10b981;
}

.status-badge.status-off {
  background: #f1f5f9;
  color: #94a3b8;
}

.status-badge.status-off .status-dot {
  background: #cbd5e1;
}

.desc {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #64748b;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 1;
  overflow: hidden;
  word-break: break-all;
}

/* 价格与库存 */
.meta-row {
  margin-top: 10rpx;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.price-box {
  display: flex;
  align-items: baseline;
  color: #ff5722;
}

.price-symbol {
  font-size: 22rpx;
  font-weight: 600;
}

.price-value {
  font-size: 34rpx;
  font-weight: 700;
  margin-left: 2rpx;
}

.tags-box {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.tag-minbuy {
  font-size: 20rpx;
  color: #ff7a45;
  background: #fff3ea;
  border-radius: 6rpx;
  padding: 2rpx 8rpx;
}

.tag-stock {
  font-size: 21rpx;
  color: #94a3b8;
}

.tag-stock.stock-low {
  color: #ef4444;
  font-weight: 500;
}

/* 操作按钮 */
.action-row {
  margin-top: 14rpx;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12rpx;
}

.action-btn {
  padding: 6rpx 20rpx;
  border-radius: 24rpx;
  font-size: 22rpx;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  transition: opacity 0.15s;
}

.action-btn:active {
  opacity: 0.7;
}

.btn-edit {
  background: #2468f2;
  color: #ffffff;
}

.btn-price {
  background: #eff6ff;
  color: #2468f2;
  border: 1rpx solid #bfdbfe;
}

.btn-more {
  background: #f8fafc;
  color: #ef4444;
  border: 1rpx solid #fee2e2;
}

/* ==================== 空状态 & 加载状态 ==================== */
.empty-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100rpx 40rpx;
}

.empty-icon-box {
  font-size: 80rpx;
  margin-bottom: 20rpx;
}

.empty-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #334155;
}

.empty-sub {
  font-size: 24rpx;
  color: #94a3b8;
  margin-top: 8rpx;
  text-align: center;
}

.empty-action-btn {
  margin-top: 32rpx;
  padding: 14rpx 36rpx;
  background: linear-gradient(135deg, #2468f2, #1a56d6);
  color: #ffffff;
  font-size: 26rpx;
  font-weight: 600;
  border-radius: 36rpx;
  box-shadow: 0 6rpx 16rpx rgba(36, 104, 242, 0.3);
}

.list-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60rpx 0;
  color: #94a3b8;
}

.list-loading.list-loading-more {
  padding: 24rpx 0;
}

.list-loading-text {
  font-size: 24rpx;
  margin-left: 14rpx;
}

.mini-spinner {
  width: 32rpx;
  height: 32rpx;
  border: 3rpx solid rgba(36, 104, 242, 0.15);
  border-top-color: #2468f2;
  border-radius: 50%;
  animation: spinner-rotate 0.7s linear infinite;
}

.list-end {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 30rpx 0;
}

.end-line {
  width: 60rpx;
  height: 1rpx;
  background: #cbd5e1;
}

.end-text {
  font-size: 22rpx;
  color: #94a3b8;
  margin: 0 16rpx;
}

.list-bottom-space {
  height: 240rpx;
}

/* ==================== 悬浮新增按钮 ==================== */
.fab-btn {
  position: fixed;
  right: 32rpx;
  bottom: calc(150rpx + env(safe-area-inset-bottom));
  display: flex;
  align-items: center;
  padding: 0 32rpx 0 24rpx;
  height: 84rpx;
  border-radius: 42rpx;
  background: linear-gradient(135deg, #2468f2 0%, #1a56d6 100%);
  color: #ffffff;
  box-shadow: 0 10rpx 28rpx rgba(36, 104, 242, 0.38);
  z-index: 100;
  transition: transform 0.15s ease;
}

.fab-btn:active {
  transform: scale(0.95);
}

.fab-icon {
  font-size: 38rpx;
  margin-right: 8rpx;
  font-weight: 300;
}

.fab-label {
  font-size: 26rpx;
  font-weight: 600;
  letter-spacing: 0.5rpx;
}

/* ==================== 全局加载蒙版 ==================== */
.loading-mask {
  position: fixed;
  inset: 0;
  z-index: 999;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: mask-fade-in 0.2s ease;
}

.loading-box {
  min-width: 220rpx;
  padding: 36rpx 40rpx;
  background: rgba(15, 23, 42, 0.85);
  border-radius: 24rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.loading-spinner {
  width: 52rpx;
  height: 52rpx;
  border: 5rpx solid rgba(255, 255, 255, 0.2);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spinner-rotate 0.7s linear infinite;
}

.loading-text {
  margin-top: 20rpx;
  font-size: 26rpx;
  color: #ffffff;
}

@keyframes spinner-rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes mask-fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* ==================== 分类统计弹框 ==================== */
.stat-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(15, 23, 42, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: mask-fade-in 0.2s ease;
  padding: 30rpx;
  box-sizing: border-box;
}

.stat-dialog {
  width: 100%;
  max-width: 620rpx;
  max-height: 75vh;
  background: #ffffff;
  border-radius: 28rpx;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 20rpx 50rpx rgba(15, 23, 42, 0.15);
}

.stat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx 32rpx 20rpx;
  border-bottom: 1rpx solid #f1f5f9;
}

.stat-title-wrap {
  display: flex;
  flex-direction: column;
}

.stat-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #0f172a;
}

.stat-subtitle {
  font-size: 22rpx;
  color: #94a3b8;
  margin-top: 4rpx;
}

.stat-close-btn {
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #f1f5f9;
}

.stat-close-text {
  font-size: 24rpx;
  color: #64748b;
  line-height: 1;
}

.stat-body {
  flex: 1;
  max-height: 48vh;
  padding: 16rpx 32rpx;
  box-sizing: border-box;
}

.stat-card-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22rpx 0;
  border-bottom: 1rpx solid #f8fafc;
  transition: opacity 0.15s;
}

.stat-card-row:active {
  opacity: 0.65;
}

.stat-row-left {
  flex: 1;
  margin-right: 24rpx;
}

.stat-name {
  font-size: 28rpx;
  color: #334155;
  font-weight: 500;
}

.stat-progress-bg {
  width: 100%;
  height: 8rpx;
  border-radius: 4rpx;
  background: #f1f5f9;
  margin-top: 10rpx;
  overflow: hidden;
}

.stat-progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #2468f2, #38bdf8);
  border-radius: 4rpx;
  transition: width 0.3s ease;
}

.stat-row-right {
  display: flex;
  align-items: baseline;
}

.stat-count {
  font-size: 32rpx;
  font-weight: 700;
  color: #2468f2;
}

.stat-unit {
  font-size: 22rpx;
  color: #94a3b8;
  margin-left: 4rpx;
}

.stat-empty {
  padding: 60rpx 0;
  text-align: center;
  font-size: 26rpx;
  color: #94a3b8;
}

.stat-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 32rpx calc(24rpx + env(safe-area-inset-bottom));
  background: #f8fafc;
  border-top: 1rpx solid #f1f5f9;
}

.stat-total-label {
  font-size: 26rpx;
  color: #64748b;
  font-weight: 500;
}

.stat-total-val-box {
  display: flex;
  align-items: baseline;
}

.stat-total-value {
  font-size: 36rpx;
  font-weight: 700;
  color: #ff5722;
}

.stat-total-unit {
  font-size: 22rpx;
  color: #94a3b8;
  margin-left: 4rpx;
}
</style>
