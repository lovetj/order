<template>
  <view class="bottom-nav">
    <view
      v-for="(item, index) in list"
      :key="item.path"
      class="nav-item"
      :class="{ active: current === item.path }"
      @click="go(item.path)"
    >
      <view class="nav-icon">{{item.icon}}</view>
      <view class="nav-text">{{item.text}}</view>
    </view>
  </view>
</template>

<script>
// 跨端底部导航：按角色（顾客 / 店家）渲染不同的导航项
const CUSTOMER_TABS = [
  { path: '/pages/index/index', text: '首页', icon: '🏠' },
  { path: '/pages/menu/menu', text: '点餐', icon: '🍽️' },
  { path: '/pages/order/order', text: '订单', icon: '🧾' },
  { path: '/pages/mine/mine', text: '我的', icon: '👤' }
]

const MERCHANT_TABS = [
  { path: '/pages/merchant/dashboard/dashboard', text: '管理后台', icon: '📊' },
  { path: '/pages/merchant/goods/goods', text: '商品', icon: '📦' },
  { path: '/pages/merchant/orders/orders', text: '订单', icon: '🧾' },
  { path: '/pages/merchant/mine/mine', text: '我的', icon: '🏪' }
]

export default {
  data() {
    return {
      list: [],
      current: ''
    }
  },

  created() {
    const app = getApp()
    const role = (app && app.globalData && app.globalData.role) || uni.getStorageSync('role') || 'customer'
    this.list = role === 'merchant' ? MERCHANT_TABS : CUSTOMER_TABS
    const pages = getCurrentPages()
    const route = pages.length ? '/' + pages[pages.length - 1].route : ''
    this.current = route
  },

  methods: {
    // 页签切换：reLaunch 关闭其他页面，由目标页 onShow 自行刷新数据
    go(path) {
      if (path === this.current) {
        uni.reLaunch({ url: path })
        return
      }
      uni.reLaunch({ url: path })
    }
  }
}
</script>

<style>
.bottom-nav {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 999;
  display: flex;
  background: #ffffff;
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.05);
  padding-bottom: env(safe-area-inset-bottom);
}

.nav-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 12rpx 0 8rpx;
}

.nav-icon {
  font-size: 44rpx;
  line-height: 1.1;
}

.nav-text {
  font-size: 20rpx;
  color: #8a8a8a;
  margin-top: 4rpx;
}

.nav-item.active .nav-text {
  color: #ff6b35;
  font-weight: 600;
}
</style>
