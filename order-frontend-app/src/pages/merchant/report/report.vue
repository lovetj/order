<template>
  <view class="page">
    <!-- 日期筛选 -->
    <view class="filter">
      <view class="range-tabs">
        <view
          v-for="item in ranges"
          :key="item.key"
          class="range-tab"
          :class="{ active: activeRange === item.key }"
          @click="switchRange(item.key)"
        >{{item.label}}</view>
      </view>
      <view class="date-row">
        <picker mode="date" :value="startDate" @change="onStartChange">
          <view class="date-picker">{{startDate}}</view>
        </picker>
        <text class="date-sep">至</text>
        <picker mode="date" :value="endDate" @change="onEndChange">
          <view class="date-picker">{{endDate}}</view>
        </picker>
      </view>
      <view class="export-row">
        <view class="export-btn" @click="exportReport">📥 导出报表</view>
        <view class="export-btn" @click="exportOrders">📋 导出订单</view>
      </view>
    </view>

    <!-- 汇总卡片 -->
    <view class="summary-card">
      <view class="summary-main">
        <view class="summary-label">营业额</view>
        <view class="summary-value">¥{{summary.amount || '0.00'}}</view>
      </view>
      <view class="summary-sub">
        <view class="sub-item">
          <view class="sub-value">{{summary.orderCount || 0}}</view>
          <view class="sub-label">订单数</view>
        </view>
        <view class="sub-item">
          <view class="sub-value">¥{{summary.avgAmount || '0.00'}}</view>
          <view class="sub-label">客单价</view>
        </view>
        <view class="sub-item">
          <view class="sub-value">{{summary.canceledCount || 0}}</view>
          <view class="sub-label">已取消</view>
        </view>
      </view>
    </view>

    <!-- 每日明细 -->
    <view class="card">
      <view class="section-title">每日营业额</view>
      <template v-if="dailyList.length > 0">
        <view class="daily-item" v-for="item in dailyList" :key="item.date">
          <view class="daily-date">{{item.date}}</view>
          <view class="daily-bar-wrap">
            <view class="daily-bar" :style="{ width: item.percent + '%' }"></view>
          </view>
          <view class="daily-right">
            <text class="daily-amount">¥{{item.amount}}</text>
            <text class="daily-count">{{item.orderCount}} 单</text>
          </view>
        </view>
      </template>
      <view v-else class="empty">当前区间暂无数据</view>
    </view>

    <!-- 菜品销量排行 -->
    <view class="card">
      <view class="section-title">菜品销量排行 TOP10</view>
      <template v-if="topDishes.length > 0">
        <view class="rank-item" v-for="(item, index) in topDishes" :key="item.dishName">
          <view class="rank-no" :class="{ top: index < 3 }">{{index + 1}}</view>
          <view class="rank-name">{{item.dishName}}</view>
          <view class="rank-num">x{{item.quantity}}</view>
          <view class="rank-amount">¥{{item.amount}}</view>
        </view>
      </template>
      <view v-else class="empty">当前区间暂无数据</view>
    </view>

    <!-- 分类销量 -->
    <view class="card">
      <view class="section-title">分类销售额</view>
      <template v-if="categoryList.length > 0">
        <view class="cat-item" v-for="item in categoryList" :key="item.categoryName">
          <view class="cat-head">
            <text class="cat-name">{{item.categoryName || '未分类'}}</text>
            <text class="cat-amount">¥{{item.amount}} · {{item.quantity}} 份</text>
          </view>
          <view class="cat-bar-wrap">
            <view class="cat-bar" :style="{ width: getBarWidth(item.amount, categoryList) + '%' }"></view>
          </view>
        </view>
      </template>
      <view v-else class="empty">当前区间暂无数据</view>
    </view>
  </view>
</template>

<script>
const app = getApp()
import api from '@/api/index'
import { getToken } from '@/utils/request'

// 快捷日期范围
const RANGES = [
  { key: 'today', label: '今日' },
  { key: 'week', label: '近7天' },
  { key: 'month', label: '近30天' }
]

