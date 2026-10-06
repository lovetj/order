<template>
  <view class="page">
    <view class="tip">长按桌位可修改桌号或删除</view>

    <view class="grid">
      <view
        class="table-card"
        :class="item.status ? 'enabled' : 'disabled'"
        v-for="item in list"
        :key="item.id"
        @longpress="onTableLongPress(item.id)"
      >
        <view class="table-no">{{item.tableNo}}</view>
        <view class="table-capacity">{{item.capacity}} 人桌</view>
        <view class="table-ops">
          <view class="op qr" @click.stop="showQrcode(item.id)">二维码</view>
          <view class="op status" @click.stop="toggleStatus(item.id)">
            {{item.status ? '停用' : '启用'}}
          </view>
        </view>
      </view>
    </view>

    <view v-if="list.length === 0" class="empty">暂无桌位，点击右下角新增</view>

    <view class="fab" @click="openAdd">＋</view>

    <!-- 桌位弹窗（新增/编辑共用）：桌号 + 人数 -->
    <view v-if="addVisible" class="mask" @click="closeAdd">
      <view class="dialog" @click.stop="noop">
        <view class="dialog-head">
          <text class="dialog-title">{{ addId ? '编辑桌位' : '新增桌位' }}</text>
          <text class="dialog-close" @click="closeAdd">×</text>
        </view>
        <view class="dialog-body">
          <view class="form-item">
            <text class="form-label">桌号</text>
            <input
              class="form-input"
              :value="addNo"
              placeholder="如 A01"
              maxlength="10"
              @input="onNoInput"
            />
          </view>
          <view class="form-item">
            <text class="form-label">人数</text>
            <input
              class="form-input"
              type="number"
              :value="String(addCapacity)"
              placeholder="可容纳人数"
              @input="onCapacityInput"
            />
          </view>
        </view>
        <view class="dialog-foot">
          <view class="dialog-btn cancel" @click="closeAdd">取消</view>
          <view class="dialog-btn ok" @click="confirmAdd">确定</view>
        </view>
      </view>
    </view>

    <!-- 点餐链接弹窗：展示链接 + 一键复制 -->
    <view v-if="qrcodeVisible" class="mask" @click="closeQrcode">
      <view class="dialog" @click.stop="noop">
        <view class="dialog-head">
          <text class="dialog-title">桌位点餐链接</text>
          <text class="dialog-close" @click="closeQrcode">×</text>
        </view>
        <view class="dialog-body">
          <view class="link-box" @longpress="copyLink">
            <text class="link-text">{{ qrcodeLink }}</text>
          </view>
          <view class="link-tip">长按复制链接，将链接生成二维码后贴在桌面即可点餐</view>
        </view>
        <view class="dialog-foot">
          <view class="dialog-btn cancel" @click="closeQrcode">关闭</view>
          <view class="dialog-btn ok" @click="copyLink">复制链接</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
const app = getApp()
import api from '@/api/index'

