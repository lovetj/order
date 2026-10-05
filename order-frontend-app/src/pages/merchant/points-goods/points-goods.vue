<template>
  <view class="page">
    <!-- 核销区 -->
    <view class="verify-card">
      <view class="verify-title">🎫 核销码核销</view>
      <view class="verify-row">
        <input
          class="verify-input"
          placeholder="请输入顾客出示的核销码"
          :value="verifyCode"
          @input="onVerifyInput"
        />
        <view class="verify-btn" @click="doVerify">核销</view>
      </view>
    </view>

    <view class="tip">共 {{list.length}} 个积分商品</view>

    <view class="goods-item" v-for="item in list" :key="item.id">
      <view class="thumb">
        <image v-if="item.imageUrl" class="thumb-img" :src="item.image" mode="aspectFill"></image>
        <text v-else>{{item.image}}</text>
      </view>
      <view class="info">
        <view class="name">{{item.name}}</view>
        <view class="desc text-sub">{{item.description}}</view>
        <view class="meta">
          <text class="points">{{item.points}} 积分</text>
          <text class="stock text-sub">{{item.stock < 0 ? '不限量' : '库存 ' + item.stock}}</text>
          <text class="sales text-sub">已兑 {{item.exchangedCount}}</text>
        </view>
      </view>
      <view class="actions">
        <view class="shelf-tag" :class="item.status === 1 ? 'on' : 'off'" @click.stop="toggleStatus(item.id)">
          {{item.status === 1 ? '已上架' : '已下架'}}
        </view>
        <view class="del-btn" @click.stop="deleteGoods(item.id)">删除</view>
      </view>
    </view>

    <view v-if="list.length === 0" class="empty">暂无积分商品，点击右下角新增</view>

    <view class="fab" @click="addGoods">＋</view>
  </view>
</template>

<script>
const app = getApp()
import api from '@/api/index'

