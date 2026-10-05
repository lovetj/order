<script>
export default {
  globalData: {
    // 'customer' | 'merchant'
    role: '',
    // 当前店铺ID（扫码得到，多店铺隔离的核心标识）
    shopId: '',
    // 扫码进入时携带的桌号
    tableNo: '',
    // 购物车：{ [dishId|specText]: { id, key, name, price, image, hasImage, imageUrl, specIds, specText, count } }
    cart: {},
    // 登录用户信息
    userInfo: {
      nickName: '用餐用户',
      avatar: '🙋',
      memberLevel: '普通会员'
    },
    // 店家账号信息（登录后写入，含所属 shopId）
    admin: null,

    /**
     * 解析启动/切入参数，提取店铺ID与桌号
     * 支持两种二维码格式：
     *   1) 应用内页面路径：pages/role/role?shopId=1&tableNo=A01
     *   2) 普通 URL：https://xxx?shopId=1&tableNo=A01
     */
    handleLaunchQuery(query) {
      const shopId = query.shopId || ''
      const tableNo = query.tableNo || ''
      if (shopId) {
        this.setShopId(shopId)
      }
      if (tableNo) {
        this.setTableNo(tableNo)
      }
    },

    // 设置身份
    setRole(role) {
      this.role = role
      uni.setStorageSync('role', role)
    },

    // 清空身份（切换身份 / 退出）
    clearRole() {
      this.role = ''
      this.cart = {}
      this.admin = null
      uni.removeStorageSync('role')
    },

    // 设置当前店铺
    setShopId(shopId) {
      this.shopId = shopId || ''
      if (shopId) {
        uni.setStorageSync('shopId', shopId)
      } else {
        uni.removeStorageSync('shopId')
      }
    },

    // 设置桌号
    setTableNo(tableNo) {
      this.tableNo = tableNo || ''
      if (tableNo) {
        uni.setStorageSync('tableNo', tableNo)
      } else {
        uni.removeStorageSync('tableNo')
      }
    },

    // 是否已登录（有 token）
    isLogin() {
      return !!(uni.getStorageSync('token') || '')
    },

    // 退出登录
    logout() {
      uni.removeStorageSync('token')
      this.clearRole()
    }
  },
  onLaunch() {
    // 恢复上次选择的身份、店铺与桌号
    this.globalData.role = uni.getStorageSync('role') || ''
    this.globalData.shopId = uni.getStorageSync('shopId') || ''
    this.globalData.tableNo = uni.getStorageSync('tableNo') || ''

    this.globalData.handleLaunchQuery(uni.getLaunchOptionsSync().query || {})
  },
  onShow(options) {
    // 从扫码/分享等场景再次进入时，同样解析 query
    this.globalData.handleLaunchQuery((options && options.query) || {})
  }
}
</script>

<style lang="scss">
/* 全局基础样式 */
page {
  background: #f5f6f8;
  color: #1f1f1f;
  font-size: 28rpx;
  font-family: -apple-system, BlinkMacSystemFont, "Helvetica Neue", "PingFang SC", "Microsoft YaHei", sans-serif;
  --primary: #ff6b35;
  --primary-light: #fff1eb;
  --text-main: #1f1f1f;
  --text-sub: #8a8a8a;
  --line: #ececec;
  --card: #ffffff;
  --radius: 20rpx;
}

view,
text,
image,
scroll-view,
button {
  box-sizing: border-box;
}

.page,
.container {
  min-height: 100vh;
  padding-bottom: 140rpx; /* 给底部导航留出空间 */
}

/* 三段式固定布局（tab 主页面）：
   根容器铺满视口并禁止自身滚动，头部/内容在内部 flex 布局，
   只让中部（layout-body）独立滚动，底部导航保持 fixed。 */
.layout-page {
  height: 100vh;
  min-height: 0; /* 覆盖 .page/.container 的 min-height:100vh，避免 H5 上被撑高产生页面级滚动 */
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-sizing: border-box;
  padding-bottom: 0; /* 底部留白由 layout-body 内部承担 */
  /* #ifdef H5 */
  /* H5：减去原生导航栏高度（与 uni-h5 的 uni-page-wrapper 公式一致），避免页面级滚动条 */
  height: calc(100vh - 44px - env(safe-area-inset-top));
  /* #endif */
}

/* 固定头部：不参与中部滚动 */
.layout-header {
  flex-shrink: 0;
  background: #fff;
}

/* 中部滚动区：仅这里出现滚动条 */
.layout-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  box-sizing: border-box;
  padding-bottom: 140rpx; /* 避免内容被底部导航遮挡 */
}

.card {
  background: var(--card);
  border-radius: var(--radius);
  padding: 28rpx;
  margin: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.section-title {
  font-size: 32rpx;
  font-weight: 600;
  color: var(--text-main);
}

.text-sub {
  color: var(--text-sub);
  font-size: 24rpx;
}

.price {
  color: var(--primary);
  font-weight: 600;
}

.btn-primary {
  background: linear-gradient(135deg, #ff8a5c, #ff6b35);
  color: #fff;
  border-radius: 999rpx;
  font-size: 30rpx;
  border: none;
}

.btn-primary::after {
  border: none;
}

.empty {
  text-align: center;
  color: var(--text-sub);
  font-size: 26rpx;
  padding: 120rpx 0;
}

/* 通用按钮重置（uni-app 需要清除 button 默认边框） */
button::after {
  border: none;
}
</style>
