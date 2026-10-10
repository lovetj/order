<template>
  <view class="page-container">
    <!-- 未保存修改浮动高亮提示条 -->
    <view v-if="isModified" class="unsaved-alert-bar">
      <view class="unsaved-alert-left">
        <text class="alert-icon">⚠️</text>
        <text class="alert-text">当前支付配置已修改，离开前请记得保存！</text>
      </view>
      <view class="alert-btn" @click="saveConfig">立即保存</view>
    </view>

    <scroll-view scroll-y class="scroll-area" :class="{ 'has-unsaved-bar': isModified }">
      <!-- 顶部提示 -->
      <view class="tip-card">
        <text class="tip-icon">💡</text>
        <view class="tip-text-wrap">
          <text class="tip-title">支付方式管理</text>
          <text class="tip-desc">开启或关闭支付通道。未开启的支付方式将在顾客端结算时自动置灰不可选。</text>
        </view>
      </view>

      <!-- 模块1：线下支付 -->
      <view class="card">
        <view class="card-header">
          <view class="header-left">
            <text class="header-icon">🏪</text>
            <view>
              <text class="header-title">线下支付</text>
              <text class="header-subtitle">顾客可直接下单，展示收款码，店员核验后完成订单</text>
            </view>
          </view>
          <switch :checked="form.offlinePayEnabled" @change="onOfflineSwitchChange" color="#ff4d4f" />
        </view>

        <view v-if="form.offlinePayEnabled" class="sub-section">
          <!-- 收款码上传区：微信、支付宝、其他 -->
          <view class="qrcode-grid">
            <!-- 微信收款码 -->
            <view class="qrcode-item">
              <view class="qrcode-title-wrap">
                <text class="qrcode-tag wx">微信收款码</text>
              </view>
              <view class="qrcode-box" @click="chooseQrcode('offlineWxQrcode')">
                <image v-if="form.offlineWxQrcode" :src="form.offlineWxQrcode" mode="aspectFit" class="qrcode-img" />
                <view v-else class="upload-placeholder">
                  <text class="plus">+</text>
                  <text class="text">上传微信码</text>
                </view>
              </view>
              <view v-if="form.offlineWxQrcode" class="del-btn" @click.stop="delQrcode('offlineWxQrcode')">删除</view>
            </view>

            <!-- 支付宝收款码 -->
            <view class="qrcode-item">
              <view class="qrcode-title-wrap">
                <text class="qrcode-tag ali">支付宝收款码</text>
              </view>
              <view class="qrcode-box" @click="chooseQrcode('offlineAlipayQrcode')">
                <image v-if="form.offlineAlipayQrcode" :src="form.offlineAlipayQrcode" mode="aspectFit" class="qrcode-img" />
                <view v-else class="upload-placeholder">
                  <text class="plus">+</text>
                  <text class="text">上传支付宝码</text>
                </view>
              </view>
              <view v-if="form.offlineAlipayQrcode" class="del-btn" @click.stop="delQrcode('offlineAlipayQrcode')">删除</view>
            </view>

            <!-- 其他收款码 -->
            <view class="qrcode-item">
              <view class="qrcode-title-wrap">
                <text class="qrcode-tag other">其他/聚合码</text>
              </view>
              <view class="qrcode-box" @click="chooseQrcode('offlineOtherQrcode')">
                <image v-if="form.offlineOtherQrcode" :src="form.offlineOtherQrcode" mode="aspectFit" class="qrcode-img" />
                <view v-else class="upload-placeholder">
                  <text class="plus">+</text>
                  <text class="text">上传其他码</text>
                </view>
              </view>
              <view v-if="form.offlineOtherQrcode" class="del-btn" @click.stop="delQrcode('offlineOtherQrcode')">删除</view>
            </view>
          </view>
        </view>
      </view>

      <!-- 模块2：线上微信支付 -->
      <view class="card">
        <view class="card-header">
          <view class="header-left">
            <text class="header-icon">💬</text>
            <view>
              <text class="header-title">线上微信支付</text>
              <text class="header-subtitle">支持微信小程序支付/微信H5支付，实时对账入账</text>
            </view>
          </view>
          <switch :checked="form.onlineWxPayEnabled" @change="onOnlineWxSwitchChange" color="#07c160" />
        </view>
        <view v-if="form.onlineWxPayEnabled" class="online-desc-box">
          <text class="desc-text">已开启微信线上支付。系统采用微信支付标准通道，已配置默认商户支付，顾客下单可直接拉起微信支付。</text>
        </view>
      </view>

      <!-- 模块3：线上支付宝支付 -->
      <view class="card">
        <view class="card-header">
          <view class="header-left">
            <text class="header-icon">🪙</text>
            <view>
              <text class="header-title">线上支付宝支付</text>
              <text class="header-subtitle">支持支付宝网页/小程序/APP支付通道</text>
            </view>
          </view>
          <switch :checked="form.onlineAlipayPayEnabled" @change="onOnlineAlipaySwitchChange" color="#1677ff" />
        </view>
        <view v-if="form.onlineAlipayPayEnabled" class="online-desc-box">
          <text class="desc-text">已开启支付宝线上支付。顾客端在结算时可选择支付宝直接付款并即时回调核销订单。</text>
        </view>
      </view>
    </scroll-view>

    <!-- 底部固定保存栏 -->
    <view class="action-bar">
      <view v-if="isModified" class="unsaved-hint-tag">
        <text class="dot"></text>
        <text>有未保存的更改，点击下方保存生效</text>
      </view>
      <button
        class="save-btn"
        :class="{ 'is-modified': isModified }"
        :loading="saving"
        :disabled="saving"
        @click="saveConfig"
      >
        {{ isModified ? '保存支付设置 (待保存)' : '保存支付设置' }}
      </button>
    </view>
  </view>
