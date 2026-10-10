<template>
  <view class="menu-page">
    <bottom-nav />

    <!-- 顶部状态栏：就餐方式切换、桌号提示与快捷重扫 -->
    <view class="top-bar" v-if="!needScan">
      <view class="dining-switch">
        <view
          class="switch-btn"
          :class="{ active: diningType === 1 }"
          @click="changeDiningType(1)"
        >
          <text class="btn-icon">🍽️</text>
          <text>堂食</text>
        </view>
        <view
          class="switch-btn"
          :class="{ active: diningType === 2 }"
          @click="changeDiningType(2)"
        >
          <text class="btn-icon">🛍️</text>
          <text>外带</text>
        </view>
      </view>

      <view class="table-badge" v-if="diningType === 1">
        <text class="table-icon">🪑</text>
        <text class="table-label">桌位：</text>
        <text class="table-no">{{currentTableText}}</text>
      </view>
      <view class="table-badge takeout-badge" v-else>
        <text class="table-icon">🥡</text>
        <text class="table-label">免占桌</text>
      </view>

      <view class="rescan-btn" @click="scanThenReload">
        <text class="rescan-icon">📷</text>
        <text class="rescan-text">{{ diningType === 1 ? '换桌/扫码' : '扫码' }}</text>
      </view>
    </view>

    <!-- 未识别店铺/桌号时提示扫码 -->
    <view v-if="needScan" class="scan-tip">
      <view class="scan-tip-card">
        <view class="scan-tip-icon">📷</view>
        <view class="scan-tip-text">
          <view class="scan-tip-title">请先扫描桌位二维码</view>
          <view class="scan-tip-desc">未识别到店铺与桌号，无法点餐</view>
        </view>
        <view class="scan-tip-btn" @click="scanThenReload">立即扫码</view>
      </view>
    </view>

    <!-- 主体双栏区域 -->
    <view class="body-content">
      <!-- 左侧分类侧边栏 -->
      <view class="cat-wrap">
        <scroll-view class="cat-list" scroll-y enhanced :show-scrollbar="false">
          <view
            class="cat-item"
            :class="{ active: currentCategory === 'all' }"
            @click="switchCategory('all')"
          >
            <view class="cat-indicator"></view>
            <text class="cat-name">全部菜品</text>
          </view>
          <view
            v-for="item in categories"
            :key="item.id"
            class="cat-item"
            :class="{ active: currentCategory === item.id }"
            @click="switchCategory(item.id)"
          >
            <view class="cat-indicator"></view>
            <text class="cat-name">{{item.name}}</text>
          </view>
          <view class="cat-bottom-space"></view>
        </scroll-view>
      </view>

      <!-- 右侧菜品列表 -->
      <view class="goods-wrap">
        <scroll-view
          class="goods-list"
          scroll-y
          enhanced
          :show-scrollbar="false"
          :lower-threshold="60"
          @scrolltolower="loadMore"
        >
          <!-- 菜品卡片列表 -->
          <view class="goods-item" v-for="item in goodsList" :key="item.key || item.id">
            <!-- 缩略图 -->
            <view class="thumb">
              <image v-if="item.hasImage" class="thumb-img" :src="item.imageUrl" mode="aspectFill"></image>
              <view v-else class="thumb-placeholder">
                <text class="thumb-icon">{{item.image || '🍽️'}}</text>
              </view>
              <!-- 热销标签浮标 -->
              <view v-if="item.isHot" class="badge-hot">
                <text class="badge-hot-icon">🔥</text>
                <text class="badge-hot-text">热销</text>
              </view>
            </view>

            <!-- 菜品信息 -->
            <view class="info">
              <!-- 标题行 -->
              <view class="name-row">
                <text class="name">{{item.name}}</text>
              </view>

              <!-- 描述 -->
              <view class="desc" v-if="item.desc">{{item.desc}}</view>

              <!-- 价格与销量 -->
              <view class="meta">
                <view class="price-box">
                  <text class="price-symbol">¥</text>
                  <text class="price-val">{{item.price}}</text>
                </view>
                <view class="tags-box">
                  <text v-if="item.minBuy > 1" class="minbuy-tag">{{item.minBuy}}份起购</text>
                  <text class="sales">已售 {{item.sales || 0}}</text>
                </view>
              </view>

              <!-- 加减操作/选规格按钮 -->
              <view class="ops">
                <view v-if="item.count > 0" class="step-btn minus" @click.stop="minusGoodsItem(item)">
                  <text class="step-icon">−</text>
                </view>
                <text v-if="item.count > 0" class="step-num">{{item.count}}</text>

                <view v-if="item.minBuy > 1" class="min-buy-btn" @click.stop="addItem(item.id)">
                  <text>{{item.minBuy}}份起购</text>
                </view>
                <view v-else class="step-btn plus" @click.stop="addItem(item.id)">
                  <text class="step-icon">＋</text>
                </view>
              </view>
            </view>
          </view>

          <!-- 空状态 -->
          <view v-if="!listLoading && goodsList.length === 0" class="empty-wrap">
            <view class="empty-icon-box">🍱</view>
            <view class="empty-title">该分类暂无在售菜品</view>
            <view class="empty-sub">去看看其他分类的美味佳肴吧</view>
          </view>

          <!-- 首屏加载动画 -->
          <view v-if="listLoading" class="list-loading">
            <view class="mini-spinner"></view>
            <text class="list-loading-text">正在加载菜单…</text>
          </view>

          <!-- 触底追加加载动画 -->
          <view v-if="!listLoading && loadingMore" class="list-loading list-loading-more">
            <view class="mini-spinner"></view>
            <text class="list-loading-text">加载更多中…</text>
          </view>

          <!-- 到底了提示 -->
          <view v-if="!listLoading && !loadingMore && !hasMore && goodsList.length > 0" class="list-end">
            <view class="end-line"></view>
            <text class="end-text">已展示全部菜品</text>
            <view class="end-line"></view>
          </view>

          <view class="bottom-space"></view>
        </scroll-view>
      </view>
    </view>

    <!-- 购物车面板蒙版与弹窗 -->
    <view v-if="showCart" class="cart-mask" @click="closeCart"></view>
    <view v-if="showCart" class="cart-panel">
      <view class="cart-head">
        <view class="cart-head-title">
          <text class="cart-head-icon">🛒</text>
          <text>已选商品 ({{cartCount}})</text>
        </view>
        <view class="cart-clear" @click="clearCart">
          <text class="clear-icon">🗑</text>
          <text>清空购物车</text>
        </view>
      </view>

      <scroll-view scroll-y class="cart-scroll-list">
        <view class="cart-item" v-for="item in cartList" :key="item.key">
          <view class="cart-img">
            <image v-if="item.hasImage" class="cart-img-real" :src="item.imageUrl" mode="aspectFill"></image>
            <text v-else class="cart-img-icon">{{item.image || '🍽️'}}</text>
          </view>
          <view class="cart-info">
            <view class="cart-name">{{item.name}}</view>
            <view v-if="item.specText" class="cart-spec">{{item.specText}}</view>
            <view class="cart-price-line">
              <text class="cart-price-symbol">¥</text>
              <text class="cart-price-val">{{item.price}}</text>
            </view>
          </view>
          <view class="stepper small">
            <view class="step-btn minus" @click.stop="minusItem(item.key)">
              <text class="step-icon">−</text>
            </view>
            <text class="step-num">{{item.count}}</text>
            <view class="step-btn plus" @click.stop="plusCartItem(item.key)">
              <text class="step-icon">＋</text>
            </view>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 悬浮结算栏 -->
    <view class="checkout-bar" v-if="!needScan">
      <!-- 购物车胶囊入口 -->
      <view class="cart-entry" @click="toggleCart">
        <view class="cart-icon-bg" :class="{ active: cartCount > 0 }">
          <text class="cart-emoji">🛒</text>
          <view v-if="cartCount" class="badge">{{cartCount}}</view>
        </view>
      </view>

      <!-- 金额展示 -->
      <view class="checkout-amount" @click="toggleCart">
        <view v-if="cartCount" class="amount-wrap">
          <view class="amount-main">
            <text class="amount-label">合计</text>
            <text class="amount-symbol">¥</text>
            <text class="amount-val">{{cartAmount}}</text>
          </view>
          <text class="amount-tip">已自动匹配最优优惠</text>
        </view>
        <view v-else class="empty-tip">未选购任何菜品</view>
      </view>

      <!-- 结算按钮 -->
      <view class="checkout-btn" :class="{ disabled: !cartCount }" @click="submitOrder">
        <text class="checkout-btn-text">去结算</text>
      </view>
    </view>

    <!-- 支付方式选择与结算弹窗 -->
    <view v-if="showPayModal" class="pay-modal-mask" @click="closePayModal"></view>
    <view v-if="showPayModal" class="pay-modal">
      <view class="pay-modal-head">
        <text class="pay-modal-title">确认下单与支付</text>
        <text class="pay-modal-close" @click="closePayModal">✕</text>
      </view>

      <view class="pay-modal-summary">
        <view class="summary-line">
          <text class="label">就餐方式</text>
          <text class="val highlight">{{diningType === 2 ? '外带自提' : (tableNo ? `${tableNo} 号桌` : '堂食点餐')}}</text>
        </view>
        <view class="summary-line">
          <text class="label">商品件数</text>
          <text class="val">{{cartCount}} 件</text>
        </view>
        <view class="summary-line" v-if="currentBestCoupon">
          <text class="label">已享优惠</text>
          <text class="val coupon-label">{{currentBestCoupon.label}}</text>
        </view>
        <view class="summary-line total-line">
          <text class="label">应付金额</text>
          <text class="total-price">¥{{cartAmount}}</text>
        </view>
      </view>

      <view class="pay-options-title">选择支付方式</view>
      <view class="pay-options-list">
        <!-- 线下支付选项 -->
        <view
          class="pay-option-item"
          :class="{
            disabled: !shopPaymentConfig.offlinePayEnabled,
            selected: selectedPayMode === 2 && shopPaymentConfig.offlinePayEnabled
          }"
          @click="selectPayment(2, 3)"
        >
          <view class="pay-opt-left">
            <text class="pay-opt-icon">🏪</text>
            <view class="pay-opt-info">
              <view class="pay-opt-name-wrap">
                <text class="pay-opt-name">线下支付</text>
                <text v-if="!shopPaymentConfig.offlinePayEnabled" class="disabled-tag">暂未开通</text>
              </view>
              <text class="pay-opt-desc">扫描店家二维码付款，店员核验后制作</text>
            </view>
          </view>
          <view class="pay-radio" :class="{ checked: selectedPayMode === 2 && shopPaymentConfig.offlinePayEnabled }">
            <view class="radio-inner" v-if="selectedPayMode === 2 && shopPaymentConfig.offlinePayEnabled"></view>
          </view>
        </view>

        <!-- 线上微信支付 -->
        <view
          class="pay-option-item"
          :class="{
            disabled: !shopPaymentConfig.onlineWxPayEnabled,
            selected: selectedPayMode === 1 && selectedPayType === 1 && shopPaymentConfig.onlineWxPayEnabled
          }"
          @click="selectPayment(1, 1)"
        >
          <view class="pay-opt-left">
            <text class="pay-opt-icon">💬</text>
            <view class="pay-opt-info">
              <view class="pay-opt-name-wrap">
                <text class="pay-opt-name">微信支付</text>
                <text v-if="!shopPaymentConfig.onlineWxPayEnabled" class="disabled-tag">暂未开通</text>
              </view>
              <text class="pay-opt-desc">微信安全线上极速支付</text>
            </view>
          </view>
          <view class="pay-radio" :class="{ checked: selectedPayMode === 1 && selectedPayType === 1 && shopPaymentConfig.onlineWxPayEnabled }">
            <view class="radio-inner" v-if="selectedPayMode === 1 && selectedPayType === 1 && shopPaymentConfig.onlineWxPayEnabled"></view>
          </view>
        </view>

        <!-- 线上支付宝支付 -->
        <view
          class="pay-option-item"
          :class="{
            disabled: !shopPaymentConfig.onlineAlipayPayEnabled,
            selected: selectedPayMode === 1 && selectedPayType === 2 && shopPaymentConfig.onlineAlipayPayEnabled
          }"
          @click="selectPayment(1, 2)"
        >
          <view class="pay-opt-left">
            <text class="pay-opt-icon">🪙</text>
            <view class="pay-opt-info">
              <view class="pay-opt-name-wrap">
                <text class="pay-opt-name">支付宝支付</text>
                <text v-if="!shopPaymentConfig.onlineAlipayPayEnabled" class="disabled-tag">暂未开通</text>
              </view>
              <text class="pay-opt-desc">支付宝便捷支付</text>
            </view>
          </view>
          <view class="pay-radio" :class="{ checked: selectedPayMode === 1 && selectedPayType === 2 && shopPaymentConfig.onlineAlipayPayEnabled }">
            <view class="radio-inner" v-if="selectedPayMode === 1 && selectedPayType === 2 && shopPaymentConfig.onlineAlipayPayEnabled"></view>
          </view>
        </view>
      </view>

      <view class="pay-submit-wrap">
        <button class="pay-confirm-btn" :loading="submitting" :disabled="submitting" @click="confirmCreateOrder">
          {{selectedPayMode === 2 ? '立即下单（线下付款）' : '立即支付 ¥' + cartAmount}}
        </button>
      </view>
    </view>
  </view>
