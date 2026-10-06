<template>
  <view class="menu-page">
    <bottom-nav />
    <!-- 未识别店铺/桌号时提示扫码 -->
    <view v-if="needScan" class="scan-tip">
      <view class="scan-tip-card">
        <view class="scan-tip-icon">📷</view>
        <view class="scan-tip-text">
          <view class="scan-tip-title">请先扫描桌位二维码</view>
          <view class="scan-tip-desc">未识别到店铺与桌号，无法点餐</view>
        </view>
        <view class="scan-tip-btn" @click="scanThenReload">立即扫码</view>
      </view>
    </view>
    <!-- 左侧分类 -->
    <scroll-view class="cat-list" scroll-y>
      <view
        class="cat-item"
        :class="{ active: currentCategory === 'all' }"
        @click="switchCategory('all')"
      >全部</view>
      <view
        v-for="item in categories"
        :key="item.id"
        class="cat-item"
        :class="{ active: currentCategory === item.id }"
        @click="switchCategory(item.id)"
      >{{item.name}}</view>
    </scroll-view>

    <!-- 右侧商品 -->
    <scroll-view
      class="goods-list"
      scroll-y
      :lower-threshold="60"
      @scrolltolower="loadMore"
    >
      <view class="goods-item" v-for="item in goodsList" :key="item.key || item.id">
        <view class="thumb">
          <image v-if="item.hasImage" class="thumb-img" :src="item.imageUrl" mode="aspectFill"></image>
          <text v-else>{{item.image}}</text>
        </view>
        <view class="info">
          <view class="name-row">
            <view class="name">{{item.name}}</view>
            <view v-if="item.isHot" class="hot-tag">热销推荐</view>
          </view>
          <view class="desc">{{item.desc}}</view>
          <view class="meta">
            <text class="price">¥{{item.price}}</text>
            <text v-if="item.minBuy > 1" class="minbuy-tag">{{item.minBuy}}份起购</text>
            <text class="sales">已售 {{item.sales || 0}}</text>
          </view>
          <view class="ops">
            <view v-if="item.count > 0" class="step-btn minus" @click="minusItem(item.key)">−</view>
            <text v-if="item.count > 0" class="step-num">{{item.count}}</text>
            <view v-if="item.minBuy > 1" class="min-buy-btn" @click="addItem(item.id)">{{item.minBuy}}份起购</view>
            <view v-else class="step-btn plus" @click="addItem(item.id)">＋</view>
          </view>
        </view>
      </view>

      <!-- 无数据 -->
      <view v-if="!listLoading && goodsList.length === 0" class="empty">该分类暂无在售菜品</view>

      <!-- 首屏加载 -->
      <view v-if="listLoading" class="list-loading">
        <view class="mini-spinner"></view>
        <text class="list-loading-text">加载中…</text>
      </view>

      <!-- 下拉加载更多 -->
      <view v-if="!listLoading && loadingMore" class="list-loading list-loading-more">
        <view class="mini-spinner"></view>
        <text class="list-loading-text">加载中…</text>
      </view>

      <!-- 到底了 -->
      <view v-if="!listLoading && !loadingMore && !hasMore && goodsList.length > 0" class="list-end">到底了</view>

      <view class="bottom-space"></view>
    </scroll-view>

    <!-- 购物车面板 -->
    <view v-if="showCart" class="cart-mask" @click="closeCart"></view>
    <view v-if="showCart" class="cart-panel">
      <view class="cart-head">
        <text>已选商品</text>
        <text class="cart-clear" @click="clearCart">🗑 清空</text>
      </view>
      <scroll-view scroll-y class="cart-list">
        <view class="cart-item" v-for="item in cartList" :key="item.key">
          <view class="cart-img">
            <image v-if="item.hasImage" class="cart-img-real" :src="item.imageUrl" mode="aspectFill"></image>
            <text v-else>{{item.image}}</text>
          </view>
          <view class="cart-info">
            <view class="cart-name">{{item.name}}</view>
            <view v-if="item.specText" class="cart-spec">{{item.specText}}</view>
          </view>
          <text class="price">¥{{item.price}}</text>
          <view class="stepper small">
            <view class="step-btn minus" @click="minusItem(item.key)">−</view>
            <text class="step-num">{{item.count}}</text>
            <view class="step-btn plus" @click="addItem(item.id)">＋</view>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 结算栏 -->
    <view class="checkout-bar">
      <view class="cart-entry" @click="toggleCart">
        <view class="cart-icon" :class="{ active: cartCount }">🛒</view>
        <view v-if="cartCount" class="badge">{{cartCount}}</view>
      </view>
      <view class="checkout-amount">
        <text v-if="cartCount" class="total">合计 <text class="price">¥{{cartAmount}}</text></text>
        <text v-else class="empty-tip">请选择菜品</text>
      </view>
      <view class="checkout-btn" :class="{ disabled: !cartCount }" @click="submitOrder">去结算</view>
    </view>
  </view>
