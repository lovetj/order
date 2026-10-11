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

    <!-- 线下付款提示/收款二维码卡片（若属于线下支付） -->
    <view v-if="order.id && order.payMode === 2" class="offline-pay-card" :class="{ 'customer-mode': !isMerchant }">
      <view class="offline-pay-head">
        <view class="offline-title-row">
          <text class="offline-icon">🏪</text>
          <text class="offline-title">{{ isMerchant ? '线下收款二维码' : '线下支付' }}</text>
          <text class="offline-status-badge" :class="order.payStatus === 2 ? 'paid' : 'unpaid'">
            {{ order.payStatus === 2 ? '店家已确认收款' : (isMerchant ? '等待店员确认收款' : '等待店家核验收款') }}
          </text>
        </view>
        <text class="offline-subtitle">
          {{ order.payStatus === 2 ? '本次款项已核验入账，祝您用餐愉快！' : (isMerchant ? '请出示下方店铺收款码，确认款项后点击核验' : '已选择线下支付，请扫描店家二维码付款，店员核验后将确认制作') }}
        </text>
      </view>

      <!-- 仅店家端支持出示/切换二维码展示（顾客端不展示二维码，仅保留提示信息） -->
      <template v-if="isMerchant">
        <view class="offline-type-tabs">
          <view
            class="offline-type-tab"
            :class="{ active: cardQrType === 'wx' }"
            @click="cardQrType = 'wx'"
          >
            微信收款码
          </view>
          <view
            class="offline-type-tab"
            :class="{ active: cardQrType === 'alipay' }"
            @click="cardQrType = 'alipay'"
          >
            支付宝收款码
          </view>
          <view
            class="offline-type-tab"
            :class="{ active: cardQrType === 'other' }"
            @click="cardQrType = 'other'"
          >
            其他收款码
          </view>
        </view>

        <view class="qrcode-display-area" v-if="currentCardQrcodeUrl">
          <image :src="currentCardQrcodeUrl" mode="aspectFit" class="pay-qrcode-image" @click="openFullPreview(currentCardQrcodeUrl)" />
          <text class="qrcode-tip">点击二维码可全屏放大展示</text>
        </view>
        <view class="qrcode-empty-box" v-else>
          <text class="empty-qr-text">当前类型未上传收款码，可切换其他方式或向店员付款</text>
        </view>
      </template>
    </view>

    <!-- 统一加载中占位动效 -->
    <page-loading
      v-if="loading && !order.id"
      :role="isMerchant ? 'merchant' : 'customer'"
      text="正在加载订单详情..."
    />

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

          <view class="info-row" v-if="order.payTypeText">
            <text class="info-label">支付方式</text>
            <text class="info-val">{{ order.payTypeText }}</text>
          </view>

          <view class="info-row" v-if="order.payStatusText">
            <text class="info-label">支付状态</text>
            <text class="info-val" :class="order.payStatus === 2 ? 'pay-paid' : 'pay-unpaid'">{{ order.payStatusText }}</text>
          </view>

          <view class="info-row" v-if="order.offlinePayTime">
            <text class="info-label">线下收款时间</text>
            <text class="info-val">{{ order.offlinePayTime }}</text>
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
        <!-- 线下支付未确认收款时，店家可点击“确认收款” -->
        <view
          v-if="order.payMode === 2 && order.payStatus !== 2 && order.status !== 'canceled'"
          class="action-btn offline-confirm-btn"
          @click="openMerchantOfflineModal"
        >
          确认已收款
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

    <!-- 店家端出示收款码 & 核验收款弹窗 -->
    <view v-if="showMerchantOfflineModal" class="merchant-qr-mask" @click="closeMerchantOfflineModal">
      <view class="merchant-qr-card" @click.stop="noop">
        <view class="merchant-qr-head">
          <text class="merchant-qr-title">出示收款码 & 核验收款</text>
          <text class="merchant-qr-close" @click="closeMerchantOfflineModal">✕</text>
        </view>

        <view class="merchant-qr-order-info">
          <view class="merchant-qr-row">
            <text class="row-label">订单编号：</text>
            <text class="row-val">{{ order.orderNo || order.id }}</text>
          </view>
          <view class="merchant-qr-row">
            <text class="row-label">应收金额：</text>
            <text class="row-val price">¥{{ order.amount }}</text>
          </view>
        </view>

        <!-- 收款方式类型选择切换 -->
        <view class="merchant-qr-tabs">
          <view
            class="merchant-qr-tab"
            :class="{ active: activeMerchantQrType === 'wx' }"
            @click="activeMerchantQrType = 'wx'"
          >
            微信收款码
          </view>
          <view
            class="merchant-qr-tab"
            :class="{ active: activeMerchantQrType === 'alipay' }"
            @click="activeMerchantQrType = 'alipay'"
          >
            支付宝收款码
          </view>
          <view
            class="merchant-qr-tab"
            :class="{ active: activeMerchantQrType === 'other' }"
            @click="activeMerchantQrType = 'other'"
          >
            其他收款码
          </view>
        </view>

        <!-- 收款码展示区域 -->
        <view class="merchant-qr-img-wrap">
          <image
            v-if="currentMerchantQrcodeUrl"
            :src="currentMerchantQrcodeUrl"
            mode="aspectFit"
            class="merchant-qr-img"
            @click="openFullPreview(currentMerchantQrcodeUrl)"
          />
          <view v-else class="merchant-qr-empty">
            <text class="empty-icon">📷</text>
            <text class="empty-text">未上传该类型的收款码</text>
            <text class="empty-tip">可在“我的 -> 支付管理”中上传维护</text>
          </view>
          <text v-if="currentMerchantQrcodeUrl" class="merchant-qr-hint">点击二维码可全屏放大展示给顾客</text>
        </view>

        <!-- 底部操作按钮 -->
        <view class="merchant-qr-actions">
          <button class="qr-btn cancel" @click="closeMerchantOfflineModal">取消</button>
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
        payType: null,
        payTypeText: '',
        payMode: null,
        payStatus: null,
        payStatusText: '',
        payQrcode: '',
        offlineWxQrcode: '',
        offlineAlipayQrcode: '',
        offlineOtherQrcode: '',
        offlinePayTime: '',
        action: null,
        items: []
      },
      itemErrors: {},
      // 详情页顶部线下收款卡片切换状态
      cardQrType: 'wx', // 'wx' | 'alipay' | 'other'
      // 全屏二维码大图浮层（层级高于一切弹窗，避免被遮挡）
      fullPreviewUrl: '',
      // 店家端线下收款弹窗
      showMerchantOfflineModal: false,
      activeMerchantQrType: 'wx', // 'wx' | 'alipay' | 'other'
      confirmingOffline: false,
      merchantPaymentConfig: {
        offlineWxQrcode: '',
        offlineAlipayQrcode: '',
        offlineOtherQrcode: ''
      }
    }
  },

  computed: {
    // 详情卡片当前展示的二维码
    currentCardQrcodeUrl() {
      const wxQr = this.merchantPaymentConfig.offlineWxQrcode || this.order.offlineWxQrcode || ''
      const alipayQr = this.merchantPaymentConfig.offlineAlipayQrcode || this.order.offlineAlipayQrcode || ''
      const otherQr = this.merchantPaymentConfig.offlineOtherQrcode || this.order.offlineOtherQrcode || ''
      if (this.cardQrType === 'wx') return wxQr || (this.order.payType === 3 ? this.order.payQrcode : '')
      if (this.cardQrType === 'alipay') return alipayQr || (this.order.payType === 4 ? this.order.payQrcode : '')
      if (this.cardQrType === 'other') return otherQr || (this.order.payType === 5 ? this.order.payQrcode : '')
      return this.order.payQrcode || ''
    },
    currentMerchantQrcodeUrl() {
      if (this.activeMerchantQrType === 'wx') return this.merchantPaymentConfig.offlineWxQrcode || ''
      if (this.activeMerchantQrType === 'alipay') return this.merchantPaymentConfig.offlineAlipayQrcode || ''
      if (this.activeMerchantQrType === 'other') return this.merchantPaymentConfig.offlineOtherQrcode || ''
      return ''
    },
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

    if (this.isMerchant) {
      this.loadMerchantPaymentConfig()
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

    // 全屏放大查看二维码（最高层级无遮挡）
    openFullPreview(url) {
      if (!url) return
      this.fullPreviewUrl = url
    },

    closeFullPreview() {
      this.fullPreviewUrl = ''
    },

    previewQrcode(url) {
      this.openFullPreview(url)
    },

    noop() {},

    loadMerchantPaymentConfig() {
      api.getMerchantPaymentConfig().then((res) => {
        if (res) {
          this.merchantPaymentConfig = {
            offlineWxQrcode: res.offlineWxQrcode || '',
            offlineAlipayQrcode: res.offlineAlipayQrcode || '',
            offlineOtherQrcode: res.offlineOtherQrcode || ''
          }
        }
      }).catch(() => {})
    },

    openMerchantOfflineModal() {
      this.showMerchantOfflineModal = true
      if (!this.merchantPaymentConfig.offlineWxQrcode && this.merchantPaymentConfig.offlineAlipayQrcode) {
        this.activeMerchantQrType = 'alipay'
      } else if (!this.merchantPaymentConfig.offlineWxQrcode && !this.merchantPaymentConfig.offlineAlipayQrcode && this.merchantPaymentConfig.offlineOtherQrcode) {
        this.activeMerchantQrType = 'other'
      } else {
        this.activeMerchantQrType = 'wx'
      }
      this.loadMerchantPaymentConfig()
    },

    closeMerchantOfflineModal() {
      this.showMerchantOfflineModal = false
    },

    confirmOfflinePaySubmit() {
      const id = this.order.id || this.order.orderNo || this.orderId
      this.confirmingOffline = true
      api.confirmOfflinePay(id).then(() => {
        this.confirmingOffline = false
        this.closeMerchantOfflineModal()
        uni.showToast({ title: '已确认收款', icon: 'success' })
        this.fetchOrderDetail()
      }).catch((err) => {
        this.confirmingOffline = false
        uni.showToast({ title: (err && err.message) || '确认失败', icon: 'none' })
      })
    },

    merchantConfirmOffline() {
      uni.showModal({
        title: '收款核验确认',
        content: `请确认已在线下收到该笔款项 ¥${this.order.amount}？`,
        confirmText: '确认收到',
        confirmColor: '#07c160',
        success: (res) => {
          if (!res.confirm) return
          uni.showLoading({ title: '确认中...' })
          api.confirmOfflinePay(this.order.id).then(() => {
            uni.hideLoading()
            uni.showToast({ title: '已确认收款', icon: 'success' })
            this.fetchOrderDetail()
          }).catch((err) => {
            uni.hideLoading()
            uni.showToast({ title: (err && err.message) || '确认失败', icon: 'none' })
          })
        }
      })
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

/* ==================== 线下付款二维码卡片 ==================== */
.offline-pay-card {
  margin: 20rpx 24rpx;
  background: #ffffff;
  border-radius: 24rpx;
  padding: 28rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.05);
  border: 2rpx dashed #faad14;
}

.offline-pay-head {
  margin-bottom: 20rpx;
}

.offline-pay-card.customer-mode .offline-pay-head {
  margin-bottom: 0;
}

.offline-title-row {
  display: flex;
  align-items: center;
  margin-bottom: 8rpx;
}

.offline-icon {
  font-size: 36rpx;
  margin-right: 12rpx;
}

.offline-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #1e293b;
  flex: 1;
}

.offline-status-badge {
  font-size: 22rpx;
  padding: 4rpx 14rpx;
  border-radius: 20rpx;
  font-weight: 500;
}

.offline-status-badge.unpaid {
  background: #fff7e6;
  color: #fa8c16;
}

.offline-status-badge.paid {
  background: #e6f7ef;
  color: #07c160;
}

.offline-subtitle {
  font-size: 24rpx;
  color: #8c8c8c;
  line-height: 1.4;
  display: block;
}

.qrcode-display-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20rpx 0;
  background: #fafafa;
  border-radius: 16rpx;
}

