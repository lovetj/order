<template>
  <view class="page">
    <bottom-nav />
    <!-- 顶部统计 + 搜索（固定，不随内容滚动） -->
    <view class="header">
      <view class="summary">
        <view class="summary-item" @click="showStatDialog('all')">
          <view class="summary-value">{{goods.length}}</view>
          <view class="summary-label">商品总数</view>
        </view>
        <view class="summary-item" @click="showStatDialog('onShelf')">
          <view class="summary-value">{{onShelfCount}}</view>
          <view class="summary-label">在售中</view>
        </view>
      </view>
      <view class="search-bar">
        <text class="search-icon">🔍</text>
        <input
          class="search-input"
          :value="keyword"
          placeholder="搜索商品名称"
          confirm-type="search"
          @input="onSearchInput"
        />
        <text v-if="keyword" class="search-clear" @click="clearSearch">×</text>
      </view>
    </view>

    <!-- 双栏主体：左分类 / 右商品，各自独立滚动 -->
    <view class="body">
      <!-- 左侧分类 -->
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
          >全部</view>
          <view
            v-for="item in categories"
            :key="item.id"
            class="side-item"
            :class="{ active: activeCategory === item.id }"
            @click="switchCategory(item.id)"
          >{{item.name}}</view>
          <view class="side-bottom-space"></view>
        </scroll-view>
        <!-- 自绘细滚动条 -->
        <view class="thin-bar side-bar" :style="{ height: sideBarH + 'rpx', top: sideBarTop + 'rpx' }"></view>
      </view>

      <!-- 右侧商品 -->
      <view class="main-wrap">
        <scroll-view
          class="main"
          scroll-y
          enhanced
          :show-scrollbar="false"
          :scroll-top="mainScrollTop"
          @scroll="onMainScroll"
        >
          <view
            class="goods-item"
            v-for="item in list"
            :key="item.id"
            :id="'goods-' + item.id"
            @longpress="onGoodsLongPress(item.id)"
          >
            <view class="thumb">
              <image v-if="item.hasImage" class="thumb-img" :src="item.imageUrl" mode="aspectFill"></image>
              <text v-else>{{item.image}}</text>
            </view>
            <view class="info">
              <view class="name-row">
                <view class="name">{{item.name}}</view>
                <view class="shelf-tag" :class="item.status ? 'on' : 'off'" @click.stop="toggleShelf(item.id)">
                  {{item.status ? '已上架' : '已下架'}}
                </view>
              </view>
              <view class="desc text-sub">
                <text v-if="item.isHot" class="hot-tag">热销推荐</text>{{item.desc}}
              </view>
              <view class="meta">
                <text class="price">¥{{item.price}}</text>
                <text v-if="item.minBuy > 1" class="minbuy-tag">{{item.minBuy}}份起购</text>
                <text class="stock text-sub">库存 {{item.stock}}</text>
              </view>
              <view class="ops">
                <view class="edit-btn" @click.stop="editGoods(item.id)">编辑</view>
                <view class="edit-btn price-btn" @click.stop="editPrice(item.id)">改价</view>
              </view>
            </view>
          </view>

          <view v-if="list.length === 0" class="empty">
            {{keyword ? '未找到相关商品' : '暂无商品，点击右下角新增'}}
          </view>
          <!-- 底部留白，避免被右下角加号遮挡 -->
          <view class="list-bottom-space"></view>
        </scroll-view>
        <!-- 自绘细滚动条 -->
        <view class="thin-bar main-bar" :style="{ height: mainBarH + 'rpx', top: mainBarTop + 'rpx' }"></view>
      </view>
    </view>

    <view class="fab" @click="addGoods">＋</view>

    <!-- 全局加载蒙版：上下架等操作时展示 -->
    <view class="loading-mask" v-if="shelfLoading">
      <view class="loading-box">
        <view class="loading-spinner"></view>
        <text class="loading-text">{{loadingText}}</text>
      </view>
    </view>

    <!-- 分类统计弹框：点击「商品总数 / 在售中」展示各分类下的数量 -->
    <view v-if="statVisible" class="stat-mask" @click="closeStatDialog">
      <view class="stat-dialog" @click.stop="noop">
        <view class="stat-head">
          <text class="stat-title">{{statTitle}}</text>
          <text class="stat-close" @click="closeStatDialog">×</text>
        </view>
        <scroll-view scroll-y class="stat-body">
          <view class="stat-row" v-for="item in statList" :key="item.id">
            <text class="stat-name">{{item.name}}</text>
            <text class="stat-count">{{item.count}}</text>
          </view>
          <view v-if="statList.length === 0" class="stat-empty">暂无数据</view>
        </scroll-view>
        <view class="stat-foot">
          <text class="stat-total">合计</text>
          <text class="stat-total-value">{{statTotal}}</text>
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
    // 加载分类 + 全部商品（含下架）
    loadData() {
      this.loading = true
      Promise.all([
        api.getAllCategories(),
        api.getAdminDishes({ categoryId: 'all' })
      ]).then(([categories, goods]) => {
        const list = goods || []
        // 从编辑页返回：若商品分类变更，先切到其新分类，保证能被定位到
        const focusId = this._focusId
        let activeCategory = this.activeCategory
        if (focusId) {
          const target = list.find((g) => String(g.id) === String(focusId))
          // 目标被删除则不定位
          if (!target) {
            this._focusId = null
          } else if (activeCategory !== 'all' && target.categoryId !== activeCategory) {
            activeCategory = target.categoryId
          }
        }
        this.categories = categories || []
        this.goods = list
        this.activeCategory = activeCategory
        this._restoreTimer && clearTimeout(this._restoreTimer)
        this._restoreTimer = setTimeout(() => { this._restoreTimer = null }, 5000)
        this.filter(activeCategory)
      }).catch(() => {
        this.categories = []
        this.goods = []
        this.list = []
      }).then(() => {
        this.loading = false
        // 数据就绪后测量视口高度，初始化细滚动条
        this.$nextTick(() => this.updateBars())
      })
    },

    switchCategory(id) {
      this.activeCategory = id
      this.filter(id)
      // 切换分类后内容高度变化，重置右侧细滚动条并回到顶部
      this.$nextTick(() => this.updateBars())
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

    // 测量两栏视口高度与内容高度（数据/分类变化后调用），并刷新滚动条
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

        // 用「单个子项高度 × 数量」估算内容高度（拿不到精确 scrollHeight 时的兜底），
        // 待首次 scroll 事件带回真实 scrollHeight 后会被修正。
        const sideItemH = res[2] ? res[2].height : 0
        const sideCount = this.categories.length + 1 // 含「全部」
        const mainItemH = res[3] ? res[3].height : 0
        const mainCount = this.list.length
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
          this.$nextTick(() => this.scrollToGoods(fid))
        }
      })
    },

    // 搜索输入：仅按名称过滤，实时刷新
    onSearchInput(e) {
      this.keyword = e.detail.value || ''
      this.filter(this.activeCategory)
    },

    // 清空搜索
    clearSearch() {
      this.keyword = ''
      this.filter(this.activeCategory)
    },

    filter(categoryId) {
      const keyword = (this.keyword || '').trim().toLowerCase()
      let list = categoryId === 'all'
        ? this.goods
        : this.goods.filter((g) => g.categoryId === categoryId)
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
      this.list = mapped
      this.onShelfCount = this.goods.filter((g) => g.status).length
      // 编辑返回定位场景：保留滚动位置，待 updateBars 完成后精确定位
      if (this._focusId) {
        this.$nextTick(() => this.updateBars())
        return
      }
      // 列表内容变化后，重置右侧滚动条（回到顶部并重新计算长度）
      this._mainScrollTop = 0
      this._mainScrollHeight = 0
      // scroll-top 需从非 0 值变化才会触发，先置 0 再复位
      this.mainScrollTop = this.mainScrollTop === 0 ? 0.1 : 0
      this.$nextTick(() => this.updateBars())
    },

    // ==================== 分类统计弹框 ====================

    // 空操作：阻止弹框内部点击冒泡到遮罩
    noop() {},

    /**
     * 点击「商品总数 / 在售中」：
     * 按分类统计对应口径下的商品数量，弹框展示
     * @param {string} type all=全部商品 / onShelf=仅上架商品
     */
    showStatDialog(type) {
      const all = this.goods || []
      // 口径过滤：在售中只统计上架商品
      const scoped = type === 'onShelf' ? all.filter((g) => g.status) : all
      // 按分类聚合数量（未匹配到分类的归入「未分类」）
      const categories = this.categories || []
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
      this.statVisible = true
      this.statTitle = type === 'onShelf' ? '在售中各分类数量' : '商品总数各分类数量'
      this.statList = statList
      this.statTotal = scoped.length
    },

    closeStatDialog() {
      this.statVisible = false
    },

    // 上下架：status 前端是布尔值，接口传 1/0
    toggleShelf(id) {
      const target = this.goods.find((g) => g.id === id)
      if (!target) return
      // 已有操作进行中，忽略重复点击
      if (this.shelfLoading) return

      const nextStatus = target.status ? 0 : 1
      this.shelfLoading = true
      this.loadingText = nextStatus === 1 ? '正在上架…' : '正在下架…'

      api.updateDishStatus(id, nextStatus).then(() => {
        const goods = this.goods.map((g) =>
          g.id === id ? { ...g, status: !!nextStatus } : g
        )
        this.goods = goods
        // 操作后保持在原商品位置
        this.focusGoods(id)
        this.filter(this.activeCategory)
        uni.showToast({ title: nextStatus === 1 ? '已上架' : '已下架', icon: 'none' })
      }).catch(() => {}).then(() => {
        this.shelfLoading = false
      })
    },

    // 标记需要定位的商品，使列表刷新后回到该商品位置
    focusGoods(id) {
      this._focusId = id
      this._focusDone = false
    },

    // 改价
    editPrice(id) {
      const target = this.goods.find((g) => g.id === id)
      if (!target) return
      uni.showModal({
        title: '修改价格',
        editable: true,
        placeholderText: `当前 ¥${target.price}`,
        confirmColor: '#2f80ed',
        success: (res) => {
          if (!res.confirm) return
          const price = Number(res.content)
          if (!price || price <= 0) {
            uni.showToast({ title: '请输入有效价格', icon: 'none' })
            return
          }
          // 展示改价加载动画（复用全局蒙版）
          this.shelfLoading = true
          this.loadingText = '正在改价…'
          api.updateDishPrice(id, price).then(() => {
            const goods = this.goods.map((g) => (g.id === id ? { ...g, price } : g))
            this.goods = goods
            // 操作后保持在原商品位置
            this.focusGoods(id)
            this.filter(this.activeCategory)
            uni.showToast({ title: '价格已更新', icon: 'none' })
          }).catch(() => {}).then(() => {
            this.shelfLoading = false
          })
        }
      })
    },

    // 新增商品：跳转表单页
    addGoods() {
      uni.navigateTo({ url: '/pages/merchant/dish-edit/dish-edit' })
    },

    // 编辑商品：跳转表单页（带 id），并记录，返回列表时定位到该商品
    editGoods(id) {
      this.focusGoods(id)
      uni.navigateTo({ url: `/pages/merchant/dish-edit/dish-edit?id=${id}` })
    },

    // 返回列表后，滚动定位到指定商品（尽量置中显示）
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
          // 精确命中：目标节点已渲染，按其在视口中的相对位置计算滚动距离
          const delta = res[1].top - view.top - (viewHeight - res[1].height) / 2
          this.applyScroll((this._mainScrollTop || 0) + delta)
          return
        }
        // 兜底：节点未渲染（目标在可视区外），按等距估算其偏移
        const index = this.list.findIndex((g) => String(g.id) === String(id))
        if (index < 0 || !listH) return
        this.applyScroll(index * listH - viewHeight / 2 + listH / 2)
      })
    },

    // 统一设置滚动位置（scroll-top 需数值变化才会触发滚动）
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
        content: '确定删除该商品吗？',
        confirmText: '删除',
        confirmColor: '#ff3b30',
        success: (res) => {
          if (!res.confirm) return
          api.deleteDish(id).then(() => {
            uni.showToast({ title: '已删除', icon: 'none' })
            this.loadData()
          }).catch(() => {})
        }
      })
    },

    // 长按商品弹出操作菜单
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
/* 页面整体占满视口，头部固定 + 主体双栏滚动 */
.page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  box-sizing: border-box;
  background: #f5f6f8;
}

