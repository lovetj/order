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

    <view class="fab" @click="addTable">＋</view>
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
      loading: false
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

    // 新增桌位
    addTable() {
      uni.showModal({
        title: '新增桌位',
        editable: true,
        placeholderText: '请输入桌号，如 A01',
        confirmColor: '#2f80ed',
        success: (res) => {
          if (!res.confirm) return
          const tableNo = (res.content || '').trim().toUpperCase()
          if (!tableNo) {
            uni.showToast({ title: '桌号不能为空', icon: 'none' })
            return
          }
          // 店铺ID由后端从登录态注入，前端不再传，避免越权
          api.addTable({
            tableNo,
            capacity: 4,
            status: 1
          }).then(() => {
            uni.showToast({ title: '新增成功', icon: 'success' })
            this.loadList()
          }).catch(() => {})
        }
      })
    },

    // 启用/停用
    toggleStatus(id) {
      api.toggleTable(id).then(() => {
        this.loadList()
        uni.showToast({ title: '已更新状态', icon: 'none' })
      }).catch(() => {})
    },

    // 生成/查看桌位二维码（提示扫码路径）
    showQrcode(id) {
      const table = this.list.find((t) => t.id === id)
      if (!table) return
      // 二维码必须携带 shopId，顾客扫码后才能识别所属店铺
      const path = `pages/role/role?shopId=${this.shopId}&tableNo=${table.tableNo}`
      uni.showModal({
        title: `桌位 ${table.tableNo} 二维码`,
        content: `顾客扫码后进入的地址：\n${path}\n\n将上面的地址生成二维码（如草料二维码等在线工具），打印后贴在餐桌上。`,
        showCancel: false,
        confirmText: '知道了',
        confirmColor: '#2f80ed'
      })
    },

    // 修改桌号
    editTable(id) {
      const table = this.list.find((t) => t.id === id)
      if (!table) return
      uni.showModal({
        title: '修改桌号',
        editable: true,
        placeholderText: `当前 ${table.tableNo}`,
        confirmColor: '#2f80ed',
        success: (res) => {
          if (!res.confirm) return
          const tableNo = (res.content || '').trim().toUpperCase()
          if (!tableNo) {
            uni.showToast({ title: '桌号不能为空', icon: 'none' })
            return
          }
          api.updateTable({ id, tableNo }).then(() => {
            uni.showToast({ title: '已修改', icon: 'none' })
            this.loadList()
          }).catch(() => {})
        }
      })
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
</style>
