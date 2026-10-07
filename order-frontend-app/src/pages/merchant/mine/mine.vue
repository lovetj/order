<template>
  <view class="page layout-page">
    <bottom-nav />
    <scroll-view class="layout-body" scroll-y>
    <view class="profile">
      <view class="avatar-wrap" @click="onAvatarTap">
        <image v-if="shop.avatarUrl" class="avatar-img" :src="shop.avatarUrl" mode="aspectFill" />
        <view v-else class="avatar-default">
          <text class="avatar-icon">👤</text>
        </view>
        <view class="avatar-camera">📷</view>
      </view>
      <view class="profile-info">
        <view class="shop-name">{{shop.name}}</view>
        <view class="owner">{{shop.owner}}<text v-if="shop.owner && shop.phone"> · </text>{{shop.phone}}</view>
      </view>
      <view class="status-btn" :class="shop.status === '营业中' ? 'open' : 'closed'" @click="toggleShop">{{shop.status}}</view>
    </view>

    <view class="card menu-card">
      <view
        class="menu-row"
        v-for="item in menus"
        :key="item.key"
        @click="handleMenu(item.key)"
      >
        <text class="menu-icon">{{item.icon}}</text>
        <text class="menu-label">{{item.name}}</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <view class="card switch-card" @click="switchRole">
      <text class="switch-text">切换到「我是顾客」</text>
      <text class="arrow">›</text>
    </view>

    <view class="reset" @click="backToRoleSelect">重新选择身份</view>
    </scroll-view>
  </view>
</template>

<script>
const app = getApp()
import api from '@/api/index'

export default {
  data() {
    return {
      shop: {
        name: '扫码点餐',
        owner: '',
        // 头像完整展示地址，空串表示未设置（展示灰色默认头像）
        avatarUrl: '',
        phone: '',
        status: '营业中'
      },
      uploadingAvatar: false,
      menus: [
        { icon: '🏬', name: '店铺信息', key: 'info' },
        { icon: '🏷️', name: '分类管理', key: 'category' },
        { icon: '🎫', name: '优惠券管理', key: 'coupon' },
        { icon: '🎁', name: '积分商品管理', key: 'pointsGoods' },
        { icon: '🪑', name: '桌位与二维码管理', key: 'table' },
        { icon: '📊', name: '经营报表', key: 'report' },
        { icon: '🕙', name: '营业时间设置', key: 'time' },
        { icon: '💬', name: '联系平台客服', key: 'service' }
      ]
    }
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
    this.loadShop()
    this.loadProfile()
  },
  // 底部导航切换时刷新当前页面数据
  onTabRefresh() {
    if (!app.globalData.isLogin()) return
    this.loadShop()
    this.loadProfile()
  },
  methods: {
    loadShop() {
      // 确保店铺ID与登录账号一致（切换账号后刷新）
      const admin = app.globalData.admin || {}
      if (admin.shopId) {
        app.globalData.setShopId(admin.shopId)
      }
      api.getMerchantShopInfo().then((shop) => {
        if (!shop) return
        this.shop = {
          ...this.shop,
          name: shop.name || '扫码点餐',
          // 展示店家真实姓名，数据源为 shop.real_name
          owner: shop.realName || '',
          phone: shop.phone || admin.phone || '',
          status: shop.businessStatus === 1 ? '营业中' : '休息中'
        }
      }).catch(() => {})
    },

    // 加载店家账号头像（后端返回完整可访问地址，未设置则为空）
    loadProfile() {
      api.getMerchantProfile().then((profile) => {
        this.shop.avatarUrl = (profile && profile.avatar) || ''
      }).catch(() => {})
    },

    // ==================== 头像上传 ====================

    /** 点击头像：未设置则选择上传，已设置则预览 */
    onAvatarTap() {
      if (this.uploadingAvatar) return
      if (this.shop.avatarUrl) {
        this.showAvatarActions()
      } else {
        this.chooseAvatar()
      }
    },

    /** 已设置头像时：提供预览 / 更换 / 删除 */
    showAvatarActions() {
      uni.showActionSheet({
        itemList: ['预览头像', '更换头像', '删除头像'],
        success: (res) => {
          if (res.tapIndex === 0) {
            uni.previewImage({ urls: [this.shop.avatarUrl] })
          } else if (res.tapIndex === 1) {
            this.chooseAvatar()
          } else if (res.tapIndex === 2) {
            this.removeAvatar()
          }
        }
      })
    },

    /** 选择本地图片并上传，成功后写入后端并刷新展示 */
    chooseAvatar() {
      if (this.uploadingAvatar) return
      uni.chooseImage({
        count: 1,
        sourceType: ['album', 'camera'],
        sizeType: ['compressed'],
        success: (res) => {
          const filePath = res.tempFiles[0].tempFilePath
          if (!filePath) return
          this.uploadingAvatar = true
          uni.showLoading({ title: '上传中', mask: true })
          api.uploadFile(filePath, 'avatar').then((data) => {
            // 提交上传接口返回的 url（完整地址），后端会归一化为相对路径入库；
            // 展示直接用后端返回的完整地址字段
            if (!data.url) {
              uni.showToast({ title: '上传结果异常：未获取到地址', icon: 'none' })
              return
            }
            return api.updateMerchantProfile({ avatar: data.url }).then((profile) => {
              this.shop.avatarUrl = (profile && profile.avatar) || data.url
              // 同步到全局，便于其它页面复用
              if (app.globalData.admin) {
                app.globalData.admin.avatar = this.shop.avatarUrl
              }
              uni.showToast({ title: '头像已更新', icon: 'success' })
            })
          }).catch((err) => {
            uni.showToast({ title: (err && err.message) || '头像上传失败', icon: 'none' })
          }).then(() => {
            uni.hideLoading()
            this.uploadingAvatar = false
          })
        }
      })
    },

    /** 删除头像：清空后端头像字段，展示灰色默认头像 */
    removeAvatar() {
      uni.showModal({
        title: '删除头像',
        content: '确定删除当前头像吗？',
        confirmColor: '#ff3b30',
        success: (res) => {
          if (!res.confirm) return
          api.updateMerchantProfile({ avatar: '' }).then(() => {
            this.shop.avatarUrl = ''
            if (app.globalData.admin) {
              app.globalData.admin.avatar = ''
            }
            uni.showToast({ title: '已删除头像', icon: 'success' })
          }).catch((err) => {
            uni.showToast({ title: (err && err.message) || '删除失败', icon: 'none' })
          })
        }
      })
    },

    handleMenu(key) {
      if (key === 'table') {
        uni.navigateTo({ url: '/pages/merchant/tables/tables' })
        return
      }
      if (key === 'report') {
        uni.navigateTo({ url: '/pages/merchant/report/report' })
        return
      }
      if (key === 'category') {
        uni.navigateTo({ url: '/pages/merchant/category/category' })
        return
      }
      if (key === 'coupon') {
        uni.navigateTo({ url: '/pages/merchant/coupon/coupon' })
        return
      }
      if (key === 'pointsGoods') {
        uni.navigateTo({ url: '/pages/merchant/points-goods/points-goods' })
        return
      }
      if (key === 'info') {
        uni.navigateTo({ url: '/pages/merchant/shop-info/shop-info' })
        return
      }
      uni.showToast({ title: '功能开发中', icon: 'none' })
    },

    // 营业状态切换（走接口）
    toggleShop() {
      api.toggleBusiness().then(() => {
        const next = this.shop.status === '营业中' ? '休息中' : '营业中'
        this.shop.status = next
        uni.showToast({ title: `已切换为${next}`, icon: 'none' })
      }).catch(() => {})
    },

    switchRole() {
      uni.showModal({
        title: '切换身份',
        content: '切换到「我是顾客」需要重新登录',
        confirmText: '去登录',
        confirmColor: '#ff6b35',
        success: (res) => {
          if (!res.confirm) return
          app.globalData.logout()
          uni.reLaunch({ url: '/pages/login/login?role=customer' })
        }
      })
    },

    backToRoleSelect() {
      app.globalData.logout()
      uni.reLaunch({ url: '/pages/role/role' })
    }
  }
}
</script>

