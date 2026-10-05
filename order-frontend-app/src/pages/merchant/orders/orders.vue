<template>
  <view class="page layout-page">
    <bottom-nav />
    <view class="tabs layout-header">
      <view
        v-for="item in tabs"
        :key="item.key"
        class="tab"
        :class="{ active: activeTab === item.key }"
        @click="switchTab(item.key)"
      >
        <text>{{item.label}}</text>
        <text v-if="counts[item.key] > 0" class="count">{{counts[item.key]}}</text>
      </view>
    </view>

    <scroll-view class="layout-body" scroll-y>
    <template v-if="list.length > 0">
      <view class="card order-card" v-for="item in list" :key="item.id">
        <view class="order-head">
          <view>
            <text class="table-no">{{item.table}} 桌</text>
            <text class="order-id">单号 {{item.id}}</text>
          </view>
          <text class="order-status" :class="'status-' + item.status">{{item.statusText}}</text>
        </view>

        <view class="order-body">
          <view class="order-item" v-for="goods in item.items" :key="goods.name">
            <text class="item-name">{{goods.name}}</text>
            <text class="item-num text-sub">x{{goods.count}}</text>
            <text class="item-price">¥{{goods.amount}}</text>
          </view>
        </view>

        <view class="order-remark" v-if="item.remark">备注：{{item.remark}}</view>

        <view class="order-foot">
          <text class="text-sub">{{item.createTime}}</text>
          <text class="price">合计 ¥{{item.amount}}</text>
        </view>

        <view class="order-actions">
          <view class="act-btn print" @click="printReceipt(item.id)">打印小票</view>
          <template v-if="item.action">
            <view class="act-btn ghost" @click="rejectOrder(item.id)">拒单</view>
            <view
              class="act-btn primary"
              @click="handleNext(item.id, item.action.next)"
            >{{item.action.text}}</view>
          </template>
        </view>
      </view>
    </template>

    <view v-else class="empty">
      <view class="empty-icon">📭</view>
      <view>暂无相关订单</view>
    </view>
    </scroll-view>
  </view>
</template>

<script>
const app = getApp()
import api from '@/api/index'

const TABS = [
  { key: 'pending', label: '待接单' },
  { key: 'cooking', label: '制作中' },
  { key: 'done', label: '已完成' },
  { key: 'all', label: '全部' }
]

export default {
  data() {
    return {
      tabs: TABS,
      activeTab: 'pending',
      list: [],
      counts: {},
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
    this.loadCounts()
    this.loadList()
  },
  // 底部导航切换时刷新当前页面数据
  onTabRefresh() {
    if (!app.globalData.isLogin()) return
    this.loadCounts()
    this.loadList()
  },
  methods: {
    // 各状态数量（Tab 角标）
    loadCounts() {
      api.getAdminOrderCounts().then((counts) => {
        this.counts = counts || {}
      }).catch(() => {})
    },

    // 当前 Tab 订单列表
    loadList() {
      this.loading = true
      api.getAdminOrders({ pageNum: 1, pageSize: 50, status: this.activeTab })
        .then((page) => {
          this.list = (page && page.records) || []
        })
        .catch(() => {
          this.list = []
        })
        .then(() => {
          this.loading = false
        })
    },

    switchTab(key) {
      this.activeTab = key
      this.loadList()
    },

    // 接单 / 出餐：根据服务端返回的 action.next 决定
    handleNext(id, next) {
      if (next === 'cooking') {
        api.acceptOrder(id).then(() => {
          uni.showToast({ title: '接单成功', icon: 'none' })
          this.afterAction()
        }).catch(() => {})
      } else if (next === 'done') {
        api.finishOrder(id).then(() => {
          uni.showToast({ title: '出餐成功', icon: 'none' })
          this.afterAction()
        }).catch(() => {})
      }
    },

    rejectOrder(id) {
      uni.showModal({
        title: '拒单确认',
        content: '确定拒绝该订单吗？',
        confirmText: '拒单',
        confirmColor: '#ff3b30',
        success: (res) => {
          if (!res.confirm) return
          api.rejectOrder(id, '店家拒单').then(() => {
            uni.showToast({ title: '已拒单', icon: 'none' })
            this.afterAction()
          }).catch(() => {})
        }
      })
    },

    // 操作后刷新列表与角标
    afterAction() {
      this.loadCounts()
      this.loadList()
    },

    // 查看/打印小票
    printReceipt(id) {
      uni.navigateTo({ url: `/pages/merchant/receipt/receipt?id=${id}` })
    }
  }
}
</script>

<style lang="scss" scoped>
.tabs {
  display: flex;
  background: #fff;
  padding: 0 12rpx;
  position: sticky;
  top: 0;
  z-index: 10;
}

.tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 26rpx 0;
  font-size: 28rpx;
  color: #666;
  position: relative;
}

.tab.active {
  color: #2f80ed;
  font-weight: 600;
}

.tab.active::after {
  content: '';
  position: absolute;
  bottom: 8rpx;
  left: 50%;
  transform: translateX(-50%);
  width: 44rpx;
  height: 6rpx;
  border-radius: 6rpx;
  background: #2f80ed;
}

.count {
  margin-left: 8rpx;
  min-width: 32rpx;
  height: 32rpx;
  padding: 0 8rpx;
  border-radius: 999rpx;
  background: #ff3b30;
  color: #fff;
  font-size: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.order-card {
  padding: 0;
  overflow: hidden;
}

.order-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 24rpx 28rpx;
  border-bottom: 1rpx solid #f5f5f5;
}

.table-no {
  font-size: 34rpx;
  font-weight: 700;
  margin-right: 18rpx;
}

.order-id {
  font-size: 23rpx;
  color: #a0a0a0;
}

.order-status {
  font-size: 27rpx;
  font-weight: 600;
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

.status-canceled {
  color: #999;
}

.order-body {
  padding: 18rpx 28rpx;
}

.order-item {
  display: flex;
  align-items: center;
  padding: 10rpx 0;
  font-size: 28rpx;
}

.item-name {
  flex: 1;
}

.item-num {
  width: 90rpx;
  text-align: center;
}

.item-price {
  width: 160rpx;
  text-align: right;
  font-weight: 600;
}

.order-remark {
  padding: 0 28rpx 16rpx;
  font-size: 24rpx;
  color: #ff9500;
}

.order-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 28rpx;
  background: #fafafa;
}

.order-actions {
  display: flex;
  justify-content: flex-end;
  gap: 20rpx;
  padding: 22rpx 28rpx;
}

.act-btn {
  padding: 16rpx 44rpx;
  border-radius: 999rpx;
  font-size: 27rpx;
}

.act-btn.ghost {
  border: 2rpx solid #d8d8d8;
  color: #666;
}

.act-btn.primary {
  background: linear-gradient(135deg, #4d95f5, #2f80ed);
  color: #fff;
  font-weight: 600;
}

.act-btn.print {
  margin-right: auto;
  border: 2rpx solid #34c759;
  color: #34c759;
}

.empty-icon {
  font-size: 90rpx;
  margin-bottom: 20rpx;
}
</style>
