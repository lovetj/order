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

    <!-- 就餐方式二级筛选栏 -->
    <view class="filter-type-bar">
      <view
        class="filter-chip"
        :class="{ active: diningTypeFilter === 0 }"
        @click="switchDiningFilter(0)"
      >全部方式</view>
      <view
        class="filter-chip"
        :class="{ active: diningTypeFilter === 1 }"
        @click="switchDiningFilter(1)"
      >🍽️ 堂食订单</view>
      <view
        class="filter-chip"
        :class="{ active: diningTypeFilter === 2 }"
        @click="switchDiningFilter(2)"
      >🛍️ 外带订单</view>
    </view>

    <scroll-view
      class="layout-body"
      scroll-y
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
      @scrolltolower="loadMore"
    >
      <!-- 页面/Tab切换或初次加载动画 -->
      <view v-if="loading" class="list-loading list-loading-first">
        <view class="mini-spinner merchant-spinner"></view>
        <text class="list-loading-text">正在加载订单...</text>
      </view>

      <!-- 空状态 -->
      <view v-else-if="list.length === 0" class="empty">
        <view class="empty-icon">📭</view>
        <view class="empty-text">暂无相关订单</view>
      </view>

      <!-- 订单列表 -->
      <template v-else>
        <view class="order-card" v-for="item in list" :key="item.id" @click="showDetail(item)">
          <view class="order-head">
            <view class="order-head-left">
              <text class="dining-badge" :class="item.diningType === 2 ? 'takeout' : 'dinein'">
                {{ item.diningType === 2 ? '外带' : '堂食' }}
              </text>
              <text class="order-table">{{ item.diningType === 2 ? '自提免占桌' : '桌号 ' + item.table }}</text>
            </view>
            <view class="order-status" :class="'status-' + item.status">{{item.statusText}}</view>
          </view>

          <view class="order-items">
            <view class="order-item" v-for="(goods, gIdx) in item.items" :key="gIdx">
              <view class="item-name">
                <text>{{goods.name}}</text>
                <text class="item-spec" v-if="goods.specText || goods.spec || goods.specName">（{{goods.specText || goods.spec || goods.specName}}）</text>
              </view>
              <view class="item-count">x{{goods.count}}</view>
              <view class="item-price">¥{{goods.amount != null ? goods.amount : goods.price}}</view>
            </view>
          </view>

          <view class="order-remark" v-if="item.remark">
            <text class="remark-tag">备注</text>
            <text class="remark-text">{{item.remark}}</text>
          </view>

          <view class="order-foot">
            <view class="order-time">{{item.createTime}}</view>
            <view class="order-total">合计 <text class="price">¥{{item.amount}}</text></view>
          </view>

          <view
            class="order-actions"
            v-if="(item.status === 'done' || item.statusText === '已完成') || item.action"
          >
            <view
              class="act-btn"
              v-if="item.status === 'done' || item.statusText === '已完成'"
              @click.stop="printReceipt(item)"
            >打印小票</view>
            <template v-if="item.action">
              <view class="act-btn danger" @click.stop="rejectOrder(item.id)">拒单</view>
              <view
                class="act-btn primary"
                @click.stop="handleNext(item.id, item.action.next)"
              >{{item.action.text}}</view>
            </template>
          </view>
        </view>

        <!-- 加载更多动画 -->
        <view v-if="loadingMore" class="list-loading list-loading-more">
          <view class="mini-spinner merchant-spinner"></view>
          <text class="list-loading-text">加载更多中...</text>
        </view>

        <!-- 触底无更多提示 -->
        <view v-if="!loading && !loadingMore && !hasMore && list.length > 0" class="list-end">
          <view class="end-line"></view>
          <text class="end-text">已展示全部订单</text>
          <view class="end-line"></view>
        </view>
      </template>

      <!-- 底部安全留白 -->
      <view class="list-bottom-space"></view>
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
      diningTypeFilter: 0, // 0全部, 1堂食, 2外带
      list: [],
      counts: {},
      pageNum: 1,
      pageSize: 10,
      total: 0,
      hasMore: true,
      loading: false,
      loadingMore: false,
      refreshing: false
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
    this.loadList(true)
  },
  // 底部导航切换时刷新当前页面数据
  onTabRefresh() {
    if (!app.globalData.isLogin()) return
    this.loadCounts()
    this.loadList(true)
  },
  methods: {
    // 各状态数量（Tab 角标）
    loadCounts() {
      api.getAdminOrderCounts().then((counts) => {
        this.counts = counts || {}
      }).catch(() => {})
    },

    // 切换就餐方式筛选
    switchDiningFilter(type) {
      if (this.diningTypeFilter === type) return
      this.diningTypeFilter = type
      this.list = [] // 立即清空，显示加载动画
      this.loadList(true)
    },

    // 下拉刷新
    onRefresh() {
      this.refreshing = true
      this.loadCounts()
      this.loadList(true)
    },

    // 触底加载更多
    loadMore() {
      if (this.loading || this.loadingMore || !this.hasMore) return
      this.pageNum += 1
      this.loadList(false)
    },

    // 当前 Tab 订单列表
    loadList(reset = false) {
      if (reset) {
        this.pageNum = 1
        this.hasMore = true
        if (!this.refreshing) {
          this.loading = true
        }
      } else {
        if (this.loading || this.loadingMore || !this.hasMore) return
        this.loadingMore = true
      }

      const params = {
        pageNum: this.pageNum,
        pageSize: this.pageSize,
        status: this.activeTab
      }
      if (this.diningTypeFilter) {
        params.diningType = this.diningTypeFilter
      }

      api.getAdminOrders(params)
        .then((page) => {
          const records = (page && page.records) || []
          this.total = (page && page.total) || 0
          const pages = (page && page.pages) || 0
          this.list = reset ? records : this.list.concat(records)
          this.hasMore = this.pageNum < pages
        })
        .catch(() => {
          if (reset) {
            this.list = []
          }
          this.hasMore = false
        })
        .finally(() => {
          this.loading = false
          this.loadingMore = false
          this.refreshing = false
        })
    },

    switchTab(key) {
      if (this.activeTab === key) return
      this.activeTab = key
      this.list = [] // 立即清空，显示加载动画
      this.loadList(true)
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
      this.loadList(true)
    },

    // 查看/打印小票（仅已完成订单）
    printReceipt(orderOrId) {
      const id = typeof orderOrId === 'object' && orderOrId ? orderOrId.id : orderOrId
      const order = typeof orderOrId === 'object' && orderOrId ? orderOrId : this.list.find((o) => o.id === id)
      if (order && order.status !== 'done' && order.statusText !== '已完成') {
        uni.showToast({ title: '只有已完成的订单才支持打印小票', icon: 'none' })
        return
      }
      uni.navigateTo({ url: `/pages/merchant/receipt/receipt?id=${id}` })
    },

    // 查看订单详情页
    showDetail(itemOrId) {
      const order = typeof itemOrId === 'object' && itemOrId ? itemOrId : this.list.find((o) => o.id === itemOrId)
      const id = order ? order.id : itemOrId
      if (!id) return
      if (order) {
        try {
          uni.setStorageSync('preview_order_detail', JSON.stringify(order))
        } catch (e) {}
      }
      uni.navigateTo({
        url: `/pages/order/detail?id=${id}`
      })
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

.filter-type-bar {
  display: flex;
  gap: 16rpx;
  padding: 16rpx 24rpx;
  background: #f8fafc;
  border-bottom: 1rpx solid #e2e8f0;
}

.filter-chip {
  font-size: 24rpx;
  padding: 8rpx 22rpx;
  border-radius: 999rpx;
  background: #ffffff;
  color: #64748b;
  border: 1rpx solid #cbd5e1;
  transition: all 0.2s;
}

.filter-chip.active {
  background: #2f80ed;
  color: #ffffff;
  border-color: #2f80ed;
  font-weight: 600;
  box-shadow: 0 2rpx 8rpx rgba(47, 128, 237, 0.25);
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
  background: #fff;
  border-radius: 20rpx;
  margin: 24rpx;
  padding: 28rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;

  &:active {
    transform: scale(0.99);
  }
}

.order-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid #f2f2f2;
}

.order-head-left {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.dining-badge {
  font-size: 20rpx;
  font-weight: 600;
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
}

.dining-badge.takeout {
  background: #fff7ed;
  color: #ea580c;
  border: 1rpx solid #fed7aa;
}

.dining-badge.dinein {
  background: #eff6ff;
  color: #2563eb;
  border: 1rpx solid #bfdbfe;
}

.order-table {
  font-size: 30rpx;
  font-weight: 600;
  color: #111827;
}

.order-id {
  font-size: 22rpx;
  color: #94a3b8;
  margin-left: 4rpx;
}

.order-status {
  font-size: 26rpx;
  font-weight: 600;
}

.status-pending {
  color: #ff9500;
}

.status-cooking {
  color: #007aff;
}

.status-done {
  color: #34c759;
}

.status-canceled {
  color: #999;
}

.order-items {
  padding: 20rpx 0;
}

.order-item {
  display: flex;
  align-items: center;
  padding: 10rpx 0;
  font-size: 26rpx;
}

.item-name {
  flex: 1;
  color: #444;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.item-spec {
  font-size: 22rpx;
  color: #888;
}

.item-count {
  color: #999;
  margin-right: 24rpx;
}

.item-price {
  color: #333;
  width: 130rpx;
  text-align: right;
}

.order-remark {
  display: flex;
  align-items: flex-start;
  gap: 10rpx;
  padding: 12rpx 18rpx;
  background: #fffbf0;
  border-radius: 12rpx;
  margin-bottom: 16rpx;
  border: 1rpx dashed #ffe58f;
}

.remark-tag {
  font-size: 20rpx;
  padding: 2rpx 10rpx;
  border-radius: 6rpx;
  background: #fa8c16;
  color: #fff;
  font-weight: 600;
  flex-shrink: 0;
}

.remark-text {
  font-size: 24rpx;
  color: #d46b08;
  line-height: 1.4;
  word-break: break-all;
}

.order-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 20rpx;
  border-top: 1rpx solid #f2f2f2;
  font-size: 24rpx;
  color: #999;
}

.order-total .price {
  font-size: 34rpx;
  font-weight: 700;
  color: #ff6b35;
}

.order-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-top: 24rpx;
  gap: 16rpx;
}