/* ---------- 顶部：统计 + 搜索 ---------- */
.header {
  flex-shrink: 0;
}

.summary {
  display: flex;
  background: linear-gradient(135deg, #2f80ed, #1a5fd0);
  color: #fff;
  padding: 32rpx 0 36rpx;
}

.summary-item {
  flex: 1;
  text-align: center;
}

/* 统计项可点击：按下时轻微反馈 */
.summary-item:active {
  opacity: 0.7;
}

.summary-value {
  font-size: 48rpx;
  font-weight: 700;
}

.summary-label {
  font-size: 24rpx;
  opacity: 0.85;
  margin-top: 8rpx;
}

.search-bar {
  display: flex;
  align-items: center;
  background: #fff;
  margin: 16rpx 24rpx;
  padding: 0 24rpx;
  height: 76rpx;
  border-radius: 999rpx;
  box-shadow: 0 4rpx 14rpx rgba(0, 0, 0, 0.04);
}

.search-icon {
  font-size: 28rpx;
  margin-right: 12rpx;
}

.search-input {
  flex: 1;
  font-size: 28rpx;
  color: #222;
}

.search-clear {
  font-size: 40rpx;
  color: #bbb;
  padding: 0 6rpx;
  line-height: 1;
}

/* ---------- 主体：左分类 / 右商品 ---------- */
.body {
  flex: 1;
  display: flex;
  min-height: 0;
}

/* 左侧分类栏：外层相对定位，承载自绘滚动条 */
.side-wrap {
  position: relative;
  width: 180rpx;
  flex-shrink: 0;
  height: 100%;
}

.side {
  width: 100%;
  height: 100%;
  background: #f0f1f3;
}

/* 右侧商品栏：外层相对定位，承载自绘滚动条 */
.main-wrap {
  position: relative;
  flex: 1;
  height: 100%;
  min-width: 0;
}

/* 自绘细滚动条：轨道透明，滑块细窄圆角 */
.thin-bar {
  position: absolute;
  right: 4rpx;
  width: 6rpx;
  border-radius: 6rpx;
  background: rgba(0, 0, 0, 0.22);
  pointer-events: none;
  transition: top 0.05s linear;
}

.side-item {
  padding: 32rpx 16rpx;
  font-size: 27rpx;
  color: #555;
  text-align: center;
  position: relative;
}

.side-item.active {
  background: #fff;
  color: #2f80ed;
  font-weight: 600;
}

.side-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 6rpx;
  height: 36rpx;
  background: #2f80ed;
  border-radius: 0 6rpx 6rpx 0;
}

