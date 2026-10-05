<template>
  <view class="map-pick-page">
    <!-- 顶部搜索栏 -->
    <view class="search-bar">
      <view class="search-box">
        <input
          class="search-input"
          v-model="keyword"
          placeholder="搜索地点/关键词"
          confirm-type="search"
          @input="onKeywordInput"
          @confirm="doSearch"
        />
        <view v-if="keyword" class="search-clear" @click="clearKeyword">×</view>
      </view>
      <text class="search-btn" @click="doSearch">搜索</text>
    </view>

    <!-- 搜索结果列表（浮层） -->
    <view v-if="showResult" class="result-list">
      <scroll-view scroll-y class="result-scroll">
        <view class="result-item" v-for="(item, i) in list" :key="i" @click="onPickResult(item)">
          <text class="result-name">{{ item.name }}</text>
          <text class="result-addr" v-if="item.address">{{ item.address }}</text>
        </view>
        <view v-if="!list.length" class="result-empty">
          <text>未找到相关地点，试试其他关键词</text>
        </view>
      </scroll-view>
    </view>

    <!-- 全屏地图容器（高德 JS API 挂载） -->
    <view id="mapContainer" class="map-container"></view>

    <!-- 中心准星（覆盖在地图中点，视觉上指向地图中心） -->
    <view class="center-cross">
      <view class="cross-pin"></view>
    </view>

    <!-- 底部地址反显条 -->
    <view class="addr-bar">
      <text class="addr-text">{{ addressResult || addressPlaceholder || '拖动地图选择中心点' }}</text>
    </view>

    <!-- 确认按钮 -->
    <view class="confirm-btn" @click="onConfirm">确认此位置</view>
  </view>
</template>

<script>
// 高德 JS API 配置（仅 H5 使用；key 与安全密钥直接放前端）
const AMAP_KEY = '7ae199cfff9c1744f0b1f023b39d3fbd'
const AMAP_SECURITY_CODE = 'a351a510a7e9747354fb55d5b0a4025d'
// v2.0 安全密钥不能放 URL，须通过 _AMapSecurityConfig 注入
const AMAP_SDK_URL = `https://webapi.amap.com/maps?v=2.0&key=${AMAP_KEY}&plugin=AMap.Geocoder,AMap.PlaceSearch`

// 高德 SDK 加载 Promise：全局只加载一次
let amapPromise = null
function loadAmap() {
  if (window.AMap) return Promise.resolve(window.AMap)
  if (amapPromise) return amapPromise
  amapPromise = new Promise((resolve, reject) => {
    // 必须在加载 SDK 之前注入安全密钥，否则 Geocoder/PlaceSearch 等插件鉴权失败
    window._AMapSecurityConfig = { securityJsCode: AMAP_SECURITY_CODE }
    const script = document.createElement('script')
    script.src = AMAP_SDK_URL
    script.async = true
    script.onload = () => resolve(window.AMap)
    script.onerror = () => {
      amapPromise = null
      reject(new Error('高德地图加载失败'))
    }
    document.head.appendChild(script)
  })
  return amapPromise
}