.pay-qrcode-image {
  width: 320rpx;
  height: 320rpx;
  border-radius: 12rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.08);
}

.qrcode-tip {
  font-size: 22rpx;
  color: #94a3b8;
  margin-top: 14rpx;
}

.qrcode-empty-box {
  padding: 30rpx 20rpx;
  text-align: center;
  background: #fafafa;
  border-radius: 16rpx;
}

.empty-qr-text {
  font-size: 26rpx;
  color: #fa8c16;
}

.pay-paid {
  color: #07c160 !important;
  font-weight: 600;
}

.pay-unpaid {
  color: #fa8c16 !important;
  font-weight: 600;
}

.action-btn.offline-confirm-btn {
  background: #52c41a;
  color: #ffffff;
}

/* ==================== 店家端线下核验收款弹窗 ==================== */
.merchant-qr-mask {
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

.merchant-qr-card {
  width: 100%;
  max-width: 620rpx;
  background: #ffffff;
  border-radius: 28rpx;
  overflow: hidden;
  box-shadow: 0 16rpx 48rpx rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
}

.merchant-qr-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 32rpx 20rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.merchant-qr-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1f2937;
}

.merchant-qr-close {
  font-size: 36rpx;
  color: #9ca3af;
  padding: 0 10rpx;
}