.act-btn {
  padding: 14rpx 32rpx;
  border-radius: 999rpx;
  border: 2rpx solid #ddd;
  font-size: 26rpx;
  color: #666;
  transition: all 0.2s;

  &:active {
    opacity: 0.8;
  }
}

.act-btn.primary {
  border-color: #ff6b35;
  color: #ff6b35;
  font-weight: 600;
}

.act-btn.danger {
  border-color: #ff4d4f;
  color: #ff4d4f;
}

.act-btn.detail {
  border-color: #cbd5e1;
  color: #64748b;
}

.empty {
  padding: 160rpx 0;
  text-align: center;
}

.empty-icon {
  font-size: 120rpx;
}

.empty-text {
  color: #999;
  font-size: 28rpx;
  margin-top: 24rpx;
}

.list-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx 0;
  color: #94a3b8;
}

.list-loading-first {
  padding: 140rpx 0;
}

.list-loading-more {
  padding: 24rpx 0;
}

.list-loading-text {
  font-size: 24rpx;
  margin-left: 14rpx;
}

.mini-spinner {
  width: 32rpx;
  height: 32rpx;
  border-radius: 50%;
  animation: spinner-rotate 0.7s linear infinite;
}

.merchant-spinner {
  border: 4rpx solid rgba(47, 128, 237, 0.15);
  border-top-color: #2f80ed;
}

.list-end {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 30rpx 0;
}

.end-line {
  width: 60rpx;
  height: 1rpx;
  background: #cbd5e1;
}

.end-text {
  font-size: 22rpx;
  color: #94a3b8;
  margin: 0 16rpx;
}

.list-bottom-space {
  height: 120rpx;
}

@keyframes spinner-rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
