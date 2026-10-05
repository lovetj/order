<template>
  <view class="container">
    <bottom-nav />
    <!-- 门店卡片 -->
    <view class="shop-card">
      <view class="shop-top">
        <view class="shop-name">{{shop.name}}</view>
        <view class="table-tag">桌号 {{tableNo}}</view>
      </view>
      <view class="shop-slogan">{{shop.slogan}}</view>
      <view class="scan-btn" @click="scanTable">📷 扫码识别桌号</view>
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

    <!-- 快捷入口 -->
    <view class="quick">
      <view class="quick-item" @click="goMenu">
        <view class="quick-icon">🍽️</view>
        <view class="quick-text">立即点餐</view>
      </view>
      <view class="quick-item" @click="goMenu">
        <view class="quick-icon">🔥</view>
        <view class="quick-text">热销榜单</view>
      </view>
      <view class="quick-item" @click="goOrders">
        <view class="quick-icon">🧾</view>
        <view class="quick-text">我的订单</view>
      </view>
      <view class="quick-item" @click="scanTable">
        <view class="quick-icon">📷</view>
        <view class="quick-text">扫码点餐</view>
      </view>
    </view>

    <!-- 推荐菜品 -->
    <view class="section">
      <view class="section-head">
        <view class="section-title">店长推荐</view>
        <view class="section-more" @click="goMenu">全部 ›</view>
      </view>
      <view class="goods-grid">
        <view class="goods-item" v-for="item in recommend" :key="item.id" @click="goMenu">
          <view class="goods-img">
            <image v-if="item.hasImage" class="goods-img-real" :src="item.imageUrl" mode="aspectFill"></image>
            <text v-else>{{item.image}}</text>
          </view>
          <view class="goods-name ellipsis">{{item.name}}</view>
          <view class="goods-desc ellipsis">{{item.desc}}</view>
          <view class="goods-bottom">
            <text class="price">¥{{item.price}}</text>
            <text class="add-btn">＋</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
// 顾客端首页
const app = getApp()
import api from '@/api/index'
import { formatImageUrl } from '@/utils/util'

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
      recommend: [],
      notices: []
    }
  },

  onShow() {
    if (!app.globalData.role) {
      uni.reLaunch({ url: '/pages/role/role' })
      return
    }
    // 顾客端未登录（如退出后返回）时引导重新登录
    if (!app.globalData.isLogin()) {
      uni.reLaunch({ url: '/pages/login/login?role=customer' })
      return
    }
    this.tableNo = app.globalData.tableNo || '未获取'
    this.loadShop()
    this.loadRecommend()
  },

  methods: {
    // 底部导航切换时刷新当前页面数据
    onTabRefresh() {
      if (!app.globalData.isLogin()) return
      this.tableNo = app.globalData.tableNo || '未获取'
      this.loadShop()
      this.loadRecommend()
    },

    // 加载门店信息
    loadShop() {
      api.getShopInfo().then((shop) => {
        if (!shop) return
        // 公告：后端用 | 分隔多条
        const notices = shop.notice
          ? String(shop.notice).split('|').filter((n) => n && n.trim())
          : []
        this.shop = {
          name: shop.name || '扫码点餐',
          slogan: shop.slogan || '',
          score: shop.score || 5.0,
          monthSales: shop.monthSales || 0
        }
        this.notices = notices
      }).catch(() => {})
    },

    // 加载店长推荐
    loadRecommend() {
      api.getRecommend(4).then((list) => {
        const recommend = (list || []).map((g) => {
          const img = g.image || ''
          const hasImage = /^https?:\/\//.test(img) || img.startsWith('/')
          return { ...g, hasImage, imageUrl: hasImage ? formatImageUrl(img) : '' }
        })
        this.recommend = recommend
      }).catch(() => {})
    },

    scanTable() {
      uni.scanCode({
        success: (res) => {
          const parsed = this.parseQrContent(res.result || '')
          if (parsed.shopId) {
            app.globalData.setShopId(parsed.shopId)
          }
          if (parsed.tableNo) {
            app.globalData.setTableNo(parsed.tableNo)
          }
          this.tableNo = parsed.tableNo || app.globalData.tableNo || '未获取'
          uni.showToast({
            title: parsed.tableNo ? `已识别桌号 ${parsed.tableNo}` : '未识别到桌号',
            icon: 'none'
          })
          // 切换店铺后重新加载本店菜单
          this.loadShop()
          this.loadRecommend()
        },
        fail: () => uni.showToast({ title: '扫码已取消', icon: 'none' })
      })
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
    },

    goMenu() {
      uni.reLaunch({ url: '/pages/menu/menu' })
    },

    goOrders() {
      uni.reLaunch({ url: '/pages/order/order' })
    }
  }
}
</script>

<style lang="scss" scoped>
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
}

.scan-btn {
  margin-top: 28rpx;
  display: inline-block;
  background: #fff;
  color: #ff6b35;
  font-size: 26rpx;
  font-weight: 600;
  padding: 14rpx 36rpx;
  border-radius: 999rpx;
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

.quick {
  display: flex;
  background: #fff;
  margin: 24rpx;
  border-radius: 20rpx;
  padding: 32rpx 0;
}

.quick-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.quick-icon {
  font-size: 52rpx;
}

.quick-text {
  font-size: 24rpx;
  color: #555;
  margin-top: 12rpx;
}

.section {
  margin: 24rpx;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.section-title {
  font-size: 34rpx;
  font-weight: 700;
}

.section-more {
  font-size: 24rpx;
  color: #999;
}

.goods-grid {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
}

.goods-item {
  width: 48.5%;
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.goods-img {
  font-size: 90rpx;
  text-align: center;
  line-height: 1.4;
  height: 180rpx;
  border-radius: 16rpx;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f7f7f7;
}

.goods-img-real {
  width: 100%;
  height: 100%;
}

.goods-name {
  font-size: 30rpx;
  font-weight: 600;
  margin-top: 12rpx;
}

.goods-desc {
  font-size: 22rpx;
  color: #999;
  margin-top: 8rpx;
}

.goods-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16rpx;
}

.add-btn {
  width: 48rpx;
  height: 48rpx;
  line-height: 44rpx;
  text-align: center;
  border-radius: 50%;
  background: #ff6b35;
  color: #fff;
  font-size: 34rpx;
}
</style>