.merchant-qr-order-info {
  background: #f8fafc;
  padding: 16rpx 32rpx;
  border-bottom: 1rpx solid #f1f5f9;
}

.merchant-qr-row {
  display: flex;
  align-items: center;
  font-size: 24rpx;
  line-height: 1.6;
}

.merchant-qr-row .row-label {
  color: #64748b;
  width: 140rpx;
}

.merchant-qr-row .row-val {
  color: #334155;
  font-weight: 500;
}

.merchant-qr-row .row-val.price {
  color: #ff4d4f;
  font-size: 30rpx;
  font-weight: 700;
}

.merchant-qr-tabs {
  display: flex;
  padding: 20rpx 32rpx 10rpx;
  gap: 16rpx;
}

.merchant-qr-tab {
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

.merchant-qr-tab.active {
  color: #2563eb;
  background: #eff6ff;
  border-color: #93c5fd;
  font-weight: 600;
}

.merchant-qr-img-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24rpx 32rpx;
  min-height: 400rpx;
}

.merchant-qr-img {
  width: 380rpx;
  height: 380rpx;
  border-radius: 16rpx;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.08);
  background: #fff;
}

.merchant-qr-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40rpx 0;
}

.merchant-qr-empty .empty-icon {
  font-size: 72rpx;
  margin-bottom: 16rpx;
}