</template>

<script>
import api from '@/api/index'

export default {
  data() {
    return {
      saving: false,
      initialLoaded: false,
      forceBack: false,
      form: {
        offlinePayEnabled: true,
        offlineWxQrcode: '',
        offlineAlipayQrcode: '',
        offlineOtherQrcode: '',
        onlineWxPayEnabled: false,
        onlineAlipayPayEnabled: false
      },
      initialForm: {
        offlinePayEnabled: true,
        offlineWxQrcode: '',
        offlineAlipayQrcode: '',
        offlineOtherQrcode: '',
        onlineWxPayEnabled: false,
        onlineAlipayPayEnabled: false
      }
    }
  },
  computed: {
    // 检测表单是否有被改动且未保存
    isModified() {
      if (!this.initialLoaded) return false
      return (
        this.form.offlinePayEnabled !== this.initialForm.offlinePayEnabled ||
        (this.form.offlineWxQrcode || '') !== (this.initialForm.offlineWxQrcode || '') ||
        (this.form.offlineAlipayQrcode || '') !== (this.initialForm.offlineAlipayQrcode || '') ||
        (this.form.offlineOtherQrcode || '') !== (this.initialForm.offlineOtherQrcode || '') ||
        this.form.onlineWxPayEnabled !== this.initialForm.onlineWxPayEnabled ||
        this.form.onlineAlipayPayEnabled !== this.initialForm.onlineAlipayPayEnabled
      )
    }
  },
  onLoad() {
    this.loadConfig()
  },
  // 页面离开/返回拦截保护：若有未保存更改，提示用户确认
  onBackPress(options) {
    if (this.forceBack) {
      return false
    }
    if (this.isModified) {
      uni.showModal({
        title: '未保存提醒',
        content: '当前支付管理配置已修改，尚未保存生效。是否放弃更改并返回？',
        confirmText: '放弃离开',
        cancelText: '继续保存',
        confirmColor: '#999999',
        cancelColor: '#ff4d4f',
        success: (res) => {
          if (res.confirm) {
            this.forceBack = true
            uni.navigateBack()
          }
        }
      })
      return true
    }
    return false
  },
  methods: {
    loadConfig() {
      uni.showLoading({ title: '加载中...' })
      api.getMerchantPaymentConfig().then((res) => {
        uni.hideLoading()
        if (res) {
          const configData = {
            offlinePayEnabled: res.offlinePayEnabled == null ? true : res.offlinePayEnabled === 1,
            offlineWxQrcode: res.offlineWxQrcode || '',
            offlineAlipayQrcode: res.offlineAlipayQrcode || '',
            offlineOtherQrcode: res.offlineOtherQrcode || '',
            onlineWxPayEnabled: res.onlineWxPayEnabled === 1,
            onlineAlipayPayEnabled: res.onlineAlipayPayEnabled === 1
          }
          this.form = { ...configData }
          this.initialForm = { ...configData }
          this.initialLoaded = true
        }
      }).catch((err) => {
        uni.hideLoading()
        uni.showToast({ title: (err && err.message) || '加载失败', icon: 'none' })
      })
    },

    onOfflineSwitchChange(e) {
      this.form.offlinePayEnabled = e.detail.value
    },

    onOnlineWxSwitchChange(e) {
      this.form.onlineWxPayEnabled = e.detail.value
    },

    onOnlineAlipaySwitchChange(e) {
      this.form.onlineAlipayPayEnabled = e.detail.value
    },

    chooseQrcode(field) {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          const filePath = res.tempFilePaths[0]
          uni.showLoading({ title: '上传中...' })
          api.uploadFile(filePath, 'other').then((uploadRes) => {
            uni.hideLoading()
            if (uploadRes && uploadRes.url) {
              this.form[field] = uploadRes.url
              uni.showToast({ title: '上传成功，请记得保存', icon: 'none' })
            }
          }).catch((err) => {
            uni.hideLoading()
            uni.showToast({ title: (err && err.message) || '上传失败', icon: 'none' })
          })
        }
      })
    },

    delQrcode(field) {
      this.form[field] = ''
    },

    saveConfig() {
      if (!this.form.offlinePayEnabled && !this.form.onlineWxPayEnabled && !this.form.onlineAlipayPayEnabled) {
        uni.showToast({ title: '至少需要开启一种支付方式', icon: 'none' })
        return
      }

      this.saving = true
      const payload = {
        offlinePayEnabled: this.form.offlinePayEnabled ? 1 : 0,
        offlineWxQrcode: this.form.offlineWxQrcode,
        offlineAlipayQrcode: this.form.offlineAlipayQrcode,
        offlineOtherQrcode: this.form.offlineOtherQrcode,
        onlineWxPayEnabled: this.form.onlineWxPayEnabled ? 1 : 0,
        onlineAlipayPayEnabled: this.form.onlineAlipayPayEnabled ? 1 : 0
      }

      api.updateMerchantPaymentConfig(payload).then(() => {
        this.saving = false
        // 保存成功后更新比对基准快照
        this.initialForm = { ...this.form }
        uni.showToast({ title: '设置已保存成功', icon: 'success' })
      }).catch((err) => {
        this.saving = false
        uni.showToast({ title: (err && err.message) || '保存失败', icon: 'none' })
      })
    }
  }
}
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f5f6f8;
}

