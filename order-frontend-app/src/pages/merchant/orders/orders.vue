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
      <!-- 页面/Tab切换或初次加载统一动画 -->
      <page-loading v-if="loading" text="正在加载订单..." role="merchant" />

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
              <text v-if="item.payMode === 2" class="pay-tag" :class="item.payStatus === 2 ? 'paid' : 'unpaid'">
                {{ item.payStatus === 2 ? '线下已收款' : '线下待核验' }}
              </text>
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
            v-if="(item.status === 'done' || item.statusText === '已完成') || item.action || (item.payMode === 2 && item.payStatus !== 2 && item.status !== 'canceled')"
          >
            <!-- 线下支付收款核验按钮 -->
            <view
              class="act-btn success"
              v-if="item.payMode === 2 && item.payStatus !== 2 && item.status !== 'canceled'"
              @click.stop="openOfflinePayModal(item)"
            >确认已收款</view>
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

    <!-- 线下收款核验与出示收款码弹窗 -->
    <view v-if="showQrcodeModal" class="qr-modal-mask" @click="closeOfflinePayModal">
      <view class="qr-modal-card" @click.stop="noop">
        <view class="qr-modal-head">
          <text class="qr-modal-title">出示收款码 & 核验收款</text>
          <text class="qr-modal-close" @click="closeOfflinePayModal">✕</text>
        </view>

        <view class="qr-modal-order-info" v-if="currentOfflineOrder">
          <view class="qr-order-row">
            <text class="qr-label">订单编号：</text>
            <text class="qr-val">{{ currentOfflineOrder.orderNo || currentOfflineOrder.id }}</text>
          </view>
          <view class="qr-order-row">
            <text class="qr-label">应收金额：</text>
            <text class="qr-val price">¥{{ currentOfflineOrder.amount }}</text>
          </view>
        </view>

        <!-- 收款方式类型选择切换 -->
        <view class="qr-type-tabs">
          <view
            class="qr-type-tab"
            :class="{ active: activeQrType === 'wx' }"
            @click="switchQrType('wx')"
          >
            微信收款码
          </view>
          <view
            class="qr-type-tab"
            :class="{ active: activeQrType === 'alipay' }"
            @click="switchQrType('alipay')"
          >
            支付宝收款码
          </view>
          <view
            class="qr-type-tab"
            :class="{ active: activeQrType === 'other' }"
            @click="switchQrType('other')"
          >
            其他收款码
          </view>
        </view>

        <!-- 收款码展示区域 -->
        <view class="qr-image-wrapper">
          <image
            v-if="currentQrcodeUrl"
            :src="currentQrcodeUrl"
            mode="aspectFit"
            class="qr-image-display"
            @click="openFullPreview(currentQrcodeUrl)"
          />
          <view v-else class="qr-empty-box">
            <text class="qr-empty-icon">📷</text>
            <text class="qr-empty-tip">未上传该类型的收款码</text>
            <text class="qr-empty-sub">可在“我的 -> 支付管理”中上传维护</text>
          </view>
          <text v-if="currentQrcodeUrl" class="qr-preview-hint">点击二维码可全屏放大展示给顾客</text>
        </view>

        <!-- 底部操作按钮 -->
        <view class="qr-modal-actions">
          <button class="qr-btn cancel" @click="closeOfflinePayModal">取消</button>
          <button class="qr-btn confirm" :loading="confirmingOffline" :disabled="confirmingOffline" @click="confirmOfflinePaySubmit">
            确认已收到款项
          </button>
        </view>
      </view>
    </view>

    <!-- 全屏收款码大图浮层（层级高于一切弹窗，避免被确认弹窗遮盖） -->
    <view v-if="fullPreviewUrl" class="full-screen-qr-mask" @click="closeFullPreview">
      <view class="full-screen-qr-content" @click.stop="noop">
        <image :src="fullPreviewUrl" mode="aspectFit" class="full-screen-qr-img" />
        <view class="full-screen-qr-bar">
          <text class="full-screen-qr-tip">轻触任意空白区域关闭全屏</text>
        </view>
      </view>
    </view>
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
      refreshing: false,
      // 线下收款出示二维码弹窗
      showQrcodeModal: false,
      currentOfflineOrder: null,
      activeQrType: 'wx', // 'wx' | 'alipay' | 'other'
      confirmingOffline: false,
      fullPreviewUrl: '',
      paymentConfig: {
        offlineWxQrcode: '',
        offlineAlipayQrcode: '',
        offlineOtherQrcode: ''
      }
    }
  },
  computed: {
    currentQrcodeUrl() {
      if (this.activeQrType === 'wx') return this.paymentConfig.offlineWxQrcode || ''
      if (this.activeQrType === 'alipay') return this.paymentConfig.offlineAlipayQrcode || ''
      if (this.activeQrType === 'other') return this.paymentConfig.offlineOtherQrcode || ''
      return ''
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
    this.loadPaymentConfig()
    this.loadCounts()
    this.loadList(true)
  },
  // 底部导航切换时刷新当前页面数据
  onTabRefresh() {
    if (!app.globalData.isLogin()) return
    this.loadPaymentConfig()
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

    noop() {},

    // 加载店铺收款码配置
    loadPaymentConfig() {
      api.getMerchantPaymentConfig().then((res) => {
        if (res) {
          this.paymentConfig = {
            offlineWxQrcode: res.offlineWxQrcode || '',
            offlineAlipayQrcode: res.offlineAlipayQrcode || '',
            offlineOtherQrcode: res.offlineOtherQrcode || ''
          }
        }
      }).catch(() => {})
    },

    // 点击“确认已收款”：先打开出示收款码弹窗，支持选择收款码类型
    openOfflinePayModal(order) {
      this.currentOfflineOrder = order
      this.showQrcodeModal = true
      // 默认选择有图片的收款码类型
      if (!this.paymentConfig.offlineWxQrcode && this.paymentConfig.offlineAlipayQrcode) {
        this.activeQrType = 'alipay'
      } else if (!this.paymentConfig.offlineWxQrcode && !this.paymentConfig.offlineAlipayQrcode && this.paymentConfig.offlineOtherQrcode) {
        this.activeQrType = 'other'
      } else {
        this.activeQrType = 'wx'
      }
      this.loadPaymentConfig()
    },

    closeOfflinePayModal() {
      this.showQrcodeModal = false
      this.currentOfflineOrder = null
    },

    switchQrType(type) {
      this.activeQrType = type
    },

    openFullPreview(url) {
      if (!url) return
      this.fullPreviewUrl = url
    },

    closeFullPreview() {
      this.fullPreviewUrl = ''
    },

    previewQr(url) {
      this.openFullPreview(url)
    },

    confirmOfflinePaySubmit() {
      if (!this.currentOfflineOrder) return
      const order = this.currentOfflineOrder
      const id = order.id || order.orderNo
      this.confirmingOffline = true
      api.confirmOfflinePay(id).then(() => {
        this.confirmingOffline = false
        this.closeOfflinePayModal()
        uni.showToast({ title: '已确认收款', icon: 'success' })
        this.afterAction()
      }).catch((err) => {
        this.confirmingOffline = false
        uni.showToast({ title: (err && err.message) || '确认失败', icon: 'none' })
      })
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

.act-btn.success {
  background: #52c41a;
  color: #fff;
  border-color: #52c41a;
}

.pay-tag {
  font-size: 20rpx;
  padding: 2rpx 10rpx;
  border-radius: 6rpx;
  margin-left: 12rpx;
}

.pay-tag.unpaid {
  background: #fff7e6;
  color: #fa8c16;
  border: 1rpx solid #ffd591;
}

.pay-tag.paid {
  background: #f6ffed;
  color: #52c41a;
  border: 1rpx solid #b7eb8f;
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

/* ==================== 线下收款核验弹窗 ==================== */
.qr-modal-mask {
  position: fixed;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.65);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 32rpx;
  box-sizing: border-box;
}

.qr-modal-card {
  width: 100%;
  max-width: 620rpx;
  background: #ffffff;
  border-radius: 28rpx;
  overflow: hidden;
  box-shadow: 0 16rpx 48rpx rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
}

.qr-modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 32rpx 20rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.qr-modal-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1f2937;
}

.qr-modal-close {
  font-size: 36rpx;
  color: #9ca3af;
  padding: 0 10rpx;
}

.qr-modal-order-info {
  background: #f8fafc;
  padding: 16rpx 32rpx;
  border-bottom: 1rpx solid #f1f5f9;
}

.qr-order-row {
  display: flex;
  align-items: center;
  font-size: 24rpx;
  line-height: 1.6;
}

.qr-order-row .qr-label {
  color: #64748b;
  width: 140rpx;
}

.qr-order-row .qr-val {
  color: #334155;
  font-weight: 500;
}

.qr-order-row .qr-val.price {
  color: #ff4d4f;
  font-size: 30rpx;
  font-weight: 700;
}

.qr-type-tabs {
  display: flex;
  padding: 20rpx 32rpx 10rpx;
  gap: 16rpx;
}

.qr-type-tab {
  flex: 1;
  text-align: center;
  padding: 14rpx 0;
  font-size: 24rpx;
  color: #64748b;
  background: #f1f5f9;
  border-radius: 12rpx;
  border: 1rpx solid transparent;
  transition: all 0.2s;
}

.qr-type-tab.active {
  color: #2563eb;
  background: #eff6ff;
  border-color: #93c5fd;
  font-weight: 600;
}

.qr-image-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24rpx 32rpx;
  min-height: 400rpx;
}

.qr-image-display {
  width: 380rpx;
  height: 380rpx;
  border-radius: 16rpx;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.08);
  background: #fff;
}

