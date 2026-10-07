<template>
  <view class="container layout-page">
    <bottom-nav />
    <!-- 未识别店铺/桌号时提示扫码 -->
    <view v-if="needScan" class="scan-tip">
      <view class="scan-tip-card">
        <view class="scan-tip-icon">
          <view class="scan-corner tl"></view>
          <view class="scan-corner tr"></view>
          <view class="scan-corner bl"></view>
          <view class="scan-corner br"></view>
          <view class="scan-line"></view>
        </view>
        <view class="scan-tip-text">
          <view class="scan-tip-title">请先扫描桌位二维码</view>
          <view class="scan-tip-desc">未识别到店铺与桌号，点餐需先识别桌位</view>
        </view>
        <view class="scan-tip-btn" @click="scanTable">立即扫码</view>
      </view>
    </view>
    <!-- 中部滚动区 -->
    <scroll-view class="layout-body" scroll-y>
    <!-- 门店卡片 -->
    <view class="shop-card">
      <view class="shop-top">
        <view class="shop-name">{{shop.name}}</view>
        <view class="table-tag">桌号 {{tableNo}}</view>
      </view>
      <view class="shop-slogan">{{shop.slogan}}</view>
      <view class="scan-btn" @click="scanTable">
        <view class="scan-icon">
          <view class="scan-corner tl"></view>
          <view class="scan-corner tr"></view>
          <view class="scan-corner bl"></view>
          <view class="scan-corner br"></view>
          <view class="scan-line"></view>
        </view>
        <text>扫码识别桌号</text>
      </view>
    </view>

    <!-- 轮播 -->
    <swiper class="banner" circular autoplay interval="3500" indicator-dots indicator-active-color="#ff6b35">
      <swiper-item v-for="item in banners" :key="item.id">
        <view class="banner-item">
          <view class="banner-emoji">{{item.emoji}}</view>
          <view class="banner-info">
            <view class="banner-title">{{item.title}}</view>
            <view class="banner-desc">{{item.desc}}</view>
          </view>
        </view>
      </swiper-item>
    </swiper>

    <!-- 公告 -->
    <view class="notice-card">
      <view class="notice-row" v-for="item in notices" :key="item">
        <text class="notice-dot">·</text>
        <text class="notice-text">{{item}}</text>
      </view>
    </view>
    </scroll-view>
  </view>
</template>

<script>
// 顾客端首页
const app = getApp()
import api from '@/api/index'
import { hasCustomerContext, scanOrderContext, ensureCustomerContext, resolveTableNo } from '@/utils/scan'