export default {
  data() {
    return {
      list: [],
      loading: false,
      verifyCode: ''
    }
  },
  onShow() {
    if (app.globalData.role !== 'merchant') {
      uni.reLaunch({ url: '/pages/role/role' })
      return
    }
    if (!app.globalData.isLogin()) {
      uni.reLaunch({ url: '/pages/login/login?role=merchant' })
      return
    }
    this.loadList()
  },
  methods: {
    loadList() {
      this.loading = true
      api.getAdminPointsGoods().then((list) => {
        // 标记是否为真实图片 URL，用于 WXML 区分 image / emoji 渲染
        const goods = (list || []).map((g) => ({
          ...g,
          imageUrl: /^https?:\/\//.test(g.image || '')
        }))
        this.list = goods
      }).catch(() => {
        this.list = []
      }).then(() => {
        this.loading = false
      })
    },

    // 新增积分商品
    addGoods() {
      uni.showModal({
        title: '新增积分商品',
        editable: true,
        placeholderText: '请输入商品名称',
        confirmColor: '#d4a24a',
        success: (res) => {
          if (!res.confirm) return
          const name = (res.content || '').trim()
          if (!name) {
            uni.showToast({ title: '名称不能为空', icon: 'none' })
            return
          }
          this.inputPoints(name)
        }
      })
    },

    inputPoints(name) {
      uni.showModal({
        title: `新增（${name}）`,
        editable: true,
        placeholderText: '请输入兑换所需积分，如 100',
        confirmColor: '#d4a24a',
        success: (res) => {
          if (!res.confirm) return
          const points = Number(res.content)
          if (!points || points <= 0) {
            uni.showToast({ title: '请输入有效积分', icon: 'none' })
            return
          }
          api.addPointsGoods({
            name,
            description: '凭核销码到店兑换',
            image: '🎁',
            points,
            stock: -1,
            perLimit: 1,
            sort: 0,
            status: 1
          }).then(() => {
            uni.showToast({ title: '新增成功', icon: 'success' })
            this.loadList()
          }).catch(() => {})
        }
      })
    },

    // 上下架
    toggleStatus(id) {
      const target = this.list.find((g) => g.id === id)
      if (!target) return
      const nextStatus = target.status === 1 ? 0 : 1
      api.updatePointsGoodsStatus(id, nextStatus).then(() => {
        this.loadList()
        uni.showToast({ title: nextStatus === 1 ? '已上架' : '已下架', icon: 'none' })
      }).catch(() => {})
    },

    deleteGoods(id) {
      const target = this.list.find((g) => g.id === id)
      if (!target) return
      uni.showModal({
        title: '删除确认',
        content: `确定删除「${target.name}」吗？`,
        confirmText: '删除',
        confirmColor: '#ff3b30',
        success: (res) => {
          if (!res.confirm) return
          api.deletePointsGoods(id).then(() => {
            uni.showToast({ title: '已删除', icon: 'none' })
            this.loadList()
          }).catch(() => {})
        }
      })
    },

    // 核销
    onVerifyInput(e) {
      this.verifyCode = e.detail.value
    },

    doVerify() {
      const code = (this.verifyCode || '').trim()
      if (!code) {
        uni.showToast({ title: '请输入核销码', icon: 'none' })
        return
      }
      api.verifyPoints(code).then(() => {
        uni.showToast({ title: '核销成功', icon: 'success' })
        this.verifyCode = ''
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx;
  padding-bottom: 160rpx;
}

.verify-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.05);
  margin-bottom: 24rpx;
}

.verify-title {
  font-size: 29rpx;
  font-weight: 600;
  margin-bottom: 20rpx;
}

.verify-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.verify-input {
  flex: 1;
  height: 76rpx;
  background: #f5f6f8;
  border-radius: 999rpx;
  padding: 0 32rpx;
  font-size: 28rpx;
}

.verify-btn {
  padding: 0 44rpx;
  height: 76rpx;
  line-height: 76rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #d4a24a, #b8860b);
  color: #fff;
  font-size: 28rpx;
  font-weight: 600;
}

.tip {
  font-size: 24rpx;
  color: #8a8a8a;
  margin-bottom: 20rpx;
}

.goods-item {
  display: flex;
  align-items: center;
  background: #fff;
  padding: 26rpx 24rpx;
  margin-bottom: 20rpx;
  border-radius: 20rpx;
  box-shadow: 0 4rpx 14rpx rgba(0, 0, 0, 0.04);
}

.thumb {
  width: 120rpx;
  height: 120rpx;
  border-radius: 16rpx;
  background: #f7f7f7;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 62rpx;
  flex-shrink: 0;
  overflow: hidden;
}

.thumb-img {
  width: 100%;
  height: 100%;
}

.info {
  flex: 1;
  margin-left: 22rpx;
  min-width: 0;
}

.name {
  font-size: 30rpx;
  font-weight: 600;
}

.desc {
  margin-top: 8rpx;
}

.meta {
  margin-top: 14rpx;
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.meta .points {
  font-size: 28rpx;
  font-weight: 700;
  color: #d4a24a;
}

.actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 14rpx;
}

.shelf-tag {
  padding: 8rpx 22rpx;
  border-radius: 999rpx;
  font-size: 23rpx;
}

.shelf-tag.on {
  background: #e8f6ec;
  color: #34c759;
}

.shelf-tag.off {
  background: #f2f3f5;
  color: #999;
}

.del-btn {
  padding: 8rpx 26rpx;
  border-radius: 999rpx;
  font-size: 23rpx;
  border: 2rpx solid #ffd6d2;
  color: #ff3b30;
}

.fab {
  position: fixed;
  right: 40rpx;
  bottom: 60rpx;
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #d4a24a, #b8860b);
  color: #fff;
  font-size: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10rpx 26rpx rgba(184, 134, 11, 0.4);
  z-index: 100;
}
</style>
