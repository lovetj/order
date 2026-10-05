<template>
  <!-- 登录页（顾客=昵称+头像快速登录 / 店家=账号密码） -->
  <view class="login-page">
    <view class="back" @click="goBack">‹ 返回</view>

    <view class="hero">
      <view class="logo">{{role === 'merchant' ? '🏪' : '🍔'}}</view>
      <view class="title">{{role === 'merchant' ? '店家登录' : '欢迎光临'}}</view>
      <view class="subtitle">{{role === 'merchant' ? '请使用店家账号密码登录' : '填写昵称即可快速登录'}}</view>
      <view v-if="tableNo" class="table-tag">当前桌号：{{tableNo}}</view>
    </view>

    <!-- ==================== 顾客端：昵称+头像快速登录 ==================== -->
    <view v-if="role === 'customer'" class="panel">
      <view class="avatar-row">
        <view class="avatar-btn" @click="chooseAvatar">
          <image v-if="avatarUrl" class="avatar-img" :src="avatarUrl" mode="aspectFill" />
          <view v-else class="avatar-placeholder">＋</view>
        </view>
        <view class="avatar-tip">点击选择头像</view>
      </view>

      <view class="field">
        <view class="field-label">昵称</view>
        <input
          class="field-input"
          placeholder="请输入昵称"
          :value="nickName"
          @input="onNicknameInput"
          @blur="onNicknameInput"
        />
      </view>

      <button class="submit-btn guest-btn" :loading="logging" @click="customerLogin">
        <text class="quick-icon">🚀</text> 一键登录
      </button>

      <view class="hint">首次登录将自动创建账号，无需单独注册</view>
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
// 顾客端：昵称+头像快速登录（本地设备标识作为登录凭证，后端接口沿用 /api/user/wx-login）
// 店家端：账号密码登录（不提供注册）
const app = getApp()
import api from '@/api/index'
import { setToken } from '@/utils/request'

export default {
  data() {
    return {
      // customer | merchant
      role: 'customer',
      tableNo: '',
      logging: false,
      // 顾客端：头像昵称
      avatarUrl: '',
      nickName: '',
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
  },

  methods: {
    goBack() {
      uni.reLaunch({ url: '/pages/role/role' })
    },

    // 从相册选择头像（跨端通用）
    chooseAvatar() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        success: (res) => {
          const path = res.tempFilePaths && res.tempFilePaths[0]
          if (path) this.avatarUrl = path
        }
      })
    },

    onNicknameInput(e) {
      this.nickName = (e.detail.value || '').trim()
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

    // ==================== 顾客端：快速登录 ====================
    customerLogin() {
      if (this.logging) return
      const nick = (this.nickName || '').trim()
      if (!nick) {
        uni.showToast({ title: '请输入昵称', icon: 'none' })
        return
      }
      this.logging = true

      // 本地生成稳定的设备标识作为登录凭证（后端 Mock 模式下任意非空 code 均可建档）
      api.login({
        code: this.getDeviceCode(),
        nickname: nick,
        avatar: '',
        role: 'customer'
      }).then((data) => {
        // 先写入 token，后续头像上传需要鉴权
        setToken(data.token)
        const user = data.user || {}

        // 本地选择的是临时路径，需上传到后端持久化
        return this.uploadAvatarIfNeeded().then((avatarUrl) => {
          if (avatarUrl) {
            user.avatar = avatarUrl
            // 同步更新后端用户资料
            api.updateProfile({ nickname: user.nickname, avatar: avatarUrl }).catch(() => {})
          }
          app.globalData.userInfo = {
            nickName: user.nickname || nick,
            avatar: avatarUrl || user.avatar || '🙋',
            memberLevel: user.memberLevel || '普通会员'
          }
          app.globalData.admin = null
          app.globalData.setRole('customer')
          this.logging = false
          uni.showToast({ title: '登录成功', icon: 'success', duration: 700 })
          setTimeout(() => {
            uni.reLaunch({ url: '/pages/index/index' })
            // 未识别店铺时提示扫码，避免点餐提交被后端拒绝
            if (!app.globalData.shopId) {
              setTimeout(() => {
                uni.showToast({ title: '请扫描桌位二维码以识别店铺', icon: 'none', duration: 2000 })
              }, 800)
            }
          }, 700)
        })
      }).catch((err) => {
        this.logging = false
        uni.showToast({ title: (err && err.message) || '登录失败', icon: 'none' })
      })
    },

    // 生成/读取本地设备标识（首次生成后持久化，保证同一设备账号稳定）
    getDeviceCode() {
      let code = uni.getStorageSync('deviceCode')
      if (!code) {
        code = 'device_' + Date.now().toString(36) + '_' + Math.random().toString(36).slice(2, 10)
        uni.setStorageSync('deviceCode', code)
      }
      return code
    },

    /**
     * 上传本地选择头像到后端（临时路径无法长期使用）
     * @returns {Promise<string>} 后端返回的持久化头像地址，失败返回空串
     */
    uploadAvatarIfNeeded() {
      const avatarUrl = this.avatarUrl
      if (!avatarUrl) {
        return Promise.resolve('')
      }
      // 已是远端完整地址（http/https 开头）则无需再上传
      const isRemote = /^https?:\/\//i.test(avatarUrl)
      if (isRemote) {
        return Promise.resolve(avatarUrl)
      }
      return api.uploadFile(avatarUrl, 'avatar')
        .then((data) => (data && (data.url || data.relativePath)) || '')
        .catch(() => '')
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
  min-height: 100vh;
  padding: 120rpx 60rpx 80rpx;
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

.panel {
  background: #fff;
  border-radius: 28rpx;
  padding: 50rpx 40rpx;
  box-shadow: 0 10rpx 36rpx rgba(0, 0, 0, 0.06);
}

/* ---------- 顾客端头像授权 ---------- */
.avatar-row {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 44rpx;
}

.avatar-btn {
  width: 160rpx;
  height: 160rpx;
  padding: 0;
  border-radius: 50%;
  background: #f5f6f8;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  line-height: 1;
}

.avatar-btn::after {
  border: none;
}

.avatar-img {
  width: 160rpx;
  height: 160rpx;
  border-radius: 50%;
}

.avatar-placeholder {
  font-size: 64rpx;
  color: #c8c8c8;
}

.avatar-tip {
  margin-top: 18rpx;
  font-size: 24rpx;
  color: #8a8a8a;
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
</style>