</template>

<script>
// 顾客端点餐
const app = getApp()
import api from '@/api/index'
import { formatImageUrl } from '@/utils/util'
import { hasCustomerContext, scanOrderContext, ensureCustomerContext } from '@/utils/scan'

export default {
  data() {
    return {
      tableNo: (app.globalData && app.globalData.tableNo) || uni.getStorageSync('tableNo') || '',
      diningType: (app.globalData && app.globalData.diningType) || (uni.getStorageSync('diningType') ? Number(uni.getStorageSync('diningType')) : 1),
      _resolvedTableId: '',
      categories: [],
      currentCategory: 'all',
      goodsList: [],
      // 分页加载状态
      currentPage: 1,
      pageSize: 10,
      hasMore: true,
      listLoading: false,   // 首屏 / 切换分类加载
      loadingMore: false,   // 触底追加加载
      cart: {},          // { dishId: count }
      cartList: [],
      cartCount: 0,
      cartAmount: 0,
      showCart: false,
      needScan: false,
      // 支付弹窗与店铺支付方式配置
      showPayModal: false,
      submitting: false,
      selectedPayType: 3, // 1微信线上 2支付宝线上 3线下微信 4线下支付宝 5线下其他
      selectedPayMode: 2, // 1线上 2线下
      shopPaymentConfig: {
        offlinePayEnabled: true,
        onlineWxPayEnabled: false,
        onlineAlipayPayEnabled: false
      },
      currentBestCoupon: null
    }
  },

  computed: {
    currentTableText() {
      const tableNo = this.tableNo || (app.globalData && app.globalData.tableNo) || uni.getStorageSync('tableNo') || ''
      return tableNo ? `${tableNo} 号桌` : '堂食点餐'
    }
  },

  onLoad(options) {
    // 进入点餐页即处理路由上下文：shopId/tableId/tableNo 落缓存；未登录则跳登录页（防重入），
    // 未登录时返回 false，onShow 里不会再发起任何需要登录/三要素的接口。
    this._ctxReady = ensureCustomerContext(options)
    if (options && options.tableNo) {
      this.tableNo = String(options.tableNo)
    } else if (app.globalData && app.globalData.tableNo) {
      this.tableNo = String(app.globalData.tableNo)
    }
  },

  onShow() {
    // 扫码进入或具备店铺/桌位上下文：明确是顾客行为，自动确认为顾客身份，坚决不跳 role 选择页
    if (app.globalData.shopId || app.globalData.tableId || uni.getStorageSync('shopId') || uni.getStorageSync('tableId')) {
      app.globalData.setRole('customer')
    } else if (app.globalData.role !== 'customer') {
      uni.reLaunch({ url: '/pages/role/role' })
      return
    }
    // 未登录：onLoad 已通过 ensureCustomerContext 跳转登录，这里若仍未登录则直接中止后续请求
    if (!app.globalData.isLogin()) return
    this.needScan = !hasCustomerContext(app)
    // 无店铺/桌位上下文时不发请求，仅提示扫码
    if (this.needScan) return
    this.tableNo = (app.globalData && app.globalData.tableNo) || uni.getStorageSync('tableNo') || this.tableNo
    // 扫码进入时链接仅携带桌位ID，回查桌号供结算使用
    this.resolveTableNo()
    if (!this.categories.length) {
      this.loadData()
    } else {
      // 回到页面时同步后端最新菜品（价格/库存可能变化）
      this.loadList(true)
    }
  },

  methods: {
    // 切换就餐方式
    changeDiningType(type) {
      this.diningType = type
      if (app.globalData && app.globalData.setDiningType) {
        app.globalData.setDiningType(type)
      } else {
        uni.setStorageSync('diningType', type)
      }
      uni.showToast({
        title: type === 1 ? '已切换为堂食用餐' : '已切换为外带自提',
        icon: 'none'
      })
    },

    // 桌号回查：按桌位ID查询桌号，及时同步响应式 tableNo 与 globalData
    resolveTableNo() {
      const tableId = (app.globalData && app.globalData.tableId) || uni.getStorageSync('tableId') || ''
      if (!tableId) return
      // 已有桌号且桌位未发生变更时无需重复请求
      if (this.tableNo && app.globalData && app.globalData.tableNo && this._resolvedTableId === tableId) {
        return
      }
      this._resolvedTableId = tableId
      api.getTableByCustomerId(tableId).then((t) => {
        if (t && t.tableNo) {
          this.tableNo = String(t.tableNo)
          if (app.globalData && app.globalData.setTableNo) {
            app.globalData.setTableNo(t.tableNo)
          }
        }
      }).catch(() => {})
    },

    // 底部导航切换时刷新当前页面数据
    onTabRefresh() {
      if (!app.globalData.isLogin()) return
      this.needScan = !hasCustomerContext(app)
      if (this.needScan) return
      this.tableNo = (app.globalData && app.globalData.tableNo) || uni.getStorageSync('tableNo') || this.tableNo
      this.resolveTableNo()
      if (!this.categories.length) {
        this.loadData()
      } else {
        this.loadList(true)
      }
    },

    // 扫码识别店铺/桌号后刷新页面
    async scanThenReload() {
      const { ok, shopChanged, tableNo } = await scanOrderContext(app)
      if (!ok) return
      this.needScan = false
      if (tableNo) {
        this.tableNo = String(tableNo)
      }
      this.resolveTableNo()
      // 切换店铺：清空旧店分类与购物车缓存，强制重新拉取本店菜单
      if (shopChanged) {
        this.currentCategory = 'all'
        this.showCart = false
        this.cart = app.globalData.cart
        this.cartList = []
        this.cartCount = 0
        this.cartAmount = 0
        this.loadData()
      } else if (!this.categories.length) {
        this.loadData()
      } else {
        this.loadList(true)
      }
    },

    // 加载分类 + 第一页菜品
    loadData() {
      this.categories = []
      this.loadCategories()
      this.loadList(true)
    },

    loadCategories() {
      api.getCategories().then((categories) => {
        this.categories = categories || []
      }).catch(() => {
        this.categories = []
      })
    },

    /**
     * 统一处理菜品图片：数据库存相对路径（历史数据可能是完整 URL），
     * hasImage 标记是否为真实图片，imageUrl 为拼好的完整展示地址。
     */
    decorateImage(g) {
      const img = (g && g.image) || ''
      const hasImage = /^https?:\/\//.test(img) || img.startsWith('/')
      return { ...g, hasImage, imageUrl: hasImage ? formatImageUrl(img) : '' }
    },

    // 分页查询：reset=true 重新加载第一页，否则追加一页
    loadList(reset) {
      if (reset) {
        if (this.listLoading) return
        this.listLoading = true
        this.currentPage = 1
        this.hasMore = true
      } else {
        if (this.listLoading || this.loadingMore) return
        this.loadingMore = true
      }
      const params = {
        pageNum: this.currentPage,
        pageSize: this.pageSize,
        categoryId: this.currentCategory === 'all' ? null : this.currentCategory
      }
      api.customerPageDishes(params).then((page) => {
        const records = ((page && page.records) || []).map((g) => {
          // 列表行减号使用的购物车 key：无规格菜品的主条目为「id|」
          const decorated = this.decorateImage(g)
          return { ...decorated, key: `${g.id}|` }
        })
        this.goodsList = reset ? records : this.goodsList.concat(records)
        this.hasMore = this.currentPage < ((page && page.pages) || 0)
        this.currentPage += 1
        this.listLoading = false
        this.loadingMore = false
        // 用当前购物车刷新列表角标
        this.syncCartCounts()
      }).catch(() => {
        this.listLoading = false
        this.loadingMore = false
      })
    },

    // 切换分类：重置到顶部并按新分类加载第一页
    switchCategory(id) {
      if (this.currentCategory === id) return
      this.currentCategory = id
      this.loadList(true)
    },

    // 触底加载下一页（scroll-view scrolltolower 触发）
    loadMore() {
      if (this.listLoading || this.loadingMore || !this.hasMore) return
      this.loadList(false)
    },

    // 用购物车份数刷新列表角标
    syncCartCounts() {
      const { cart, goodsList } = this
      const countByDish = {}
      Object.keys(cart).forEach((key) => {
        const entry = cart[key]
        if (!entry) return
        countByDish[entry.id] = (countByDish[entry.id] || 0) + entry.count
      })
      this.goodsList = goodsList.map((g) => ({
        ...g,
        count: countByDish[g.id] || 0
      }))
    },

    /**
     * 点击 + ：跳转规格选择页
     * 规格页会拉取菜品规格；若无规格，返回时直接加入购物车
     * 注：起购份数(minBuy) 由规格页的步进器保证首次加入时不少于 minBuy
     */
    addItem(id) {
      uni.navigateTo({ url: `/pages/spec-choose/spec-choose?id=${id}` })
    },

    /**
     * 规格选择页回调：加入带规格的购物车项
     * 无规格菜品返回时 specIds 为空、extraPrice 为 0
     * @param {Object} result { dishId, quantity, specIds, specText, extraPrice, unitPrice }
     */
    onSpecChosen(result) {
      this.addToCart(result.dishId, result.quantity, result.specIds, result.specText, result.extraPrice)
    },

    /**
     * 加入购物车
     * 购物车 key = dishId + '|' + specText（同菜品不同规格视为不同条目）
     */
    addToCart(dishId, quantity, specIds, specText, extraPrice) {
      const dish = this.goodsList.find((g) => g.id === dishId)
      if (!dish) return
      const unitPrice = Number(dish.price) + (Number(extraPrice) || 0)
      const key = dishId + '|' + (specText || '')
      const cart = this.cart
      if (cart[key]) {
        cart[key].count += quantity
      } else {
        cart[key] = {
          id: dishId,
          key,
          name: dish.name,
          price: unitPrice,
          image: dish.image,
          hasImage: dish.hasImage,
          imageUrl: dish.imageUrl,
          specIds: specIds || [],
          specText: specText || '',
          count: quantity
        }
      }
      this.updateCart(cart)
    },

    minusItem(key) {
      const cart = { ...this.cart }
      if (!cart[key]) return
      cart[key].count -= 1
      if (cart[key].count <= 0) delete cart[key]
      this.updateCart(cart)
    },

    minusGoodsItem(item) {
      if (!item || !item.id) return
      const dishId = item.id
      const cart = { ...this.cart }
      const matchedKeys = Object.keys(cart).filter((k) => cart[k] && cart[k].id === dishId)
      if (matchedKeys.length === 0) return

      // 若有多个规格，优先减少最后添加的条目
      const key = matchedKeys[matchedKeys.length - 1]
      cart[key].count -= 1
      if (cart[key].count <= 0) {
        delete cart[key]
      }
      this.updateCart(cart)
    },

    plusCartItem(key) {
      const cart = { ...this.cart }
      if (!cart[key]) return
      cart[key].count += 1
      this.updateCart(cart)
    },

    updateCart(cart) {
      let cartCount = 0
      let cartAmount = 0
      const cartList = []
      // 汇总每个菜品的总数量（用于列表角标）
      const countByDish = {}
      Object.keys(cart).forEach((key) => {
        const entry = cart[key]
        cartCount += entry.count
        cartAmount += entry.count * entry.price
        countByDish[entry.id] = (countByDish[entry.id] || 0) + entry.count
        cartList.push(entry)
      })
      const goodsList = this.goodsList.map((g) => ({
        ...g,
        count: countByDish[g.id] || 0
      }))
      app.globalData.cart = cart
      this.cart = cart
      this.cartList = cartList
      this.cartCount = cartCount
      this.cartAmount = Number(cartAmount.toFixed(2))
      this.goodsList = goodsList
      if (cartCount === 0) {
        this.showCart = false
      }
    },

    toggleCart() {
      if (!this.cartCount) return
      this.showCart = !this.showCart
    },

    closeCart() {
      this.showCart = false
    },

    clearCart() {
      this.showCart = false
      this.updateCart({})
    },

    submitOrder() {
      if (!this.cartCount) {
        uni.showToast({ title: '请先选择菜品', icon: 'none' })
        return
      }
      const diningType = this.diningType || (app.globalData && app.globalData.diningType) || 1
      let tableNo = app.globalData.tableNo || this.tableNo || ''
      if (diningType === 1 && !tableNo) {
        uni.showToast({ title: '堂食点餐请先扫码获取桌号', icon: 'none' })
        return
      }
      if (diningType === 2 && !tableNo) {
        tableNo = 'TAKEOUT'
      }

      const shopId = (app.globalData && app.globalData.shopId) || uni.getStorageSync('shopId') || '1'
      uni.showLoading({ title: '加载支付信息...' })
      // 1. 查询店铺支付开关配置
      api.getShopPaymentConfig(shopId).then((payConf) => {
        if (payConf) {
          this.shopPaymentConfig = payConf
          // 智能预选可用支付方式
          if (payConf.offlinePayEnabled) {
            this.selectedPayMode = 2
            this.selectedPayType = 3
          } else if (payConf.onlineWxPayEnabled) {
            this.selectedPayMode = 1
            this.selectedPayType = 1
          } else if (payConf.onlineAlipayPayEnabled) {
            this.selectedPayMode = 1
            this.selectedPayType = 2
          }
        }
        // 2. 匹配最优优惠券
        return api.getBestCoupon(this.cartAmount)
      }).then((coupon) => {
        uni.hideLoading()
        this.currentBestCoupon = coupon || null
        this.showCart = false
        this.showPayModal = true
      }).catch(() => {
        uni.hideLoading()
        this.currentBestCoupon = null
        this.showCart = false
        this.showPayModal = true
      })
    },

    closePayModal() {
      this.showPayModal = false
    },

    selectPayment(mode, type) {
      if (mode === 2 && !this.shopPaymentConfig.offlinePayEnabled) {
        uni.showToast({ title: '商家暂未开通线下支付', icon: 'none' })
        return
      }
      if (mode === 1 && type === 1 && !this.shopPaymentConfig.onlineWxPayEnabled) {
        uni.showToast({ title: '商家暂未开通微信支付', icon: 'none' })
        return
      }
      if (mode === 1 && type === 2 && !this.shopPaymentConfig.onlineAlipayPayEnabled) {
        uni.showToast({ title: '商家暂未开通支付宝支付', icon: 'none' })
        return
      }
      this.selectedPayMode = mode
      this.selectedPayType = type
    },

    confirmCreateOrder() {
      if (this.submitting) return
      const diningType = this.diningType || (app.globalData && app.globalData.diningType) || 1
      let tableNo = app.globalData.tableNo || this.tableNo || ''
      if (diningType === 2 && !tableNo) {
        tableNo = 'TAKEOUT'
      }
      const { cart } = this
      const items = Object.keys(cart).map((key) => {
        const entry = cart[key]
        return {
          dishId: entry.id,
          quantity: entry.count,
          specIds: entry.specIds || [],
          specText: entry.specText || ''
        }
      })

      const payload = {
        tableNo,
        diningType,
        items,
        payMode: this.selectedPayMode,
        payType: this.selectedPayType,
        peopleCount: null
      }
      if (this.currentBestCoupon && this.currentBestCoupon.id) {
        payload.userCouponId = this.currentBestCoupon.id
      }

      this.submitting = true
      api.createOrder(payload).then((orderRes) => {
        this.submitting = false
        this.showPayModal = false
        this.updateCart({})

        // 线下支付分支：直接下单成功并跳订单页展示收款码
        if (this.selectedPayMode === 2) {
          uni.showToast({ title: '下单成功，请扫码付款', icon: 'success' })
          setTimeout(() => {
            uni.reLaunch({ url: '/pages/order/order' })
          }, 800)
          return
        }

        // 线上支付分支：调用 /api/payment/create 调起支付
        this.handleOnlinePay(orderRes)
      }).catch((err) => {
        this.submitting = false
        uni.showToast({ title: (err && err.message) || '下单失败', icon: 'none' })
      })
    },

    handleOnlinePay(orderRes) {
      const orderId = (orderRes && (orderRes.orderNo || orderRes.id)) || ''
      uni.showLoading({ title: '正在调起支付...' })
      api.createPayment({
        orderId,
        payType: this.selectedPayType,
        platform: 'h5'
      }).then((payResp) => {
        uni.hideLoading()
        const isMock = payResp && payResp.payParams && payResp.payParams.mock
        if (isMock) {
          uni.showModal({
            title: '模拟线上支付',
            content: `本次为开发测试模式，支付金额：¥${payResp.payAmount}。点击确定完成模拟支付。`,
            confirmText: '模拟付款',
            cancelText: '稍后付款',
            success: (mRes) => {
              if (mRes.confirm) {
                uni.showLoading({ title: '正在支付...' })
                api.mockPaySuccess(payResp.paymentNo).then(() => {
                  uni.hideLoading()
                  uni.showToast({ title: '支付成功！', icon: 'success' })
                  setTimeout(() => {
                    uni.reLaunch({ url: '/pages/order/order' })
                  }, 800)
                }).catch(() => {
                  uni.hideLoading()
                  uni.reLaunch({ url: '/pages/order/order' })
                })
              } else {
                uni.reLaunch({ url: '/pages/order/order' })
              }
            }
          })
        } else {
          // 真实第三方支付
          uni.showToast({ title: '已发起支付', icon: 'success' })
          setTimeout(() => {
            uni.reLaunch({ url: '/pages/order/order' })
          }, 800)
        }
      }).catch((err) => {
        uni.hideLoading()
        uni.showToast({ title: (err && err.message) || '支付调起失败', icon: 'none' })
        setTimeout(() => {
          uni.reLaunch({ url: '/pages/order/order' })
        }, 1000)
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.menu-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f4f6fa;
  box-sizing: border-box;
  min-height: 0;
  overflow: hidden;
  /* #ifdef H5 */
  height: calc(100vh - 44px - env(safe-area-inset-top));
  /* #endif */
}

/* ==================== 顶部状态条 ==================== */
.top-bar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12rpx 20rpx;
  background: #ffffff;
  border-bottom: 1rpx solid #f1f5f9;
  z-index: 10;
  gap: 12rpx;
}

.dining-switch {
  display: flex;
  background: #f1f5f9;
  padding: 4rpx;
  border-radius: 24rpx;
}

.switch-btn {
  display: flex;
  align-items: center;
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
  font-size: 22rpx;
  color: #64748b;
  font-weight: 500;
  transition: all 0.2s;
}

.switch-btn.active {
  background: #2468f2;
  color: #ffffff;
  font-weight: 600;
  box-shadow: 0 2rpx 8rpx rgba(36, 104, 242, 0.3);
}

.switch-btn .btn-icon {
  font-size: 22rpx;
  margin-right: 4rpx;
}

.table-badge {
  display: flex;
  align-items: center;
  background: #eff6ff;
  padding: 8rpx 18rpx;
  border-radius: 30rpx;
}

.table-badge.takeout-badge {
  background: #fff7ed;
}

.table-badge.takeout-badge .table-label {
  color: #ea580c;
  font-weight: 600;
}

.table-icon {
  font-size: 26rpx;
  margin-right: 8rpx;
}

.table-label {
  font-size: 24rpx;
  color: #64748b;
}

.table-no {
  font-size: 26rpx;
  font-weight: 700;
  color: #2468f2;
}

.rescan-btn {
  display: flex;
  align-items: center;
  padding: 8rpx 20rpx;
  background: #f8fafc;
  border-radius: 30rpx;
  border: 1rpx solid #e2e8f0;
}

.rescan-icon {
  font-size: 24rpx;
  margin-right: 6rpx;
}

.rescan-text {
  font-size: 23rpx;
  color: #64748b;
  font-weight: 500;
}

/* ==================== 扫码提示蒙版 ==================== */
.scan-tip {
  position: fixed;
  z-index: 999;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 50rpx;
  box-sizing: border-box;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(4px);
}

.scan-tip-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  width: 100%;
  max-width: 580rpx;
  padding: 48rpx 36rpx;
  border-radius: 28rpx;
  background: #ffffff;
  box-shadow: 0 20rpx 50rpx rgba(15, 23, 42, 0.15);
}

.scan-tip-icon {
  font-size: 80rpx;
  margin-bottom: 16rpx;
}

.scan-tip-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #0f172a;
}

.scan-tip-desc {
  font-size: 24rpx;
  color: #94a3b8;
  margin-top: 10rpx;
  line-height: 1.4;
}

.scan-tip-btn {
  margin-top: 36rpx;
  width: 100%;
  padding: 20rpx 0;
  border-radius: 40rpx;
  background: linear-gradient(135deg, #2468f2 0%, #1a56d6 100%);
  color: #ffffff;
  font-size: 28rpx;
  font-weight: 600;
  box-shadow: 0 8rpx 20rpx rgba(36, 104, 242, 0.3);
}

/* ==================== 主体双栏区域 ==================== */
.body-content {
  flex: 1;
  display: flex;
  min-height: 0;
  background: #f4f6fa;
}

/* 左侧分类侧边栏 */
.cat-wrap {
  position: relative;
  width: 172rpx;
  flex-shrink: 0;
  height: 100%;
}

.cat-list {
  width: 100%;
  height: 100%;
  background: #ebedf2;
}

.cat-item {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32rpx 14rpx;
  font-size: 26rpx;
  color: #64748b;
  font-weight: 400;
  transition: all 0.15s ease;
}

.cat-indicator {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 8rpx;
  height: 32rpx;
  border-radius: 0 6rpx 6rpx 0;
  background: transparent;
  transition: all 0.2s ease;
}

.cat-name {
  text-align: center;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  word-break: break-all;
}

.cat-item.active {
  background: #ffffff;
  color: #1e293b;
  font-weight: 600;
}

.cat-item.active .cat-indicator {
  background: #2468f2;
  height: 38rpx;
}

.cat-bottom-space {
  height: 300rpx;
}

/* 右侧菜品列表 */
.goods-wrap {
  position: relative;
  flex: 1;
  height: 100%;
  min-width: 0;
}

.goods-list {
  width: 100%;
  height: 100%;
  padding: 0 16rpx;
  box-sizing: border-box;
}

.goods-item {
  display: flex;
  align-items: stretch;
  background: #ffffff;
  padding: 20rpx 18rpx;
  margin: 16rpx 0;
  border-radius: 18rpx;
  box-shadow: 0 4rpx 16rpx rgba(15, 23, 42, 0.04);
  transition: transform 0.1s ease;
}

.goods-item:active {
  transform: scale(0.995);
}

/* 缩略图 */
.thumb {
  position: relative;
  width: 144rpx;
  height: 144rpx;
  border-radius: 14rpx;
  background: #f8fafc;
  flex-shrink: 0;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.thumb-img {
  width: 100%;
  height: 100%;
}

.thumb-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: #f1f5f9;
}

.thumb-icon {
  font-size: 56rpx;
}

.badge-hot {
  position: absolute;
  left: 0;
  top: 0;
  background: linear-gradient(135deg, #ff5722, #ff7a45);
  color: #ffffff;
  padding: 2rpx 10rpx 4rpx 8rpx;
  border-bottom-right-radius: 12rpx;
  display: flex;
  align-items: center;
  font-size: 18rpx;
  font-weight: 600;
  line-height: 1;
}

.badge-hot-icon {
  font-size: 16rpx;
  margin-right: 2rpx;
}

/* 菜品信息区 */
.info {
  flex: 1;
  min-width: 0;
  margin-left: 18rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.name-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.name {
  font-size: 29rpx;
  font-weight: 600;
  color: #1e293b;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  word-break: break-all;
}

.desc {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #64748b;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 1;
  overflow: hidden;
  word-break: break-all;
}

/* 价格与已售 */
.meta {
  margin-top: 10rpx;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.price-box {
  display: flex;
  align-items: baseline;
  color: #ff5722;
}

.price-symbol {
  font-size: 22rpx;
  font-weight: 600;
}

.price-val {
  font-size: 34rpx;
  font-weight: 700;
  margin-left: 2rpx;
}

.tags-box {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.minbuy-tag {
  font-size: 20rpx;
  color: #ff7a45;
  background: #fff3ea;
  border-radius: 6rpx;
  padding: 2rpx 8rpx;
}

.sales {
  font-size: 21rpx;
  color: #94a3b8;
}

/* 步进器操作行 */
.ops {
  margin-top: 12rpx;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12rpx;
}

.step-btn {
  width: 50rpx;
  height: 50rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.1s ease;
}

.step-btn:active {
  transform: scale(0.9);
}

.step-btn.plus {
  background: linear-gradient(135deg, #2468f2, #1a56d6);
  color: #ffffff;
  box-shadow: 0 4rpx 12rpx rgba(36, 104, 242, 0.3);
}

.step-btn.minus {
  border: 2rpx solid #cbd5e1;
  color: #64748b;
  background: #ffffff;
}

.step-icon {
  font-size: 30rpx;
  line-height: 1;
  font-weight: 600;
}

.step-num {
  min-width: 44rpx;
  text-align: center;
  font-size: 28rpx;
  font-weight: 600;
  color: #1e293b;
}

.min-buy-btn {
  height: 52rpx;
  padding: 0 24rpx;
  border-radius: 26rpx;
  background: linear-gradient(135deg, #2468f2, #1a56d6);
  color: #ffffff;
  font-size: 23rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 12rpx rgba(36, 104, 242, 0.3);
}

.min-buy-btn:active {
  opacity: 0.85;
}

/* ==================== 空状态 & 加载状态 ==================== */
.empty-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100rpx 40rpx;
}

.empty-icon-box {
  font-size: 80rpx;
  margin-bottom: 20rpx;
}

.empty-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #334155;
}

.empty-sub {
  font-size: 24rpx;
  color: #94a3b8;
  margin-top: 8rpx;
  text-align: center;
}

.list-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60rpx 0;
  color: #94a3b8;
}

.list-loading.list-loading-more {
  padding: 24rpx 0;
}

.list-loading-text {
  font-size: 24rpx;
  margin-left: 14rpx;
}

.mini-spinner {
  width: 32rpx;
  height: 32rpx;
  border: 3rpx solid rgba(36, 104, 242, 0.15);
  border-top-color: #2468f2;
  border-radius: 50%;
  animation: spinner-rotate 0.7s linear infinite;
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

.bottom-space {
  height: 300rpx;
}

@keyframes spinner-rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* ==================== 购物车面板与蒙版 ==================== */
.cart-mask {
  position: fixed;
  inset: 0;
  bottom: calc(100rpx + env(safe-area-inset-bottom));
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(2px);
  z-index: 1000;
}

.cart-panel {
  position: fixed;
  left: 0;
  right: 0;
  bottom: calc(100rpx + env(safe-area-inset-bottom));
  background: #ffffff;
  border-radius: 32rpx 32rpx 0 0;
  z-index: 1001;
  max-height: 60vh;
  display: flex;
  flex-direction: column;
  padding-bottom: 130rpx;
  box-shadow: 0 -10rpx 40rpx rgba(15, 23, 42, 0.16);
  overflow: hidden;
}

.cart-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx 32rpx 24rpx;
  border-bottom: 1rpx solid #f1f5f9;
}

.cart-head-title {
  display: flex;
  align-items: center;
  font-size: 28rpx;
  font-weight: 700;
  color: #0f172a;
}

.cart-head-icon {
  font-size: 32rpx;
  margin-right: 10rpx;
}

.cart-clear {
  display: flex;
  align-items: center;
  font-size: 24rpx;
  color: #94a3b8;
  padding: 6rpx 14rpx;
  border-radius: 20rpx;
  background: #f8fafc;
}

.clear-icon {
  font-size: 24rpx;
  margin-right: 6rpx;
}

.cart-scroll-list {
  flex: 1;
  padding: 10rpx 32rpx 30rpx;
  box-sizing: border-box;
}

.cart-item {
  display: flex;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f8fafc;
}

.cart-img {
  width: 90rpx;
  height: 90rpx;
  background: #f1f5f9;
  border-radius: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20rpx;
  overflow: hidden;
  flex-shrink: 0;
}

.cart-img-real {
  width: 100%;
  height: 100%;
}

.cart-img-icon {
  font-size: 44rpx;
}

.cart-info {
  flex: 1;
  min-width: 0;
  margin-right: 20rpx;
}

.cart-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cart-spec {
  font-size: 22rpx;
  color: #94a3b8;
  margin-top: 4rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cart-price-line {
  margin-top: 6rpx;
  display: flex;
  align-items: baseline;
  color: #ff5722;
}

.cart-price-symbol {
  font-size: 20rpx;
  font-weight: 600;
}

.cart-price-val {
  font-size: 28rpx;
  font-weight: 700;
  margin-left: 2rpx;
}

.stepper {
  display: flex !important;
  flex-direction: row !important;
  align-items: center !important;
  justify-content: flex-end !important;
  flex-shrink: 0;
  white-space: nowrap;
}

.stepper .step-btn {
  width: 50rpx;
  height: 50rpx;
  border-radius: 50%;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  flex-shrink: 0;
  box-sizing: border-box;
}

.stepper .step-num {
  min-width: 44rpx;
  text-align: center;
  font-size: 28rpx;
  font-weight: 600;
  color: #1e293b;
  margin: 0 10rpx;
  flex-shrink: 0;
  line-height: 1;
}

.stepper.small .step-btn {
  width: 46rpx;
  height: 46rpx;
}

.stepper.small .step-icon {
  font-size: 26rpx;
  line-height: 1;
}

.stepper.small .step-num {
  min-width: 40rpx;
  font-size: 26rpx;
  margin: 0 8rpx;
}

/* ==================== 悬浮结算栏 ==================== */
.checkout-bar {
  position: fixed;
  left: 24rpx;
  right: 24rpx;
  bottom: calc(116rpx + env(safe-area-inset-bottom));
  height: 104rpx;
  background: #1e293b;
  border-radius: 52rpx;
  display: flex;
  align-items: center;
  padding: 0 12rpx 0 20rpx;
  box-shadow: 0 14rpx 36rpx rgba(15, 23, 42, 0.28);
  z-index: 1002;
}

.cart-entry {
  position: relative;
  margin-right: 18rpx;
}

.cart-icon-bg {
  width: 76rpx;
  height: 76rpx;
  border-radius: 50%;
  background: #334155;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.cart-icon-bg.active {
  background: linear-gradient(135deg, #2468f2, #1a56d6);
  box-shadow: 0 4rpx 14rpx rgba(36, 104, 242, 0.45);
}

.cart-emoji {
  font-size: 38rpx;
}

.badge {
  position: absolute;
  top: -6rpx;
  right: -8rpx;
  min-width: 34rpx;
  height: 34rpx;
  padding: 0 8rpx;
  border-radius: 17rpx;
  background: #ff5722;
  color: #ffffff;
  font-size: 20rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2rpx 6rpx rgba(0, 0, 0, 0.2);
}

.checkout-amount {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.amount-wrap {
  display: flex;
  flex-direction: column;
}

.amount-main {
  display: flex;
  align-items: baseline;
}

.amount-label {
  font-size: 22rpx;
  color: #cbd5e1;
  margin-right: 6rpx;
}

.amount-symbol {
  font-size: 22rpx;
  color: #ffffff;
  font-weight: 600;
}

.amount-val {
  font-size: 36rpx;
  font-weight: 700;
  color: #ffffff;
  margin-left: 2rpx;
}

.amount-tip {
  font-size: 20rpx;
  color: #94a3b8;
  margin-top: 2rpx;
}

.empty-tip {
  font-size: 26rpx;
  color: #64748b;
}

.checkout-btn {
  width: 190rpx;
  height: 80rpx;
  border-radius: 40rpx;
  background: linear-gradient(135deg, #2468f2, #1a56d6);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6rpx 18rpx rgba(36, 104, 242, 0.35);
  transition: all 0.2s ease;
}

.checkout-btn:active {
  transform: scale(0.97);
}

.checkout-btn.disabled {
  background: #475569;
  box-shadow: none;
  opacity: 0.7;
}

.checkout-btn-text {
  color: #ffffff;
  font-size: 28rpx;
  font-weight: 600;
  letter-spacing: 1rpx;
}

/* ==================== 支付弹窗样式 ==================== */
.pay-modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.65);
  z-index: 10000;
}

.pay-modal {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #ffffff;
  border-radius: 32rpx 32rpx 0 0;
  padding: 32rpx 32rpx calc(32rpx + env(safe-area-inset-bottom));
  z-index: 10001;
  box-shadow: 0 -12rpx 48rpx rgba(0, 0, 0, 0.25);
  animation: slideUp 0.25s ease-out;
}

@keyframes slideUp {
  from {
    transform: translateY(100%);
  }
  to {
    transform: translateY(0);
  }
}

.pay-modal-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 24rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.pay-modal-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #1e293b;
}

.pay-modal-close {
  font-size: 36rpx;
  color: #94a3b8;
  padding: 10rpx;
}

.pay-modal-summary {
  background: #f8fafc;
  border-radius: 20rpx;
  padding: 24rpx;
  margin: 24rpx 0;
}

.summary-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}

.summary-line .label {
  font-size: 26rpx;
  color: #64748b;
}

.summary-line .val {
  font-size: 26rpx;
  color: #1e293b;
}

.summary-line .highlight {
  color: #2468f2;
  font-weight: 600;
}

.summary-line .coupon-label {
  color: #ff4d4f;
  font-weight: 500;
}

.summary-line.total-line {
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx dashed #e2e8f0;
  margin-bottom: 0;
}

.total-price {
  font-size: 38rpx;
  font-weight: 800;
  color: #ff4d4f;
}

.pay-options-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #334155;
  margin-bottom: 16rpx;
}

.pay-options-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.pay-option-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx;
  border-radius: 20rpx;
  background: #ffffff;
  border: 2rpx solid #e2e8f0;
  transition: all 0.2s ease;
}

.pay-option-item.selected {
  border-color: #2468f2;
  background: #f0f7ff;
}

.pay-option-item.disabled {
  background: #f8fafc;
  border-color: #e2e8f0;
  opacity: 0.55;
}

.pay-opt-left {
  display: flex;
  align-items: center;
  flex: 1;
}

.pay-opt-icon {
  font-size: 44rpx;
  margin-right: 20rpx;
}

.pay-opt-info {
  flex: 1;
}

.pay-opt-name-wrap {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.pay-opt-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #1e293b;
}

.disabled-tag {
  font-size: 20rpx;
  background: #f1f5f9;
  color: #94a3b8;
  padding: 2rpx 8rpx;
  border-radius: 6rpx;
}

.pay-opt-desc {
  font-size: 22rpx;
  color: #94a3b8;
  margin-top: 4rpx;
  display: block;
}

.pay-radio {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  border: 2rpx solid #cbd5e1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pay-radio.checked {
  border-color: #2468f2;
}

.radio-inner {
  width: 22rpx;
  height: 22rpx;
  border-radius: 50%;
  background: #2468f2;
}

.pay-submit-wrap {
  margin-top: 32rpx;
}

.pay-confirm-btn {
  background: linear-gradient(135deg, #2468f2, #1a56d6);
  color: #ffffff;
  font-size: 32rpx;
  font-weight: 700;
  border-radius: 44rpx;
  height: 88rpx;
  line-height: 88rpx;
}
</style>
