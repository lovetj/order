<template>
  <view class="page">
    <template v-if="list.length > 0">
      <view class="record-card" v-for="item in list" :key="item.id">
        <view class="record-thumb">
          <image v-if="item.imageUrl" class="record-thumb-img" :src="item.goodsImage" mode="aspectFill"></image>
          <text v-else>{{item.goodsImage}}</text>
        </view>
        <view class="record-info">
          <view class="record-name">{{item.goodsName}}</view>
          <view class="record-meta">兑换单号：{{item.exchangeNo}}</view>
          <view class="record-meta">消耗积分：{{item.points}} · 数量 x{{item.quantity}}</view>
          <view class="record-time">{{item.createTime}}</view>
        </view>
        <view class="record-right">
          <view class="record-status" :class="'status-' + item.status">
            {{item.status === 0 ? '待发放' : (item.status === 1 ? '已发放' : '已核销')}}
          </view>
          <view
            v-if="item.status !== 2"
            class="code-btn"
            @click="showCode(item.verifyCode, item.goodsName)"
          >出示核销码</view>
        </view>
      </view>
    </template>

    <view v-else class="empty">
      <view class="empty-icon">🎁</view>
      <view class="empty-text">暂无兑换记录</view>
      <view class="empty-btn" @click="goPoints">去积分商城</view>
    </view>
  </view>
</template>

<script>
// 我的兑换记录
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
      api.getMyExchanges().then((list) => {
        // 标记是否为真实图片 URL，用于 WXML 区分 image / emoji 渲染
        const records = (list || []).map((r) => ({
          ...r,
          imageUrl: /^https?:\/\//.test(r.goodsImage || '')
        }))
        this.list = records
      }).catch(() => {
        this.list = []
      }).then(() => this.loading = false)
    },

    showCode(code, name) {
      uni.showModal({
        title: '核销码',
        content: `商品：${name}\n核销码：${code}\n\n请到店出示核销码领取`,
        showCancel: false,
        confirmText: '知道了',
        confirmColor: '#d4a24a'
      })
    },

    goPoints() {
      uni.navigateTo({ url: '/pages/points/points' })
    }
  }
}
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx;
}

.record-card {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 4rpx 14rpx rgba(0, 0, 0, 0.04);
}

.record-thumb {
  width: 120rpx;
  height: 120rpx;
  border-radius: 16rpx;
  background: #f7f7f7;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 60rpx;
  flex-shrink: 0;
  overflow: hidden;
}

.record-thumb-img {
  width: 100%;
  height: 100%;
}

.record-info {
  flex: 1;
  margin-left: 22rpx;
  min-width: 0;
}

.record-name {
  font-size: 29rpx;
  font-weight: 600;
}

.record-meta {
  font-size: 22rpx;
  color: #8a8a8a;
  margin-top: 8rpx;
}

.record-time {
  font-size: 21rpx;
  color: #b0b0b0;
  margin-top: 8rpx;
}

.record-right {
  text-align: right;
  flex-shrink: 0;
}

.record-status {
  font-size: 24rpx;
  font-weight: 600;
}

.status-0 {
  color: #ff9500;
}

.status-1 {
  color: #34c759;
}

.status-2 {
  color: #999;
}

.code-btn {
  margin-top: 16rpx;
  padding: 10rpx 24rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #d4a24a, #b8860b);
  color: #fff;
  font-size: 23rpx;
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
  background: linear-gradient(135deg, #d4a24a, #b8860b);
  color: #fff;
  font-size: 28rpx;
}
</style>
