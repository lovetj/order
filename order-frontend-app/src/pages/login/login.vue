<template>
  <!-- 登录页（顾客=昵称+头像快速登录 / 店家=账号密码） -->
  <view class="login-page">
    <!-- 顾客登录页不显示返回按钮；店家登录保留返回 -->
    <view v-if="role === 'merchant'" class="back" @click="goBack">‹ 返回</view>
    <!-- 顾客扫码识别桌位：固定右上角 -->
    <view v-if="role === 'customer'" class="scan-btn" @click="scanTable">
      <view class="scan-icon">
        <view class="scan-corner tl"></view>
        <view class="scan-corner tr"></view>
        <view class="scan-corner bl"></view>
        <view class="scan-corner br"></view>
        <view class="scan-line"></view>
      </view>
      <text>扫码识别桌位</text>
    </view>

    <view class="hero">
      <!-- 顾客登录页不展示汉堡图标，仅店家登录展示门店图标 -->
      <view v-if="role === 'merchant'" class="logo">🏪</view>
      <view class="title">{{role === 'merchant' ? '店家登录' : '欢迎光临'}}</view>
      <view class="subtitle">{{role === 'merchant' ? '请使用店家账号密码登录' : '使用手机号即可登录'}}</view>
      <view v-if="tableNo" class="table-tag">当前桌号：{{tableNo}}</view>
    </view>

    <!-- ==================== 顾客端：手机号登录 ==================== -->
    <view v-if="role === 'customer'" class="panel">
      <!-- 小程序端：优先微信手机号授权自动获取，拒绝/失败后回退手动输入 -->
      <!-- #ifdef MP-WEIXIN -->
      <template v-if="!manualPhone">
        <button
          class="submit-btn guest-btn"
          open-type="getPhoneNumber"
          :loading="logging"
          @getphonenumber="onGetPhoneNumber"
        >
          <text class="quick-icon">📱</text> 手机号一键登录
        </button>
        <view class="hint">授权后将自动获取您的微信绑定手机号，首次自动创建账号</view>
        <view class="manual-link" @click="switchToManual">无法授权？点此手动输入手机号</view>
      </template>
      <!-- #endif -->

      <!-- 手动输入手机号（H5 端默认 / 小程序端授权失败后回退） -->
      <template v-if="manualPhone">
        <view class="field">
          <view class="field-label">手机号</view>
          <input
            class="field-input"
            type="number"
            maxlength="11"
            name="tel"
            autocomplete="tel"
            placeholder="请输入手机号"
            :value="phone"
            @input="onPhoneInput"
          />
        </view>
        <button class="submit-btn guest-btn" :loading="logging" @click="customerLogin">
          <text class="quick-icon">📱</text> 手机号登录
        </button>
        <view class="hint">首次登录将自动创建账号，无需单独注册</view>
        <!-- #ifdef MP-WEIXIN -->
        <view class="manual-link" @click="manualPhone = false">使用微信手机号授权登录</view>
        <!-- #endif -->
      </template>
    </view>

    <!-- ==================== 店家端：账号密码登录 ==================== -->
    <view v-else class="panel">
      <view class="field">
        <view class="field-label">账号</view>
        <input
          class="field-input"
          type="text"
          placeholder="请输入店家账号"
          :value="username"
          @input="onUsernameInput"
        />
      </view>

      <view class="field">
        <view class="field-label">密码</view>
        <view class="pwd-wrap">
          <input
            class="field-input pwd-input"
            :password="!showPassword"
            placeholder="请输入密码"
            :value="password"
            @input="onPasswordInput"
          />
          <view class="pwd-toggle" @click.stop="togglePassword">{{showPassword ? '🙈' : '👁'}}</view>
        </view>
      </view>

      <button class="submit-btn merchant-btn" :loading="logging" @click="merchantLogin">
        登 录
      </button>

      <view class="hint">店家账号由平台统一开通，暂不支持自助注册</view>
    </view>
  </view>