export default {
  data() {
    return {
      shopId: '',
      list: [],
      loading: false,
      // 新增桌位弹窗
      addVisible: false,
      addId: null,
      addNo: '',
      addCapacity: 4,
      // 点餐链接弹窗
      qrcodeVisible: false,
      qrcodeLink: ''
    }
  },
  onShow() {
    if (app.globalData.role !== 'merchant') {
      uni.reLaunch({ url: '/pages/role/role' })
      return
    }
    // 门店ID：从店家登录信息中获取（由后端 token 决定，不硬编码）
    const admin = app.globalData.admin || {}
    const shopId = admin.shopId || app.globalData.shopId || ''
    if (!shopId) {
      uni.showToast({ title: '未获取到店铺信息，请重新登录', icon: 'none' })
      return
    }
    this.shopId = shopId
    this.loadList()
  },
  methods: {
    loadList() {
      this.loading = true
      api.getTables(this.shopId).then((list) => {
        this.list = list || []
      }).catch(() => {
        this.list = []
      }).then(() => {
        this.loading = false
      })
    },

    // 新增桌位弹窗：录入桌号与人数的辅助状态
    noop() {},
    // 打开新增弹窗
    openAdd() {
      this.addId = null
      this.addNo = ''
      this.addCapacity = 4
      this.addVisible = true
    },
    closeAdd() {
      this.addVisible = false
    },
    onNoInput(e) {
      this.addNo = (e.detail.value || '').trim().toUpperCase()
    },
    onCapacityInput(e) {
      this.addCapacity = Number(e.detail.value)
    },
    // 确认新增/编辑
    confirmAdd() {
      const tableNo = (this.addNo || '').trim().toUpperCase()
      if (!tableNo) {
        uni.showToast({ title: '请输入桌号', icon: 'none' })
        return
      }
      const capacity = this.addCapacity >= 1 ? this.addCapacity : 1
      const payload = { tableNo, capacity }
      const done = () => {
        this.addVisible = false
        uni.showToast({ title: this.addId ? '已修改' : '新增成功', icon: 'none' })
        this.loadList()
      }
      if (this.addId) {
        // 编辑：店铺ID由后端从登录态校验归属
        api.updateTable({ id: this.addId, ...payload }).then(done).catch(() => {})
      } else {
        // 新增：店铺ID由后端从登录态注入，前端不再传，避免越权
        api.addTable({ ...payload, status: 1 }).then(done).catch(() => {})
      }
    },

    // 启用/停用
    toggleStatus(id) {
      api.toggleTable(id).then(() => {
        this.loadList()
        uni.showToast({ title: '已更新状态', icon: 'none' })
      }).catch(() => {})
    },

    // 查看桌位点餐链接：展示指向顾客点餐页的完整链接，支持复制
    showQrcode(id) {
      const table = this.list.find((t) => t.id === id)
      if (!table) return
      const path = `#/pages/menu/menu?shopId=${this.shopId}&tableId=${table.id}`
      let link = path
      // H5 端拼接运行时域名得到完整链接；非 H5 端无 Web 域名，仅展示应用内路径
      // #ifdef H5
      const host = (typeof location !== 'undefined' && location.origin) ? location.origin : ''
      link = host + path
      // #endif
      this.qrcodeLink = link
      this.qrcodeVisible = true
    },
    closeQrcode() {
      this.qrcodeVisible = false
    },
    // 复制链接到剪贴板
    copyLink() {
      if (!this.qrcodeLink) return
      uni.setClipboardData({
        data: this.qrcodeLink,
        success: () => {
          uni.showToast({ title: '已复制', icon: 'none' })
        }
      })
    },

    // 修改桌号/人数
    editTable(id) {
      const table = this.list.find((t) => t.id === id)
      if (!table) return
      this.addId = id
      this.addNo = table.tableNo || ''
      this.addCapacity = table.capacity || 4
      this.addVisible = true
    },

    deleteTable(id) {
      uni.showModal({
        title: '删除确认',
        content: '确定删除该桌位吗？',
        confirmText: '删除',
        confirmColor: '#ff3b30',
        success: (res) => {
          if (!res.confirm) return
          api.deleteTable(id).then(() => {
            uni.showToast({ title: '已删除', icon: 'none' })
            this.loadList()
          }).catch(() => {})
        }
      })
    },

    // 长按操作菜单
    onTableLongPress(id) {
      uni.showActionSheet({
        itemList: ['修改桌号', '删除桌位'],
        success: (res) => {
          if (res.tapIndex === 0) {
            this.editTable(id)
          } else if (res.tapIndex === 1) {
            this.deleteTable(id)
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx;
  padding-bottom: 160rpx;
}

.tip {
  font-size: 24rpx;
  color: #8a8a8a;
  margin-bottom: 20rpx;
}

.grid {
  display: flex;
  flex-wrap: wrap;
  gap: 24rpx;
}

.table-card {
  width: calc(50% - 12rpx);
  background: #fff;
  border-radius: 20rpx;
  padding: 36rpx 0 0;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.05);
  overflow: hidden;
  border-top: 8rpx solid #2f80ed;
}

.table-card.disabled {
  border-top-color: #c8c8c8;
  opacity: 0.7;
}

.table-no {
  font-size: 48rpx;
  font-weight: 700;
  text-align: center;
  color: #1f1f1f;
}

.table-capacity {
  font-size: 24rpx;
  color: #8a8a8a;
  text-align: center;
  margin-top: 10rpx;
}

.table-ops {
  display: flex;
  margin-top: 28rpx;
  border-top: 1rpx solid #f5f5f5;
}

.op {
  flex: 1;
  text-align: center;
  padding: 22rpx 0;
  font-size: 26rpx;
}

.op.qr {
  color: #2f80ed;
  border-right: 1rpx solid #f5f5f5;
}

.op.status {
  color: #ff6b35;
}

.fab {
  position: fixed;
  right: 40rpx;
  bottom: 60rpx;
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #4d95f5, #2f80ed);
  color: #fff;
  font-size: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10rpx 26rpx rgba(47, 128, 237, 0.4);
  z-index: 100;
}

/* ---------- 新增桌位弹窗 ---------- */
.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 500;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dialog {
  width: 600rpx;
  background: #fff;
  border-radius: 20rpx;
  overflow: hidden;
}

.dialog-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx 30rpx 10rpx;
}

.dialog-title {
  font-size: 32rpx;
  font-weight: 600;
}

.dialog-close {
  font-size: 40rpx;
  color: #999;
  line-height: 1;
}

.dialog-body {
  padding: 10rpx 30rpx;
}

.form-item {
  display: flex;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.form-label {
  width: 120rpx;
  font-size: 28rpx;
  color: #333;
}

.form-input {
  flex: 1;
  font-size: 28rpx;
}

.dialog-foot {
  display: flex;
  padding: 24rpx 30rpx 30rpx;
  gap: 20rpx;
}

.dialog-btn {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  border-radius: 12rpx;
  font-size: 30rpx;
}

.dialog-btn.cancel {
  background: #f2f3f5;
  color: #666;
}

.dialog-btn.ok {
  background: #2f80ed;
  color: #fff;
}

.link-box {
  background: #f5f6f8;
  border-radius: 12rpx;
  padding: 20rpx 24rpx;
  margin: 6rpx 0 16rpx;
  border: 1rpx solid #e5e6eb;
}

.link-text {
  font-size: 26rpx;
  color: #2f80ed;
  word-break: break-all;
  line-height: 1.5;
}

.link-tip {
  font-size: 24rpx;
  color: #999;
}
</style>