.merchant-qr-empty .empty-text {
  font-size: 26rpx;
  color: #64748b;
  font-weight: 600;
}

.merchant-qr-empty .empty-tip {
  font-size: 22rpx;
  color: #94a3b8;
  margin-top: 8rpx;
}

.merchant-qr-hint {
  font-size: 22rpx;
  color: #64748b;
  margin-top: 16rpx;
}

.merchant-qr-actions {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 20rpx 32rpx 32rpx;
  border-top: 1rpx solid #f1f5f9;
}

.merchant-qr-actions .qr-btn {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 40rpx;
  font-size: 28rpx;
  text-align: center;
  font-weight: 600;
}

.merchant-qr-actions .qr-btn.cancel {
  background: #f1f5f9;
  color: #64748b;
}

.merchant-qr-actions .qr-btn.confirm {
  background: #16a34a;
  color: #ffffff;
  box-shadow: 0 6rpx 16rpx rgba(22, 163, 74, 0.35);
}

.action-btn.offline-confirm-btn:active {
  background: #389e0d;
}

.offline-type-tabs {
  display: flex;
  margin: 20rpx 0 14rpx;
  gap: 16rpx;
}

.offline-type-tab {
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

.offline-type-tab.active {
  color: #2563eb;
  background: #eff6ff;
  border-color: #93c5fd;
  font-weight: 600;
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
