<template>
  <view class="menu-page">
    <bottom-nav />
    <!-- 左侧分类 -->
    <scroll-view class="cat-list" scroll-y>
      <view
        class="cat-item"
        :class="{ active: currentCategory === 'all' }"
        @click="switchCategory('all')"
      >热销</view>
      <view
        v-for="item in categories"
        :key="item.id"
        class="cat-item"
        :class="{ active: currentCategory === item.id }"
        @click="switchCategory(item.id)"
      >{{item.name}}</view>
    </scroll-view>

    <!-- 右侧商品 -->
    <scroll-view class="goods-list" scroll-y>
      <view class="goods-card" v-for="item in goodsList" :key="item.id">
        <view class="goods-img">
          <image v-if="item.hasImage" class="goods-img-real" :src="item.imageUrl" mode="aspectFill"></image>
          <text v-else>{{item.image}}</text>
        </view>
        <view class="goods-main">
          <view class="goods-name">{{item.name}}</view>
          <view class="goods-desc">{{item.desc}}</view>
          <view class="goods-sales">已售 {{item.sales}}</view>
          <view class="goods-row">
            <text class="price">¥{{item.price}}</text>
            <view class="stepper">
              <view v-if="item.count > 0" class="step-btn minus" @click="minusItem(item.key)">−</view>
              <text v-if="item.count > 0" class="step-num">{{item.count}}</text>
              <!-- 起购份数 > 1 时展示「*份起购」，否则展示加号 -->
              <view
                v-if="item.minBuy > 1"
                class="min-buy-tag"
                @click="addItem(item.id)"
              >{{item.minBuy}}份起购</view>
              <view v-else class="step-btn plus" @click="addItem(item.id)">＋</view>
            </view>
          </view>
        </view>
      </view>
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

export default {
  data() {
    return {
      categories: [],
      goods: [],
      currentCategory: '',
      goodsList: [],
      cart: {},          // { dishId: count }
      cartList: [],
      cartCount: 0,
      cartAmount: 0,
      showCart: false
    }
  },

  onShow() {
    if (app.globalData.role !== 'customer') {
      uni.reLaunch({ url: '/pages/role/role' })
      return
    }
    if (!app.globalData.isLogin()) {
      uni.reLaunch({ url: '/pages/login/login?role=customer' })
      return
    }
    if (!this.categories.length) {
      this.loadData()
    } else {
      // 回到页面时同步后端最新菜品（价格/库存可能变化）
      this.loadGoods()
    }
  },

  methods: {
    // 底部导航切换时刷新当前页面数据
    onTabRefresh() {
      if (!app.globalData.isLogin()) return
      if (!this.categories.length) {
        this.loadData()
      } else {
        this.loadGoods()
      }
    },

    // 加载分类 + 全部菜品 + 规格标记
    loadData() {
      Promise.all([api.getCategories(), api.getDishes({ categoryId: 'all' })]).then(([categories, goods]) => {
        const firstId = categories && categories.length ? categories[0].id : 'all'
        this.categories = categories || []
        this.goods = (goods || []).map((g) => this.decorateImage(g))
        this.currentCategory = firstId
        this.buildList()
        this.markSpecDishes()
      }).catch(() => {
        this.categories = []
        this.goods = []
      })
    },

    /**
     * 标记哪些菜品带规格（带规格的点击 + 需弹窗选择）
     * 后端列表接口不返回 specs，这里逐个查询详情较重，
     * 优化方案：由后端在列表接口返回 hasSpec 字段；当前先按需懒加载
     */
    markSpecDishes() {
      // 简洁做法：列表接口暂不含 specs，用户点 + 时统一走规格页，
      // 规格页无规格时直接返回并加入购物车。
      this.dishHasSpec = {}
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

    // 仅刷新菜品（保留分类与购物车）
    loadGoods() {
      api.getDishes({ categoryId: 'all' }).then((goods) => {
        this.goods = (goods || []).map((g) => this.decorateImage(g))
        this.buildList()
      }).catch(() => {})
    },

    buildList() {
      const { goods, currentCategory, cart } = this
      const list = (currentCategory === 'all'
        ? [...goods].sort((a, b) => (b.sales || 0) - (a.sales || 0))
        : goods.filter((g) => g.categoryId === currentCategory))
        // 只展示上架菜品
        .filter((g) => g.status)

      // 购物车 key 为「dishId|specText」，此处按 dishId 汇总每种菜品的总份数
      const countByDish = {}
      Object.keys(cart).forEach((key) => {
        const entry = cart[key]
        if (!entry) return
        countByDish[entry.id] = (countByDish[entry.id] || 0) + entry.count
      })

      this.goodsList = list.map((g) => ({
        ...g,
        count: countByDish[g.id] || 0
      }))
    },

    switchCategory(id) {
      this.currentCategory = id
      this.buildList()
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
      const dish = this.goods.find((g) => g.id === dishId)
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
.menu-page {
  height: 100vh;
  display: flex;
  background: #f6f7fb;
  padding-bottom: 110rpx;
}

.cat-list {
  width: 190rpx;
  height: 100%;
  background: #f0f1f5;
}

.cat-item {
  padding: 36rpx 16rpx;
  font-size: 26rpx;
  color: #666;
  text-align: center;
}

.cat-item.active {
  background: #fff;
  color: #ff6b35;
  font-weight: 600;
  position: relative;
}

.cat-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 8rpx;
  height: 40rpx;
  background: #ff6b35;
  border-radius: 0 8rpx 8rpx 0;
}

.goods-list {
  flex: 1;
  height: 100%;
  padding: 20rpx;
}

.goods-card {
  display: flex;
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.goods-img {
  width: 140rpx;
  height: 140rpx;
  background: #fff1eb;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 72rpx;
  margin-right: 24rpx;
  overflow: hidden;
  flex-shrink: 0;
}

.goods-img-real {
  width: 100%;
  height: 100%;
}

.goods-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.goods-name {
  font-size: 30rpx;
  font-weight: 600;
}

.goods-desc {
  font-size: 22rpx;
  color: #999;
  margin-top: 8rpx;
}

.goods-sales {
  font-size: 22rpx;
  color: #bbb;
  margin-top: 8rpx;
}

.goods-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
}

.goods-row .price {
  font-size: 34rpx;
}

.stepper {
  display: flex;
  align-items: center;
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

.stepper.small .step-btn {
  width: 44rpx;
  height: 44rpx;
  font-size: 30rpx;
}

.step-num {
  min-width: 56rpx;
  text-align: center;
  font-size: 30rpx;
}

.min-buy-tag {
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
