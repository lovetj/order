<template>
  <view class="page">
    <!-- 菜品信息头部卡片 -->
    <view class="dish-head">
      <view class="dish-thumb">
        <image v-if="dish.hasImage" class="dish-thumb-img" :src="dish.imageUrl" mode="aspectFill"></image>
        <view v-else class="dish-thumb-placeholder">
          <text class="dish-thumb-icon">{{dish.image || '🍽️'}}</text>
        </view>
      </view>
      <view class="dish-info">
        <view class="dish-name">{{dish.name}}</view>
        <view class="dish-desc" v-if="dish.desc">{{dish.desc}}</view>
        <view class="dish-price-row">
          <view class="dish-price">
            <text class="price-sym">¥</text>
            <text class="price-val">{{dish.price}}</text>
          </view>
          <text v-if="minBuy > 1" class="dish-minbuy">{{minBuy}}份起购</text>
        </view>
      </view>
    </view>

    <!-- 规格分组 -->
    <view class="spec-group" v-for="(group, gi) in specGroups" :key="group.groupName">
      <view class="group-head">
        <view class="group-title-box">
          <text class="group-name">{{group.groupName}}</text>
          <text class="group-type">（{{group.selectType === 2 ? '可多选' : '单选'}}）</text>
        </view>
        <view v-if="group.required === 1" class="group-required-badge">
          <text>必选</text>
        </view>
      </view>
      <view class="option-list">
        <view
          v-for="(opt, oi) in group.options"
          :key="opt.id"
          class="option"
          :class="{ active: opt.selected }"
          @click="toggleOption(gi, oi, group.selectType)"
        >
          <text class="option-name">{{opt.name}}</text>
          <text v-if="opt.extraPrice > 0" class="option-price">+¥{{opt.extraPrice}}</text>
        </view>
      </view>
    </view>

    <!-- 空规格提示 -->
    <view v-if="specGroups.length === 0" class="empty-spec-card">
      <text class="empty-spec-icon">✨</text>
      <text class="empty-spec-text">该菜品暂无可选规格，可直接加入购物车</text>
    </view>

    <view class="bottom-space"></view>

    <!-- 底部操作栏 -->
    <view class="bottom-bar">
      <!-- 数量步进器 -->
      <view class="qty-wrap">
        <text class="qty-label">选购数量</text>
        <view class="stepper">
          <view class="step-btn minus" :class="{ disabled: quantity <= minBuy }" @click="changeQty(-1)">
            <text class="step-icon">−</text>
          </view>
          <text class="step-num">{{quantity}}</text>
          <view class="step-btn plus" @click="changeQty(1)">
            <text class="step-icon">＋</text>
          </view>
        </view>
      </view>

      <!-- 合计与确认按钮 -->
      <view class="action-wrap">
        <view class="total-wrap">
          <text class="total-label">合计</text>
          <text class="total-sym">¥</text>
          <text class="total-price">{{totalPrice}}</text>
        </view>
        <view class="confirm-btn" @click="confirm">
          <text class="confirm-btn-text">加入购物车</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
// 菜品规格/口味选择
import api from '@/api/index'
import { formatImageUrl } from '@/utils/util'

