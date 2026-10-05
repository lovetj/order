<template>
  <view class="page">
    <!-- 店铺 Logo -->
    <view class="card">
      <view class="card-title"><text class="required">*</text>店铺 Logo</view>
      <view class="logo-wrap">
        <view class="logo-item">
          <view class="logo-box" @click="onLogoTap">
            <image v-if="logoUrl" class="logo-img" :src="logoUrl" mode="aspectFill" />
            <text v-else class="logo-placeholder">＋</text>
          </view>
          <view v-if="logoUrl" class="logo-del" @click.stop="removeLogo">×</view>
        </view>
      </view>
    </view>

    <!-- 店铺图片（多图） -->
    <view class="card">
      <view class="card-title"><text class="required">*</text>店铺图片<text class="card-sub">（最多 9 张，单张不超过 10MB）</text></view>
      <view class="image-grid">
        <view
          class="image-item"
          v-for="(item, index) in imageUrls"
          :key="index"
        >
          <image class="image-thumb" :src="item" mode="aspectFill" @click="previewImage(index)" />
          <view class="image-del" @click.stop="removeImage(index)">×</view>
        </view>
        <view v-if="imageUrls.length < 9" class="image-add" @click="chooseImages">
          <text class="add-icon">＋</text>
          <text class="add-text">上传图片</text>
        </view>
      </view>
    </view>

    <!-- 基础信息 -->
    <view class="card">
      <view class="card-title">基础信息</view>
      <view class="field">
        <text class="label"><text class="required">*</text>店铺名称</text>
        <input class="input" :value="form.name" placeholder="请输入店铺名称" @input="onInput('name', $event)" />
      </view>
      <view class="field">
        <text class="label"><text class="required">*</text>真实姓名</text>
        <input class="input" :value="form.realName" placeholder="请输入店家真实姓名" @input="onInput('realName', $event)" />
      </view>
      <view class="field">
        <text class="label"><text class="required">*</text>联系电话</text>
        <input class="input" type="number" :value="form.phone" placeholder="请输入联系电话" @input="onInput('phone', $event)" />
      </view>
      <view class="field">
        <text class="label"><text class="required">*</text>店铺地址</text>
        <input class="input" :value="form.address" placeholder="请输入或点击右侧定位" @input="onInput('address', $event)" />
        <view class="loc-btn" @click.stop="onChooseLocation">定位</view>

        <!-- 定位中：全屏蒙版加载效果 -->
        <view v-if="locating" class="loc-mask">
          <view class="loc-mask-box">
            <view class="loc-mask-spinner"></view>
            <text class="loc-mask-text">定位中…</text>
          </view>
        </view>
      </view>
      <view class="map-preview-wrap" v-if="form.latitude && form.longitude">
        <map
          :key="mapPreviewKey"
          class="map-preview"
          :latitude="form.latitude"
          :longitude="form.longitude"
          :markers="mapMarkers"
          scale="16"
          :enable-zoom="false"
          :enable-scroll="false"
        ></map>
      </view>
      <view class="field">
        <text class="label">宣传语</text>
        <input class="input" :value="form.slogan" placeholder="如：现点现做 · 用心出餐" @input="onInput('slogan', $event)" />
      </view>
      <view class="field column">
        <text class="label">店内公告</text>
        <textarea class="textarea" :value="form.notice" placeholder="多条公告可用 | 分隔，如：营业时间 10:00-22:00" maxlength="500" @input="onInput('notice', $event)" />
      </view>
    </view>

    <!-- 底部漂浮操作栏 -->
    <view class="action-bar">
      <view v-if="hasUnsaved" class="unsaved-tip">有未保存的修改</view>
      <button class="cancel-btn" :disabled="saving" @click="cancel">取消</button>
      <button class="save-btn" :class="{ 'save-btn-active': hasUnsaved }" :loading="saving" :disabled="saving || uploading" @click="save">保存</button>
    </view>
  </view>
</template>

<script>
import api from '@/api/index'
import { toRelativePath } from '@/utils/util'

const MAX_IMAGES = 9