.scroll-area {
  flex: 1;
  padding: 24rpx;
  box-sizing: border-box;
  padding-bottom: 150rpx;
}

.tip-card {
  display: flex;
  align-items: flex-start;
  background: #fff8e6;
  border-radius: 16rpx;
  padding: 20rpx 24rpx;
  margin-bottom: 24rpx;
  border: 1rpx solid #ffe58f;
}

.tip-icon {
  font-size: 32rpx;
  margin-right: 16rpx;
}

.tip-text-wrap {
  flex: 1;
}

.tip-title {
  font-size: 28rpx;
  font-weight: bold;
  color: #d46b08;
  display: block;
}

.tip-desc {
  font-size: 24rpx;
  color: #8c8c8c;
  margin-top: 6rpx;
  line-height: 1.4;
  display: block;
}

.card {
  background: #ffffff;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  flex: 1;
  margin-right: 20rpx;
}

.header-icon {
  font-size: 40rpx;
  margin-right: 16rpx;
}

.header-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #333333;
  display: block;
}

.header-subtitle {
  font-size: 22rpx;
  color: #999999;
  margin-top: 6rpx;
  display: block;
}

.sub-section {
  margin-top: 24rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid #f0f0f0;
}

.input-item {
  margin-bottom: 24rpx;
}