/* 分类列表底部留白 */
.side-bottom-space {
  height: 200rpx;
}

/* 右侧商品栏 */
.main {
  width: 100%;
  height: 100%;
  padding: 0 20rpx;
  box-sizing: border-box;
}

.goods-item {
  display: flex;
  align-items: center;
  background: #fff;
  padding: 24rpx 20rpx;
  margin: 16rpx 0;
  border-radius: 16rpx;
  box-shadow: 0 4rpx 14rpx rgba(0, 0, 0, 0.04);
}

.thumb {
  width: 120rpx;
  height: 120rpx;
  border-radius: 14rpx;
  background: #f7f7f7;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 60rpx;
  flex-shrink: 0;
  overflow: hidden;
}

.thumb-img {
  width: 100%;
  height: 100%;
}

.info {
  flex: 1;
  min-width: 0;
  margin-left: 18rpx;
}

/* 名称与上下架标签同一行 */
.name-row {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
}

/* 商品名称最多显示两行，超出省略 */
.name {
  flex: 1;
  min-width: 0;
  font-size: 29rpx;
  font-weight: 600;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  word-break: break-all;
}

/* 商品描述最多显示两行，超出省略 */
.desc {
  margin-top: 6rpx;
  font-size: 22rpx;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  word-break: break-all;
}