.qr-empty-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40rpx 0;
}

.qr-empty-icon {
  font-size: 72rpx;
  margin-bottom: 16rpx;
}

.qr-empty-tip {
  font-size: 26rpx;
  color: #64748b;
  font-weight: 600;
}

.qr-empty-sub {
  font-size: 22rpx;
  color: #94a3b8;
  margin-top: 8rpx;
}

.qr-preview-hint {
  font-size: 22rpx;
  color: #64748b;
  margin-top: 16rpx;
}

.qr-modal-actions {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 20rpx 32rpx 32rpx;
  border-top: 1rpx solid #f1f5f9;
}

.qr-btn {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 40rpx;
  font-size: 28rpx;
  text-align: center;
  font-weight: 600;
}

.qr-btn.cancel {
  background: #f1f5f9;
  color: #64748b;
}

.qr-btn.confirm {
  background: #16a34a;
  color: #ffffff;
  box-shadow: 0 6rpx 16rpx rgba(22, 163, 74, 0.35);
}

/* ==================== 全屏二维码无遮挡放大浮层 ==================== */
.full-screen-qr-mask {
  position: fixed;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.92);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100005;
  padding: 32rpx;
  box-sizing: border-box;
}

.full-screen-qr-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
}

.full-screen-qr-img {
  width: 580rpx;
  height: 580rpx;
  max-width: 85vw;
  max-height: 85vw;
  background: #ffffff;
  border-radius: 20rpx;
  box-shadow: 0 10rpx 40rpx rgba(0, 0, 0, 0.5);
  padding: 16rpx;
  box-sizing: border-box;
}

.full-screen-qr-bar {
  margin-top: 36rpx;
}

.full-screen-qr-tip {
  color: #e2e8f0;
  font-size: 26rpx;
  background: rgba(255, 255, 255, 0.15);
  padding: 10rpx 28rpx;
  border-radius: 40rpx;
}
</style>
