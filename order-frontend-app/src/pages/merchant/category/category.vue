<template>
  <view class="page">
    <view class="tip">共 {{list.length}} 个分类，点击名称可修改</view>

    <view class="cat-card" v-for="(item, index) in list" :key="item.id">
      <view class="cat-sort">{{index + 1}}</view>
      <view class="cat-info" @click="editCategory(item.id)">
        <view class="cat-name">{{item.name}}</view>
        <view class="cat-code text-sub">编码：{{item.code}}</view>
      </view>
      <view class="cat-actions">
        <view class="cat-status" :class="item.status === 1 ? 'on' : 'off'" @click.stop="toggleStatus(item.id)">
          {{item.status === 1 ? '启用' : '禁用'}}
        </view>
        <view
          class="cat-op up"
          :class="{ disabled: index === 0 }"
          @click.stop="moveUp(item.id)"
        >↑</view>
        <view
          class="cat-op down"
          :class="{ disabled: index === list.length - 1 }"
          @click.stop="moveDown(item.id)"
        >↓</view>
        <view class="cat-op del" @click.stop="deleteCategory(item.id)">删除</view>
      </view>
    </view>

    <view v-if="list.length === 0" class="empty">暂无分类，点击右下角新增</view>

    <view class="fab" @click="openAdd">＋</view>

    <!-- 自定义输入弹窗：新增 / 编辑分类（避免 showModal editable 残留上次内容） -->
    <view class="dialog-mask" v-if="dialogVisible" @touchmove.stop.prevent @click="closeDialog">
      <view class="dialog" @click.stop="noop">
        <view class="dialog-title">{{dialogMode === 'edit' ? '修改分类名称' : '新增分类'}}</view>
        <input
          class="dialog-input"
          :value="dialogValue"
          :placeholder="dialogMode === 'edit' ? '请输入新的分类名称' : '请输入分类名称，如：凉菜'"
          :focus="dialogFocus"
          maxlength="20"
          @input="onDialogInput"
          @confirm="confirmDialog"
        />
        <view class="dialog-actions">
          <view class="dialog-btn cancel" @click="closeDialog">取消</view>
          <view class="dialog-btn confirm" @click="confirmDialog">确定</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
const app = getApp()
import api from '@/api/index'

// 常用图标
const ICONS = ['🍽️', '🔥', '🍚', '🍜', '🍲', '🥤', '🍟', '🥗', '🍤', '🍱', '🍢', '🍰']

