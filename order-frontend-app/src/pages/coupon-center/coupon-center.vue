<template>
  <view class="page">
    <view class="header">
      <view class="header-title">领券中心</view>
      <view class="header-sub">领取后可在「我的-优惠券」查看</view>
      <view class="header-link" @click="goMyCoupons">我的优惠券 ›</view>
    </view>

    <template v-if="list.length > 0">
      <view class="coupon-card" v-for="item in list" :key="item.id">
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
          <view class="coupon-desc">{{item.description}}</view>
          <view class="coupon-meta">
            <text v-if="item.validDays > 0">领取后 {{item.validDays}} 天内有效</text>
            <text v-else>有效期至 {{item.endTime}}</text>
            <text class="coupon-limit" v-if="item.perLimit > 1">每人限领 {{item.perLimit}} 张</text>
          </view>
        </view>
        <view class="receive-btn" @click="receive(item.id)">立即领取</view>
      </view>
    </template>

    <view v-else class="empty">
      <view class="empty-icon">🎫</view>
      <view class="empty-text">暂无可领取的优惠券</view>
    </view>
  </view>
</template>

<script>
// 领券中心
const app = getApp()
import api from '@/api/index'

export default {
  data() {
    return {
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
    loadList() {
      this.loading = true
      api.getAvailableCoupons().then((list) => {
        // 预计算折扣文案，避免 WXML 中浮点运算精度问题（如 0.88*10 = 8.799999）
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

    receive(id) {
      api.receiveCoupon(id).then(() => {
        uni.showToast({ title: '领取成功', icon: 'success' })
        this.loadList()
      }).catch(() => {})
    },

    // 券面文案
    labelOf(item) {
      if (item.type === 2) {
        return `${item.discount * 10} 折`
      }
      return `减 ${item.amount} 元`
    },

    goMyCoupons() {
      uni.navigateTo({ url: '/pages/coupon/coupon' })
    }
  }
}
</script>

<style lang="scss" scoped>
.header {
  position: relative;
  padding: 50rpx 40rpx 60rpx;
  background: linear-gradient(135deg, #ff8a5c, #ff6b35);
  color: #fff;
}

.header-title {
  font-size: 44rpx;
  font-weight: 700;
}

.header-sub {
  font-size: 24rpx;
  opacity: 0.9;
  margin-top: 14rpx;
}

.header-link {
  position: absolute;
  right: 40rpx;
  bottom: 30rpx;
  font-size: 26rpx;
  background: rgba(255, 255, 255, 0.25);
  padding: 10rpx 26rpx;
  border-radius: 999rpx;
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
  padding: 0 24rpx;
  min-width: 0;
}

.coupon-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #1f1f1f;
}

.coupon-desc {
  font-size: 23rpx;
  color: #8a8a8a;
  margin-top: 10rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.coupon-meta {
  font-size: 22rpx;
  color: #a0a0a0;
  margin-top: 12rpx;
}

.coupon-limit {
  margin-left: 16rpx;
  color: #ff9500;
}

.receive-btn {
  flex-shrink: 0;
  padding: 16rpx 30rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #ff8a5c, #ff6b35);
  color: #fff;
  font-size: 26rpx;
  font-weight: 600;
}

.empty-icon {
  font-size: 100rpx;
  margin-bottom: 20rpx;
}
</style>
