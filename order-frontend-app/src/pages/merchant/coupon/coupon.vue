<template>
  <view class="page">
    <view class="tip">共 {{list.length}} 张优惠券</view>

    <view class="coupon-card" v-for="item in list" :key="item.id">
      <view class="coupon-head">
        <view class="coupon-name">{{item.name}}</view>
        <view class="coupon-status" :class="item.status === 1 ? 'on' : 'off'" @click.stop="toggleStatus(item.id)">
          {{item.status === 1 ? '启用中' : '已停用'}}
        </view>
      </view>
      <view class="coupon-body">
        <view class="coupon-value">{{couponDesc(item)}}</view>
        <view class="coupon-meta">
          <text>已发放 {{item.issuedCount}} 张</text>
          <text class="dot-sep">·</text>
          <text>{{item.totalCount < 0 ? '不限量' : '总量 ' + item.totalCount}}</text>
          <text class="dot-sep">·</text>
          <text>每人限领 {{item.perLimit}} 张</text>
        </view>
        <view class="coupon-valid">
          {{item.validDays > 0 ? '领取后 ' + item.validDays + ' 天内有效' : '固定有效期'}}
        </view>
      </view>
      <view class="coupon-foot">
        <view class="del-btn" @click.stop="deleteCoupon(item.id)">删除</view>
      </view>
    </view>

    <view v-if="list.length === 0" class="empty">暂无优惠券，点击右下角新增</view>

    <view class="fab" @click="addCoupon">＋</view>
  </view>
</template>

<script>
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
      api.getAdminCoupons().then((list) => {
        this.list = list || []
      }).catch(() => {
        this.list = []
      }).then(() => {
        this.loading = false
      })
    },

    // 新增满减券
    addCoupon() {
      uni.showModal({
        title: '新增优惠券',
        editable: true,
        placeholderText: '请输入券名称，如：满 80 减 15',
        confirmColor: '#2f80ed',
        success: (res) => {
          if (!res.confirm) return
          const name = (res.content || '').trim()
          if (!name) {
            uni.showToast({ title: '券名称不能为空', icon: 'none' })
            return
          }
          // 简化流程：默认生成一张满 50 减 10 的券，可在后续详情页调整
          api.addCoupon({
            name,
            type: 1,
            threshold: 50,
            amount: 10,
            totalCount: -1,
            perLimit: 1,
            validDays: 30,
            status: 1,
            description: '扫码点餐专享'
          }).then(() => {
            uni.showToast({ title: '新增成功', icon: 'success' })
            this.loadList()
          }).catch(() => {})
        }
      })
    },

    // 启用/停用
    toggleStatus(id) {
      const target = this.list.find((c) => c.id === id)
      if (!target) return
      const nextStatus = target.status === 1 ? 0 : 1
      api.updateCouponStatus(id, nextStatus).then(() => {
        this.loadList()
        uni.showToast({ title: nextStatus === 1 ? '已启用' : '已停用', icon: 'none' })
      }).catch(() => {})
    },

    deleteCoupon(id) {
      const target = this.list.find((c) => c.id === id)
      if (!target) return
      uni.showModal({
        title: '删除确认',
        content: `确定删除「${target.name}」吗？`,
        confirmText: '删除',
        confirmColor: '#ff3b30',
        success: (res) => {
          if (!res.confirm) return
          api.deleteCoupon(id).then(() => {
            uni.showToast({ title: '已删除', icon: 'none' })
            this.loadList()
          }).catch(() => {})
        }
      })
    },

    // 券面文案（折扣保留 1 位小数，避免浮点误差）
    couponDesc(item) {
      if (item.type === 2 && item.discount != null) {
        const zhe = Math.round(Number(item.discount) * 100) / 10
        return `${item.threshold > 0 ? '满' + item.threshold + '元享' : ''}${zhe} 折`
      }
      return `${item.threshold > 0 ? '满' + item.threshold + '减' : '立减'}${item.amount} 元`
    }
  }
}
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx;
  padding-bottom: 160rpx;
}

.tip {
  font-size: 24rpx;
  color: #8a8a8a;
  margin-bottom: 20rpx;
}

.coupon-card {
  background: #fff;
  border-radius: 20rpx;
  margin-bottom: 24rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.05);
}

.coupon-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 26rpx 28rpx;
  border-bottom: 1rpx solid #f5f5f5;
}

.coupon-name {
  font-size: 30rpx;
  font-weight: 600;
}

.coupon-status {
  padding: 8rpx 22rpx;
  border-radius: 999rpx;
  font-size: 23rpx;
}

.coupon-status.on {
  background: #e8f6ec;
  color: #34c759;
}

.coupon-status.off {
  background: #f2f3f5;
  color: #999;
}

.coupon-body {
  padding: 26rpx 28rpx;
}

.coupon-value {
  font-size: 36rpx;
  font-weight: 700;
  color: #ff6b35;
}

.coupon-meta {
  font-size: 23rpx;
  color: #8a8a8a;
  margin-top: 14rpx;
}

.dot-sep {
  margin: 0 12rpx;
}

.coupon-valid {
  font-size: 22rpx;
  color: #a0a0a0;
  margin-top: 10rpx;
}

.coupon-foot {
  display: flex;
  justify-content: flex-end;
  padding: 20rpx 28rpx;
  background: #fafafa;
}

.del-btn {
  padding: 10rpx 32rpx;
  border-radius: 999rpx;
  border: 2rpx solid #ffd6d2;
  color: #ff3b30;
  font-size: 25rpx;
}

.fab {
  position: fixed;
  right: 40rpx;
  bottom: 60rpx;
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #4d95f5, #2f80ed);
  color: #fff;
  font-size: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10rpx 26rpx rgba(47, 128, 237, 0.4);
  z-index: 100;
}
</style>