export default {
  data() {
    return {
      list: [],
      icons: ICONS,
      loading: false,
      // 自定义输入弹窗状态
      dialogVisible: false,
      dialogMode: 'add', // add | edit
      dialogValue: '',
      dialogFocus: false,
      // 编辑时的目标分类ID
      dialogEditId: ''
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
    this.loadList()
  },
  methods: {
    loadList() {
      this.loading = true
      api.getAllCategories().then((list) => {
        this.list = list || []
      }).catch(() => {
        this.list = []
      }).then(() => {
        this.loading = false
      })
    },

    // ==================== 自定义输入弹窗（新增 / 编辑） ====================

    // 空操作：用于阻止弹窗内部点击冒泡到遮罩
    noop() {},

    // 打开新增弹窗：每次打开都清空输入框，避免残留上次内容
    openAdd() {
      this.dialogVisible = true
      this.dialogMode = 'add'
      this.dialogValue = ''
      this.dialogEditId = ''
      this.dialogFocus = true
    },

    // 打开编辑弹窗：预填当前名称
    editCategory(id) {
      const target = this.list.find((c) => c.id === id)
      if (!target) return
      this.dialogVisible = true
      this.dialogMode = 'edit'
      this.dialogValue = target.name || ''
      this.dialogEditId = id
      this.dialogFocus = true
    },

    onDialogInput(e) {
      this.dialogValue = e.detail.value
    },

    closeDialog() {
      this.dialogVisible = false
      this.dialogFocus = false
    },

    // 确定：按模式执行新增 / 编辑
    confirmDialog() {
      const name = (this.dialogValue || '').trim()
      if (!name) {
        uni.showToast({ title: '分类名称不能为空', icon: 'none' })
        return
      }
      if (this.dialogMode === 'edit') {
        this.updateCategoryName(this.dialogEditId, name)
      } else {
        this.addCategory(name)
      }
    },

    addCategory(name) {
      api.addCategory({
        name,
        code: 'c_' + Date.now(),
        icon: '🍽️',
        sort: this.list.length + 1,
        status: 1
      }).then(() => {
        this.closeDialog()
        uni.showToast({ title: '新增成功', icon: 'success' })
        this.loadList()
      }).catch(() => {})
    },

    updateCategoryName(id, name) {
      api.updateCategory({ id, name }).then(() => {
        this.closeDialog()
        uni.showToast({ title: '已修改', icon: 'none' })
        this.loadList()
      }).catch(() => {})
    },

    // 切换启用/禁用
    toggleStatus(id) {
      const target = this.list.find((c) => c.id === id)
      if (!target) return
      const nextStatus = target.status === 1 ? 0 : 1
      api.updateCategory({ id, status: nextStatus }).then(() => {
        this.loadList()
        uni.showToast({ title: nextStatus === 1 ? '已启用' : '已禁用', icon: 'none' })
      }).catch(() => {})
    },

    // 删除分类
    deleteCategory(id) {
      const target = this.list.find((c) => c.id === id)
      if (!target) return
      uni.showModal({
        title: '删除确认',
        content: `确定删除分类「${target.name}」吗？\n（分类下若仍有菜品将无法删除）`,
        confirmText: '删除',
        confirmColor: '#ff3b30',
        success: (res) => {
          if (!res.confirm) return
          api.deleteCategory(id).then(() => {
            uni.showToast({ title: '已删除', icon: 'none' })
            this.loadList()
          }).catch(() => {})
        }
      })
    },

    // 排序调整（上移）
    moveUp(id) {
      const list = [...this.list]
      const index = list.findIndex((c) => c.id === id)
      if (index <= 0) return
      this.swapSort(list, index, index - 1)
    },

    // 排序调整（下移）
    moveDown(id) {
      const list = [...this.list]
      const index = list.findIndex((c) => c.id === id)
      if (index < 0 || index >= list.length - 1) return
      this.swapSort(list, index, index + 1)
    },

    // 交换两个分类的 sort 值（升序排列，数值小的在前）
    swapSort(list, i, j) {
      const a = list[i]
      const b = list[j]
      // 若两者 sort 相同（历史数据），按索引兜底生成可交换的值，避免顺序不变
      let aSort = a.sort
      let bSort = b.sort
      if (aSort === bSort) {
        aSort = i
        bSort = j
      }
      Promise.all([
        api.updateCategory({ id: a.id, sort: bSort }),
        api.updateCategory({ id: b.id, sort: aSort })
      ]).then(() => this.loadList()).catch(() => {})
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

.cat-card {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 4rpx 14rpx rgba(0, 0, 0, 0.04);
}

.cat-sort {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  background: #eaf3ff;
  color: #2f80ed;
  font-size: 28rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 24rpx;
}

.cat-info {
  flex: 1;
  min-width: 0;
}

.cat-name {
  font-size: 30rpx;
  font-weight: 600;
}

.cat-code {
  margin-top: 8rpx;
  font-size: 22rpx;
}

.cat-actions {
  display: flex;
  align-items: center;
}

.cat-status {
  padding: 8rpx 22rpx;
  border-radius: 999rpx;
  font-size: 23rpx;
  margin-right: 16rpx;
}

.cat-status.on {
  background: #e8f6ec;
  color: #34c759;
}

.cat-status.off {
  background: #f2f3f5;
  color: #999;
}

.cat-op {
  padding: 8rpx 14rpx;
  font-size: 24rpx;
  color: #2f80ed;
}

/* 到顶/到底时置灰，视觉上表示不可再移动 */
.cat-op.disabled {
  color: #c8c8c8;
}

.cat-op.del {
  color: #ff3b30;
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

.empty {
  text-align: center;
  color: #8a8a8a;
  font-size: 26rpx;
  padding: 120rpx 0;
}

/* ---------- 自定义输入弹窗 ---------- */
.dialog-mask {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: mask-fade-in 0.2s ease;
}

.dialog {
  width: 600rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 40rpx 40rpx 24rpx;
  box-sizing: border-box;
}

.dialog-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #222;
  text-align: center;
  margin-bottom: 30rpx;
}

.dialog-input {
  width: 100%;
  height: 88rpx;
  padding: 0 24rpx;
  box-sizing: border-box;
  background: #f5f6f8;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: #222;
}

.dialog-actions {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin-top: 36rpx;
}

.dialog-btn {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  border-radius: 999rpx;
  font-size: 30rpx;
}

.dialog-btn.cancel {
  background: #f2f3f5;
  color: #555;
}

.dialog-btn.confirm {
  background: #2f80ed;
  color: #fff;
}

@keyframes mask-fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
</style>
