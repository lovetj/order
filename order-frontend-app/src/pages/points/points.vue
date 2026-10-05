<template>
  <view class="page">
    <!-- 积分头部 -->
    <view class="points-header">
      <view class="points-left">
        <view class="points-label">我的积分</view>
        <view class="points-value">{{myPoints}}</view>
      </view>
      <view class="points-right" @click="goMyExchanges">
        <text class="record-icon">📋</text>
        <text class="record-text">兑换记录</text>
      </view>
    </view>

    <view class="tip">1 元 = 1 积分，消费自动累积</view>

    <view class="goods-grid">
      <view class="goods-item" v-for="item in goods" :key="item.id">
        <view class="goods-thumb">
          <image v-if="item.imageUrl" class="goods-thumb-img" :src="item.image" mode="aspectFill"></image>
          <text v-else>{{item.image}}</text>
        </view>
        <view class="goods-name">{{item.name}}</view>
        <view class="goods-desc">{{item.description}}</view>
        <view class="goods-stock">
          <text v-if="item.stock < 0">不限量</text>
          <text v-else>剩余 {{item.stock}} 件</text>
        </view>
        <view class="goods-bottom">
          <view class="goods-points">{{item.points}} 积分</view>
          <view class="exchange-btn" :class="{ disabled: myPoints < item.points }" @click="exchange(item.id)">兑换</view>
        </view>
      </view>
    </view>

    <view v-if="goods.length === 0" class="empty">暂无可兑换商品</view>
  </view>
</template>

<script>
// 积分商城
const app = getApp()
import api from '@/api/index'

export default {
  data() {
    return {
      goods: [],
      myPoints: 0,
      loading: false
    }
  },

  onShow() {
    if (!app.globalData.role) {
      uni.reLaunch({ url: '/pages/role/role' })
      return
    }
    this.loadPoints()
    this.loadGoods()
  },

  methods: {
    loadPoints() {
      api.getMemberInfo().then((data) => {
        if (data) {
          this.myPoints = data.points || 0
        }
      }).catch(() => {})
    },

    loadGoods() {
      this.loading = true
      api.getPointsGoods().then((list) => {
        // 标记是否为真实图片 URL，用于 WXML 区分 image / emoji 渲染
        const goods = (list || []).map((g) => ({
          ...g,
          imageUrl: /^https?:\/\//.test(g.image || '')
        }))
        this.goods = goods
      }).catch(() => {
        this.goods = []
      }).then(() => this.loading = false)
    },

    exchange(id) {
      const item = this.goods.find((g) => g.id === id)
      if (!item) return

      if (this.myPoints < item.points) {
        uni.showModal({
          title: '积分不足',
          content: `兑换「${item.name}」需要 ${item.points} 积分，当前仅 ${this.myPoints} 积分。\n消费即可累积积分（1 元 = 1 积分）。`,
          showCancel: false,
          confirmText: '知道了',
          confirmColor: '#ff6b35'
        })
        return
      }

      uni.showModal({
        title: '确认兑换',
        content: `使用 ${item.points} 积分兑换「${item.name}」？`,
        confirmText: '确认兑换',
        confirmColor: '#d4a24a',
        success: (res) => {
          if (!res.confirm) return
          api.exchangePoints(id, 1).then((record) => {
            uni.showModal({
              title: '兑换成功',
              content: `核销码：${record.verifyCode}\n\n请到店出示核销码领取`,
              showCancel: false,
              confirmText: '知道了',
              confirmColor: '#d4a24a',
              complete: () => {
                this.loadPoints()
                this.loadGoods()
              }
            })
          }).catch(() => {})
        }
      })
    },

    goMyExchanges() {
      uni.navigateTo({ url: '/pages/points-record/points-record' })
    }
  }
}
</script>

<style lang="scss" scoped>
.page {
  padding-bottom: 60rpx;
}

.points-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 50rpx 40rpx 70rpx;
  background: linear-gradient(135deg, #d4a24a, #b8860b);
  color: #fff;
}

.points-label {
  font-size: 26rpx;
  opacity: 0.9;
}

.points-value {
  font-size: 72rpx;
  font-weight: 700;
  margin-top: 8rpx;
}

.points-right {
  text-align: center;
  background: rgba(255, 255, 255, 0.22);
  padding: 18rpx 28rpx;
  border-radius: 999rpx;
}

.record-icon {
  font-size: 36rpx;
  display: block;
}

.record-text {
  font-size: 22rpx;
  display: block;
  margin-top: 6rpx;
}

.tip {
  margin: -40rpx 24rpx 0;
  padding: 24rpx 28rpx;
  background: #fff;
  border-radius: 20rpx;
  font-size: 24rpx;
  color: #8a8a8a;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.06);
}

.goods-grid {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  padding: 24rpx;
}

.goods-item {
  width: 48.5%;
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.05);
}

.goods-thumb {
  height: 180rpx;
  border-radius: 16rpx;
  background: #f7f7f7;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 90rpx;
  overflow: hidden;
}

.goods-thumb-img {
  width: 100%;
  height: 100%;
}

.goods-name {
  font-size: 28rpx;
  font-weight: 600;
  margin-top: 16rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-desc {
  font-size: 22rpx;
  color: #8a8a8a;
  margin-top: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-stock {
  font-size: 22rpx;
  color: #a0a0a0;
  margin-top: 8rpx;
}

.goods-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 18rpx;
}

.goods-points {
  font-size: 28rpx;
  font-weight: 700;
  color: #d4a24a;
}

.exchange-btn {
  padding: 12rpx 28rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #d4a24a, #b8860b);
  color: #fff;
  font-size: 25rpx;
}

.exchange-btn.disabled {
  background: #d8d8d8;
}
</style>