export default {
  data() {
    return {
      keyword: '',
      list: [],
      showResult: false,
      addressResult: '',
      addressPlaceholder: '',
      map: null,
      selectedPoi: null,
      searchTimer: null
    }
  },
  onLoad(options) {
    if (options.address) {
      this.addressPlaceholder = decodeURIComponent(options.address)
    }
    const saved = {
      latitude: options.latitude,
      longitude: options.longitude
    }
    this._init(saved)
  },
  onReady() {
    // 容器就绪后创建地图
    this.createMap()
  },
  onUnload() {
    if (this.searchTimer) clearTimeout(this.searchTimer)
    if (this.map) this.map.destroy()
    this.map = null
  },
  methods: {
    // 初始化：先拿定位（无初始坐标），再交给 createMap
    _init(saved) {
      if (saved.latitude && saved.longitude) {
        this._initCenter = { latitude: Number(saved.latitude), longitude: Number(saved.longitude) }
        return
      }
      uni.getLocation({
        type: 'gcj02',
        success: (res) => {
          this._initCenter = { latitude: res.latitude, longitude: res.longitude }
          // 若定位晚于地图创建（SDK 加载更快），直接把地图中心移到当前位置
          if (this.map) {
            this.map.setCenter([res.longitude, res.latitude])
            this.regeo()
          }
        },
        fail: () => { }
      })
    },

    // 创建高德地图
    createMap() {
      loadAmap().then(() => {
        // 等待 _initCenter 就绪（无初始坐标时可能尚未定位完成，先给兜底中心）
        const c = this._initCenter || { latitude: 22.5431, longitude: 114.0579 }
        this.map = new window.AMap.Map('mapContainer', {
          zoom: 16,
          center: [c.longitude, c.latitude]
        })
        // 移动 / 缩放结束后反显中心地址
        this.map.on('moveend', () => this.onMapSettle())
        this.map.on('zoomend', () => this.onMapSettle())
        // 有初始中心则先反显一次
        if (this._initCenter) this.regeo()
      }).catch(() => {
        uni.showToast({ title: '高德地图加载失败，请检查网络', icon: 'none' })
      })
    },

    // 地图移动结束：取中心坐标并逆地理
    onMapSettle() {
      const center = this.getCenter()
      if (center) this.regeo(center)
    },

    getCenter() {
      if (!this.map) return null
      const c = this.map.getCenter()
      return c ? { latitude: c.getLat(), longitude: c.getLng() } : null
    },

    // 逆地理编码：坐标 -> 地址文本（AMap.Geocoder）
    regeo(center) {
      const c = center || this.getCenter()
      if (!c || !window.AMap) return
      // 插件可能尚未加载完成，用 AMap.plugin 确保就绪后再实例化
      window.AMap.plugin(['AMap.Geocoder'], () => {
        const geocoder = new window.AMap.Geocoder({ city: '', radius: 1000 })
        geocoder.getAddress([c.longitude, c.latitude], (status, result) => {
          if (status === 'complete' && result && result.regeocode) {
            const addr = result.regeocode.formattedAddress || ''
            this.addressResult = addr
            this.addressPlaceholder = ''
          } else {
            this.addressResult = ''
            this.addressPlaceholder = ''
          }
        })
      })
    },

    // 搜索：输入防抖 + 回车搜索
    onKeywordInput() {
      clearTimeout(this.searchTimer)
      this.searchTimer = setTimeout(() => this.doSearch(), 300)
    },
    doSearch() {
      const kw = (this.keyword || '').trim()
      if (!kw || !window.AMap) {
        this.showResult = false
        return
      }
      // 确保 PlaceSearch 插件就绪后再实例化
      window.AMap.plugin(['AMap.PlaceSearch'], () => {
        const placeSearch = new window.AMap.PlaceSearch({ pageSize: 10, pageIndex: 1, citylimit: false })
        placeSearch.search(kw, (status, result) => {
          if (status === 'complete' && result && result.poiList) {
            this.list = (result.poiList.pois || []).map((p) => ({
              name: p.name,
              address: p.address || p.pname + p.cityname + p.adname,
              latitude: p.location.getLat(),
              longitude: p.location.getLng()
            }))
            this.showResult = true
          } else {
            this.list = []
            this.showResult = true
          }
        })
      })
    },
    clearKeyword() {
      this.keyword = ''
      this.showResult = false
    },

    // 点击搜索结果：移动到该点并直接反显
    onPickResult(item) {
      this.selectedPoi = item
      this.keyword = item.name
      this.showResult = false
      this.map.setCenter([item.longitude, item.latitude])
      this.addressResult = item.address || item.name || ''
      this.addressPlaceholder = ''
    },

    // 确认并回传选点结果（坐标取地图当前中心 = 准星指向）
    onConfirm() {
      const c = this.getCenter()
      if (!c) {
        uni.showToast({ title: '地图未就绪，请稍后再试', icon: 'none' })
        return
      }
      const address = this.addressResult || this.addressPlaceholder || ''
      uni.$emit('shop-location-selected', {
        address,
        name: (this.selectedPoi && this.selectedPoi.name) || address || '店铺位置',
        latitude: c.latitude,
        longitude: c.longitude
      })
      uni.navigateBack()
    }
  }
}
</script>

