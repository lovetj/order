<template>
  <view class="role-page">
    <view class="hero">
      <view class="logo">🍔</view>
      <view class="title">扫码点餐</view>
      <view class="subtitle">请选择你的身份进入</view>
      <view v-if="tableNo" class="table-tag">当前桌号：{{tableNo}}</view>
    </view>

    <view class="role-list">
      <view class="role-card customer" @click="chooseRole('customer')">
        <view class="role-icon">🙋</view>
        <view class="role-info">
          <view class="role-name">我是顾客</view>
          <view class="role-desc">浏览菜单 · 在线点餐 · 查看订单</view>
        </view>
        <view class="arrow">›</view>
      </view>

      <view class="role-card merchant" @click="chooseRole('merchant')">
        <view class="role-icon">🏪</view>
        <view class="role-info">
          <view class="role-name">我是店家</view>
          <view class="role-desc">管理后台 · 商品维护 · 处理订单</view>
        </view>
        <view class="arrow">›</view>
      </view>
    </view>

    <view class="footer">进入后可在「我的」中切换身份</view>
  </view>
</template>

<script>
const app = getApp()

export default {
  data() {
    return {
      tableNo: ''
    }
  },

  onLoad() {
    this.tableNo = app.globalData.tableNo || ''
  },

  methods: {
    chooseRole(role) {
      // 顾客 -> 快速登录；店家 -> 账号密码登录
      uni.navigateTo({ url: `/pages/login/login?role=${role}` })
    }
  }
}
</script>

<style lang="scss" scoped>
.role-page {
  min-height: 100vh;
  padding: 200rpx 60rpx 80rpx;
  background: linear-gradient(180deg, #fff4ef 0%, #f5f6f8 45%);
  display: flex;
  flex-direction: column;
}

.hero {
  text-align: center;
  margin-bottom: 90rpx;
}

.logo {
  font-size: 130rpx;
  line-height: 1;
  margin-bottom: 24rpx;
}

.title {
  font-size: 52rpx;
  font-weight: 700;
  color: #1f1f1f;
  letter-spacing: 4rpx;
}

.subtitle {
  font-size: 26rpx;
  color: #8a8a8a;
  margin-top: 14rpx;
}

.table-tag {
  display: inline-block;
  margin-top: 24rpx;
  padding: 8rpx 26rpx;
  background: #fff1eb;
  color: #ff6b35;
  border-radius: 999rpx;
  font-size: 24rpx;
}

.role-list {
  display: flex;
  flex-direction: column;
  gap: 32rpx;
}

.role-card {
  display: flex;
  align-items: center;
  padding: 44rpx 36rpx;
  border-radius: 26rpx;
  background: #fff;
  box-shadow: 0 8rpx 30rpx rgba(0, 0, 0, 0.06);
}

.role-card:active {
  transform: scale(0.98);
}

.role-card.customer {
  border-left: 10rpx solid #ff6b35;
}

.role-card.merchant {
  border-left: 10rpx solid #2f80ed;
}

.role-icon {
  font-size: 72rpx;
  margin-right: 28rpx;
}

.role-info {
  flex: 1;
}

.role-name {
  font-size: 36rpx;
  font-weight: 600;
  color: #1f1f1f;
}

.role-desc {
  font-size: 24rpx;
  color: #8a8a8a;
  margin-top: 10rpx;
}

.arrow {
  font-size: 52rpx;
  color: #c8c8c8;
}

.footer {
  margin-top: auto;
  text-align: center;
  font-size: 22rpx;
  color: #b0b0b0;
}
</style>
