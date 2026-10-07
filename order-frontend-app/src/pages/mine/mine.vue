<template>
  <view class="container layout-page">
    <bottom-nav />
    <!-- 未识别店铺/桌号时提示扫码 -->
    <view v-if="needScan" class="scan-tip">
      <view class="scan-tip-card">
        <view class="scan-tip-icon">📷</view>
        <view class="scan-tip-text">
          <view class="scan-tip-title">请先扫描桌位二维码</view>
          <view class="scan-tip-desc">未识别到店铺与桌号，订单/积分等信息不可用</view>
        </view>
        <view class="scan-tip-btn" @click="scanThenReload">立即扫码</view>
      </view>
    </view>
    <scroll-view class="layout-body" scroll-y>
    <view class="user-card" @click="goMember">
      <view class="avatar">
        <image v-if="isAvatarUrl" class="avatar-img" :src="userInfo.avatar" mode="aspectFill" />
        <text v-else>{{userInfo.avatar || '🙋'}}</text>
      </view>
      <view class="user-info">
        <view class="nickname">{{userInfo.nickName || '用餐用户'}}</view>
        <view class="user-tip">{{userInfo.memberLevel || '普通会员'}} · 桌号 {{tableNo}}</view>
      </view>
      <view class="member-entry">
        <view class="member-points">{{points}} 积分</view>
        <view class="member-coupon">{{couponCount}} 张券</view>
      </view>
    </view>

    <view class="stats">
      <view class="stat" v-for="item in stats" :key="item.label">
        <view class="stat-value">{{item.value}}</view>
        <view class="stat-label">{{item.label}}</view>
      </view>
    </view>

    <view class="menu-card">
      <view class="menu-item" v-for="item in menus" :key="item.key" @click="handleMenu(item.key)">
        <view class="menu-icon">{{item.icon}}</view>
        <view class="menu-name">{{item.name}}</view>
        <view v-if="item.key === 'coupon' && couponCount > 0" class="menu-badge">{{couponCount}}</view>
        <view class="menu-arrow">›</view>
      </view>
    </view>

    <view class="btn-plain action-btn" @click="logout">退出登录</view>
    </scroll-view>
  </view>
</template>

<script>
// 顾客端我的
const app = getApp()
import api from '@/api/index'
import { hasCustomerContext, scanOrderContext } from '@/utils/scan'

