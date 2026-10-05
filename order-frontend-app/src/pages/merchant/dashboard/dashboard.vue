<template>
  <view class="page">
    <bottom-nav />
    <view class="shop-bar">
      <view>
        <view class="shop-name">{{shop.name}}</view>
        <view class="shop-status">今日经营数据</view>
      </view>
      <view class="status-btn" :class="shop.status === '营业中' ? 'open' : 'closed'" @click="toggleShop">{{shop.status}}</view>
    </view>

    <view class="overview">
      <view class="overview-item" v-for="item in overview" :key="item.label">
        <view class="ov-label">{{item.label}}</view>
        <view class="ov-value" :class="'ov-' + item.color">{{item.value}}</view>
        <view class="ov-extra">{{item.extra}}</view>
      </view>
    </view>

    <view class="card">
      <view class="section-title">近 7 日营业额</view>
      <view class="chart">
        <view class="chart-col" v-for="item in weekSales" :key="item.day">
          <view class="chart-bar-wrap">
            <view class="chart-bar" :style="{ height: item.percent + '%' }"></view>
          </view>
          <view class="chart-day">{{item.day}}</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
const app = getApp()
import api from '@/api/index'

export default {
  data() {
    return {
      shop: { name: '扫码点餐', status: '营业中' },
      overview: [],
      weekSales: []
    }
  },
  onShow() {
    if (app.globalData.role !== 'merchant') {
      uni.reLaunch({ url: '/pages/role/role' })
      return
    }
    // 店家未登录（token 丢失/过期）时回到店家登录页
    if (!app.globalData.isLogin()) {
      uni.reLaunch({ url: '/pages/login/login?role=merchant' })
      return
    }
    this.loadShop()
    this.loadDashboard()
  },
  // 底部导航切换时刷新当前页面数据
  onTabRefresh() {
    if (!app.globalData.isLogin()) return
    this.loadShop()
    this.loadDashboard()
  },
  methods: {
    loadShop() {
      api.getMerchantShopInfo().then((shop) => {
        if (!shop) return
        this.shop = {
          name: shop.name || '扫码点餐',
          status: shop.businessStatus === 1 ? '营业中' : '休息中'
        }
      }).catch(() => {})
    },
    loadDashboard() {
      api.getDashboard().then((data) => {
        if (!data) return
        this.overview = data.overview || []
        this.weekSales = data.weekSales || []
      }).catch(() => {})
    },
    toggleShop() {
      api.toggleBusiness().then(() => {
        const next = this.shop.status === '营业中' ? '休息中' : '营业中'
        this.shop.status = next
        uni.showToast({ title: `已切换为${next}`, icon: 'none' })
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.shop-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 40rpx 36rpx 120rpx;
  background: linear-gradient(135deg, #2f80ed, #1a5fd0);
  color: #fff;
}

.shop-name {
  font-size: 40rpx;
  font-weight: 700;
}

.shop-status {
  font-size: 24rpx;
  opacity: 0.85;
  margin-top: 10rpx;
}

.status-btn {
  padding: 14rpx 32rpx;
  border-radius: 999rpx;
  font-size: 26rpx;
  background: rgba(255, 255, 255, 0.22);
}

.status-btn.closed {
  background: rgba(0, 0, 0, 0.25);
}

.overview {
  display: flex;
  flex-wrap: wrap;
  margin: -80rpx 24rpx 0;
  background: #fff;
  border-radius: 22rpx;
  overflow: hidden;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.06);
}

.overview-item {
  width: 50%;
  padding: 30rpx 28rpx;
  border-bottom: 1rpx solid #f5f5f5;
}

.overview-item:nth-child(odd) {
  border-right: 1rpx solid #f5f5f5;
}

.overview-item:nth-child(3),
.overview-item:nth-child(4) {
  border-bottom: none;
}

.ov-label {
  font-size: 24rpx;
  color: #8a8a8a;
}

.ov-value {
  font-size: 40rpx;
  font-weight: 700;
  margin-top: 10rpx;
}

.ov-primary {
  color: #ff6b35;
}

.ov-blue {
  color: #2f80ed;
}

.ov-green {
  color: #34c759;
}

.ov-orange {
  color: #ff9500;
}

.ov-extra {
  font-size: 22rpx;
  color: #a0a0a0;
  margin-top: 6rpx;
}

.chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  height: 300rpx;
  margin-top: 28rpx;
}

.chart-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
}

.chart-bar-wrap {
  flex: 1;
  width: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.chart-bar {
  width: 40rpx;
  border-radius: 10rpx 10rpx 0 0;
  background: linear-gradient(180deg, #6aa9ff, #2f80ed);
  min-height: 12rpx;
}

.chart-day {
  font-size: 22rpx;
  color: #8a8a8a;
  margin-top: 12rpx;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.more {
  font-size: 25rpx;
  color: #2f80ed;
}

.mini-order {
  display: flex;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}

.mini-order:last-child {
  border-bottom: none;
}

.mini-left {
  width: 220rpx;
}

.mini-table {
  font-size: 28rpx;
  font-weight: 600;
}

.mini-time {
  font-size: 22rpx;
}

.mini-mid {
  flex: 1;
}

.mini-goods {
  font-size: 26rpx;
  color: #666;
}

.mini-right {
  text-align: right;
}

.mini-status {
  font-size: 23rpx;
  margin-top: 8rpx;
}

.status-pending {
  color: #ff9500;
}

.status-cooking {
  color: #2f80ed;
}

.status-done {
  color: #34c759;
}

.mini-empty {
  padding: 40rpx 0;
  text-align: center;
}

.quick-grid {
  display: flex;
  flex-wrap: wrap;
  margin: 24rpx;
  gap: 20rpx;
}

.quick-cell {
  width: calc(25% - 15rpx);
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx 0;
  text-align: center;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.quick-icon {
  font-size: 48rpx;
}

.quick-label {
  font-size: 24rpx;
  margin-top: 12rpx;
}
</style>