<style lang="scss" scoped>
.map-pick-page {
  position: relative;
  width: 100%;
  height: 100vh; /* 小程序端兜底 */
  overflow: hidden;
  /* #ifdef H5 */
  /* H5：减去原生导航栏高度（与 uni-h5 uni-page-wrapper 公式一致），避免底部确认按钮被挤出屏幕 */
  height: calc(100vh - 44px - env(safe-area-inset-top));
  /* #endif */
}

.map-container {
  width: 100%;
  height: 100%;
}

/* 顶部搜索栏 */
.search-bar {
  position: absolute;
  top: 20rpx;
  left: 24rpx;
  right: 24rpx;
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.search-box {
  flex: 1;
  display: flex;
  align-items: center;
  height: 76rpx;
  padding: 0 24rpx;
  background: #fff;
  border-radius: 999rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.1);
}

.search-input {
  flex: 1;
  font-size: 28rpx;
  color: #222;
}

.search-clear {
  width: 40rpx;
  height: 40rpx;
  line-height: 38rpx;
  text-align: center;
  font-size: 32rpx;
  color: #999;
}

.search-btn {
  flex-shrink: 0;
  padding: 0 28rpx;
  height: 76rpx;
  line-height: 76rpx;
  font-size: 28rpx;
  color: #fff;
  background: #2f80ed;
  border-radius: 999rpx;
}

/* 搜索结果列表 */
.result-list {
  position: absolute;
  top: 112rpx;
  left: 24rpx;
  right: 24rpx;
  z-index: 20;
  max-height: 520rpx;
  background: #fff;
  border-radius: 16rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.12);
  overflow: hidden;
}

.result-scroll {
  max-height: 520rpx;
}

.result-item {
  padding: 22rpx 24rpx;
  border-bottom: 1rpx solid #f2f3f5;
}

.result-item:last-child {
  border-bottom: none;
}

.result-name {
  display: block;
  font-size: 28rpx;
  color: #222;
}

.result-addr {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #999;
}

.result-empty {
  padding: 40rpx;
  text-align: center;
  font-size: 26rpx;
  color: #999;
}

/* 中心准星 */
.center-cross {
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: 10;
  transform: translate(-50%, -50%);
  width: 44rpx;
  height: 44rpx;
  pointer-events: none;
}

.cross-pin {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50% 50% 50% 0;
  transform: rotate(-45deg);
  background: #2f80ed;
  border: 4rpx solid #fff;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.3);
}

/* 底部地址条 */
.addr-bar {
  position: absolute;
  left: 24rpx;
  right: 24rpx;
  bottom: 160rpx;
  z-index: 20;
  padding: 20rpx 24rpx;
  background: #fff;
  border-radius: 16rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.12);
}

.addr-text {
  font-size: 28rpx;
  color: #333;
  line-height: 1.5;
}

/* 确认按钮 */
.confirm-btn {
  position: absolute;
  left: 24rpx;
  right: 24rpx;
  bottom: calc(40rpx + env(safe-area-inset-bottom));
  z-index: 20;
  height: 96rpx;
  line-height: 96rpx;
  text-align: center;
  font-size: 32rpx;
  font-weight: 600;
  color: #fff;
  background: #2f80ed;
  border-radius: 999rpx;
  box-shadow: 0 8rpx 24rpx rgba(47, 128, 237, 0.4);
}
</style>