export default {
  data() {
    return {
      userInfo: {},
      // 头像是否为真实图片 URL（否则按 emoji/文字渲染）
      isAvatarUrl: false,
      tableNo: '',
      stats: [
        { label: '待接单', value: 0 },
        { label: '制作中', value: 0 },
        { label: '已完成', value: 0 }
      ],
      menus: [
        { icon: '📋', name: '我的订单', key: 'order' },
        { icon: '👑', name: '会员中心', key: 'member' },
        { icon: '🎁', name: '积分商城', key: 'points' },
        { icon: '🎫', name: '我的优惠券', key: 'coupon' },
        { icon: '🏷️', name: '领券中心', key: 'couponCenter' },
        { icon: '📍', name: '收货地址', key: 'address' },
        { icon: '💬', name: '联系客服', key: 'service' }
      ],
      couponCount: 0,
      points: 0,
      needScan: false
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
    this.needScan = !hasCustomerContext(app)
    if (this.needScan) return
    const userInfo = app.globalData.userInfo || {}
    this.userInfo = userInfo
    this.isAvatarUrl = /^https?:\/\//.test(userInfo.avatar || '')
    this.tableNo = app.globalData.tableNo || '未获取'
    this.loadUserInfo()
    this.loadCounts()
    this.loadMember()
  },

  methods: {
    // 底部导航切换时刷新当前页面数据
    onTabRefresh() {
      if (!app.globalData.isLogin()) return
      this.needScan = !hasCustomerContext(app)
      if (this.needScan) return
      const userInfo = app.globalData.userInfo || {}
      this.userInfo = userInfo
      this.isAvatarUrl = /^https?:\/\//.test(userInfo.avatar || '')
      this.tableNo = app.globalData.tableNo || '未获取'
      this.loadUserInfo()
      this.loadCounts()
      this.loadMember()
    },

    // 扫码识别店铺/桌号后刷新页面
    async scanThenReload() {
      const { ok } = await scanOrderContext(app)
      if (!ok) return
      this.needScan = false
      const userInfo = app.globalData.userInfo || {}
      this.tableNo = app.globalData.tableNo || '未获取'
      this.loadUserInfo()
      this.loadCounts()
      this.loadMember()
    },

    // 会员信息（积分 / 优惠券数量 / 等级）
    loadMember() {
      if (!app.globalData.isLogin()) return
      api.getMemberInfo().then((data) => {
        if (!data) return
        this.couponCount = data.couponCount || 0
        this.points = data.points || 0
      }).catch(() => {})
    },

    // 进入会员中心
    goMember() {
      uni.navigateTo({ url: '/pages/member/member' })
    },

    // 拉取最新用户信息
    loadUserInfo() {
      if (!app.globalData.isLogin()) return
      api.getUserInfo().then((user) => {
        if (!user) return
        const userInfo = {
          nickName: user.nickname || '用餐用户',
          avatar: user.avatar || '🙋',
          memberLevel: user.memberLevel || '普通会员'
        }
        app.globalData.userInfo = userInfo
        this.userInfo = userInfo
        this.isAvatarUrl = /^https?:\/\//.test(userInfo.avatar || '')
      }).catch(() => {})
    },

    // 订单各状态数量
    loadCounts() {
      if (!app.globalData.isLogin()) return
      api.getOrderCounts().then((counts) => {
        if (!counts) return
        this.stats = [
          { label: '待接单', value: counts.pending || 0 },
          { label: '制作中', value: counts.cooking || 0 },
          { label: '已完成', value: counts.done || 0 }
        ]
      }).catch(() => {})
    },

    handleMenu(key) {
      // 未识别店铺/桌号时，拦截需要三要素登录态的功能，引导先扫码
      const needContextKeys = ['order', 'coupon', 'member', 'points', 'couponCenter']
      if (this.needScan && needContextKeys.includes(key)) {
        uni.showToast({ title: '请先扫描桌位二维码', icon: 'none' })
        this.scanThenReload()
        return
      }
      if (key === 'order') {
        uni.reLaunch({ url: '/pages/order/order' })
        return
      }
      if (key === 'coupon') {
        uni.navigateTo({ url: '/pages/coupon/coupon' })
        return
      }
      if (key === 'couponCenter') {
        uni.navigateTo({ url: '/pages/coupon-center/coupon-center' })
        return
      }
      if (key === 'member') {
        uni.navigateTo({ url: '/pages/member/member' })
        return
      }
      if (key === 'points') {
        uni.navigateTo({ url: '/pages/points/points' })
        return
      }
      if (key === 'address') {
        uni.chooseAddress({
          fail: () => uni.showToast({ title: '未授权或暂不可用', icon: 'none' })
        })
        return
      }
      uni.showToast({ title: '功能开发中', icon: 'none' })
    },

    logout() {
      uni.showModal({
        title: '提示',
        content: '确定退出登录吗？',
        success: (res) => {
          if (res.confirm) {
            app.globalData.logout()
            uni.reLaunch({ url: '/pages/login/login?role=customer' })
          }
        }
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
.user-card {
  display: flex;
  align-items: center;
  padding: 60rpx 40rpx 50rpx;
  background: linear-gradient(135deg, #ff6b35, #ff9a62);
  color: #fff;
  border-radius: 0 0 40rpx 40rpx;
}

.member-entry {
  text-align: right;
}

.member-points {
  font-size: 28rpx;
  font-weight: 700;
}

.member-coupon {
  font-size: 22rpx;
  opacity: 0.9;
  margin-top: 10rpx;
}

.menu-badge {
  min-width: 36rpx;
  height: 36rpx;
  padding: 0 10rpx;
  border-radius: 999rpx;
  background: #ff3b30;
  color: #fff;
  font-size: 22rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12rpx;
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 60rpx;
  margin-right: 28rpx;
  overflow: hidden;
}

.avatar-img {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
}

.nickname {
  font-size: 36rpx;
  font-weight: 700;
}

.user-tip {
  font-size: 24rpx;
  opacity: 0.9;
  margin-top: 12rpx;
}

.stats {
  display: flex;
  background: #fff;
  margin: -30rpx 24rpx 0;
  border-radius: 20rpx;
  padding: 32rpx 0;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.06);
}

.stat {
  flex: 1;
  text-align: center;
}

.stat-value {
  font-size: 36rpx;
  font-weight: 700;
  color: #333;
}

.stat-label {
  font-size: 24rpx;
  color: #999;
  margin-top: 10rpx;
}

.menu-card {
  background: #fff;
  margin: 24rpx;
  border-radius: 20rpx;
  padding: 0 28rpx;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 32rpx 0;
  border-bottom: 1rpx solid #f4f4f4;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-icon {
  font-size: 40rpx;
  margin-right: 24rpx;
}

.menu-name {
  flex: 1;
  font-size: 30rpx;
}

.menu-arrow {
  color: #ccc;
  font-size: 44rpx;
}

.action-btn {
  margin: 24rpx;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  border-radius: 999rpx;
  font-size: 30rpx;
}
</style>