export default {
  data() {
    return {
      dish: {},
      specGroups: [],
      // 已选项：{ groupName: [optionId, ...] }
      selected: {},
      extraPrice: 0,
      totalPrice: 0,
      quantity: 1,
      // 起购份数（后端 minBuy，默认 1）
      minBuy: 1
    }
  },

  onLoad(options) {
    const dishId = options && options.id ? options.id : ''
    if (!dishId) {
      uni.showToast({ title: '参数错误', icon: 'none' })
      setTimeout(() => uni.navigateBack(), 800)
      return
    }
    this.dishId = dishId
    this.loadDetail(dishId)
  },

  methods: {
    loadDetail(dishId) {
      api.getDishDetail(dishId).then((dish) => {
        if (!dish) return
        const specGroups = (dish.specs || []).map((group) => ({
          ...group,
          options: (group.options || []).map((o) => ({
            ...o,
            selected: !!o.isDefault
          }))
        }))
        const img = dish.image || ''
        const hasImage = /^https?:\/\//.test(img) || img.startsWith('/')
        // 起购份数：默认 1，至少为 1；初始数量直接取起购份数
        const minBuy = Number(dish.minBuy) > 1 ? Math.floor(Number(dish.minBuy)) : 1
        this.dish = {
          ...dish,
          hasImage,
          imageUrl: hasImage ? formatImageUrl(img) : ''
        }
        this.specGroups = specGroups
        this.minBuy = minBuy
        this.quantity = minBuy
        this.calcPrice()
      }).catch(() => {})
    },

    // 选择规格：WXML 不支持 indexOf，直接用 selected 布尔标记
    toggleOption(gi, oi, type) {
      const specGroups = this.specGroups
      const group = specGroups[gi]
      if (!group) return
      const option = group.options[oi]
      if (!option) return

      if (type === 2) {
        // 多选：切换
        option.selected = !option.selected
      } else {
        // 单选：先清除本组，再选中当前
        group.options.forEach((o) => { o.selected = false })
        option.selected = true
      }
      this.specGroups = [...specGroups]
      this.calcPrice()
    },

    // 计算加价与总价
    calcPrice() {
      const { specGroups, dish, quantity } = this
      let extra = 0
      specGroups.forEach((group) => {
        group.options.forEach((opt) => {
          if (opt.selected) {
            extra += Number(opt.extraPrice) || 0
          }
        })
      })
      const unit = Number(dish.price || 0) + extra
      this.extraPrice = extra
      this.totalPrice = (unit * quantity).toFixed(2)
    },

    // 生成规格文案，如「微辣, 加蛋」
    buildSpecText() {
      const names = []
      this.specGroups.forEach((group) => {
        group.options.forEach((opt) => {
          if (opt.selected) {
            names.push(opt.name)
          }
        })
      })
      return names.join(', ')
    },

    changeQty(delta) {
      // 数量不能低于起购份数（minBuy）
      const floor = this.minBuy || 1
      let quantity = this.quantity + delta
      if (quantity < floor) quantity = floor
      this.quantity = quantity
      this.calcPrice()
    },

    // 无规格菜品：直接加入购物车（保留默认数量 1）
    confirmNoSpec() {
      const { dish } = this
      const result = {
        dishId: dish.id,
        quantity: this.quantity,
        specIds: [],
        specText: '',
        extraPrice: 0,
        unitPrice: Number(dish.price || 0)
      }
      const pages = getCurrentPages()
      const prevPage = pages[pages.length - 2]
      if (prevPage && prevPage.onSpecChosen) {
        prevPage.onSpecChosen(result)
      }
      uni.navigateBack()
    },

    confirm() {
      const { specGroups, dish, quantity } = this

      if (specGroups.length === 0) {
        this.confirmNoSpec()
        return
      }

      // 校验必选组
      for (const group of specGroups) {
        if (group.required === 1) {
          const hasChosen = group.options.some((o) => o.selected)
          if (!hasChosen) {
            uni.showToast({ title: `请选择${group.groupName}`, icon: 'none' })
            return
          }
        }
      }

      // 收集所有选中选项ID
      const specIds = []
      specGroups.forEach((group) => {
        group.options.forEach((opt) => {
          if (opt.selected) {
            specIds.push(opt.id)
          }
        })
      })

      const result = {
        dishId: dish.id,
        quantity,
        specIds,
        specText: this.buildSpecText(),
        extraPrice: this.extraPrice,
        unitPrice: Number(dish.price || 0) + this.extraPrice
      }

      // 通过事件通道返回给上一页
      const pages = getCurrentPages()
      const prevPage = pages[pages.length - 2]
      if (prevPage && prevPage.onSpecChosen) {
        prevPage.onSpecChosen(result)
      }
      uni.navigateBack()
    }
  }
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #f4f6fa;
  padding: 20rpx 20rpx 180rpx;
  box-sizing: border-box;
}

/* 菜品信息头部卡片 */
.dish-head {
  display: flex;
  background: #ffffff;
  padding: 28rpx;
  border-radius: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(15, 23, 42, 0.04);
}

.dish-thumb {
  width: 160rpx;
  height: 160rpx;
  border-radius: 18rpx;
  background: #f8fafc;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}

.dish-thumb-img {
  width: 100%;
  height: 100%;
}

.dish-thumb-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: #f1f5f9;
}

.dish-thumb-icon {
  font-size: 64rpx;
}