export default {
  data() {
    return {
      saving: false,
      uploading: false,
      // 是否存在未保存的改动：用于离页提醒与「未保存」提示
      hasUnsaved: false,
      // 店铺地址「定位」按钮点击后的加载中状态
      locating: false,
      // 图片完整展示地址（用于 image 渲染）
      logoUrl: '',
      imageUrls: [],
      // 图片是否被用户改动过：未改动的字段不提交，避免空值覆盖数据库已有图片
      logoDirty: false,
      imagesDirty: false,
      form: {
        name: '',
        realName: '',
        logo: '',
        images: [],
        slogan: '',
        notice: '',
        address: '',
        phone: '',
        latitude: null,
        longitude: null
      },
      // 地图选点标记
      mapMarkers: [],
      // 地图预览重建标记：H5 上 map 组件从其他页面返回后偶发空白，通过变更 key 强制重建
      mapPreviewKey: 0
    }
  },
  onLoad() {
    this.loadShop()
    // 监听地图选点页回传
    uni.$on('shop-location-selected', this.onLocationSelected)
  },
  onShow() {
    // H5：map 组件在页面切换/返回过程中挂载时偶发空白，返回本页时强制重建预览地图
    if (this._pageShown) {
      this.mapPreviewKey = Date.now()
    }
    this._pageShown = true
  },
  onUnload() {
    // 解绑选点事件，避免重复触发/内存泄漏
    uni.$off('shop-location-selected', this.onLocationSelected)
    // 关闭系统级离页拦截，避免已保存后仍弹窗
    if (this._alertEnabled && uni.disableAlertBeforeUnload) {
      uni.disableAlertBeforeUnload()
      this._alertEnabled = false
    }
  },
  methods: {
    /**
     * 离页守护：有未保存改动时，阻止直接返回并二次确认。
     * 尤其是「已上传图片但没点保存」的情况，此时图片文件已在服务器上，
     * 但路径尚未入库，必须提醒用户保存。
     */
    enableLeaveGuard() {
      if (uni.enableAlertBeforeUnload && !this._alertEnabled) {
        uni.enableAlertBeforeUnload({
          message: this.logoDirty || this.imagesDirty
            ? '图片已上传但尚未保存，离开将不会生效，确定离开吗？'
            : '店铺信息尚未保存，确定离开吗？'
        })
        this._alertEnabled = true
      }
    },

    // 标记有未保存改动（图片上传/删除、文本输入都会触发）
    markDirty() {
      if (!this.hasUnsaved) {
        this.hasUnsaved = true
      }
      this.enableLeaveGuard()
    },

    loadShop() {
      // 店家端接口：从登录 token 解析店铺，保证读到的是本店真实数据
      api.getMerchantShopInfo().then((shop) => {
        if (!shop) return
        // 后端已用 file.base-server 把 logo/images 拼成完整可访问地址（url/logo/images 字段），
        // 这里直接采用接口字段展示与提交；提交时后端会归一化回相对路径入库
        const logo = shop.logo || ''
        const images = this.parseImages(shop.images)

        // 关键点：只更新「图片」相关字段，且用合并方式写回 form。
        // 早先直接整体替换 form，若用户已先上传完图片、本请求才返回，
        // 会把刚上传的路径覆盖成空值，导致最终保存时写入空字符串。
        const latitude = shop.latitude != null ? Number(shop.latitude) : null
        const longitude = shop.longitude != null ? Number(shop.longitude) : null

        this.form = {
          ...this.form,
          name: shop.name || '',
          realName: shop.realName || '',
          logo: this.logoDirty ? this.form.logo : logo,
          images: this.imagesDirty ? this.form.images : images,
          slogan: shop.slogan || '',
          notice: shop.notice || '',
          address: shop.address || '',
          phone: shop.phone || '',
          latitude,
          longitude
        }
        this.logoUrl = this.logoDirty ? this.form.logo : logo
        this.imageUrls = this.imagesDirty ? this.form.images : images
        this.mapMarkers = latitude && longitude
          ? [{
              id: 1,
              latitude,
              longitude,
              title: shop.address || '店铺位置',
              iconPath: '/static/map/marker.png',
              width: 32,
              height: 32
            }]
          : []
      }).catch(() => {})
    },

    // 解析图片字段：支持逗号分隔 / JSON 数组，返回相对路径数组
    parseImages(images) {
      if (!images) return []
      let str = String(images).trim()
      if (str.startsWith('[') && str.endsWith(']')) {
        str = str.slice(1, -1)
      }
      return str.split(',')
        .map((s) => s.trim().replace(/^["']|["']$/g, ''))
        .filter(Boolean)
    },

    onInput(field, e) {
      this.form[field] = e.detail.value
      this.markDirty()
    },

    // ==================== 店铺地址（站内地图定位选点） ====================

    /**
     * 店铺地址（站内地图选点 / 实时定位）：
     * 已有保存的定位地址（经纬度）时，以该地址为中心打开选点页；
     * 否则获取当前实时定位作为初始中心。选点页支持拖动选点 + 地址搜索，
     * 确认后通过全局事件回传地址与经纬度。
     */
    onChooseLocation() {
      // 防重入：选点页正在打开/已经打开时，忽略重复触发
      if (this._picking) return
      this._picking = true
      // 定位中：按钮显示加载动画
      this.locating = true

      const that = this
      const done = () => { that._picking = false; that.locating = false }

      const openMapChooser = (latitude, longitude) => {
        // 跳转自研地图选点页，携带当前坐标与已填地址作为初始中心/占位
        const query = []
        if (latitude && longitude) {
          query.push(`latitude=${latitude}`, `longitude=${longitude}`)
        }
        if (this.form.address) {
          query.push(`address=${encodeURIComponent(this.form.address)}`)
        }
        uni.navigateTo({
          url: `/pages/merchant/map-pick/map-pick${query.length ? '?' + query.join('&') : ''}`,
          success: () => { that.locating = false },
          fail: done
        })
      }

      // 已有定位地址（保存过经纬度）：以店铺原地址为中心打开选点页
      if (this.form.latitude && this.form.longitude) {
        openMapChooser(this.form.latitude, this.form.longitude)
        return
      }

      // 地址为空：获取当前位置作为选点初始中心
      uni.getLocation({
        type: 'gcj02',
        success: (locRes) => {
          openMapChooser(locRes.latitude, locRes.longitude)
        },
        fail: (err) => {
          const msg = (err && err.errMsg) || ''
          // 权限相关才引导设置；其余情况（如定位超时）直接打开选点页
          if (msg.indexOf('auth') > -1 || msg.indexOf('deny') > -1) {
            this._picking = false
            this.locating = false
            uni.showModal({
              title: '提示',
              content: '需要获取您的地理位置权限以在地图上选点，请前往设置开启',
              confirmText: '去开启',
              success: (modalRes) => {
                if (modalRes.confirm) uni.openSetting()
              }
            })
            return
          }
          // 获取当前位置失败时直接打开选点页
          openMapChooser()
        }
      })
    },

    /**
     * 地图选点页回传处理：写回地址与经纬度，并更新地图预览标记
     */
    onLocationSelected(data) {
      this._picking = false
      if (!data) return
      const latitude = Number(data.latitude)
      const longitude = Number(data.longitude)
      if (isNaN(latitude) || isNaN(longitude)) return
      this.form.latitude = latitude
      this.form.longitude = longitude
      // 未返回地址时保留旧值，避免空串覆盖已有的地址
      this.form.address = (data && data.address) || this.form.address || ''
      this.mapMarkers = [{
        id: 1,
        latitude,
        longitude,
        title: (data && (data.name || data.address)) || '店铺位置',
        iconPath: '/static/map/marker.png',
        width: 32,
        height: 32
      }]
      this.markDirty()
      uni.showToast({ title: '已成功定位选点', icon: 'success' })
    },

    // ==================== 店铺 Logo（单张） ====================

    /**
     * 点击 Logo：与店铺图片交互保持一致
     * - 已上传：预览大图
     * - 未上传：唤起选择（此时方框显示 ＋）
     */
    onLogoTap() {
      if (this.logoUrl) {
        this.previewLogo()
      } else {
        this.chooseLogo()
      }
    },

    chooseLogo() {
      if (this.uploading) return
      uni.chooseMedia({
        count: 1,
        mediaType: ['image'],
        sourceType: ['album', 'camera'],
        sizeType: ['compressed'],
        success: (res) => {
          const filePath = res.tempFiles[0].tempFilePath
          this.uploading = true
          uni.showLoading({ title: '上传中', mask: true })
          api.uploadFile(filePath, 'shop').then((data) => {
            // 直接使用上传接口返回的 url（已是完整可访问地址），提交时后端归一化入库
            if (!data.url) {
              uni.showToast({ title: '上传结果异常：未获取到地址', icon: 'none' })
              return
            }
            this.form.logo = data.url
            this.logoUrl = data.url
            this.logoDirty = true
            this.markDirty()
            uni.showToast({ title: '上传成功，记得点保存', icon: 'none' })
          }).catch((err) => {
            uni.showToast({ title: err.message || '上传失败', icon: 'none' })
          }).then(() => {
            uni.hideLoading()
            this.uploading = false
          })
        }
      })
    },

    previewLogo() {
      if (!this.logoUrl) return
      uni.previewImage({ urls: [this.logoUrl] })
    },

    removeLogo() {
      const logo = this.form.logo
      if (!logo) return
      uni.showModal({
        title: '删除确认',
        content: '确定删除店铺 Logo 吗？删除后服务器上的图片也会被移除。',
        confirmColor: '#ff3b30',
        success: (res) => {
          if (!res.confirm) return
          // 先更新本地表单，再真实删除服务器文件；删除失败不影响本地已移除的结果
          this.form.logo = ''
          this.logoUrl = ''
          this.logoDirty = true
          this.markDirty()
          // form.logo 是完整地址，删除接口需要相对路径
          api.deleteFile(toRelativePath(logo)).catch((err) => {
            console.warn('[shop-info] 删除 Logo 文件失败', logo, err)
          })
        }
      })
    },

    // ==================== 店铺图片（多图上传） ====================

    chooseImages() {
      if (this.uploading) return
      const remain = MAX_IMAGES - this.form.images.length
      if (remain <= 0) {
        uni.showToast({ title: `最多上传 ${MAX_IMAGES} 张`, icon: 'none' })
        return
      }
      uni.chooseMedia({
        count: remain,
        mediaType: ['image'],
        sourceType: ['album', 'camera'],
        sizeType: ['compressed'],
        success: (res) => {
          const paths = (res.tempFiles || []).map((f) => f.tempFilePath)
          if (!paths.length) return
          this.uploading = true
          uni.showLoading({ title: `上传中 0/${paths.length}`, mask: true })
          // 多文件逐个上传；后端原图保存不做压缩，仅校验单张不超过 10MB
          api.uploadFiles(paths, 'shop').then((list) => {
            // 直接使用上传接口返回的 url（完整可访问地址），提交时后端归一化入库
            const added = (list || []).map((item) => item.url).filter(Boolean)
            if (!added.length) {
              uni.showToast({ title: '上传结果异常：未获取到地址', icon: 'none' })
              return
            }
            const images = this.form.images.concat(added)
            this.form.images = images
            this.imageUrls = images
            this.imagesDirty = true
            this.markDirty()
            uni.showToast({ title: `成功上传 ${added.length} 张，记得点保存`, icon: 'none' })
          }).catch((err) => {
            uni.showToast({ title: err.message || '上传失败', icon: 'none' })
          }).then(() => {
            uni.hideLoading()
            this.uploading = false
          })
        }
      })
    },

    previewImage(index) {
      uni.previewImage({
        current: this.imageUrls[index],
        urls: this.imageUrls
      })
    },

    removeImage(index) {
      const removed = this.form.images[index]
      uni.showModal({
        title: '删除确认',
        content: '确定删除这张店铺图片吗？删除后服务器上的图片也会被移除。',
        confirmColor: '#ff3b30',
        success: (res) => {
          if (!res.confirm) return
          const images = [...this.form.images]
          images.splice(index, 1)
          this.form.images = images
          this.imageUrls = images
          this.imagesDirty = true
          this.markDirty()
          // 真实删除服务器文件；删除失败不影响本地已移除的结果
          if (removed) {
            // removed 是完整地址，删除接口需要相对路径
            api.deleteFile(toRelativePath(removed)).catch((err) => {
              console.warn('[shop-info] 删除店铺图片文件失败', removed, err)
            })
          }
        }
      })
    },

    save() {
      if (this.saving) return
      const { form } = this
      if (!form.name || !form.name.trim()) {
        uni.showToast({ title: '请输入店铺名称', icon: 'none' })
        return
      }
      if (!form.realName || !form.realName.trim()) {
        uni.showToast({ title: '请输入真实姓名', icon: 'none' })
        return
      }
      if (!form.phone || !form.phone.trim()) {
        uni.showToast({ title: '请输入联系电话', icon: 'none' })
        return
      }
      if (!/^\d{6,20}$/.test(form.phone.trim())) {
        uni.showToast({ title: '请输入有效的联系电话', icon: 'none' })
        return
      }
      if (!form.address || !form.address.trim()) {
        uni.showToast({ title: '请输入店铺地址', icon: 'none' })
        return
      }
      if (!form.logo) {
        uni.showToast({ title: '请上传店铺 Logo', icon: 'none' })
        return
      }

      const list = Array.isArray(form.images) ? form.images : this.parseImages(form.images)
      if (!list.length) {
        uni.showToast({ title: '请至少上传一张店铺图片', icon: 'none' })
        return
      }

      const payload = {
        name: form.name.trim(),
        realName: form.realName.trim(),
        slogan: form.slogan,
        notice: form.notice,
        address: form.address.trim(),
        phone: form.phone.trim(),
        latitude: form.latitude != null ? Number(form.latitude) : null,
        longitude: form.longitude != null ? Number(form.longitude) : null
      }

      // 只有用户改动过图片时才提交这两个字段。
      // 未改动时字段为 undefined（序列化后会被丢弃），后端收到 null 即「不修改」，
      // 从根本上避免空值把数据库中已有的图片路径覆盖成空字符串。
      if (this.logoDirty) {
        payload.logo = form.logo || ''
      }
      if (this.imagesDirty) {
        payload.images = list.join(',')
      }

      this.saving = true
      api.updateShop(payload).then(() => {
        // 保存成功：关闭离页拦截，避免返回时再弹确认
        if (this._alertEnabled && uni.disableAlertBeforeUnload) {
          uni.disableAlertBeforeUnload()
          this._alertEnabled = false
        }
        this.hasUnsaved = false
        this.logoDirty = false
        this.imagesDirty = false
        uni.showToast({ title: '保存成功', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 700)
      }).catch(() => {
        this.saving = false
      })
    },

    // 取消编辑：有未保存改动时二次确认后返回
    cancel() {
      if (this.saving) return
      const quit = () => {
        if (this._alertEnabled && uni.disableAlertBeforeUnload) {
          uni.disableAlertBeforeUnload()
          this._alertEnabled = false
        }
        uni.navigateBack()
      }
      if (!this.hasUnsaved) {
        quit()
        return
      }
      uni.showModal({
        title: '放弃编辑？',
        content: '当前修改尚未保存，确定要离开吗？',
        confirmText: '放弃',
        confirmColor: '#ff3b30',
        success: (res) => {
          if (res.confirm) quit()
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.page {
  /* 底部预留漂浮操作栏高度 + 安全区域，避免内容被遮挡 */
  padding: 24rpx 24rpx calc(160rpx + env(safe-area-inset-bottom));
}

.card {
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.card-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #222;
  margin-bottom: 24rpx;
}

.card-sub {
  font-size: 22rpx;
  font-weight: 400;
  color: #999;
  margin-left: 8rpx;
}

/* ---------- Logo ---------- */
.logo-wrap {
  display: flex;
  align-items: center;
}

/* 与图片网格 .image-item 保持一致的定位容器 */
.logo-item {
  position: relative;
  width: 160rpx;
  height: 160rpx;
}

.logo-box {
  width: 100%;
  height: 100%;
  border-radius: 20rpx;
  border: 2rpx dashed #d0d0d0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: #fafafa;
}

/* 右上角删除按钮，样式与 .image-del 一致 */
.logo-del {
  position: absolute;
  top: 0;
  right: 0;
  width: 44rpx;
  height: 44rpx;
  line-height: 40rpx;
  text-align: center;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 34rpx;
  border-bottom-left-radius: 16rpx;
}

.logo-img {
  width: 100%;
  height: 100%;
}

.logo-placeholder {
  font-size: 60rpx;
  color: #c0c0c0;
}

/* ---------- 图片网格 ---------- */
.image-grid {
  display: flex;
  flex-wrap: wrap;
}

.image-item {
  position: relative;
  width: 200rpx;
  height: 200rpx;
  margin: 0 16rpx 16rpx 0;
  border-radius: 16rpx;
  overflow: hidden;
}

.image-thumb {
  width: 100%;
  height: 100%;
}

.image-del {
  position: absolute;
  top: 0;
  right: 0;
  width: 44rpx;
  height: 44rpx;
  line-height: 40rpx;
  text-align: center;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 34rpx;
  border-bottom-left-radius: 16rpx;
}

.image-add {
  width: 200rpx;
  height: 200rpx;
  margin: 0 16rpx 16rpx 0;
  border-radius: 16rpx;
  border: 2rpx dashed #d0d0d0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #fafafa;
}

.add-icon {
  font-size: 56rpx;
  color: #c0c0c0;
}

.add-text {
  font-size: 22rpx;
  color: #999;
  margin-top: 8rpx;
}

/* ---------- 表单 ---------- */
.field {
  display: flex;
  align-items: center;
  min-height: 96rpx;
  border-bottom: 1rpx solid #f5f5f5;
}

.field:last-child {
  border-bottom: none;
}

.field.column {
  flex-direction: column;
  align-items: flex-start;
  padding: 20rpx 0;
}

.label {
  width: 160rpx;
  font-size: 28rpx;
  color: #555;
}

.required {
  color: #ff3b30;
  margin-right: 4rpx;
}

.input {
  flex: 1;
  font-size: 28rpx;
  color: #222;
}

/* ---------- 地址定位 ---------- */
.loc-btn {
  flex-shrink: 0;
  padding: 8rpx 24rpx;
  font-size: 24rpx;
  color: #2f80ed;
  border: 2rpx solid #2f80ed;
  border-radius: 999rpx;
}

/* 定位中：全屏蒙版加载遮罩 */
.loc-mask {
  position: fixed;
  inset: 0;
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.35);
}

.loc-mask-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16rpx;
  padding: 36rpx 44rpx;
  border-radius: 16rpx;
  background: rgba(255, 255, 255, 0.92);
}

.loc-mask-spinner {
  width: 56rpx;
  height: 56rpx;
  border: 6rpx solid rgba(47, 128, 237, 0.2);
  border-top-color: #2f80ed;
  border-radius: 50%;
  animation: loc-spin 0.8s linear infinite;
}

.loc-mask-text {
  font-size: 26rpx;
  color: #333;
}

@keyframes loc-spin {
  to {
    transform: rotate(360deg);
  }
}

.map-preview-wrap {
  margin-top: 16rpx;
  border-radius: 16rpx;
  overflow: hidden;
}

.map-preview {
  width: 100%;
  height: 280rpx;
}

.textarea {
  width: 100%;
  min-height: 140rpx;
  font-size: 28rpx;
  color: #222;
  margin-top: 16rpx;
  line-height: 1.6;
}

/* ---------- 底部漂浮操作栏 ---------- */
.action-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 100;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 20rpx;
  padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.06);
}

.unsaved-tip {
  flex: 0 0 100%;
  width: 100%;
  font-size: 24rpx;
  color: #ff8f1f;
  line-height: 1;
  margin-bottom: 4rpx;
}

.cancel-btn,
.save-btn {
  flex: 1;
  margin: 0;
  height: 88rpx;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  font-size: 32rpx;
  border-radius: 999rpx;
}

.cancel-btn {
  background: #f2f3f5;
  color: #555;
}

.save-btn {
  background: #2f80ed;
  color: #fff;
  box-shadow: 0 6rpx 16rpx rgba(47, 128, 237, 0.3);
}

/* 有未保存改动时高亮，吸引用户点击保存 */
.save-btn-active {
  background: #ff6b35;
  box-shadow: 0 6rpx 16rpx rgba(255, 107, 53, 0.35);
}

.cancel-btn::after,
.save-btn::after {
  border: none;
}
</style>
