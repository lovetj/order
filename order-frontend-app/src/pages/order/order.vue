<template>
  <view class="container layout-page">
    <bottom-nav />
    <!-- 未识别店铺/桌号时提示扫码 -->
    <view v-if="needScan" class="scan-tip">
      <view class="scan-tip-card">
        <view class="scan-tip-icon">📷</view>
        <view class="scan-tip-text">
          <view class="scan-tip-title">请先扫描桌位二维码</view>
          <view class="scan-tip-desc">未识别到店铺与桌号，无法查看订单</view>
        </view>
        <view class="scan-tip-btn" @click="scanThenReload">立即扫码</view>
      </view>
    </view>
    <view class="tabs layout-header">
      <view
        v-for="item in tabs"
        :key="item.key"
        class="tab"
        :class="{ active: current === item.key }"
        @click="switchTab(item.key)"
      >{{item.name}}</view>
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
        <view class="mini-spinner customer-spinner"></view>
        <text class="list-loading-text">正在加载订单...</text>
      </view>

      <!-- 空状态 -->
      <view v-else-if="list.length === 0" class="empty">
        <view class="empty-icon">📭</view>
        <view class="empty-text">暂无订单</view>
        <view class="empty-btn" @click="goMenu">去点餐</view>
      </view>

      <!-- 订单列表 -->
      <template v-else>
        <view class="order-card" v-for="item in list" :key="item.id" @click="showDetail(item)">
          <view class="order-head">
            <view class="order-head-left">
              <text class="dining-badge" :class="item.diningType === 2 ? 'takeout' : 'dinein'">
                {{item.diningType === 2 ? '外带' : '堂食'}}
              </text>
              <text class="order-table">{{item.diningType === 2 ? '自提免占桌' : '桌号 ' + item.table}}</text>
            </view>
            <view class="order-status" :class="'status-' + item.status">{{item.statusText}}</view>
          </view>
          <view class="order-items">
            <view class="order-item" v-for="(g, idx) in item.items" :key="idx">
              <view class="item-name">
                <text>{{g.name}}</text>
                <text class="item-spec" v-if="g.specText || g.spec || g.specName">（{{g.specText || g.spec || g.specName}}）</text>
              </view>
              <view class="item-count">x{{g.count}}</view>
              <view class="item-price">¥{{g.amount != null ? g.amount : g.price}}</view>
            </view>
          </view>
          <view class="order-foot">
            <view class="order-time">{{item.createTime}}</view>
            <view class="order-total">合计 <text class="price">¥{{item.amount}}</text></view>
          </view>
          <view
            v-if="item.status === 'pending' || item.status === 'cooking'"
            class="order-actions"
          >
            <view
              class="act-btn"
              @click.stop="cancelOrder(item.id)"
            >取消订单</view>
          </view>
        </view>

        <!-- 加载更多动画 -->
        <view v-if="loadingMore" class="list-loading list-loading-more">
          <view class="mini-spinner customer-spinner"></view>
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
// 顾客端订单
const app = getApp()
import api from '@/api/index'
import { hasCustomerContext, scanOrderContext } from '@/utils/scan'

const TABS = [
  { key: 'all', name: '全部' },
  { key: 'pending', name: '待接单' },
  { key: 'cooking', name: '制作中' },
  { key: 'done', name: '已完成' }
]

export default {
  data() {
    return {
      tabs: TABS,
      current: 'all',
      list: [],
      pageNum: 1,
      pageSize: 10,
      total: 0,
      hasMore: true,
      loading: false,
      loadingMore: false,
      refreshing: false,
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
    this.loadList(true)
  },

  methods: {
    // 底部导航切换时刷新当前页面数据
    onTabRefresh() {
      if (!app.globalData.isLogin()) return
      this.needScan = !hasCustomerContext(app)
      if (this.needScan) return
      this.loadList(true)
    },

    // 扫码识别店铺/桌号后刷新页面
    async scanThenReload() {
      const { ok } = await scanOrderContext(app)
      if (!ok) return
      this.needScan = false
      this.loadList(true)
    },

    // 下拉刷新
    onRefresh() {
      this.refreshing = true
      this.loadList(true)
    },

    // 触底加载更多
    loadMore() {
      if (this.loading || this.loadingMore || !this.hasMore) return
      this.pageNum += 1
      this.loadList(false)
    },

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
        status: this.current
      }

      api.getOrders(params)
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
      if (this.current === key) return
      this.current = key
      this.list = [] // 切换Tab立即清空旧数据，触发加载动画
      if (this.needScan) return
      this.loadList(true)
    },

    goMenu() {
      uni.reLaunch({ url: '/pages/menu/menu' })
    },

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
    },

    // 取消订单（仅待接单/制作中可取消）
    cancelOrder(id) {
      uni.showModal({
        title: '取消订单',
        content: '确定取消该订单吗？',
        confirmText: '确定取消',
        confirmColor: '#ff3b30',
        success: (res) => {
          if (!res.confirm) return
          api.cancelOrder(id, '顾客取消').then(() => {
            uni.showToast({ title: '已取消', icon: 'none' })
            this.loadList(true)
          }).catch(() => {})
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
.tabs {
  display: flex;
  background: #fff;
  position: sticky;
  top: 0;
  z-index: 10;
}

.tab {
  flex: 1;
  text-align: center;
  padding: 28rpx 0;
  font-size: 28rpx;
  color: #666;
  position: relative;
}

.tab.active {
  color: #ff6b35;
  font-weight: 600;
}

.tab.active::after {
  content: '';
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: 8rpx;
  width: 48rpx;
  height: 6rpx;
  border-radius: 6rpx;
  background: #ff6b35;
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
  margin: 24rpx 0 32rpx;
}

.empty-btn {
  display: inline-block;
  padding: 16rpx 60rpx;
  border-radius: 999rpx;
  background: #ff6b35;
  color: #fff;
  font-size: 28rpx;
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
}

.order-status {
  font-size: 26rpx;
}

.status-pending { color: #ff9500; }
.status-cooking { color: #007aff; }
.status-done { color: #34c759; }
.status-canceled { color: #999; }

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
  width: 100rpx;
  text-align: right;
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
}

.order-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 24rpx;
}

.act-btn {
  padding: 14rpx 36rpx;
  border-radius: 999rpx;
  border: 2rpx solid #ddd;
  font-size: 26rpx;
  color: #666;
  margin-left: 20rpx;
}

.act-btn.primary {
  border-color: #ff6b35;
  color: #ff6b35;
}

.list-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx 0;
  color: #999;
}

.list-loading-first {
  padding: 120rpx 0;
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

.customer-spinner {
  border: 4rpx solid rgba(255, 107, 53, 0.15);
  border-top-color: #ff6b35;
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
  background: #e0e0e0;
}

.end-text {
  font-size: 22rpx;
  color: #aaa;
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