</template>

<script>
// 登录页
// 顾客端：手机号登录（能自动获取就自动获取，不行则手动输入，兼容安卓/苹果/鸿蒙）
//   小程序端：微信 getPhoneNumber 授权按钮自动获取微信绑定手机号；用户拒绝授权时回退手动输入
//   H5 端：浏览器无公开 API 自动读手机号，默认手动输入，并记忆上次手机号便于快速登录
// 店家端：账号密码登录（不提供注册）
const app = getApp()
import api from '@/api/index'
import { setToken } from '@/utils/request'
import { resolveTableNo, scanOrderContext } from '@/utils/scan'

const STORAGE_LAST_PHONE = 'lastPhone'

export default {
  data() {
    return {
      // customer | merchant
      role: 'customer',
      tableNo: '',
      logging: false,
      // 顾客端：是否回退为手动输入（H5 默认手动；小程序默认自动授权）
      manualPhone: false,
      phone: '',
      // 店家端：账号密码
      username: '',
      password: '',
      // 密码是否明文显示（默认密文）
      showPassword: false
    }
  },

  onLoad(options) {
    const role = (options && options.role) === 'merchant' ? 'merchant' : 'customer'
    this.role = role
    this.tableNo = app.globalData.tableNo || ''
    // 深链扫码登录：携带 back 来源页，登录成功后回点餐/首页并复用缓存的上下文
    app.globalData.__loginBack = (options && options.back) ? options.back : ''

    // 兜底：若已缓存桌位ID但桌号为空（如换桌后反查未完成），主动反查并刷新展示
    if (!this.tableNo && app.globalData.tableId) {
      resolveTableNo(app, app.globalData.tableId).then((r) => {
        if (r && r.tableNo) {
          this.tableNo = r.tableNo
        }
      })
    }

    // #ifdef MP-WEIXIN
    // 小程序端优先自动授权
    this.manualPhone = false
    // #endif
    // #ifndef MP-WEIXIN
    // H5 端只能手动输入，回填上次登录手机号
    this.manualPhone = true
    this.phone = uni.getStorageSync(STORAGE_LAST_PHONE) || ''
    // #endif
  },

  methods: {
    goBack() {
      uni.reLaunch({ url: '/pages/role/role' })
    },

    // 顾客端：扫码识别店铺/桌位，刷新当前桌号与登录成功后的回跳地址
    async scanTable() {
      const { ok, tableNo } = await scanOrderContext(app)
      if (!ok) return
      this.tableNo = tableNo || app.globalData.tableNo || ''
      // 扫码后桌位可能变化，更新登录成功回跳地址，携带最新桌位上下文
      app.globalData.__loginBack = `/pages/menu/menu?shopId=${encodeURIComponent(app.globalData.shopId || '')}&tableId=${encodeURIComponent(app.globalData.tableId || '')}&tableNo=${encodeURIComponent(app.globalData.tableNo || '')}`
    },

    switchToManual() {
      this.manualPhone = true
      // 回填上次登录手机号，减少输入成本
      this.phone = uni.getStorageSync(STORAGE_LAST_PHONE) || ''
    },

    onPhoneInput(e) {
      this.phone = (e.detail.value || '').trim()
    },

    onUsernameInput(e) {
      this.username = (e.detail.value || '').trim()
    },

    onPasswordInput(e) {
      this.password = (e.detail.value || '').trim()
    },

    // 切换密码明文/密文显示
    togglePassword() {
      this.showPassword = !this.showPassword
    },

    // ==================== 顾客端：小程序手机号授权登录（自动获取） ====================
    onGetPhoneNumber(e) {
      if (this.logging) return
      const detail = (e && e.detail) || {}
      // 用户拒绝授权 / 授权失败 -> 回退手动输入
      if (!detail.code) {
        const errMsg = detail.errMsg || ''
        if (errMsg.indexOf('deny') > -1 || errMsg.indexOf('fail') > -1) {
          this.switchToManual()
          uni.showToast({ title: '授权未通过，请手动输入手机号', icon: 'none' })
        }
        return
      }
      this.logging = true
      // 携带扫码上下文（shopId/tableId），后端据此绑定三要素登录态
      api.login({
        phoneCode: detail.code,
        role: 'customer',
        shopId: app.globalData.shopId || '',
        tableId: app.globalData.tableId || ''
      }).then((data) => this.handleLoginSuccess(data))
        .catch((err) => {
          this.logging = false
          uni.showToast({ title: (err && err.message) || '登录失败', icon: 'none' })
        })
    },

    // ==================== 顾客端：手动输入手机号登录（H5 / 授权失败回退） ====================
    customerLogin() {
      if (this.logging) return
      const phone = (this.phone || '').trim()
      if (!phone) {
        uni.showToast({ title: '请输入手机号', icon: 'none' })
        return
      }
      if (!/^1\d{10}$/.test(phone)) {
        uni.showToast({ title: '手机号格式不正确', icon: 'none' })
        return
      }
      // 记忆手机号，下次自动回填
      uni.setStorageSync(STORAGE_LAST_PHONE, phone)
      this.logging = true
      // 携带扫码上下文（shopId/tableId），后端据此绑定三要素登录态
      api.login({
        phone,
        role: 'customer',
        shopId: app.globalData.shopId || '',
        tableId: app.globalData.tableId || ''
      }).then((data) => this.handleLoginSuccess(data))
        .catch((err) => {
          this.logging = false
          uni.showToast({ title: (err && err.message) || '登录失败', icon: 'none' })
        })
    },

    // 登录成功统一处理：写入 token、同步全局用户信息、跳转首页
    handleLoginSuccess(data) {
      setToken(data.token)
      const user = data.user || {}
      // 持久化用户ID，供三要素登录态校验携带 X-User-Id
      if (user.id) {
        uni.setStorageSync('userId', user.id)
      }
      app.globalData.userInfo = {
        nickName: user.nickname || '手机用户',
        avatar: user.avatar || '🙋',
        memberLevel: user.memberLevel || '普通会员'
      }
      app.globalData.admin = null
      app.globalData.setRole('customer')
      this.logging = false
      uni.showToast({ title: '登录成功', icon: 'success', duration: 700 })
      setTimeout(() => {
        // 深链扫码进来：登录后回点餐/首页并携带 shopId/tableId，复用已落好的缓存上下文
        // 注意：跳转登录页时 back 参数被 encodeURIComponent 编码，需先解码还原真实 URL 才能 reLaunch
        let back = app.globalData.__loginBack || ''
        if (back) {
          try { back = decodeURIComponent(back) } catch (e) { /* 保持原值 */ }
        }
        app.globalData.__loginBack = ''
        uni.reLaunch({ url: back || '/pages/index/index' })
        // 未识别店铺时提示扫码，避免点餐提交被后端拒绝
        if (!app.globalData.shopId) {
          setTimeout(() => {
            uni.showToast({ title: '请扫描桌位二维码以识别店铺', icon: 'none', duration: 2000 })
          }, 800)
        }
      }, 700)
    },

    // ==================== 店家端：账号密码登录 ====================
    merchantLogin() {
      if (this.logging) return
      const { username, password } = this
      if (!username) {
        uni.showToast({ title: '请输入账号', icon: 'none' })
        return
      }
      if (!password) {
        uni.showToast({ title: '请输入密码', icon: 'none' })
        return
      }

      this.logging = true
      api.adminLogin({ username, password }).then((data) => {
        setToken(data.token)
        const admin = data.admin || {}
        app.globalData.admin = admin
        // 店家登录后绑定其所属店铺，所有管理接口据此隔离
        app.globalData.setShopId(admin.shopId || '')
        app.globalData.setRole('merchant')
        this.logging = false
        uni.showToast({ title: '登录成功', icon: 'success', duration: 700 })
        setTimeout(() => uni.reLaunch({ url: '/pages/merchant/dashboard/dashboard' }), 700)
      }).catch((err) => {
        this.logging = false
        uni.showToast({ title: (err && err.message) || '账号或密码错误', icon: 'none' })
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  /* 顶部预留高度：让 hero 内容与右上角扫码按钮上下错开，避免重叠 */
  padding: 200rpx 60rpx 80rpx;
  background: linear-gradient(180deg, #fff4ef 0%, #f5f6f8 45%);
  display: flex;
  flex-direction: column;
}

.back {
  position: absolute;
  top: 88rpx;
  left: 44rpx;
  font-size: 30rpx;
  color: #8a8a8a;
}

.hero {
  text-align: center;
  margin-bottom: 60rpx;
}

.logo {
  font-size: 120rpx;
  line-height: 1;
  margin-bottom: 22rpx;
}

.title {
  font-size: 48rpx;
  font-weight: 700;
  color: #1f1f1f;
  letter-spacing: 3rpx;
}

.subtitle {
  font-size: 26rpx;
  color: #8a8a8a;
  margin-top: 14rpx;
}

.table-tag {
  display: inline-block;
  margin-top: 22rpx;
  padding: 8rpx 26rpx;
  background: #fff1eb;
  color: #ff6b35;
  border-radius: 999rpx;
  font-size: 24rpx;
}

.scan-btn {
  position: absolute;
  top: 88rpx;
  right: 44rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 10rpx 28rpx;
  background: #fff;
  border: 2rpx solid #ff6b35;
  color: #ff6b35;
  border-radius: 999rpx;
  font-size: 26rpx;
  font-weight: 600;
  line-height: 1;
  z-index: 10;
}

/* 扫码框图标：四角括号，纯 CSS 绘制（跨端通用，非相机样式） */
.scan-icon {
  position: relative;
  width: 34rpx;
  height: 34rpx;
  flex-shrink: 0;
}

.scan-corner {
  position: absolute;
  width: 12rpx;
  height: 12rpx;
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

.scan-btn:active {
  background: #fff1eb;
}

.panel {
  background: #fff;
  border-radius: 28rpx;
  padding: 50rpx 40rpx;
  box-shadow: 0 10rpx 36rpx rgba(0, 0, 0, 0.06);
}

/* ---------- 表单 ---------- */
.field {
  margin-bottom: 34rpx;
}

.field-label {
  font-size: 26rpx;
  color: #8a8a8a;
  margin-bottom: 14rpx;
}

.field-input {
  height: 92rpx;
  background: #f6f7f9;
  border-radius: 18rpx;
  padding: 0 28rpx;
  font-size: 30rpx;
  color: #1f1f1f;
}

/* ---------- 密码输入（带明文切换） ---------- */
.pwd-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.pwd-input {
  flex: 1;
  padding-right: 88rpx;
}

.pwd-toggle {
  position: absolute;
  right: 24rpx;
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34rpx;
  color: #8a8a8a;
}

/* ---------- 按钮 ---------- */
.submit-btn {
  margin-top: 30rpx;
  height: 96rpx;
  border-radius: 999rpx;
  font-size: 32rpx;
  font-weight: 600;
  color: #fff;
  border: none;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: normal;
}

.submit-btn::after {
  border: none;
}

.guest-btn {
  background: linear-gradient(135deg, #4ecb73, #2fb457);
}

.merchant-btn {
  background: linear-gradient(135deg, #4f9cf9, #2f80ed);
}

.quick-icon {
  margin-right: 10rpx;
}

.hint {
  margin-top: 30rpx;
  text-align: center;
  font-size: 22rpx;
  color: #b0b0b0;
  line-height: 1.6;
}

.manual-link {
  margin-top: 30rpx;
  text-align: center;
  font-size: 26rpx;
  color: #ff8900;
}
</style>