</template>

<script>
// 顾客端点餐
const app = getApp()
import api from '@/api/index'
import { formatImageUrl } from '@/utils/util'
import { hasCustomerContext, scanOrderContext, ensureCustomerContext } from '@/utils/scan'

export default {
  data() {
    return {
      categories: [],
      currentCategory: 'all',
      goodsList: [],
      // 分页加载状态
      currentPage: 1,
      pageSize: 10,
      hasMore: true,
      listLoading: false,   // 首屏 / 切换分类加载
      loadingMore: false,   // 触底追加加载
      cart: {},          // { dishId: count }
      cartList: [],
      cartCount: 0,
      cartAmount: 0,
      showCart: false,
      needScan: false
    }
  },

  onLoad(options) {
    // 进入点餐页即处理路由上下文：shopId/tableId/tableNo 落缓存；未登录则跳登录页（防重入），
    // 未登录时返回 false，onShow 里不会再发起任何需要登录/三要素的接口。
    this._ctxReady = ensureCustomerContext(options)
  },

  onShow() {
    if (app.globalData.role !== 'customer') {
      uni.reLaunch({ url: '/pages/role/role' })
      return
    }
    // 未登录：onLoad 已通过 ensureCustomerContext 跳转登录，这里仅中止后续请求
    if (!app.globalData.isLogin()) return
    this.needScan = !hasCustomerContext(app)
    // 无店铺/桌位上下文时不发请求，仅提示扫码
    if (this.needScan) return
    // 扫码进入时链接仅携带桌位ID，回查桌号供结算使用
    this.resolveTableNo()
    if (!this.categories.length) {
      this.loadData()
    } else {
      // 回到页面时同步后端最新菜品（价格/库存可能变化）
      this.loadList(true)
    }
  },

  methods: {
    // 桌号回查：仅当未携带桌号但携带了桌位ID时，按桌位ID查询桌号
    resolveTableNo() {
      if (app.globalData.tableNo || !app.globalData.tableId) return
      api.getTableByCustomerId(app.globalData.tableId).then((t) => {
        if (t && t.tableNo) {
          app.globalData.setTableNo(t.tableNo)
        }
      }).catch(() => {})
    },

    // 底部导航切换时刷新当前页面数据
    onTabRefresh() {
      if (!app.globalData.isLogin()) return
      this.needScan = !hasCustomerContext(app)
      if (this.needScan) return
      if (!this.categories.length) {
        this.loadData()
      } else {
        this.loadList(true)
      }
    },

    // 扫码识别店铺/桌号后刷新页面
    async scanThenReload() {
      const { ok } = await scanOrderContext(app)
      if (!ok) return
      this.needScan = false
      this.resolveTableNo()
      if (!this.categories.length) {
        this.loadData()
      } else {
        this.loadList(true)
      }
    },

    // 加载分类 + 第一页菜品
    loadData() {
      this.categories = []
      this.loadCategories()
      this.loadList(true)
    },

    loadCategories() {
      api.getCategories().then((categories) => {
        this.categories = categories || []
      }).catch(() => {
        this.categories = []
      })
    },

    /**
     * 统一处理菜品图片：数据库存相对路径（历史数据可能是完整 URL），
     * hasImage 标记是否为真实图片，imageUrl 为拼好的完整展示地址。
     */
    decorateImage(g) {
      const img = (g && g.image) || ''
      const hasImage = /^https?:\/\//.test(img) || img.startsWith('/')
      return { ...g, hasImage, imageUrl: hasImage ? formatImageUrl(img) : '' }
    },

    // 分页查询：reset=true 重新加载第一页，否则追加一页
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
        categoryId: this.currentCategory === 'all' ? null : this.currentCategory
      }
      api.customerPageDishes(params).then((page) => {
        const records = ((page && page.records) || []).map((g) => {
          // 列表行减号使用的购物车 key：无规格菜品的主条目为「id|」
          const decorated = this.decorateImage(g)
          return { ...decorated, key: `${g.id}|` }
        })
        this.goodsList = reset ? records : this.goodsList.concat(records)
        this.hasMore = this.currentPage < ((page && page.pages) || 0)
        this.currentPage += 1
        this.listLoading = false
        this.loadingMore = false
        // 用当前购物车刷新列表角标
        this.syncCartCounts()
      }).catch(() => {
        this.listLoading = false
        this.loadingMore = false
      })
    },

    // 切换分类：重置到顶部并按新分类加载第一页
    switchCategory(id) {
      if (this.currentCategory === id) return
      this.currentCategory = id
      this.loadList(true)
    },

    // 触底加载下一页（scroll-view scrolltolower 触发）
    loadMore() {
      if (this.listLoading || this.loadingMore || !this.hasMore) return
      this.loadList(false)
    },

    // 用购物车份数刷新列表角标
    syncCartCounts() {
      const { cart, goodsList } = this
      const countByDish = {}
      Object.keys(cart).forEach((key) => {
        const entry = cart[key]
        if (!entry) return
        countByDish[entry.id] = (countByDish[entry.id] || 0) + entry.count
      })
      this.goodsList = goodsList.map((g) => ({
        ...g,
        count: countByDish[g.id] || 0
      }))
    },

    /**
     * 点击 + ：跳转规格选择页
     * 规格页会拉取菜品规格；若无规格，返回时直接加入购物车
     * 注：起购份数(minBuy) 由规格页的步进器保证首次加入时不少于 minBuy
     */
    addItem(id) {
      uni.navigateTo({ url: `/pages/spec-choose/spec-choose?id=${id}` })
    },

    /**
     * 规格选择页回调：加入带规格的购物车项
     * 无规格菜品返回时 specIds 为空、extraPrice 为 0
     * @param {Object} result { dishId, quantity, specIds, specText, extraPrice, unitPrice }
     */
    onSpecChosen(result) {
      this.addToCart(result.dishId, result.quantity, result.specIds, result.specText, result.extraPrice)
    },

    /**
     * 加入购物车
     * 购物车 key = dishId + '|' + specText（同菜品不同规格视为不同条目）
     */
    addToCart(dishId, quantity, specIds, specText, extraPrice) {
      const dish = this.goodsList.find((g) => g.id === dishId)
      if (!dish) return
      const unitPrice = Number(dish.price) + (Number(extraPrice) || 0)
      const key = dishId + '|' + (specText || '')
      const cart = this.cart
      if (cart[key]) {
        cart[key].count += quantity
      } else {
        cart[key] = {
          id: dishId,
          key,
          name: dish.name,
          price: unitPrice,
          image: dish.image,
          hasImage: dish.hasImage,
          imageUrl: dish.imageUrl,
          specIds: specIds || [],
          specText: specText || '',
          count: quantity
        }
      }
      this.updateCart(cart)
    },

    minusItem(key) {
      const cart = { ...this.cart }
      if (!cart[key]) return
      cart[key].count -= 1
      if (cart[key].count <= 0) delete cart[key]
      this.updateCart(cart)
    },

    updateCart(cart) {
      let cartCount = 0
      let cartAmount = 0
      const cartList = []
      // 汇总每个菜品的总数量（用于列表角标）
      const countByDish = {}
      Object.keys(cart).forEach((key) => {
        const entry = cart[key]
        cartCount += entry.count
        cartAmount += entry.count * entry.price
        countByDish[entry.id] = (countByDish[entry.id] || 0) + entry.count
        cartList.push(entry)
      })
      const goodsList = this.goodsList.map((g) => ({
        ...g,
        count: countByDish[g.id] || 0
      }))
      app.globalData.cart = cart
      this.cart = cart
      this.cartList = cartList
      this.cartCount = cartCount
      this.cartAmount = Number(cartAmount.toFixed(2))
      this.goodsList = goodsList
    },

    toggleCart() {
      if (!this.cartCount) return
      this.showCart = !this.showCart
    },

    closeCart() {
      this.showCart = false
    },

    clearCart() {
      this.showCart = false
      this.updateCart({})
    },

    submitOrder() {
      if (!this.cartCount) {
        uni.showToast({ title: '请先选择菜品', icon: 'none' })
        return
      }
      const tableNo = app.globalData.tableNo
      if (!tableNo) {
        uni.showToast({ title: '请先扫码获取桌号', icon: 'none' })
        return
      }
      const { cart, cartAmount, cartCount } = this
      // 自动匹配最优优惠券并展示
      api.getBestCoupon(cartAmount).then((coupon) => {
        let content = `共 ${cartCount} 件，合计 ¥${cartAmount}，桌号 ${tableNo}`
        let userCouponId = ''
        if (coupon) {
          userCouponId = coupon.id
          content += `\n已使用：${coupon.label}`
        }
        uni.showModal({
          title: '确认下单',
          content,
          confirmText: '确认下单',
          confirmColor: '#ff6b35',
          success: (res) => {
            if (!res.confirm) return
            // 组装后端需要的 items: [{ dishId, quantity, specIds, specText }]
            const items = Object.keys(cart).map((key) => {
              const entry = cart[key]
              return {
                dishId: entry.id,
                quantity: entry.count,
                specIds: entry.specIds || [],
                specText: entry.specText || ''
              }
            })
            const payload = { tableNo, items, peopleCount: null }
            if (userCouponId) {
              payload.userCouponId = userCouponId
            }
            api.createOrder(payload)
              .then(() => {
                this.showCart = false
                this.updateCart({})
                uni.showToast({ title: '下单成功', icon: 'success' })
                setTimeout(() => uni.reLaunch({ url: '/pages/order/order' }), 800)
              })
              .catch(() => {})
          }
        })
      }).catch(() => {
        // 优惠券查询失败不影响下单
        uni.showModal({
          title: '确认下单',
          content: `共 ${cartCount} 件，合计 ¥${cartAmount}，桌号 ${tableNo}`,
          confirmText: '确认下单',
          confirmColor: '#ff6b35',
          success: (res) => {
            if (!res.confirm) return
            const items = Object.keys(cart).map((key) => {
              const entry = cart[key]
              return {
                dishId: entry.id,
                quantity: entry.count,
                specIds: entry.specIds || [],
                specText: entry.specText || ''
              }
            })
            api.createOrder({ tableNo, items, peopleCount: null })
              .then(() => {
                this.showCart = false
                this.updateCart({})
                uni.showToast({ title: '下单成功', icon: 'success' })
                setTimeout(() => uni.reLaunch({ url: '/pages/order/order' }), 800)
              })
              .catch(() => {})
          }
        })
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.scan-tip {
  position: fixed;
  z-index: 999;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60rpx;
  box-sizing: border-box;
  background: rgba(0, 0, 0, 0.18);
}
.scan-tip-card {
  display: flex;
  align-items: center;
  gap: 20rpx;
  width: 100%;
  max-width: 660rpx;
  padding: 36rpx 30rpx;
  border-radius: 24rpx;
  background: #fff;
  border: 1rpx solid #ffe2c0;
  box-shadow: 0 12rpx 32rpx rgba(0, 0, 0, 0.16);
}
.scan-tip-icon { font-size: 56rpx; }
.scan-tip-title { font-size: 30rpx; font-weight: 700; color: #b35c00; }
.scan-tip-desc { font-size: 24rpx; color: #b07a3c; margin-top: 6rpx; }
.scan-tip-btn {
  flex-shrink: 0;
  padding: 16rpx 30rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #ff8a5c, #ff6b35);
  color: #fff;
  font-size: 26rpx;
  font-weight: 600;
}
.menu-page {
  height: 100vh;
  display: flex;
  background: #f6f7fb;
  padding-bottom: 110rpx;
  box-sizing: border-box;
  min-height: 0;
  /* #ifdef H5 */
  height: calc(100vh - 44px - env(safe-area-inset-top));
  /* #endif */
}

.cat-list {
  width: 180rpx;
  height: 100%;
  background: #f0f1f3;
  flex-shrink: 0;
}

.cat-item {
  padding: 32rpx 16rpx;
  font-size: 27rpx;
  color: #555;
  text-align: center;
  position: relative;
}

.cat-item.active {
  background: #fff;
  color: #2f80ed;
  font-weight: 600;
}

.cat-item.active::before {
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

.goods-list {
  flex: 1;
  height: 100%;
  padding: 0 20rpx;
  box-sizing: border-box;
  min-width: 0;
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
  background: #fff1eb;
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

/* 名称与热销标签同一行 */
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
  color: #999;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  word-break: break-all;
}

/* 热销推荐标记 */
.hot-tag {
  flex-shrink: 0;
  padding: 1rpx 10rpx;
  font-size: 20rpx;
  color: #ff6b35;
  background: #fff1ea;
  border-radius: 6rpx;
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

.sales {
  font-size: 22rpx;
  color: #bbb;
}

/* 起购份数标记（大于 1 时展示） */
.minbuy-tag {
  font-size: 20rpx;
  color: #ff6b35;
  background: #fff1ea;
  border-radius: 6rpx;
  padding: 2rpx 12rpx;
}

/* 底部操作行：减号 / 数量 / 加号 */
.ops {
  margin-top: 16rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.step-btn {
  width: 52rpx;
  height: 52rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34rpx;
  line-height: 1;
}

.step-btn.plus {
  background: #ff6b35;
  color: #fff;
}

.step-btn.minus {
  border: 2rpx solid #ddd;
  color: #666;
}

.step-num {
  min-width: 56rpx;
  text-align: center;
  font-size: 30rpx;
}

/* 购物车面板内的步进器：按钮更小更紧凑 */
.stepper.small .step-btn {
  width: 44rpx;
  height: 44rpx;
  font-size: 30rpx;
}

/* 起购份数 > 1 时以按钮形式加入购物车 */
.min-buy-btn {
  height: 52rpx;
  padding: 0 24rpx;
  border-radius: 999rpx;
  background: #ff6b35;
  color: #fff;
  font-size: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty {
  text-align: center;
  color: #8a8a8a;
  font-size: 26rpx;
  padding: 120rpx 0;
}

/* 列表加载状态：首屏 / 下拉加载更多 */
.list-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80rpx 0;
  color: #8a8a8a;
}

.list-loading.list-loading-more {
  padding: 30rpx 0;
}

.list-loading-text {
  font-size: 26rpx;
  margin-left: 16rpx;
}

.mini-spinner {
  width: 34rpx;
  height: 34rpx;
  border: 4rpx solid rgba(0, 0, 0, 0.14);
  border-top-color: #2f80ed;
  border-radius: 50%;
  animation: spinner-rotate 0.7s linear infinite;
}

/* 到底了 */
.list-end {
  text-align: center;
  color: #b8b8b8;
  font-size: 24rpx;
  padding: 30rpx 0;
}

@keyframes spinner-rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.bottom-space {
  height: 30rpx;
}

/* 购物车 */
.cart-mask {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 110rpx;
  background: rgba(0, 0, 0, 0.4);
  z-index: 100;
}

.cart-panel {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 110rpx;
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
  z-index: 101;
  max-height: 60vh;
  display: flex;
  flex-direction: column;
}

.cart-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 32rpx;
  font-size: 28rpx;
  font-weight: 600;
  border-bottom: 1rpx solid #eee;
}

.cart-clear {
  font-size: 24rpx;
  color: #999;
  font-weight: 400;
}

.cart-list {
  flex: 1;
  padding: 10rpx 32rpx;
}

.cart-item {
  display: flex;
  align-items: center;
  padding: 22rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}

.cart-img {
  width: 72rpx;
  height: 72rpx;
  background: #fff1eb;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40rpx;
  margin-right: 20rpx;
  overflow: hidden;
  flex-shrink: 0;
}

.cart-img-real {
  width: 100%;
  height: 100%;
}

.cart-info {
  flex: 1;
  min-width: 0;
}

.cart-name {
  font-size: 28rpx;
}

.cart-spec {
  font-size: 22rpx;
  color: #8a8a8a;
  margin-top: 6rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cart-item .price {
  margin-right: 24rpx;
}

/* 结算栏 */
.checkout-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: 110rpx;
  background: #fff;
  display: flex;
  align-items: center;
  padding: 0 24rpx;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.05);
  z-index: 200;
}

.cart-entry {
  position: relative;
  font-size: 56rpx;
  margin-right: 24rpx;
}

.cart-icon {
  filter: grayscale(1);
  opacity: 0.5;
}

.cart-icon.active {
  filter: none;
  opacity: 1;
}

.badge {
  position: absolute;
  top: 0;
  right: -12rpx;
  min-width: 34rpx;
  height: 34rpx;
  padding: 0 8rpx;
  border-radius: 999rpx;
  background: #ff3b30;
  color: #fff;
  font-size: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.checkout-amount {
  flex: 1;
}

.total {
  font-size: 26rpx;
  color: #333;
}

.empty-tip {
  font-size: 26rpx;
  color: #bbb;
}

.checkout-btn {
  width: 220rpx;
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  border-radius: 999rpx;
  background: #ff6b35;
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
}

.checkout-btn.disabled {
  background: #ccc;
}
</style>