/* 热销推荐标记 */
.hot-tag {
  display: inline-block;
  margin-right: 8rpx;
  padding: 1rpx 10rpx;
  font-size: 20rpx;
  color: #ff6b35;
  background: #fff1ea;
  border-radius: 6rpx;
  vertical-align: middle;
}

.meta {
  margin-top: 12rpx;
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.meta .price {
  font-size: 30rpx;
  color: #ff6b35;
  font-weight: 600;
}

.stock {
  font-size: 22rpx;
}

/* 起购份数标记（大于 1 时展示） */
.minbuy-tag {
  font-size: 20rpx;
  color: #ff6b35;
  background: #fff1ea;
  border-radius: 6rpx;
  padding: 2rpx 12rpx;
}

.ops {
  margin-top: 16rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex-wrap: wrap;
}

.shelf-tag {
  flex-shrink: 0;
  padding: 4rpx 14rpx;
  border-radius: 999rpx;
  font-size: 20rpx;
  line-height: 1.5;
  margin-top: 2rpx;
}

.shelf-tag.on {
  background: #e8f6ec;
  color: #34c759;
}

.shelf-tag.off {
  background: #f2f3f5;
  color: #999;
}

.edit-btn {
  padding: 6rpx 20rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  border: 2rpx solid #2f80ed;
  color: #2f80ed;
}

.edit-btn.price-btn {
  border-color: #ff6b35;
  color: #ff6b35;
}

.empty {
  text-align: center;
  color: #8a8a8a;
  font-size: 26rpx;
  padding: 120rpx 0;
}

/* 商品列表底部留白，避免被右下角加号遮挡 */
.list-bottom-space {
  height: 220rpx;
}

/* ---------- 悬浮新增按钮 ---------- */
.fab {
  position: fixed;
  right: 40rpx;
  bottom: calc(180rpx + env(safe-area-inset-bottom));
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #4d95f5, #2f80ed);
  color: #fff;
  font-size: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10rpx 26rpx rgba(47, 128, 237, 0.4);
  z-index: 100;
}

/* ---------- 全局加载蒙版 ---------- */
.loading-mask {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  z-index: 999;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: mask-fade-in 0.2s ease;
}

.loading-box {
  min-width: 220rpx;
  padding: 40rpx 36rpx;
  background: rgba(0, 0, 0, 0.78);
  border-radius: 20rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* 旋转圆环 */
.loading-spinner {
  width: 56rpx;
  height: 56rpx;
  border: 6rpx solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spinner-rotate 0.7s linear infinite;
}

.loading-text {
  margin-top: 24rpx;
  font-size: 26rpx;
  color: #fff;
}

@keyframes spinner-rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@keyframes mask-fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

/* ---------- 分类统计弹框 ---------- */
.stat-mask {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: mask-fade-in 0.2s ease;
}

.stat-dialog {
  width: 620rpx;
  max-height: 70vh;
  background: #fff;
  border-radius: 24rpx;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.stat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx 32rpx 20rpx;
}

.stat-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #222;
}

.stat-close {
  font-size: 48rpx;
  line-height: 1;
  color: #bbb;
  padding: 0 8rpx;
}

.stat-body {
  flex: 1;
  max-height: 46vh;
  padding: 0 32rpx;
}

.stat-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f2f3f5;
}

.stat-name {
  font-size: 28rpx;
  color: #444;
}

.stat-count {
  font-size: 28rpx;
  font-weight: 600;
  color: #2f80ed;
}

.stat-empty {
  padding: 60rpx 0;
  text-align: center;
  font-size: 26rpx;
  color: #bbb;
}

.stat-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 32rpx calc(24rpx + env(safe-area-inset-bottom));
  background: #fafbfc;
  border-top: 1rpx solid #f2f3f5;
}

.stat-total {
  font-size: 28rpx;
  color: #666;
}

.stat-total-value {
  font-size: 32rpx;
  font-weight: 700;
  color: #ff6b35;
}
</style>
