<template>
  <view
    v-if="loading"
    class="page-loading-wrapper"
    :class="[
      'mode-' + mode,
      isMerchant ? 'theme-merchant' : 'theme-customer'
    ]"
    :style="customStyle"
  >
    <view class="loading-container">
      <!-- 统一外环动态微光旋转环 -->
      <view class="spinner-box">
        <view class="outer-ring"></view>
        <view class="inner-pulse"></view>
        <view class="center-icon">
          <text class="icon-emoji">{{ displayIcon }}</text>
        </view>
      </view>

      <!-- 统一加载文案 -->
      <view class="loading-text-box">
        <text class="loading-text">{{ displayText }}</text>
        <view class="loading-dots">
          <text class="dot dot-1">.</text>
          <text class="dot dot-2">.</text>
          <text class="dot dot-3">.</text>
        </view>
      </view>

      <!-- 描述次级文案（可选） -->
      <view v-if="subText" class="loading-subtext">{{ subText }}</view>
    </view>
  </view>
</template>

<script>
/**
 * 全局统一页面加载动画组件
 * 自动识别店家端与顾客端，统一全系统加载动效规范
 */
export default {
  name: 'page-loading',
  props: {
    // 是否显示
    loading: {
      type: Boolean,
      default: true
    },
    // 角色类型：'customer' | 'merchant' | 'auto'
    role: {
      type: String,
      default: 'auto'
    },
    // 显示模式：'page' (页面居中占位) | 'fullscreen' (全屏遮罩) | 'card' (局部卡片)
    mode: {
      type: String,
      default: 'page'
    },
    // 自定义加载文字
    text: {
      type: String,
      default: ''
    },
    // 自定义次级描述文字
    subText: {
      type: String,
      default: ''
    },
    // 自定义中心图标 (不传则按角色展示)
    icon: {
      type: String,
      default: ''
    },
    // 最小高度 (mode='page' 时生效)
    minHeight: {
      type: String,
      default: ''
    }
  },
  computed: {
    isMerchant() {
      if (this.role === 'merchant') return true
      if (this.role === 'customer') return false

      // 自动判断：检查当前路由或 globalData
      const app = typeof getApp === 'function' ? getApp() : null
      const globalRole = (app && app.globalData && app.globalData.role) || uni.getStorageSync('role') || ''
      if (globalRole === 'merchant') return true

      if (typeof getCurrentPages === 'function') {
        const pages = getCurrentPages()
        if (pages.length > 0) {
          const currentRoute = pages[pages.length - 1].route || ''
          if (currentRoute.includes('merchant')) {
            return true
          }
        }
      }
      return false
    },

    displayIcon() {
      if (this.icon) return this.icon
      return this.isMerchant ? '🏪' : '🍽️'
    },

    displayText() {
      if (this.text) return this.text
      return this.isMerchant ? '正在加载数据' : '正在加载中'
    },

    customStyle() {
      if (this.mode === 'page' && this.minHeight) {
        return { minHeight: this.minHeight }
      }
      return {}
    }
  }
}
</script>

<style lang="scss" scoped>
.page-loading-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  width: 100%;
  animation: fadeIn 0.25s ease-out;

  /* 页面内主体加载模式 */
  &.mode-page {
    min-height: 520rpx;
    padding: 80rpx 32rpx;
    flex: 1;
  }

  /* 全屏遮罩模式（用于页面跳转过渡） */
  &.mode-fullscreen {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 9999;
    background: rgba(245, 246, 248, 0.94);
    backdrop-filter: blur(6px);
  }

  /* 卡片局部加载模式 */
  &.mode-card {
    min-height: 260rpx;
    padding: 40rpx 20rpx;
  }
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

/* 旋转外环与中央呼吸图标 */
.spinner-box {
  position: relative;
  width: 110rpx;
  height: 110rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.outer-ring {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  border-width: 5rpx;
  border-style: solid;
  animation: ring-rotate 0.9s cubic-bezier(0.5, 0.1, 0.5, 0.9) infinite;
}

.inner-pulse {
  position: absolute;
  width: 76rpx;
  height: 76rpx;
  border-radius: 50%;
  animation: inner-breathe 1.8s ease-in-out infinite;
}

.center-icon {
  position: relative;
  z-index: 2;
  font-size: 38rpx;
  animation: icon-float 1.8s ease-in-out infinite;
  line-height: 1;
}

/* 顾客端主题样式规范（活力暖橙） */
.theme-customer {
  .outer-ring {
    border-color: rgba(255, 107, 53, 0.15);
    border-top-color: #ff6b35;
    border-right-color: rgba(255, 107, 53, 0.7);
  }
  .inner-pulse {
    background: radial-gradient(circle, rgba(255, 107, 53, 0.16) 0%, rgba(255, 107, 53, 0) 70%);
  }
  .loading-text {
    color: #4a342a;
  }
  .dot {
    color: #ff6b35;
  }
}

/* 店家端主题样式规范（专业商务蓝） */
.theme-merchant {
  .outer-ring {
    border-color: rgba(47, 128, 237, 0.15);
    border-top-color: #2f80ed;
    border-right-color: rgba(47, 128, 237, 0.7);
  }
  .inner-pulse {
    background: radial-gradient(circle, rgba(47, 128, 237, 0.16) 0%, rgba(47, 128, 237, 0) 70%);
  }
  .loading-text {
    color: #1e293b;
  }
  .dot {
    color: #2f80ed;
  }
}

/* 文字提示区 */
.loading-text-box {
  margin-top: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-text {
  font-size: 28rpx;
  font-weight: 500;
  letter-spacing: 1rpx;
}

.loading-dots {
  display: flex;
  margin-left: 2rpx;
}

.dot {
  font-size: 32rpx;
  font-weight: bold;
  line-height: 1;
  animation: dot-jump 1.4s infinite ease-in-out;

  &.dot-1 {
    animation-delay: 0s;
  }
  &.dot-2 {
    animation-delay: 0.2s;
  }
  &.dot-3 {
    animation-delay: 0.4s;
  }
}

.loading-subtext {
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #94a3b8;
}

/* 动效关键帧 */
@keyframes ring-rotate {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

@keyframes inner-breathe {
  0%, 100% {
    transform: scale(0.85);
    opacity: 0.5;
  }
  50% {
    transform: scale(1.15);
    opacity: 1;
  }
}

@keyframes icon-float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-4rpx);
  }
}

@keyframes dot-jump {
  0%, 80%, 100% {
    transform: translateY(0);
    opacity: 0.3;
  }
  40% {
    transform: translateY(-6rpx);
    opacity: 1;
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
</style>