.label {
  font-size: 26rpx;
  color: #555555;
  margin-bottom: 12rpx;
  display: block;
}

.input-box {
  background: #f8f9fa;
  border-radius: 12rpx;
  padding: 16rpx 20rpx;
  font-size: 26rpx;
  color: #333333;
}

.qrcode-grid {
  display: flex;
  justify-content: space-between;
  gap: 16rpx;
  margin-top: 20rpx;
}

.qrcode-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.qrcode-title-wrap {
  margin-bottom: 12rpx;
}

.qrcode-tag {
  font-size: 20rpx;
  padding: 4rpx 10rpx;
  border-radius: 6rpx;
}

.qrcode-tag.wx {
  background: #e6f7ef;
  color: #07c160;
}

.qrcode-tag.ali {
  background: #e6f1fc;
  color: #1677ff;
}

.qrcode-tag.other {
  background: #fff7e6;
  color: #fa8c16;
}

.qrcode-box {
  width: 180rpx;
  height: 180rpx;
  border: 2rpx dashed #d9d9d9;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: #fafafa;
}

.qrcode-img {
  width: 100%;
  height: 100%;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.upload-placeholder .plus {
  font-size: 40rpx;
  color: #999999;
}

.upload-placeholder .text {
  font-size: 20rpx;
  color: #999999;
  margin-top: 4rpx;
}

.del-btn {
  margin-top: 10rpx;
  font-size: 22rpx;
  color: #ff4d4f;
  text-decoration: underline;
}

.online-desc-box {
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #f0f0f0;
}

.desc-text {
  font-size: 24rpx;
  color: #888888;
  line-height: 1.4;
}

/* ==================== 未保存修改提醒条 ==================== */
.unsaved-alert-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(90deg, #fff2e8 0%, #ffe7ba 100%);
  border-bottom: 2rpx solid #ffd591;
  padding: 16rpx 24rpx;
  position: sticky;
  top: 0;
  z-index: 99;
  animation: fadeInDown 0.25s ease-out;
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-20rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.unsaved-alert-left {
  display: flex;
  align-items: center;
  flex: 1;
  margin-right: 16rpx;
}

.unsaved-alert-left .alert-icon {
  font-size: 28rpx;
  margin-right: 10rpx;
}

.unsaved-alert-left .alert-text {
  font-size: 24rpx;
  color: #d4380d;
  font-weight: 600;
  line-height: 1.3;
}

.alert-btn {
  background: #ff4d4f;
  color: #ffffff;
  font-size: 22rpx;
  font-weight: 600;
  padding: 8rpx 18rpx;
  border-radius: 24rpx;
  flex-shrink: 0;
  box-shadow: 0 2rpx 8rpx rgba(255, 77, 79, 0.35);
}

.unsaved-hint-tag {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12rpx;
  font-size: 22rpx;
  color: #fa541c;
}

.unsaved-hint-tag .dot {
  width: 12rpx;
  height: 12rpx;
  background-color: #ff4d4f;
  border-radius: 50%;
  margin-right: 8rpx;
  animation: pulseDot 1.2s infinite ease-in-out;
}

@keyframes pulseDot {
  0%, 100% {
    transform: scale(0.8);
    opacity: 0.6;
  }
  50% {
    transform: scale(1.3);
    opacity: 1;
  }
}

.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #ffffff;
  padding: 16rpx 32rpx calc(16rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.05);
  z-index: 100;
}

.save-btn {
  background: #ff4d4f;
  color: #ffffff;
  font-size: 30rpx;
  font-weight: bold;
  border-radius: 44rpx;
  height: 84rpx;
  line-height: 84rpx;
  transition: all 0.25s ease;
}

.save-btn.is-modified {
  background: linear-gradient(135deg, #ff4d4f 0%, #f5222d 100%);
  box-shadow: 0 6rpx 20rpx rgba(245, 34, 45, 0.4);
}
</style>