function formatDate(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

export default {
  data() {
    return {
      ranges: RANGES,
      activeRange: 'week',
      startDate: '',
      endDate: '',
      summary: {},
      dailyList: [],
      topDishes: [],
      categoryList: [],
      loading: false
    }
  },
  onLoad() {
    this.setRange('week')
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
    this.loadReport()
  },
  methods: {
    // 选择快捷范围
    switchRange(key) {
      this.setRange(key)
      this.loadReport()
    },

    setRange(key) {
      const today = new Date()
      let start = new Date()
      if (key === 'today') {
        start = today
      } else if (key === 'week') {
        start = new Date(today.getTime() - 6 * 24 * 3600 * 1000)
      } else if (key === 'month') {
        start = new Date(today.getTime() - 29 * 24 * 3600 * 1000)
      }
      this.activeRange = key
      this.startDate = formatDate(start)
      this.endDate = formatDate(today)
    },

    // 手动选择日期
    onStartChange(e) {
      this.startDate = e.detail.value
      this.activeRange = ''
      this.loadReport()
    },

    onEndChange(e) {
      this.endDate = e.detail.value
      this.activeRange = ''
      this.loadReport()
    },

    loadReport() {
      const { startDate, endDate } = this
      if (startDate > endDate) {
        uni.showToast({ title: '开始日期不能晚于结束日期', icon: 'none' })
        return
      }
      this.loading = true
      api.getReport({ startDate, endDate }).then((data) => {
        if (!data) return
        this.summary = data.summary || {}
        this.dailyList = data.dailyList || []
        this.topDishes = data.topDishes || []
        this.categoryList = data.categoryList || []
      }).catch(() => {
        this.summary = {}
        this.dailyList = []
        this.topDishes = []
        this.categoryList = []
      }).then(() => {
        this.loading = false
      })
    },

    // 分类占比进度条宽度（按金额）
    getBarWidth(amount, list) {
      const max = Math.max.apply(null, list.map((c) => Number(c.amount) || 0))
      if (!max) return 0
      return Math.round((Number(amount) / max) * 100)
    },

    // 导出经营报表 CSV
    exportReport() {
      const { startDate, endDate } = this
      const url = api.exportReportUrl(startDate, endDate)
      this.downloadAndOpen(url, `经营报表_${startDate}_${endDate}.csv`)
    },

    // 导出订单明细 CSV
    exportOrders() {
      const { startDate, endDate } = this
      const url = api.exportOrdersUrl(startDate, endDate, null)
      this.downloadAndOpen(url, `订单明细_${startDate}_${endDate}.csv`)
    },

    /**
     * 下载文件并打开
     * H5 直接打开链接，非 H5 环境用 uni.downloadFile 拉取后通过 openDocument 打开
     */
    downloadAndOpen(url, fileName) {
      const token = getToken()
      uni.showLoading({ title: '导出中', mask: true })
      uni.downloadFile({
        url,
        header: token ? { Authorization: `Bearer ${token}` } : {},
        success: (res) => {
          if (res.statusCode !== 200) {
            uni.showToast({ title: '导出失败', icon: 'none' })
            return
          }
          uni.openDocument({
            filePath: res.tempFilePath,
            fileType: 'csv',
            showMenu: true,
            success: () => {},
            fail: () => {
              // 部分环境不支持 csv 预览，改为复制路径提示
              uni.showModal({
                title: '导出成功',
                content: `文件已下载：${fileName}\n可点击右上角「…」转发或保存。`,
                showCancel: false,
                confirmText: '知道了',
                confirmColor: '#2f80ed'
              })
            }
          })
        },
        fail: () => uni.showToast({ title: '网络异常，导出失败', icon: 'none' }),
        complete: () => uni.hideLoading()
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.page {
  padding-bottom: 60rpx;
}

.filter {
  background: #fff;
  padding: 24rpx;
}

.range-tabs {
  display: flex;
  background: #f2f3f5;
  border-radius: 999rpx;
  padding: 6rpx;
}

.range-tab {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  font-size: 27rpx;
  color: #666;
  border-radius: 999rpx;
}

.range-tab.active {
  background: #2f80ed;
  color: #fff;
  font-weight: 600;
}

.date-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 24rpx;
}

.date-picker {
  padding: 16rpx 28rpx;
  background: #f2f3f5;
  border-radius: 999rpx;
  font-size: 26rpx;
  color: #333;
}

.date-sep {
  font-size: 26rpx;
  color: #999;
}

.export-row {
  display: flex;
  gap: 20rpx;
  margin-top: 24rpx;
}

.export-btn {
  flex: 1;
  text-align: center;
  padding: 18rpx 0;
  border-radius: 999rpx;
  background: #eaf3ff;
  color: #2f80ed;
  font-size: 26rpx;
}

.summary-card {
  margin: 24rpx;
  padding: 40rpx 32rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #2f80ed, #1a5fd0);
  color: #fff;
  box-shadow: 0 10rpx 26rpx rgba(47, 128, 237, 0.28);
}

.summary-label {
  font-size: 26rpx;
  opacity: 0.85;
}

.summary-value {
  font-size: 64rpx;
  font-weight: 700;
  margin-top: 10rpx;
}

.summary-sub {
  display: flex;
  margin-top: 36rpx;
  padding-top: 28rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.25);
}

.sub-item {
  flex: 1;
  text-align: center;
}

.sub-value {
  font-size: 34rpx;
  font-weight: 600;
}

.sub-label {
  font-size: 23rpx;
  opacity: 0.85;
  margin-top: 8rpx;
}

.daily-list {
  margin-top: 24rpx;
}

.daily-item {
  display: flex;
  align-items: center;
  padding: 16rpx 0;
}

.daily-date {
  width: 180rpx;
  font-size: 24rpx;
  color: #666;
}

.daily-bar-wrap {
  flex: 1;
  height: 24rpx;
  background: #f2f3f5;
  border-radius: 999rpx;
  overflow: hidden;
  margin-right: 20rpx;
}

.daily-bar {
  height: 100%;
  border-radius: 999rpx;
  background: linear-gradient(90deg, #6aa9ff, #2f80ed);
  min-width: 8rpx;
}

.daily-right {
  width: 180rpx;
  text-align: right;
}

.daily-amount {
  font-size: 26rpx;
  font-weight: 600;
  color: #1f1f1f;
}

.daily-count {
  font-size: 22rpx;
  color: #999;
  margin-left: 12rpx;
}

.rank-list {
  margin-top: 16rpx;
}

.rank-item {
  display: flex;
  align-items: center;
  padding: 22rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}

.rank-item:last-child {
  border-bottom: none;
}

.rank-no {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  background: #f2f3f5;
  color: #999;
  font-size: 26rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20rpx;
}

.rank-no.top {
  background: #ff6b35;
  color: #fff;
  font-weight: 700;
}

.rank-name {
  flex: 1;
  font-size: 28rpx;
}

.rank-num {
  width: 110rpx;
  text-align: center;
  font-size: 25rpx;
  color: #999;
}

.rank-amount {
  width: 150rpx;
  text-align: right;
  font-size: 28rpx;
  font-weight: 600;
  color: #ff6b35;
}

.cat-item {
  margin-top: 24rpx;
}

.cat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12rpx;
}

.cat-name {
  font-size: 27rpx;
  color: #333;
}

.cat-amount {
  font-size: 24rpx;
  color: #999;
}

.cat-bar-wrap {
  height: 20rpx;
  background: #f2f3f5;
  border-radius: 999rpx;
  overflow: hidden;
}

.cat-bar {
  height: 100%;
  border-radius: 999rpx;
  background: linear-gradient(90deg, #ffb37a, #ff6b35);
  min-width: 8rpx;
}

.empty {
  text-align: center;
  color: #8a8a8a;
  font-size: 26rpx;
  padding: 60rpx 0;
}
</style>
