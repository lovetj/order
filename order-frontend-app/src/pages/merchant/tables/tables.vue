<template>
  <view class="page">
    <!-- 顶部概览数据卡片 -->
    <view class="overview-card">
      <view class="overview-item">
        <text class="overview-num">{{ list.length }}</text>
        <text class="overview-label">总桌位</text>
      </view>
      <view class="overview-divider"></view>
      <view class="overview-item">
        <text class="overview-num highlight">{{ hallCount }}</text>
        <text class="overview-label">大厅</text>
      </view>
      <view class="overview-divider"></view>
      <view class="overview-item">
        <text class="overview-num room">{{ roomCount }}</text>
        <text class="overview-label">包房</text>
      </view>
      <view class="overview-divider"></view>
      <view class="overview-item">
        <text class="overview-num active">{{ activeCount }}</text>
        <text class="overview-label">启用中</text>
      </view>
    </view>

    <!-- 筛选过滤栏 -->
    <view class="filter-box">
      <!-- 类型 Tab 切换 -->
      <view class="type-tabs">
        <view
          class="type-tab"
          :class="{ active: currentTypeTab === 'ALL' }"
          @click="setTypeTab('ALL')"
        >全部 ({{ list.length }})</view>
        <view
          class="type-tab"
          :class="{ active: currentTypeTab === '大厅' }"
          @click="setTypeTab('大厅')"
        >🏠 大厅 ({{ hallCount }})</view>
        <view
          class="type-tab"
          :class="{ active: currentTypeTab === '包房' }"
          @click="setTypeTab('包房')"
        >🚪 包房 ({{ roomCount }})</view>
      </view>

      <!-- 楼号选择与搜索 -->
      <view class="filter-sub-row">
        <scroll-view class="floor-scroll" scroll-x :show-scrollbar="false">
          <view class="floor-list">
            <view
              class="floor-chip"
              :class="{ active: selectedFloor === '' }"
              @click="setFloor('')"
            >全部楼层</view>
            <view
              class="floor-chip"
              :class="{ active: selectedFloor === f }"
              v-for="f in floorOptions"
              :key="f"
              @click="setFloor(f)"
            >{{ f }}</view>
          </view>
        </scroll-view>
      </view>

      <!-- 搜索框 -->
      <view class="search-bar">
        <text class="search-icon">🔍</text>
        <input
          class="search-input"
          v-model="keyword"
          placeholder="搜索桌号或别名 (如 A01 / 牡丹阁)"
          confirm-type="search"
        />
        <text v-if="keyword" class="search-clear" @click="keyword = ''">×</text>
      </view>
    </view>

    <!-- 桌位卡片网格 -->
    <view class="grid" v-if="filteredList.length > 0">
      <view
        class="table-card"
        :class="[item.status ? 'enabled' : 'disabled', item.type === '包房' ? 'is-room' : 'is-hall']"
        v-for="item in filteredList"
        :key="item.id"
        @longpress="onTableLongPress(item.id)"
      >
        <!-- 头部信息：楼号 + 类型 + 状态 -->
        <view class="card-head">
          <view class="tags-left">
            <text class="tag-floor">{{ item.buildingNo || '1楼' }}</text>
            <text class="tag-type" :class="item.type === '包房' ? 'room' : 'hall'">
              {{ item.type || '大厅' }}
            </text>
          </view>
          <view class="status-badge" :class="item.status ? 'online' : 'offline'">
            {{ item.status ? '启用中' : '已停用' }}
          </view>
        </view>

        <!-- 核心桌位信息 -->
        <view class="card-body" @click="editTable(item.id)">
          <view class="table-no">{{ item.tableNo }}</view>
          <view class="table-alias" v-if="item.alias">
            <text class="alias-icon">🏷️</text>
            <text class="alias-text">{{ item.alias }}</text>
          </view>
          <view class="table-alias empty-alias" v-else>
            <text class="alias-placeholder">暂无别名</text>
          </view>
          <view class="table-capacity">
            <text class="capacity-icon">👥</text>
            <text class="capacity-text">{{ item.capacity }} 人位</text>
          </view>
        </view>

        <!-- 底部快捷操作栏 -->
        <view class="table-ops">
          <view class="op qr" @click.stop="showQrcode(item.id)">
            <text class="op-icon">🔗</text>
            <text class="op-text">链接</text>
          </view>
          <view class="op edit" @click.stop="editTable(item.id)">
            <text class="op-icon">✏️</text>
            <text class="op-text">编辑</text>
          </view>
          <view class="op status" :class="{ 'to-enable': !item.status }" @click.stop="toggleStatus(item.id)">
            <text class="op-icon">{{ item.status ? '⏸️' : '▶️' }}</text>
            <text class="op-text">{{ item.status ? '停用' : '启用' }}</text>
          </view>
          <view class="op del" @click.stop="deleteTable(item.id)">
            <text class="op-icon">🗑️</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view v-else class="empty-state">
      <view class="empty-icon">🪑</view>
      <view class="empty-title">{{ list.length === 0 ? '暂无桌位' : '未找到匹配的桌位' }}</view>
      <view class="empty-desc">
        {{ list.length === 0 ? '点击右下角按钮添加新桌位' : '请尝试调整筛选条件或搜索关键词' }}
      </view>
      <view v-if="list.length > 0" class="empty-reset-btn" @click="resetFilters">重置筛选</view>
    </view>

    <!-- 底部悬浮新增按钮 -->
    <view class="fab" @click="openAdd">
      <text class="fab-icon">＋</text>
      <text class="fab-text">新增桌位</text>
    </view>

    <!-- 桌位弹窗（新增/编辑）：楼号 + 类型 + 别名 + 桌号 + 人数 -->
    <view v-if="dialogVisible" class="mask" @click="closeDialog">
      <view class="dialog" @click.stop="noop">
        <view class="dialog-head">
          <view class="dialog-title-wrap">
            <text class="dialog-title-icon">{{ form.id ? '✏️' : '✨' }}</text>
            <text class="dialog-title">{{ form.id ? '编辑桌位' : '新增桌位' }}</text>
          </view>
          <text class="dialog-close" @click="closeDialog">×</text>
        </view>

        <scroll-view class="dialog-body" scroll-y>
          <!-- 1. 类型设置 -->
          <view class="form-group">
            <view class="form-label-row">
              <text class="form-label">桌位类型</text>
              <text class="form-required">*</text>
            </view>
            <view class="type-selector">
              <view
                class="type-option"
                :class="{ selected: form.type === '大厅' }"
                @click="form.type = '大厅'"
              >
                <text class="option-icon">🏠</text>
                <text class="option-title">大厅</text>
                <text class="option-desc">开放用餐区域</text>
              </view>
              <view
                class="type-option room"
                :class="{ selected: form.type === '包房' }"
                @click="form.type = '包房'"
              >
                <text class="option-icon">🚪</text>
                <text class="option-title">包房</text>
                <text class="option-desc">独立包厢雅间</text>
              </view>
            </view>
          </view>

          <!-- 2. 楼号设置 -->
          <view class="form-group">
            <view class="form-label-row">
              <text class="form-label">所属楼号/楼层</text>
              <text class="form-required">*</text>
            </view>
            <input
              class="form-input"
              v-model="form.buildingNo"
              placeholder="如 1楼、2楼、A栋1楼"
              maxlength="20"
            />
            <view class="quick-chips">
              <view
                class="quick-chip"
                :class="{ active: form.buildingNo === chip }"
                v-for="chip in quickBuildingOptions"
                :key="chip"
                @click="form.buildingNo = chip"
              >{{ chip }}</view>
            </view>
          </view>

          <!-- 3. 桌号设置 -->
          <view class="form-group">
            <view class="form-label-row">
              <text class="form-label">桌号</text>
              <text class="form-required">*</text>
            </view>
            <input
              class="form-input"
              :value="form.tableNo"
              placeholder="如 A01、B08、888"
              maxlength="15"
              @input="onTableNoInput"
            />
          </view>

          <!-- 4. 别名设置 -->
          <view class="form-group">
            <view class="form-label-row">
              <text class="form-label">桌位别名</text>
              <text class="form-tip">(选填，如包房名称/靠窗位)</text>
            </view>
            <input
              class="form-input"
              v-model="form.alias"
              placeholder="如 牡丹阁、VIP1、靠窗雅座"
              maxlength="30"
            />
            <view class="quick-chips" v-if="form.type === '包房'">
              <view
                class="quick-chip"
                v-for="aliasChip in ['牡丹阁', '百合厅', '聚贤阁', '富贵厅', 'VIP包间']"
                :key="aliasChip"
                @click="form.alias = aliasChip"
              >{{ aliasChip }}</view>
            </view>
          </view>

          <!-- 5. 容纳人数 -->
          <view class="form-group">
            <view class="form-label-row">
              <text class="form-label">容纳人数</text>
              <text class="form-required">*</text>
            </view>
            <view class="stepper-row">
              <view class="stepper-btn" @click="changeCapacity(-1)">-</view>
              <input
                class="stepper-input"
                type="number"
                :value="String(form.capacity)"
                @input="onCapacityInput"
              />
              <view class="stepper-btn" @click="changeCapacity(1)">+</view>
              <text class="stepper-unit">人位</text>
            </view>
            <view class="quick-chips">
              <view
                class="quick-chip"
                :class="{ active: form.capacity === num }"
                v-for="num in [2, 4, 6, 8, 10, 12]"
                :key="num"
                @click="form.capacity = num"
              >{{ num }}人</view>
            </view>
          </view>
        </scroll-view>

        <view class="dialog-foot">
          <view class="dialog-btn cancel" @click="closeDialog">取消</view>
          <view class="dialog-btn ok" @click="confirmSave">确定保存</view>
        </view>
      </view>
    </view>

    <!-- 点餐链接弹窗 -->
    <view v-if="qrcodeVisible" class="mask" @click="closeQrcode">
      <view class="dialog" @click.stop="noop">
        <view class="dialog-head">
          <view class="dialog-title-wrap">
            <text class="dialog-title-icon">🔗</text>
            <text class="dialog-title">桌位点餐链接</text>
          </view>
          <text class="dialog-close" @click="closeQrcode">×</text>
        </view>
        <view class="dialog-body">
          <view class="link-table-info" v-if="activeTable">
            <text class="info-badge floor">{{ activeTable.buildingNo || '1楼' }}</text>
            <text class="info-badge type" :class="activeTable.type === '包房' ? 'room' : 'hall'">{{ activeTable.type || '大厅' }}</text>
            <text class="info-table-no">{{ activeTable.tableNo }}</text>
            <text class="info-alias" v-if="activeTable.alias">({{ activeTable.alias }})</text>
          </view>
          <view class="link-box" @longpress="copyLink">
            <text class="link-text">{{ qrcodeLink }}</text>
          </view>
          <view class="link-tip">
            💡 点击下方按钮复制链接，使用二维码生成工具生成桌贴二维码即可贴在餐桌上供顾客扫码点餐。
          </view>
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

      // 筛选条件
      currentTypeTab: 'ALL', // 'ALL' | '大厅' | '包房'
      selectedFloor: '', // '' 表示全部
      keyword: '',

      // 快捷楼号预设
      quickBuildingOptions: ['1楼', '2楼', '3楼', '4楼', 'B1负一层', '户外露台'],

      // 新增/编辑弹窗表单
      dialogVisible: false,
      form: {
        id: null,
        buildingNo: '1楼',
        type: '大厅',
        alias: '',
        tableNo: '',
        capacity: 4
      },

      // 点餐链接弹窗
      qrcodeVisible: false,
      qrcodeLink: '',
      activeTable: null
    }
  },
  computed: {
    // 大厅桌位数
    hallCount() {
      return this.list.filter((t) => (t.type || '大厅') === '大厅').length
    },
    // 包房桌位数
    roomCount() {
      return this.list.filter((t) => t.type === '包房').length
    },
    // 启用中数量
    activeCount() {
      return this.list.filter((t) => t.status === 1 || t.status === true).length
    },
    // 所有已存在的楼层列表
    floorOptions() {
      const set = new Set()
      this.list.forEach((t) => {
        if (t.buildingNo && t.buildingNo.trim()) {
          set.add(t.buildingNo.trim())
        }
      })
      if (set.size === 0) {
        return ['1楼']
      }
      return Array.from(set)
    },
    // 经过筛选后的列表
    filteredList() {
      let res = this.list
      // 类型筛选
      if (this.currentTypeTab !== 'ALL') {
        res = res.filter((t) => (t.type || '大厅') === this.currentTypeTab)
      }
      // 楼号筛选
      if (this.selectedFloor) {
        res = res.filter((t) => (t.buildingNo || '1楼') === this.selectedFloor)
      }
      // 关键字搜索 (桌号或别名)
      if (this.keyword && this.keyword.trim()) {
        const kw = this.keyword.trim().toUpperCase()
        res = res.filter((t) => {
          const noMatch = (t.tableNo || '').toUpperCase().includes(kw)
          const aliasMatch = (t.alias || '').toUpperCase().includes(kw)
          const floorMatch = (t.buildingNo || '').toUpperCase().includes(kw)
          return noMatch || aliasMatch || floorMatch
        })
      }
      return res
    }
  },
  onShow() {
    if (app.globalData.role !== 'merchant') {
      uni.reLaunch({ url: '/pages/role/role' })
      return
    }
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
        this.list = (list || []).map((t) => ({
          ...t,
          buildingNo: t.buildingNo || '1楼',
          type: t.type || '大厅',
          alias: t.alias || '',
          capacity: t.capacity || 4,
          status: t.status != null ? t.status : 1
        }))
      }).catch(() => {
        this.list = []
      }).then(() => {
        this.loading = false
      })
    },

    noop() {},

    // 筛选操作
    setTypeTab(tab) {
      this.currentTypeTab = tab
    },
    setFloor(floor) {
      this.selectedFloor = floor
    },
    resetFilters() {
      this.currentTypeTab = 'ALL'
      this.selectedFloor = ''
      this.keyword = ''
    },

    // 打开新增弹窗
    openAdd() {
      this.form = {
        id: null,
        buildingNo: this.selectedFloor || '1楼',
        type: this.currentTypeTab !== 'ALL' ? this.currentTypeTab : '大厅',
        alias: '',
        tableNo: '',
        capacity: 4
      }
      this.dialogVisible = true
    },

    // 打开编辑弹窗
    editTable(id) {
      const table = this.list.find((t) => t.id === id)
      if (!table) return
      this.form = {
        id: table.id,
        buildingNo: table.buildingNo || '1楼',
        type: table.type || '大厅',
        alias: table.alias || '',
        tableNo: table.tableNo || '',
        capacity: table.capacity || 4
      }
      this.dialogVisible = true
    },

    closeDialog() {
      this.dialogVisible = false
    },

    onTableNoInput(e) {
      this.form.tableNo = (e.detail.value || '').trim().toUpperCase()
    },

    onCapacityInput(e) {
      const val = parseInt(e.detail.value, 10)
      this.form.capacity = !isNaN(val) && val >= 1 ? val : 1
    },

    changeCapacity(delta) {
      const current = Number(this.form.capacity) || 4
      const next = current + delta
      if (next >= 1 && next <= 99) {
        this.form.capacity = next
      }
    },

    // 确认保存新增/修改
    confirmSave() {
      const tableNo = (this.form.tableNo || '').trim().toUpperCase()
      if (!tableNo) {
        uni.showToast({ title: '请输入桌号', icon: 'none' })
        return
      }
      const buildingNo = (this.form.buildingNo || '').trim() || '1楼'
      const type = this.form.type || '大厅'
      const alias = (this.form.alias || '').trim()
      const capacity = Number(this.form.capacity) >= 1 ? Number(this.form.capacity) : 4

      const payload = {
        buildingNo,
        type,
        alias,
        tableNo,
        capacity
      }

      const done = () => {
        this.dialogVisible = false
        uni.showToast({ title: this.form.id ? '已修改' : '新增成功', icon: 'none' })
        this.loadList()
      }

      if (this.form.id) {
        api.updateTable({ id: this.form.id, ...payload }).then(done).catch(() => {})
      } else {
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

    // 删除桌位
    deleteTable(id) {
      const table = this.list.find((t) => t.id === id)
      const name = table ? `${table.buildingNo} ${table.tableNo}${table.alias ? ` (${table.alias})` : ''}` : '该桌位'
      uni.showModal({
        title: '删除确认',
        content: `确定删除 ${name} 吗？删除后顾客将无法扫码点餐。`,
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

    // 查看点餐链接
    showQrcode(id) {
      const table = this.list.find((t) => t.id === id)
      if (!table) return
      this.activeTable = table
      const path = `#/pages/menu/menu?shopId=${this.shopId}&tableId=${table.id}`
      let link = path
      // #ifdef H5
      const host = (typeof location !== 'undefined' && location.origin) ? location.origin : ''
      link = host + path
      // #endif
      this.qrcodeLink = link
      this.qrcodeVisible = true
    },

    closeQrcode() {
      this.qrcodeVisible = false
      this.activeTable = null
    },

    copyLink() {
      if (!this.qrcodeLink) return
      uni.setClipboardData({
        data: this.qrcodeLink,
        success: () => {
          uni.showToast({ title: '已复制链接', icon: 'none' })
        }
      })
    },

    // 长按操作菜单
    onTableLongPress(id) {
      uni.showActionSheet({
        itemList: ['编辑桌位设置', '查看点餐链接', '删除桌位'],
        success: (res) => {
          if (res.tapIndex === 0) {
            this.editTable(id)
          } else if (res.tapIndex === 1) {
            this.showQrcode(id)
          } else if (res.tapIndex === 2) {
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
  padding-bottom: 200rpx;
  min-height: 100vh;
  background: #f7f8fa;
  box-sizing: border-box;
}

/* 顶部概览数据卡片 */
.overview-card {
  display: flex;
  align-items: center;
  justify-content: space-around;
  background: #ffffff;
  border-radius: 20rpx;
  padding: 28rpx 16rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
  margin-bottom: 24rpx;
}

.overview-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.overview-num {
  font-size: 40rpx;
  font-weight: 700;
  color: #1f1f1f;
  line-height: 1.2;

  &.highlight {
    color: #2f80ed;
  }
  &.room {
    color: #e65100;
  }
  &.active {
    color: #00b578;
  }
}

.overview-label {
  font-size: 22rpx;
  color: #8c8c8c;
  margin-top: 6rpx;
}

.overview-divider {
  width: 1rpx;
  height: 48rpx;
  background: #f0f0f0;
}

/* 筛选区 */
.filter-box {
  background: #ffffff;
  border-radius: 20rpx;
  padding: 20rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
  margin-bottom: 24rpx;
}

.type-tabs {
  display: flex;
  background: #f2f3f5;
  border-radius: 14rpx;
  padding: 6rpx;
  gap: 8rpx;
}

.type-tab {
  flex: 1;
  text-align: center;
  font-size: 26rpx;
  color: #666;
  padding: 14rpx 0;
  border-radius: 10rpx;
  transition: all 0.2s ease;

  &.active {
    background: #ffffff;
    color: #2f80ed;
    font-weight: 600;
    box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.06);
  }
}

.filter-sub-row {
  margin-top: 18rpx;
}

.floor-scroll {
  width: 100%;
  white-space: nowrap;
}

.floor-list {
  display: inline-flex;
  gap: 12rpx;
  padding: 4rpx 0;
}

.floor-chip {
  display: inline-block;
  font-size: 24rpx;
  color: #666;
  background: #f5f6f8;
  padding: 8rpx 22rpx;
  border-radius: 24rpx;
  border: 1rpx solid transparent;

  &.active {
    background: #eaf2fd;
    color: #2f80ed;
    border-color: #bcd7fa;
    font-weight: 500;
  }
}

.search-bar {
  display: flex;
  align-items: center;
  background: #f5f6f8;
  border-radius: 14rpx;
  padding: 12rpx 20rpx;
  margin-top: 18rpx;
}

.search-icon {
  font-size: 26rpx;
  margin-right: 12rpx;
}

.search-input {
  flex: 1;
  font-size: 26rpx;
  color: #333;
}

.search-clear {
  font-size: 32rpx;
  color: #999;
  padding: 0 8rpx;
}

/* 桌位卡片网格 */
.grid {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
}

.table-card {
  width: calc(50% - 10rpx);
  background: #ffffff;
  border-radius: 20rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.05);
  overflow: hidden;
  border-top: 6rpx solid #2f80ed;
  display: flex;
  flex-direction: column;
  transition: transform 0.15s ease;

  &.is-room {
    border-top-color: #ff9800;
  }

  &.disabled {
    border-top-color: #c8c8c8;
    background: #fbfbfb;

    .table-no {
      color: #999;
    }
  }
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 16rpx 0;
}

.tags-left {
  display: flex;
  gap: 8rpx;
  align-items: center;
}

.tag-floor {
  font-size: 20rpx;
  color: #555;
  background: #f0f2f5;
  padding: 2rpx 10rpx;
  border-radius: 6rpx;
}

.tag-type {
  font-size: 20rpx;
  padding: 2rpx 10rpx;
  border-radius: 6rpx;

  &.hall {
    background: #e8f3ff;
    color: #2f80ed;
  }
  &.room {
    background: #fff3e0;
    color: #e65100;
  }
}

.status-badge {
  font-size: 20rpx;
  padding: 2rpx 10rpx;
  border-radius: 12rpx;

  &.online {
    background: #e6f7ec;
    color: #00b578;
  }
  &.offline {
    background: #f2f3f5;
    color: #999;
  }
}

.card-body {
  padding: 20rpx 16rpx 16rpx;
  text-align: center;
  flex: 1;
}

.table-no {
  font-size: 44rpx;
  font-weight: 700;
  color: #1f1f1f;
  letter-spacing: 1rpx;
  line-height: 1.2;
}

.table-alias {
  margin-top: 8rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4rpx;
  min-height: 36rpx;

  .alias-icon {
    font-size: 20rpx;
  }
  .alias-text {
    font-size: 24rpx;
    font-weight: 500;
    color: #ff6b00;
    max-width: 200rpx;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .alias-placeholder {
    font-size: 22rpx;
    color: #bbbbbb;
  }
}

.table-capacity {
  margin-top: 10rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6rpx;

  .capacity-icon {
    font-size: 22rpx;
  }
  .capacity-text {
    font-size: 24rpx;
    color: #777777;
  }
}

.table-ops {
  display: flex;
  align-items: center;
  border-top: 1rpx solid #f2f3f5;
  background: #fafbfc;
}

.op {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 18rpx 0;
  font-size: 22rpx;
  color: #555;
  border-right: 1rpx solid #f2f3f5;

  &:last-child {
    border-right: none;
  }

  .op-icon {
    font-size: 22rpx;
    margin-right: 4rpx;
  }

  &.qr {
    color: #2f80ed;
  }

  &.edit {
    color: #333333;
  }

  &.status {
    color: #ff7d00;

    &.to-enable {
      color: #00b578;
    }
  }

  &.del {
    flex: 0 0 60rpx;
    color: #999999;
  }
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100rpx 40rpx;
}

.empty-icon {
  font-size: 80rpx;
  margin-bottom: 20rpx;
}

.empty-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
}

.empty-desc {
  font-size: 24rpx;
  color: #999;
  margin-top: 10rpx;
  text-align: center;
}

.empty-reset-btn {
  margin-top: 24rpx;
  padding: 12rpx 32rpx;
  background: #2f80ed;
  color: #fff;
  font-size: 24rpx;
  border-radius: 30rpx;
}

/* 底部悬浮按钮 */
.fab {
  position: fixed;
  right: 32rpx;
  bottom: 50rpx;
  height: 88rpx;
  padding: 0 36rpx;
  border-radius: 44rpx;
  background: linear-gradient(135deg, #4d95f5, #2f80ed);
  color: #fff;
  font-size: 28rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10rpx 26rpx rgba(47, 128, 237, 0.4);
  z-index: 100;
  gap: 8rpx;

  .fab-icon {
    font-size: 36rpx;
    line-height: 1;
  }
}

/* ---------- 弹窗通用样式 ---------- */
.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx;
  box-sizing: border-box;
}

.dialog {
  width: 100%;
  max-width: 620rpx;
  background: #ffffff;
  border-radius: 28rpx;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  max-height: 85vh;
  box-shadow: 0 16rpx 48rpx rgba(0, 0, 0, 0.15);
}

.dialog-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx 32rpx 20rpx;
  border-bottom: 1rpx solid #f2f3f5;
}

.dialog-title-wrap {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.dialog-title-icon {
  font-size: 32rpx;
}

.dialog-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1f1f1f;
}

.dialog-close {
  font-size: 44rpx;
  color: #999;
  line-height: 1;
  padding: 0 8rpx;
}

.dialog-body {
  padding: 24rpx 32rpx;
  max-height: 60vh;
  box-sizing: border-box;
}

.form-group {
  margin-bottom: 28rpx;

  &:last-child {
    margin-bottom: 10rpx;
  }
}

.form-label-row {
  display: flex;
  align-items: center;
  margin-bottom: 12rpx;
}

.form-label {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
}

.form-required {
  font-size: 28rpx;
  color: #ff4d4f;
  margin-left: 6rpx;
}

.form-tip {
  font-size: 22rpx;
  color: #999;
  margin-left: 10rpx;
}

.form-input {
  width: 100%;
  height: 80rpx;
  background: #f7f8fa;
  border-radius: 14rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  color: #333;
  box-sizing: border-box;
  border: 1rpx solid #ebeef5;
}

/* 类型双选选择器 */
.type-selector {
  display: flex;
  gap: 16rpx;
}

.type-option {
  flex: 1;
  background: #f7f8fa;
  border: 2rpx solid transparent;
  border-radius: 16rpx;
  padding: 20rpx 16rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  transition: all 0.2s ease;

  .option-icon {
    font-size: 36rpx;
    margin-bottom: 6rpx;
  }
  .option-title {
    font-size: 28rpx;
    font-weight: 600;
    color: #333;
  }
  .option-desc {
    font-size: 20rpx;
    color: #999;
    margin-top: 4rpx;
  }

  &.selected {
    background: #eaf2fd;
    border-color: #2f80ed;

    .option-title {
      color: #2f80ed;
    }
    .option-desc {
      color: #4a90e2;
    }
  }

  &.room.selected {
    background: #fff7e6;
    border-color: #ff9800;

    .option-title {
      color: #e65100;
    }
    .option-desc {
      color: #f57c00;
    }
  }
}

/* 快捷选择标签 */
.quick-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 14rpx;
}

.quick-chip {
  font-size: 22rpx;
  color: #666;
  background: #f2f3f5;
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
  border: 1rpx solid transparent;

  &.active {
    background: #eaf2fd;
    color: #2f80ed;
    border-color: #bcd7fa;
    font-weight: 500;
  }
}

/* 步进器 */
.stepper-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.stepper-btn {
  width: 72rpx;
  height: 72rpx;
  background: #f0f2f5;
  border-radius: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  color: #333;
  font-weight: 600;
}

.stepper-input {
  flex: 1;
  height: 72rpx;
  background: #f7f8fa;
  border-radius: 14rpx;
  text-align: center;
  font-size: 32rpx;
  font-weight: 600;
  color: #1f1f1f;
  border: 1rpx solid #ebeef5;
}

.stepper-unit {
  font-size: 26rpx;
  color: #666;
}

/* 弹窗底部按钮 */
.dialog-foot {
  display: flex;
  padding: 24rpx 32rpx 32rpx;
  gap: 20rpx;
  border-top: 1rpx solid #f2f3f5;
}

.dialog-btn {
  flex: 1;
  height: 84rpx;
  line-height: 84rpx;
  text-align: center;
  border-radius: 16rpx;
  font-size: 28rpx;
  font-weight: 600;

  &.cancel {
    background: #f2f3f5;
    color: #666;
  }
  &.ok {
    background: #2f80ed;
    color: #ffffff;
  }
}

/* 点餐链接弹窗专属样式 */
.link-table-info {
  display: flex;
  align-items: center;
  gap: 10rpx;
  margin-bottom: 16rpx;
  flex-wrap: wrap;

  .info-badge {
    font-size: 22rpx;
    padding: 4rpx 12rpx;
    border-radius: 8rpx;

    &.floor {
      background: #f0f2f5;
      color: #333;
    }
    &.hall {
      background: #eaf2fd;
      color: #2f80ed;
    }
    &.room {
      background: #fff3e0;
      color: #e65100;
    }
  }

  .info-table-no {
    font-size: 30rpx;
    font-weight: 700;
    color: #1f1f1f;
  }

  .info-alias {
    font-size: 26rpx;
    color: #ff6b00;
    font-weight: 500;
  }
}

.link-box {
  background: #f5f7fa;
  border-radius: 16rpx;
  padding: 20rpx 24rpx;
  margin-bottom: 16rpx;
  border: 1rpx solid #e1e4e8;
}

.link-text {
  font-size: 24rpx;
  color: #2f80ed;
  word-break: break-all;
  line-height: 1.6;
}

.link-tip {
  font-size: 22rpx;
  color: #8c8c8c;
  line-height: 1.5;
}
</style>