<style lang="scss" scoped>
.profile {
  display: flex;
  align-items: center;
  padding: 56rpx 40rpx 100rpx;
  background: linear-gradient(135deg, #2f80ed, #1a5fd0);
  color: #fff;
}

.avatar-wrap {
  position: relative;
  width: 130rpx;
  height: 130rpx;
  margin-right: 28rpx;
  flex-shrink: 0;
}

.avatar-img,
.avatar-default {
  width: 130rpx;
  height: 130rpx;
  border-radius: 50%;
  overflow: hidden;
  border: 4rpx solid rgba(255, 255, 255, 0.6);
  box-sizing: border-box;
}

.avatar-img {
  display: block;
}

/* 未设置头像：灰色默认头像 */
.avatar-default {
  background: #d8d8d8;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-icon {
  font-size: 64rpx;
  color: #9a9a9a;
  line-height: 1;
}

/* 右下角相机角标，提示可上传 */
.avatar-camera {
  position: absolute;
  right: -4rpx;
  bottom: -4rpx;
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.15);
}

.profile-info {
  flex: 1;
  /* min-width:0 保证 flex 中可收缩，使店铺名称省略号正常生效 */
  min-width: 0;
}

.shop-name {
  font-size: 38rpx;
  font-weight: 600;
  /* 店铺名称最多一行，超出显示省略号 */
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.owner {
  font-size: 24rpx;
  opacity: 0.85;
  margin-top: 12rpx;
}

.status-btn {
  padding: 14rpx 28rpx;
  border-radius: 999rpx;
  font-size: 25rpx;
  background: rgba(255, 255, 255, 0.22);
}

.status-btn.closed {
  background: rgba(0, 0, 0, 0.25);
}

.menu-card {
  margin-top: -60rpx;
  padding: 0 28rpx;
}

.menu-row {
  display: flex;
  align-items: center;
  height: 108rpx;
  border-bottom: 1rpx solid #f5f5f5;
}

.menu-row:last-child {
  border-bottom: none;
}

.menu-icon {
  font-size: 38rpx;
  margin-right: 22rpx;
}

.menu-label {
  flex: 1;
  font-size: 29rpx;
}

.arrow {
  color: #c8c8c8;
  font-size: 44rpx;
}

.switch-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.switch-text {
  font-size: 29rpx;
  color: #ff6b35;
  font-weight: 600;
}

.reset {
  text-align: center;
  color: #8a8a8a;
  font-size: 26rpx;
  padding: 20rpx 0 40rpx;
}
</style>
