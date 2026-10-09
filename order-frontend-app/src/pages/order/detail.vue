<template>
  <view class="detail-page" :class="{ 'is-merchant': isMerchant }">
    <!-- 顶部状态栏 -->
    <view class="status-header" :class="'status-bg-' + order.status">
      <view class="status-content">
        <view class="status-main">
          <text class="status-icon">{{ statusIcon }}</text>
          <text class="status-title">{{ order.statusText || '加载中...' }}</text>
        </view>
        <view class="status-desc">{{ statusDesc }}</view>
      </view>
      <view class="dining-pill" :class="order.diningType === 2 ? 'takeout' : 'dinein'">
        <text class="dining-pill-type">{{ order.diningType === 2 ? '外带' : '堂食' }}</text>
        <text class="dining-pill-table">{{ order.diningType === 2 ? '自提免占桌' : (order.table ? order.table + '号桌' : '未指定桌号') }}</text>
      </view>
    </view>

    <!-- 加载中占位 -->
    <view v-if="loading && !order.id" class="loading-state">
      <view class="mini-spinner" :class="isMerchant ? 'merchant-spinner' : 'customer-spinner'"></view>
      <text class="loading-text">正在加载订单详情...</text>
    </view>

    <!-- 主体内容卡片列表 -->
    <view v-else-if="order.id" class="detail-body">
      <!-- 物品清单卡片（重点：展示物品图片） -->
      <view class="detail-card items-card">
        <view class="card-head">
          <text class="card-title">商品明细</text>
          <text class="card-subtitle">共 {{ totalItemCount }} 件商品</text>
        </view>

        <view class="goods-list">
          <view class="goods-item" v-for="(g, idx) in formattedItems" :key="idx">
            <!-- 菜品缩略图 -->
            <view class="goods-thumb">
              <image
                v-if="g.hasRealImage && !g.imgError"
                class="goods-img"
                :src="g.displayImage"
                mode="aspectFill"
                @error="onImageError(idx)"
              />
              <view v-else class="goods-thumb-placeholder">
                <text class="goods-placeholder-icon">🍽️</text>
              </view>
            </view>

            <!-- 菜品详情信息 -->
            <view class="goods-info">
              <view class="goods-main-meta">
                <view class="goods-name-row">
                  <text class="goods-name">{{ g.name }}</text>
                </view>
                <view class="goods-spec" v-if="g.specText || g.spec || g.specName">
                  <text class="spec-tag">{{ g.specText || g.spec || g.specName }}</text>
                </view>
              </view>
              <view class="goods-bottom-row">
                <text class="goods-price">¥{{ g.price }}</text>
                <text class="goods-count">×{{ g.count }}</text>
              </view>
            </view>

            <!-- 商品小计 -->
            <view class="goods-subtotal">
              <text class="subtotal-val">¥{{ g.itemTotal }}</text>
            </view>
          </view>
        </view>

        <!-- 费用清单明细 -->
        <view class="fee-section">
          <view class="fee-row">
            <text class="fee-label">商品总额</text>
            <text class="fee-val">¥{{ displayProductTotal }}</text>
          </view>
          <view class="fee-row" v-if="discountAmount > 0">
            <text class="fee-label">优惠折扣</text>
            <text class="fee-val discount-val">-¥{{ discountAmount }}</text>
          </view>
          <view class="fee-divider"></view>
          <view class="fee-row total-row">
            <text class="total-label">实付金额</text>
            <view class="total-price-box">
              <text class="price-symbol">¥</text>
              <text class="price-val">{{ order.amount }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 订单信息卡片（保留所有现有详情） -->
      <view class="detail-card info-card">
        <view class="card-head">
          <text class="card-title">订单信息</text>
        </view>

        <view class="info-list">
          <view class="info-row">
            <text class="info-label">订单编号</text>
            <view class="info-val-wrap">
              <text class="info-val info-code">{{ order.orderNo || order.id }}</text>
              <view class="copy-tag" @click="copyOrderNo">复制</view>
            </view>
          </view>

          <view class="info-row">
            <text class="info-label">就餐方式</text>
            <text class="info-val">{{ order.diningType === 2 ? '外带自提' : '堂食就餐' }}</text>
          </view>

          <view class="info-row" v-if="order.diningType !== 2">
            <text class="info-label">用餐桌号</text>
            <text class="info-val highlight-val">{{ order.table ? order.table + ' 号桌' : '未指定桌号' }}</text>
          </view>

          <view class="info-row" v-if="order.peopleCount">
            <text class="info-label">用餐人数</text>
            <text class="info-val">{{ order.peopleCount }} 人</text>
          </view>

          <view class="info-row" v-if="orderPhone">
            <text class="info-label">联系电话</text>
            <view class="info-val-wrap">
              <text class="info-val">{{ orderPhone }}</text>
              <view v-if="isMerchant" class="phone-call-btn" @click="callPhone">
                拨打
              </view>
            </view>
          </view>

          <view class="info-row" v-if="order.createTime">
            <text class="info-label">下单时间</text>
            <text class="info-val">{{ order.createTime }}</text>
          </view>

          <!-- 备注提醒 -->
          <view class="remark-row" v-if="order.remark">
            <view class="remark-head">
              <text class="remark-badge">备注</text>
              <text class="remark-text">{{ order.remark }}</text>
            </view>
          </view>

          <!-- 取消原因（如有） -->
          <view class="cancel-row" v-if="order.status === 'canceled' && order.cancelReason">
            <text class="cancel-label">取消原因</text>
            <text class="cancel-text">{{ order.cancelReason }}</text>
          </view>
        </view>
      </view>

      <!-- 底部安全留白 -->
      <view class="bottom-placeholder"></view>
    </view>

    <!-- 异常状态（未找到订单） -->
    <view v-else class="empty-state">
      <text class="empty-icon">🔍</text>
      <text class="empty-text">未找到该订单信息</text>
      <view class="empty-back-btn" @click="goBack">返回列表</view>
    </view>

    <!-- 底部操作悬浮栏 -->
    <view class="footer-actions" v-if="order.id && hasActions">
      <!-- 顾客端操作 -->
      <template v-if="!isMerchant">
        <view
          v-if="order.status === 'pending' || order.status === 'cooking'"
          class="action-btn cancel-btn"
          @click="customerCancel"
        >
          取消订单
        </view>
        <view
          v-else-if="order.status === 'done'"
          class="action-btn primary-btn customer-primary"
          @click="goMenu"
        >
          再来一单
        </view>
      </template>

      <!-- 店家端操作 -->
      <template v-else>
        <!-- 已完成订单支持打印小票 -->
        <view
          v-if="order.status === 'done' || order.statusText === '已完成'"
          class="action-btn secondary-btn"
          @click="printReceipt"
        >
          打印小票
        </view>
        <!-- 待接单/制作中等阶段支持拒单与流转 -->
        <view
          v-if="order.status === 'pending' || order.status === 'cooking'"
          class="action-btn danger-btn"
          @click="merchantReject"
        >
          拒单
        </view>
        <view
          v-if="merchantNextAction"
          class="action-btn primary-btn merchant-primary"
          @click="merchantHandleNext"
        >
          {{ merchantNextAction.text }}
        </view>
      </template>
    </view>
  </view>
</template>

<script>
import api from '../../api/index'
import { formatImageUrl } from '../../utils/util'

const app = getApp()

export default {
  data() {
    return {
      orderId: '',
      loading: false,
      roleParam: '',
      order: {
        id: '',
        orderNo: '',
        table: '',
        diningType: 1,
        status: '',
        statusText: '',
        createTime: '',
        amount: '0.00',
        productTotal: '0.00',
        peopleCount: null,
        remark: '',
        phone: '',
        userPhone: '',
        cancelReason: '',
        action: null,
        items: []
      },
      itemErrors: {}
    }
  },

  computed: {
    // 判断当前页面是否处于店家端视图
    isMerchant() {
      if (this.roleParam === 'merchant') return true
      if (this.roleParam === 'customer') return false
      return (app && app.globalData && app.globalData.role === 'merchant') || false
    },

    // 状态对应图标
    statusIcon() {
      const s = this.order.status
      if (s === 'pending') return '⏳'
      if (s === 'cooking') return '🍳'
      if (s === 'done') return '✅'
      if (s === 'canceled') return '❌'
      return '📄'
    },

    // 状态对应描述文字
    statusDesc() {
      const s = this.order.status
      if (this.isMerchant) {
        if (s === 'pending') return '顾客已提交订单，请及时接单处理'
        if (s === 'cooking') return '后厨正在配餐与烹制，完成后请点击出餐'
        if (s === 'done') return '该订单已顺利出餐完成'
        if (s === 'canceled') return '该订单已被取消或拒单'
      } else {
        if (s === 'pending') return '订单已提交，等待店家确认接单'
        if (s === 'cooking') return '店家已接单，后厨全力制作中'
        if (s === 'done') return '餐品已就绪，祝您用餐愉快！'
        if (s === 'canceled') return '订单已取消，如有疑问可联系店家'
      }
      return '订单处理中'
    },

    // 格式化后的商品项列表，带图片处理
    formattedItems() {
      const items = this.order.items || []
      return items.map((item, idx) => {
        const rawImg = (item && item.image) || ''
        const hasRealImage = Boolean(rawImg && (/^https?:\/\//.test(rawImg) || rawImg.startsWith('/') || rawImg.startsWith('data:')))
        const displayImage = hasRealImage ? formatImageUrl(rawImg) : ''
        const count = Number(item.count || 1)
        const price = Number(item.price || 0)
        const itemTotal = item.amount != null ? Number(item.amount).toFixed(2) : (count * price).toFixed(2)
        return {
          ...item,
          hasRealImage,
          displayImage,
          itemTotal,
          imgError: !!this.itemErrors[idx]
        }
      })
    },

    // 商品总件数
    totalItemCount() {
      const items = this.order.items || []
      return items.reduce((sum, item) => sum + Number(item.count || 1), 0)
    },

    // 商品总金额
    displayProductTotal() {
      if (this.order.productTotal != null && Number(this.order.productTotal) > 0) {
        return Number(this.order.productTotal).toFixed(2)
      }
      const sum = (this.order.items || []).reduce((acc, item) => {
        const amt = item.amount != null ? Number(item.amount) : Number(item.price || 0) * Number(item.count || 1)
        return acc + amt
      }, 0)
      return (sum || Number(this.order.amount) || 0).toFixed(2)
    },

    // 优惠金额
    discountAmount() {
      const pTotal = Number(this.displayProductTotal)
      const payAmt = Number(this.order.amount || 0)
      if (pTotal > payAmt && payAmt > 0) {
        return (pTotal - payAmt).toFixed(2)
      }
      return '0.00'
    },

    // 手机号
    orderPhone() {
      return this.order.phone || this.order.userPhone || ''
    },

    // 商家端下一步操作定义
    merchantNextAction() {
      if (this.order.action && this.order.action.text) {
        return this.order.action
      }
      if (this.order.status === 'pending') {
        return { text: '接单', next: 'cooking' }
      }
      if (this.order.status === 'cooking') {
        return { text: '出餐', next: 'done' }
      }
      return null
    },

    // 是否有可操作项
    hasActions() {
      if (!this.isMerchant) {
        return this.order.status === 'pending' || this.order.status === 'cooking' || this.order.status === 'done'
      }
      return (
        this.order.status === 'pending' ||
        this.order.status === 'cooking' ||
        this.order.status === 'done' ||
        Boolean(this.merchantNextAction)
      )
    }
  },

  onLoad(options) {
    this.orderId = (options && options.id) || ''
    if (options && options.role) {
      this.roleParam = options.role
    }

    // 尝试读取列表带过来的本地缓存订单对象（秒开）
    this.loadCachedOrder()

    // 调接口拉取最新订单数据
    if (this.orderId) {
      this.fetchOrderDetail()
    }
  },

  onPullDownRefresh() {
    if (this.orderId) {
      this.fetchOrderDetail().finally(() => {
        uni.stopPullDownRefresh()
      })
    } else {
      uni.stopPullDownRefresh()
    }
  },

  methods: {
    // 读取本地暂存预览数据
    loadCachedOrder() {
      try {
        const raw = uni.getStorageSync('preview_order_detail')
        if (raw) {
          const cached = JSON.parse(raw)
          if (cached && (String(cached.id) === String(this.orderId) || !this.orderId)) {
            this.mergeOrderData(cached)
            if (!this.orderId && cached.id) {
              this.orderId = String(cached.id)
            }
          }
        }
      } catch (e) {}
    },

    // 合并订单数据到 data
    mergeOrderData(data) {
      if (!data) return
      this.order = {
        ...this.order,
        ...data,
        id: data.id || this.orderId,
        orderNo: data.orderNo || data.id || this.orderId,
        amount: data.amount != null ? Number(data.amount).toFixed(2) : this.order.amount,
        items: data.items || this.order.items || []
      }
    },

    // 从接口拉取订单完整详情
    fetchOrderDetail() {
      this.loading = true
      return api
        .getOrderDetail(this.orderId)
        .then((res) => {
          if (res) {
            this.mergeOrderData(res)
            // 更新本地缓存
            try {
              uni.setStorageSync('preview_order_detail', JSON.stringify(res))
            } catch (e) {}
          }
        })
        .catch(() => {
          // 若已有缓存展示，则静默失败；否则提示
          if (!this.order.id) {
            uni.showToast({ title: '加载订单详情失败', icon: 'none' })
          }
        })
        .finally(() => {
          this.loading = false
        })
    },

    // 图片加载失败降级
    onImageError(idx) {
      this.itemErrors = { ...this.itemErrors, [idx]: true }
    },

    // 复制订单编号
    copyOrderNo() {
      const no = this.order.orderNo || this.order.id
      if (!no) return
      uni.setClipboardData({
        data: String(no),
        success: () => {
          uni.showToast({ title: '已复制订单编号', icon: 'success' })
        }
      })
    },

    // 拨打顾客电话
    callPhone() {
      if (!this.orderPhone) return
      uni.makePhoneCall({
        phoneNumber: String(this.orderPhone)
      })
    },

    // 返回列表
    goBack() {
      uni.navigateBack({
        fail: () => {
          const target = this.isMerchant ? '/pages/merchant/orders/orders' : '/pages/order/order'
          uni.reLaunch({ url: target })
        }
      })
    },

    // 顾客再来一单
    goMenu() {
      uni.reLaunch({ url: '/pages/menu/menu' })
    },

    // 顾客取消订单
    customerCancel() {
      uni.showModal({
        title: '提示',
        content: '确定要取消该订单吗？',
        confirmText: '确定取消',
        confirmColor: '#ff3b30',
        success: (res) => {
          if (!res.confirm) return
          uni.showLoading({ title: '处理中...', mask: true })
          api
            .cancelOrder(this.order.id, '顾客在详情页取消')
            .then(() => {
              uni.hideLoading()
              uni.showToast({ title: '订单已取消', icon: 'success' })
              this.fetchOrderDetail()
            })
            .catch(() => {
              uni.hideLoading()
            })
        }
      })
    },

    // 店家拒单
    merchantReject() {
      uni.showModal({
        title: '拒单确认',
        content: '确定要拒绝该订单吗？',
        confirmText: '确认拒单',
        confirmColor: '#ff3b30',
        success: (res) => {
          if (!res.confirm) return
          uni.showLoading({ title: '处理中...', mask: true })
          api
            .rejectOrder(this.order.id, '商家在详情页拒单')
            .then(() => {
              uni.hideLoading()
              uni.showToast({ title: '已拒单', icon: 'none' })
              this.fetchOrderDetail()
            })
            .catch(() => {
              uni.hideLoading()
            })
        }
      })
    },

    // 店家接单/出餐
    merchantHandleNext() {
      const next = this.merchantNextAction && this.merchantNextAction.next
      if (!next) return
      if (next === 'cooking') {
        uni.showLoading({ title: '接单中...', mask: true })
        api
          .acceptOrder(this.order.id)
          .then(() => {
            uni.hideLoading()
            uni.showToast({ title: '接单成功', icon: 'success' })
            this.fetchOrderDetail()
          })
          .catch(() => {
            uni.hideLoading()
          })
      } else if (next === 'done') {
        uni.showLoading({ title: '出餐中...', mask: true })
        api
          .finishOrder(this.order.id)
          .then(() => {
            uni.hideLoading()
            uni.showToast({ title: '出餐成功', icon: 'success' })
            this.fetchOrderDetail()
          })
          .catch(() => {
            uni.hideLoading()
          })
      }
    },

    // 打印小票
    printReceipt() {
      uni.navigateTo({
        url: `/pages/merchant/receipt/receipt?id=${this.order.id}`
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.detail-page {
  min-height: 100vh;
  background-color: #f6f7f9;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

/* 顶部状态横幅 */
.status-header {
  padding: 44rpx 32rpx 36rpx;
  color: #fff;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
  background: linear-gradient(135deg, #ff7a45, #ff4d4f);
  box-shadow: 0 4rpx 20rpx rgba(255, 77, 79, 0.2);

  &.status-bg-pending {
    background: linear-gradient(135deg, #ffa940, #fa8c16);
    box-shadow: 0 4rpx 20rpx rgba(250, 140, 22, 0.25);
  }

  &.status-bg-cooking {
    background: linear-gradient(135deg, #40a9ff, #1890ff);
    box-shadow: 0 4rpx 20rpx rgba(24, 144, 255, 0.25);
  }

  &.status-bg-done {
    background: linear-gradient(135deg, #52c41a, #389e0d);
    box-shadow: 0 4rpx 20rpx rgba(82, 196, 26, 0.25);
  }

  &.status-bg-canceled {
    background: linear-gradient(135deg, #8c8c8c, #595959);
    box-shadow: 0 4rpx 20rpx rgba(89, 89, 89, 0.2);
  }
}

.status-content {
  flex: 1;
}

.status-main {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.status-icon {
  font-size: 40rpx;
}

.status-title {
  font-size: 38rpx;
  font-weight: 700;
  letter-spacing: 1rpx;
}

.status-desc {
  margin-top: 12rpx;
  font-size: 24rpx;
  opacity: 0.9;
  line-height: 1.4;
}

/* 堂食/外带浮动药丸徽章 */
.dining-pill {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.95);
  padding: 10rpx 20rpx;
  border-radius: 16rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.1);

  &.takeout .dining-pill-type {
    color: #ea580c;
  }
  &.takeout .dining-pill-table {
    color: #9a3412;
  }

  &.dinein .dining-pill-type {
    color: #2563eb;
  }
  &.dinein .dining-pill-table {
    color: #1e40af;
  }
}

.dining-pill-type {
  font-size: 26rpx;
  font-weight: 700;
}

.dining-pill-table {
  font-size: 20rpx;
  margin-top: 4rpx;
  font-weight: 500;
}

/* 详情卡片通用 */
.detail-body {
  padding: 24rpx;
}

.detail-card {
  background: #ffffff;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid #f2f4f7;
  margin-bottom: 20rpx;
}

.card-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1e293b;
}

.card-subtitle {
  font-size: 24rpx;
  color: #94a3b8;
}

/* 物品列表及缩略图 */
.goods-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.goods-item {
  display: flex;
  align-items: flex-start;
  gap: 20rpx;
}

.goods-thumb {
  width: 120rpx;
  height: 120rpx;
  border-radius: 14rpx;
  overflow: hidden;
  background: #f8fafc;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1rpx solid #edf2f7;
}

.goods-img {
  width: 100%;
  height: 100%;
}

.goods-thumb-placeholder {
  width: 100%;
  height: 100%;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
}

.goods-placeholder-icon {
  font-size: 48rpx;
}

.goods-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 120rpx;
}

.goods-main-meta {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.goods-name-row {
  display: block;
  line-height: 1.35;
}

.goods-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #1e293b;
  line-height: 1.35;
  word-break: break-all;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.goods-spec {
  display: flex;
  flex-wrap: wrap;
  line-height: 1;
  margin: 0;
}

.spec-tag {
  display: inline-block;
  font-size: 20rpx;
  color: #64748b;
  background: #f1f5f9;
  padding: 4rpx 12rpx;
  border-radius: 6rpx;
  line-height: 1.3;
  max-width: 100%;
  word-break: break-all;
}

.goods-bottom-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 12rpx;
  line-height: 1.2;
}

.goods-price {
  font-size: 26rpx;
  color: #64748b;
}

.goods-count {
  font-size: 24rpx;
  color: #94a3b8;
}

.goods-subtotal {
  text-align: right;
  flex-shrink: 0;
  align-self: flex-start;
  padding-top: 4rpx;
}

.subtotal-val {
  font-size: 30rpx;
  font-weight: 700;
  color: #1e293b;
}

/* 费用汇总 */
.fee-section {
  margin-top: 24rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #f1f5f9;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.fee-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 26rpx;
  color: #64748b;
}

.discount-val {
  color: #ff4d4f;
  font-weight: 600;
}

.fee-divider {
  height: 1rpx;
  background: #f1f5f9;
  margin: 6rpx 0;
}

.total-row {
  margin-top: 6rpx;
  font-size: 28rpx;
  color: #1e293b;
}

.total-label {
  font-weight: 600;
}

.total-price-box {
  display: flex;
  align-items: baseline;
  color: #ff6b35;
}

.price-symbol {
  font-size: 24rpx;
  font-weight: 600;
  margin-right: 4rpx;
}

.price-val {
  font-size: 38rpx;
  font-weight: 700;
}

/* 订单信息列表 */
.info-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 26rpx;
}

.info-label {
  color: #94a3b8;
  width: 150rpx;
}

.info-val {
  color: #334155;
  font-weight: 500;
}

.info-code {
  font-family: monospace;
}

.info-val-wrap {
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.copy-tag {
  font-size: 20rpx;
  color: #2563eb;
  background: #eff6ff;
  border: 1rpx solid #bfdbfe;
  padding: 4rpx 14rpx;
  border-radius: 999rpx;
}

.phone-call-btn {
  font-size: 20rpx;
  color: #16a34a;
  background: #f0fdf4;
  border: 1rpx solid #bbf7d0;
  padding: 4rpx 14rpx;
  border-radius: 999rpx;
}

.highlight-val {
  color: #ff6b35;
  font-weight: 600;
}

.remark-row {
  margin-top: 8rpx;
  background: #fffbe6;
  border: 1rpx dashed #ffe58f;
  padding: 16rpx 20rpx;
  border-radius: 12rpx;
}

.remark-head {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
}

.remark-badge {
  background: #fa8c16;
  color: #fff;
  font-size: 20rpx;
  padding: 2rpx 10rpx;
  border-radius: 6rpx;
  font-weight: 600;
  flex-shrink: 0;
}

.remark-text {
  font-size: 24rpx;
  color: #d46b08;
  line-height: 1.4;
  word-break: break-all;
}

.cancel-row {
  margin-top: 8rpx;
  background: #fef2f2;
  border: 1rpx dashed #fca5a5;
  padding: 16rpx 20rpx;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.cancel-label {
  font-size: 22rpx;
  color: #ef4444;
  font-weight: 600;
}

.cancel-text {
  font-size: 24rpx;
  color: #b91c1c;
}

/* 留白 */
.bottom-placeholder {
  height: 160rpx;
}

/* 底部固定操作栏 */
.footer-actions {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #ffffff;
  padding: 20rpx 32rpx;
  padding-bottom: calc(20rpx + constant(safe-area-inset-bottom));
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 20rpx;
  z-index: 99;
}

.action-btn {
  padding: 18rpx 36rpx;
  border-radius: 999rpx;
  font-size: 28rpx;
  font-weight: 600;
  text-align: center;
  box-sizing: border-box;
  transition: all 0.2s;

  &:active {
    opacity: 0.8;
  }
}

.cancel-btn {
  border: 2rpx solid #cbd5e1;
  color: #64748b;
  background: #ffffff;
}

.secondary-btn {
  border: 2rpx solid #cbd5e1;
  color: #334155;
  background: #f8fafc;
}

.danger-btn {
  border: 2rpx solid #ff4d4f;
  color: #ff4d4f;
  background: #fff1f0;
}

.primary-btn {
  color: #ffffff;
}

.customer-primary {
  background: linear-gradient(135deg, #ff7a45, #ff6b35);
  box-shadow: 0 4rpx 14rpx rgba(255, 107, 53, 0.35);
}

.merchant-primary {
  background: linear-gradient(135deg, #4d95f5, #2f80ed);
  box-shadow: 0 4rpx 14rpx rgba(47, 128, 237, 0.35);
}

/* 加载中状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 160rpx 0;
  color: #94a3b8;
}

.loading-text {
  font-size: 26rpx;
  margin-top: 20rpx;
}

.mini-spinner {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  animation: spinner-rotate 0.8s linear infinite;
}

.customer-spinner {
  border: 4rpx solid rgba(255, 107, 53, 0.15);
  border-top-color: #ff6b35;
}

.merchant-spinner {
  border: 4rpx solid rgba(47, 128, 237, 0.15);
  border-top-color: #2f80ed;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 160rpx 32rpx;
}

.empty-icon {
  font-size: 100rpx;
}

.empty-text {
  font-size: 28rpx;
  color: #94a3b8;
  margin-top: 24rpx;
}

.empty-back-btn {
  margin-top: 36rpx;
  padding: 16rpx 48rpx;
  border-radius: 999rpx;
  background: #ff6b35;
  color: #fff;
  font-size: 26rpx;
  font-weight: 600;
}

@keyframes spinner-rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