.dish-info {
  flex: 1;
  min-width: 0;
  margin-left: 24rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.dish-name {
  font-size: 32rpx;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.35;
}

.dish-desc {
  font-size: 23rpx;
  color: #64748b;
  margin-top: 8rpx;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.dish-price-row {
  display: flex;
  align-items: baseline;
  gap: 12rpx;
  margin-top: 12rpx;
}

.dish-price {
  color: #ff5722;
  display: flex;
  align-items: baseline;
}

.price-sym {
  font-size: 24rpx;
  font-weight: 600;
}

.price-val {
  font-size: 38rpx;
  font-weight: 700;
  margin-left: 2rpx;
}

.dish-minbuy {
  font-size: 20rpx;
  color: #ff7a45;
  background: #fff3ea;
  border-radius: 6rpx;
  padding: 2rpx 10rpx;
}

/* 规格分组卡片 */
.spec-group {
  background: #ffffff;
  margin-top: 20rpx;
  padding: 28rpx;
  border-radius: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(15, 23, 42, 0.04);
}

.group-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.group-title-box {
  display: flex;
  align-items: baseline;
}

.group-name {
  font-size: 29rpx;
  font-weight: 600;
  color: #1e293b;
}

.group-type {
  font-size: 22rpx;
  color: #94a3b8;
  margin-left: 8rpx;
}

.group-required-badge {
  padding: 2rpx 12rpx;
  border-radius: 20rpx;
  background: #fee2e2;
  color: #ef4444;
  font-size: 20rpx;
  font-weight: 600;
}

.option-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.option {
  display: flex;
  align-items: center;
  padding: 14rpx 28rpx;
  border-radius: 36rpx;
  background: #f1f5f9;
  font-size: 25rpx;
  color: #475569;
  border: 2rpx solid transparent;
  transition: all 0.2s ease;
}

.option:active {
  transform: scale(0.97);
}

.option.active {
  background: #eff6ff;
  border-color: #2468f2;
  color: #2468f2;
  font-weight: 600;
}

.option-price {
  font-size: 22rpx;
  color: #2468f2;
  margin-left: 6rpx;
  font-weight: 600;
}

/* 空规格卡片 */
.empty-spec-card {
  margin-top: 30rpx;
  padding: 40rpx 24rpx;
  background: #ffffff;
  border-radius: 24rpx;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.empty-spec-icon {
  font-size: 48rpx;
  margin-bottom: 12rpx;
}

.empty-spec-text {
  font-size: 25rpx;
  color: #94a3b8;
}

.bottom-space {
  height: 40rpx;
}

/* 底部操作栏 */
.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  padding: 20rpx 28rpx calc(20rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -4rpx 24rpx rgba(15, 23, 42, 0.08);
  z-index: 100;
}

.qty-wrap {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.qty-label {
  font-size: 20rpx;
  color: #94a3b8;
  margin-bottom: 8rpx;
}

.stepper {
  display: flex;
  align-items: center;
}

.step-btn {
  width: 52rpx;
  height: 52rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.1s ease;
}

.step-btn:active {
  transform: scale(0.9);
}

.step-btn.plus {
  background: linear-gradient(135deg, #2468f2, #1a56d6);
  color: #ffffff;
  box-shadow: 0 4rpx 12rpx rgba(36, 104, 242, 0.3);
}

.step-btn.minus {
  border: 2rpx solid #cbd5e1;
  color: #64748b;
  background: #ffffff;
}

.step-btn.disabled {
  opacity: 0.4;
  pointer-events: none;
}

.step-icon {
  font-size: 30rpx;
  line-height: 1;
  font-weight: 600;
}

.step-num {
  min-width: 56rpx;
  text-align: center;
  font-size: 30rpx;
  font-weight: 700;
  color: #0f172a;
}

.action-wrap {
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.total-wrap {
  display: flex;
  align-items: baseline;
}

.total-label {
  font-size: 22rpx;
  color: #64748b;
  margin-right: 6rpx;
}

.total-sym {
  font-size: 24rpx;
  font-weight: 700;
  color: #ff5722;
}

.total-price {
  font-size: 40rpx;
  font-weight: 700;
  color: #ff5722;
  margin-left: 2rpx;
}

.confirm-btn {
  padding: 0 36rpx;
  height: 80rpx;
  border-radius: 40rpx;
  background: linear-gradient(135deg, #2468f2 0%, #1a56d6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 20rpx rgba(36, 104, 242, 0.35);
  transition: all 0.15s ease;
}

.confirm-btn:active {
  transform: scale(0.97);
}

.confirm-btn-text {
  color: #ffffff;
  font-size: 28rpx;
  font-weight: 600;
}
</style>