export default {
  data() {
    return {
      shop: {
        name: '扫码点餐',
        slogan: '现点现做 · 用心出餐',
        score: 5.0,
        monthSales: 0
      },
      tableNo: '',
      banners: [
        { id: 1, emoji: '🎁', title: '新客立减 15 元', desc: '扫码点餐专享' },
        { id: 2, emoji: '💰', title: '满 100 减 20', desc: '堂食全场通用' },
        { id: 3, emoji: '🔥', title: '招牌菜品 8 折', desc: '每日限量供应' }
      ],
      notices: [],
      needScan: false,
      // 记录已回查过桌号的桌位ID，避免重复请求；桌位变化时强制重新反查
      _resolvedTableId: ''
    }
  },

  onLoad(options) {
    // 与点餐页一致：路由上下文落缓存 + 未登录跳登录页（防重入）
    ensureCustomerContext(options)
  },

  onShow() {
    if (!app.globalData.role) {
      uni.reLaunch({ url: '/pages/role/role' })
      return
    }
    // 顾客端未登录（如退出后返回）时引导重新登录
    if (!app.globalData.isLogin()) return
    this.needScan = !hasCustomerContext(app)
    this.tableNo = app.globalData.tableNo || '未获取'
    // 与点餐页一致：仅携带桌位ID时回查桌号，避免展示别的店的旧桌号
    this.resolveTableNo()
    // 没有店铺上下文时同样要更新门店字段，避免残留默认/上一家店的宣传语等假信息
    if (this.needScan) {
      this.resetShopInfo()
      return
    }
    this.loadShop()
  },

  methods: {
    // 无店铺上下文时重置门店展示信息（店名/宣传语/评分/月销/公告），避免展示残留的假数据
    resetShopInfo() {
      this.shop = { name: '', slogan: '', score: 0, monthSales: 0 }
      this.notices = []
    },
    // 桌号回查：按桌位ID查询桌号；桌位变化时强制重新反查，避免展示旧桌号
    resolveTableNo() {
      const tableId = app.globalData.tableId || ''
      if (!tableId) return
      // 已有桌号且桌位未变化：无需重复反查
      if (app.globalData.tableNo && this._resolvedTableId === tableId) return
      this._resolvedTableId = tableId
      api.getTableByCustomerId(tableId).then((t) => {
        if (t && t.tableNo) {
          app.globalData.setTableNo(t.tableNo)
          this.tableNo = t.tableNo
        }
      }).catch(() => {})
    },

    // 底部导航切换时刷新当前页面数据
    onTabRefresh() {
      if (!app.globalData.isLogin()) return
      this.needScan = !hasCustomerContext(app)
      this.tableNo = app.globalData.tableNo || '未获取'
      this.resolveTableNo()
      // 无店铺上下文：同样重置门店字段，避免残留旧店铺的宣传语
      if (this.needScan) {
        this.resetShopInfo()
        return
      }
      this.loadShop()
    },

    // 加载门店信息
    loadShop() {
      api.getShopInfo().then((shop) => {
        // 无店铺数据：清空门店字段，不展示残留/默认的假信息
        if (!shop) {
          this.resetShopInfo()
          return
        }
        // 公告：后端用 | 分隔多条
        const notices = shop.notice
          ? String(shop.notice).split('|').filter((n) => n && n.trim())
          : []
        this.shop = {
          name: shop.name || '',
          slogan: shop.slogan || '',
          score: shop.score || 0,
          monthSales: shop.monthSales || 0
        }
        this.notices = notices
      }).catch(() => {
        // 请求失败：清空门店字段，避免残留/串店展示上一家店的宣传语
        this.resetShopInfo()
      })
    },

    async scanTable() {
      const { ok, tableNo } = await scanOrderContext(app)
      if (!ok) return
      // 扫码后桌位可能变化，重置已回查标记，强制按新 tableId 反查桌号
      this._resolvedTableId = ''
      this.tableNo = tableNo || app.globalData.tableNo || '未获取'
      this.needScan = false
      this.resolveTableNo()
      // 切换店铺后重新加载本店信息
      this.loadShop()
    },

    /**
     * 解析二维码内容，支持：
     *   1) 应用内页面路径 pages/role/role?shopId=1&tableNo=A01
     *   2) 普通 URL https://xxx?shopId=1&tableNo=A01
     *   3) 纯桌号 A01（兼容旧码）
     */
    parseQrContent(content) {
      const text = String(content || '').trim()
      const result = { shopId: '', tableNo: '' }
      if (!text) return result

      const queryIndex = text.indexOf('?')
      if (queryIndex >= 0) {
        const query = text.substring(queryIndex + 1)
        query.split('&').forEach((pair) => {
          const [k, v] = pair.split('=')
          if (!k || v === undefined) return
          const key = decodeURIComponent(k).trim()
          const value = decodeURIComponent(v).trim()
          if (key === 'shopId') result.shopId = value
          if (key === 'tableNo') result.tableNo = value
        })
        return result
      }

      // 无 query：按纯桌号处理
      const plain = text.replace(/[^\w-]/g, '')
      if (plain && plain.length <= 10) {
        result.tableNo = plain
      }
      return result
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
.scan-tip-icon {
  position: relative;
  width: 56rpx;
  height: 56rpx;
  flex-shrink: 0;
}

/* 扫码框图标：四角括号，纯 CSS 绘制（跨端通用，非相机样式） */
.scan-tip-icon .scan-corner {
  width: 20rpx;
  height: 20rpx;
  border-width: 4rpx;
}

.scan-corner {
  position: absolute;
  border: 3rpx solid #ff6b35;
}

.scan-corner.tl { top: 0; left: 0; border-right: none; border-bottom: none; }
.scan-corner.tr { top: 0; right: 0; border-left: none; border-bottom: none; }
.scan-corner.bl { bottom: 0; left: 0; border-right: none; border-top: none; }
.scan-corner.br { bottom: 0; right: 0; border-left: none; border-top: none; }

/* 中间扫描线：贯穿取景框左右角之间，模拟扫描中的横向线条 */
.scan-line {
  position: absolute;
  top: 50%;
  left: 15%;
  right: 15%;
  height: 3rpx;
  background: #ff6b35;
  transform: translateY(-50%);
}

.scan-tip-icon .scan-line {
  height: 4rpx;
}
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
.shop-card {
  margin: 24rpx;
  padding: 32rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #ff6b35, #ff9a62);
  color: #fff;
  box-shadow: 0 8rpx 24rpx rgba(255, 107, 53, 0.25);
}

.shop-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.shop-name {
  font-size: 38rpx;
  font-weight: 700;
  flex: 1;
  margin-right: 20rpx;
  /* 店铺名称最多一行，超出显示省略号；min-width:0 保证 flex 中可收缩触发省略 */
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-tag {
  font-size: 24rpx;
  background: rgba(255, 255, 255, 0.25);
  padding: 8rpx 20rpx;
  border-radius: 999rpx;
  white-space: nowrap;
}

.shop-slogan {
  font-size: 24rpx;
  opacity: 0.92;
  margin-top: 16rpx;
  /* 宣传语最多两行，超出显示省略号 */
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.scan-btn {
  margin-top: 28rpx;
  display: inline-flex;
  align-items: center;
  gap: 12rpx;
  background: #fff;
  color: #ff6b35;
  font-size: 26rpx;
  font-weight: 600;
  padding: 14rpx 36rpx;
  border-radius: 999rpx;
  line-height: 1;
}

/* 按钮内扫码框图标：略小于提示弹窗中的图标 */
.scan-icon {
  position: relative;
  width: 34rpx;
  height: 34rpx;
  flex-shrink: 0;
}

.scan-icon .scan-corner {
  width: 12rpx;
  height: 12rpx;
  border-width: 3rpx;
}

.banner {
  height: 200rpx;
  margin: 0 24rpx;
}

.banner-item {
  height: 100%;
  border-radius: 20rpx;
  background: #fff;
  display: flex;
  align-items: center;
  padding: 0 40rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.05);
}

.banner-emoji {
  font-size: 80rpx;
  margin-right: 32rpx;
}

.banner-title {
  font-size: 34rpx;
  font-weight: 600;
}

.banner-desc {
  font-size: 24rpx;
  color: #888;
  margin-top: 12rpx;
}

.notice-card {
  background: #fff;
  margin: 24rpx;
  border-radius: 20rpx;
  padding: 24rpx 28rpx;
}

.notice-row {
  display: flex;
  align-items: flex-start;
  padding: 6rpx 0;
}

.notice-dot {
  color: #ff6b35;
  font-weight: 700;
  margin-right: 12rpx;
}

.notice-text {
  flex: 1;
  font-size: 25rpx;
  color: #666;
}
</style>
