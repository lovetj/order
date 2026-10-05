<template>
  <view class="page">
    <!-- 会员卡 -->
    <view class="member-card">
      <view class="member-top">
        <view class="avatar">{{member.avatar}}</view>
        <view class="member-info">
          <view class="nick">{{member.nickName}}</view>
          <view class="level-tag">{{member.memberLevel}}</view>
        </view>
      </view>

      <view class="progress-wrap">
        <view class="progress-head">
          <text class="progress-text">{{member.nextLevelName}}</text>
          <text class="progress-need">
            {{member.nextLevelNeed > 0 ? '还需消费 ¥' + member.nextLevelNeed : '已达最高等级'}}
          </text>
        </view>
        <view class="progress-bar-bg">
          <view class="progress-bar" :style="{ width: member.progress + '%' }"></view>
        </view>
      </view>
    </view>

    <!-- 积分与统计 -->
    <view class="stat-card">
      <view class="stat-item" @click="goPointsMall">
        <view class="stat-value">{{member.points}}</view>
        <view class="stat-label">积分</view>
      </view>
      <view class="stat-item" @click="goCoupon">
        <view class="stat-value">{{member.couponCount}}</view>
        <view class="stat-label">优惠券</view>
      </view>
      <view class="stat-item" @click="goOrders">
        <view class="stat-value">{{member.orderCount}}</view>
        <view class="stat-label">累计订单</view>
      </view>
      <view class="stat-item">
        <view class="stat-value">¥{{member.totalConsume}}</view>
        <view class="stat-label">累计消费</view>
      </view>
    </view>

    <!-- 积分说明 -->
    <view class="card rules">
      <view class="section-title">会员权益</view>
      <view class="rule-item"><text class="dot">·</text>每消费 1 元获得 1 积分</view>
      <view class="rule-item"><text class="dot">·</text>100 积分可抵扣 1 元</view>
      <view class="rule-item"><text class="dot">·</text>累计消费 500 / 2000 / 5000 元升级白银 / 黄金 / 钻石会员</view>
      <view class="rule-item"><text class="dot">·</text>会员等级越高，可领取的专属优惠券越多</view>
    </view>

    <!-- 快捷入口 -->
    <view class="card entry-card" @click="goPointsMall">
      <view class="entry-left">
        <text class="entry-icon">🎁</text>
        <text class="entry-text">积分商城</text>
      </view>
      <text class="arrow">›</text>
    </view>
    <view class="card entry-card" @click="goCouponCenter">
      <view class="entry-left">
        <text class="entry-icon">🎫</text>
        <text class="entry-text">领券中心</text>
      </view>
      <text class="arrow">›</text>
    </view>
    <view class="card entry-card" @click="goCoupon">
      <view class="entry-left">
        <text class="entry-icon">📋</text>
        <text class="entry-text">我的优惠券</text>
      </view>
      <text class="arrow">›</text>
    </view>
  </view>
</template>

<script>
// 会员中心
const app = getApp()
import api from '@/api/index'

export default {
  data() {
    return {
      member: {
        nickName: '用餐用户',
        avatar: '🙋',
        memberLevel: '普通会员',
        points: 0,
        totalConsume: 0,
        orderCount: 0,
        nextLevelName: '',
        nextLevelNeed: 0,
        progress: 0,
        couponCount: 0
      },
      loading: false
    }
  },

  onShow() {
    if (!app.globalData.role) {
      uni.reLaunch({ url: '/pages/role/role' })
      return
    }
    this.loadMember()
  },

  methods: {
    loadMember() {
      this.loading = true
      api.getMemberInfo().then((data) => {
        if (!data) return
        this.member = {
          nickName: data.nickName || '用餐用户',
          avatar: data.avatar || '🙋',
          memberLevel: data.memberLevel || '普通会员',
          points: data.points || 0,
          totalConsume: data.totalConsume || 0,
          orderCount: data.orderCount || 0,
          nextLevelName: data.nextLevelName || '',
          nextLevelNeed: data.nextLevelNeed || 0,
          progress: data.progress || 0,
          couponCount: data.couponCount || 0
        }
      }).catch(() => {}).then(() => this.loading = false)
    },

    goCoupon() {
      uni.navigateTo({ url: '/pages/coupon/coupon' })
    },

    goCouponCenter() {
      uni.navigateTo({ url: '/pages/coupon-center/coupon-center' })
    },

    goPointsMall() {
      uni.navigateTo({ url: '/pages/points/points' })
    },

    goOrders() {
      uni.reLaunch({ url: '/pages/order/order' })
    }
  }
}
</script>

<style lang="scss" scoped>
.member-card {
  margin: 24rpx;
  padding: 44rpx 36rpx;
  border-radius: 26rpx;
  background: linear-gradient(135deg, #d4a24a, #b8860b);
  color: #fff;
  box-shadow: 0 12rpx 30rpx rgba(184, 134, 11, 0.3);
}

.member-top {
  display: flex;
  align-items: center;
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 64rpx;
  margin-right: 28rpx;
}

.nick {
  font-size: 38rpx;
  font-weight: 700;
}

.level-tag {
  display: inline-block;
  margin-top: 14rpx;
  padding: 6rpx 22rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.25);
  font-size: 23rpx;
}

.progress-wrap {
  margin-top: 40rpx;
}

.progress-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 24rpx;
  opacity: 0.92;
  margin-bottom: 14rpx;
}

.progress-bar-bg {
  height: 16rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.28);
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  border-radius: 999rpx;
  background: #fff;
  min-width: 8rpx;
  transition: width 0.4s;
}

.stat-card {
  display: flex;
  background: #fff;
  margin: -30rpx 24rpx 0;
  border-radius: 20rpx;
  padding: 34rpx 0;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.06);
}

.stat-item {
  flex: 1;
  text-align: center;
  border-right: 1rpx solid #f0f0f0;
}

.stat-item:last-child {
  border-right: none;
}

.stat-value {
  font-size: 36rpx;
  font-weight: 700;
  color: #1f1f1f;
}

.stat-label {
  font-size: 23rpx;
  color: #8a8a8a;
  margin-top: 8rpx;
}

.rules {
  padding: 28rpx;
}

.rule-item {
  display: flex;
  margin-top: 18rpx;
  font-size: 25rpx;
  color: #666;
  line-height: 1.5;
}

.dot {
  color: #d4a24a;
  margin-right: 12rpx;
}

.entry-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx 28rpx;
}

.entry-left {
  display: flex;
  align-items: center;
}

.entry-icon {
  font-size: 40rpx;
  margin-right: 22rpx;
}

.entry-text {
  font-size: 29rpx;
}

.arrow {
  color: #c8c8c8;
  font-size: 44rpx;
}
</style>
