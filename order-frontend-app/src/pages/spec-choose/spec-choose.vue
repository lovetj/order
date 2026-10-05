<template>
  <view class="page">
    <!-- 菜品信息 -->
    <view class="dish-head">
      <view class="dish-thumb">
        <image v-if="dish.hasImage" class="dish-thumb-img" :src="dish.imageUrl" mode="aspectFill"></image>
        <text v-else>{{dish.image}}</text>
      </view>
      <view class="dish-info">
        <view class="dish-name">{{dish.name}}</view>
        <view class="dish-desc">{{dish.desc}}</view>
        <view class="dish-price">
          ¥{{dish.price}}
          <text v-if="minBuy > 1" class="dish-minbuy">{{minBuy}}份起购</text>
        </view>
      </view>
    </view>

    <!-- 规格分组 -->
    <view class="spec-group" v-for="(group, gi) in specGroups" :key="group.groupName">
      <view class="group-head">
        <text class="group-name">{{group.groupName}}</text>
        <text class="group-type">{{group.selectType === 2 ? '可多选' : '单选'}}</text>
        <text v-if="group.required === 1" class="group-required">必选</text>
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
          <text v-if="opt.extraPrice > 0" class="option-price">+{{opt.extraPrice}}元</text>
        </view>
      </view>
    </view>

    <view v-if="specGroups.length === 0" class="empty">该菜品暂无可选规格，可直接加入</view>

    <view class="bottom-space"></view>

    <!-- 底部操作栏 -->
    <view class="bottom-bar">
      <view class="qty-wrap">
        <text class="qty-label">数量</text>
        <view class="stepper">
          <view class="step-btn minus" @click="changeQty(-1)">−</view>
          <text class="step-num">{{quantity}}</text>
          <view class="step-btn plus" @click="changeQty(1)">+</view>
        </view>
      </view>
      <view class="total-wrap">
        <text class="total-label">合计</text>
        <text class="total-price">¥{{totalPrice}}</text>
      </view>
      <view class="confirm-btn" @click="confirm">加入购物车</view>
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
  padding-bottom: 140rpx;
}

.dish-head {
  display: flex;
  background: #fff;
  padding: 32rpx 28rpx;
}

.dish-thumb {
  width: 160rpx;
  height: 160rpx;
  border-radius: 16rpx;
  background: #f7f7f7;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 84rpx;
  flex-shrink: 0;
  overflow: hidden;
}

.dish-thumb-img {
  width: 100%;
  height: 100%;
}

.dish-info {
  flex: 1;
  margin-left: 24rpx;
}

.dish-name {
  font-size: 34rpx;
  font-weight: 700;
}

.dish-desc {
  font-size: 24rpx;
  color: #8a8a8a;
  margin-top: 10rpx;
}

.dish-price {
  font-size: 36rpx;
  font-weight: 700;
  color: #ff6b35;
  margin-top: 18rpx;
}

.dish-minbuy {
  font-size: 22rpx;
  font-weight: 400;
  color: #fff;
  background: #ff6b35;
  border-radius: 999rpx;
  padding: 2rpx 16rpx;
  margin-left: 14rpx;
  vertical-align: middle;
}

.spec-group {
  background: #fff;
  margin-top: 20rpx;
  padding: 28rpx;
}

.group-head {
  display: flex;
  align-items: center;
  margin-bottom: 20rpx;
}

.group-name {
  font-size: 30rpx;
  font-weight: 600;
}

.group-type {
  font-size: 22rpx;
  color: #8a8a8a;
  margin-left: 16rpx;
}

.group-required {
  font-size: 22rpx;
  color: #ff3b30;
  margin-left: 12rpx;
}

.option-list {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
}

.option {
  padding: 16rpx 30rpx;
  border-radius: 999rpx;
  background: #f5f6f8;
  font-size: 26rpx;
  color: #444;
  border: 2rpx solid transparent;
}

.option.active {
  background: #fff1eb;
  border-color: #ff6b35;
  color: #ff6b35;
  font-weight: 600;
}

.option-price {
  font-size: 24rpx;
  color: #ff6b35;
  margin-left: 8rpx;
}

.bottom-space {
  height: 40rpx;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  background: #fff;
  padding: 20rpx 28rpx calc(20rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.06);
}

.qty-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.qty-label {
  font-size: 20rpx;
  color: #8a8a8a;
  margin-bottom: 6rpx;
}

.stepper {
  display: flex;
  align-items: center;
}

.step-btn {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  line-height: 1;
}

.step-btn.plus {
  background: #ff6b35;
  color: #fff;
}

.step-btn.minus {
  border: 2rpx solid #ddd;
  color: #666;
}

.step-num {
  min-width: 56rpx;
  text-align: center;
  font-size: 30rpx;
}

.total-wrap {
  flex: 1;
  text-align: right;
  margin-right: 24rpx;
}

.total-label {
  font-size: 24rpx;
  color: #8a8a8a;
}

.total-price {
  font-size: 38rpx;
  font-weight: 700;
  color: #ff6b35;
  margin-left: 10rpx;
}

.confirm-btn {
  padding: 22rpx 46rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #ff8a5c, #ff6b35);
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
}
</style>
