<template>
  <view class="page">
    <view class="tabs">
      <view
        v-for="item in tabs"
        :key="item.key"
        class="tab"
        :class="{ active: current === item.status }"
        @click="switchTab(item.status)"
      >{{item.name}}</view>
    </view>

    <template v-if="list.length > 0">
      <view class="coupon-card" :class="{ disabled: item.status !== 0 }" v-for="item in list" :key="item.id">
        <view class="coupon-left">
          <view class="coupon-amount">
            <text v-if="item.type === 2" class="num">{{item.discountText}}</text>
            <text v-else class="num">{{item.amount}}</text>
            <text class="unit">{{item.type === 2 ? '折' : '元'}}</text>
          </view>
          <view class="coupon-threshold">{{item.threshold > 0 ? '满' + item.threshold + '可用' : '无门槛'}}</view>
        </view>
        <view class="coupon-right">
          <view class="coupon-name">{{item.name}}</view>
          <view class="coupon-label">{{item.label}}</view>
          <view class="coupon-time">有效期至 {{item.endTime}}</view>
        </view>
        <view class="coupon-status">{{item.statusText}}</view>
      </view>
    </template>

    <view v-else class="empty">
      <view class="empty-icon">🎫</view>
      <view class="empty-text">暂无优惠券</view>
      <view v-if="current === 0" class="empty-btn" @click="goCenter">去领券中心</view>
    </view>

    <view class="bottom-space"></view>
  </view>
</template>

<script>
// 我的优惠券
const app = getApp()
import api from '@/api/index'

const TABS = [
  { key: 'unused', name: '未使用', status: 0 },
  { key: 'used', name: '已使用', status: 1 },
  { key: 'expired', name: '已过期', status: 2 }
]

export default {
  data() {
    return {
      tabs: TABS,
      current: 0,
      list: [],
      loading: false
    }
  },

  onShow() {
    if (!app.globalData.role) {
      uni.reLaunch({ url: '/pages/role/role' })
      return
    }
    this.loadList()
  },

  methods: {
    switchTab(status) {
      this.current = Number(status)
      this.loadList()
    },

    loadList() {
      this.loading = true
      api.getMyCoupons(this.current).then((list) => {
        // 预计算折扣文案，避免 WXML 中浮点运算精度问题
        const coupons = (list || []).map((c) => ({
          ...c,
          discountText: c.type === 2 && c.discount != null
            ? String(Math.round(Number(c.discount) * 100) / 10)
            : ''
        }))
        this.list = coupons
      }).catch(() => {
        this.list = []
      }).then(() => this.loading = false)
    },

    // 去领券中心
    goCenter() {
      uni.navigateTo({ url: '/pages/coupon-center/coupon-center' })
    },

    // 去点餐
    goMenu() {
      uni.reLaunch({ url: '/pages/menu/menu' })
    }
  }
}
</script>

<style lang="scss" scoped>
.tabs {
  display: flex;
  background: #fff;
  position: sticky;
  top: 0;
  z-index: 10;
}

.tab {
  flex: 1;
  text-align: center;
  padding: 28rpx 0;
  font-size: 28rpx;
  color: #666;
  position: relative;
}

.tab.active {
  color: #ff6b35;
  font-weight: 600;
}

.tab.active::after {
  content: '';
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: 8rpx;
  width: 48rpx;
  height: 6rpx;
  border-radius: 6rpx;
  background: #ff6b35;
}

.coupon-card {
  display: flex;
  align-items: center;
  position: relative;
  background: #fff;
  margin: 24rpx;
  border-radius: 20rpx;
  padding: 32rpx 28rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.coupon-card.disabled {
  opacity: 0.5;
}

.coupon-left {
  width: 180rpx;
  text-align: center;
  border-right: 2rpx dashed #eee;
  padding-right: 20rpx;
}

.coupon-amount {
  color: #ff6b35;
  font-weight: 700;
}

.coupon-amount .num {
  font-size: 60rpx;
}

.coupon-amount .unit {
  font-size: 26rpx;
  margin-left: 4rpx;
}

.coupon-threshold {
  font-size: 22rpx;
  color: #999;
  margin-top: 8rpx;
}

.coupon-right {
  flex: 1;
  padding-left: 28rpx;
}

.coupon-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #1f1f1f;
}

.coupon-label {
  font-size: 24rpx;
  color: #ff6b35;
  margin-top: 10rpx;
}

.coupon-time {
  font-size: 22rpx;
  color: #a0a0a0;
  margin-top: 12rpx;
}

.coupon-status {
  position: absolute;
  right: 24rpx;
  top: 24rpx;
  font-size: 22rpx;
  color: #fff;
  background: #ff6b35;
  padding: 4rpx 16rpx;
  border-radius: 999rpx;
}

.coupon-card.disabled .coupon-status {
  background: #c8c8c8;
}

.empty-icon {
  font-size: 100rpx;
  margin-bottom: 20rpx;
}

.empty-text {
  color: #8a8a8a;
  font-size: 28rpx;
  margin-bottom: 32rpx;
}

.empty-btn {
  display: inline-block;
  padding: 18rpx 56rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #ff8a5c, #ff6b35);
  color: #fff;
  font-size: 28rpx;
}

.bottom-space {
  height: 40rpx;
}
</style>
