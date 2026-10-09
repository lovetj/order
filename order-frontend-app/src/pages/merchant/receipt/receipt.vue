<template>
  <view class="page">
    <!-- 小票预览 -->
    <view class="receipt-paper">
      <view class="r-shop">{{receipt.shopName}}</view>
      <view v-if="receipt.shopPhone" class="r-line">电话：{{receipt.shopPhone}}</view>
      <view v-if="receipt.shopAddress" class="r-line">{{receipt.shopAddress}}</view>

      <view class="r-dash"></view>

      <view class="r-row">
        <text class="r-label">取餐号</text>
        <text class="r-pick">{{receipt.pickNo}}</text>
      </view>
      <view class="r-row"><text class="r-label">订单号</text><text>{{receipt.orderNo}}</text></view>
      <view class="r-row"><text class="r-label">桌号</text><text>{{receipt.tableNo}}</text></view>
      <view class="r-row" v-if="receipt.peopleCount">
        <text class="r-label">人数</text><text>{{receipt.peopleCount}} 人</text>
      </view>
      <view class="r-row"><text class="r-label">下单时间</text><text>{{receipt.createTime}}</text></view>

      <view class="r-dash"></view>

      <view class="r-item-head">
        <text class="col-name">品名</text>
        <text class="col-qty">数量</text>
        <text class="col-amt">金额</text>
      </view>
      <view class="r-item" v-for="item in receipt.items" :key="item.name">
        <text class="col-name">{{item.name}}</text>
        <text class="col-qty">{{item.quantity}}</text>
        <text class="col-amt">¥{{item.amount}}</text>
      </view>

      <view class="r-dash"></view>

      <view class="r-row"><text class="r-label">小计</text><text>¥{{receipt.productTotal}}</text></view>
      <view class="r-row" v-if="receipt.discountAmount > 0">
        <text class="r-label">优惠</text><text class="r-discount">-¥{{receipt.discountAmount}}</text>
      </view>
      <view class="r-row r-total">
        <text class="r-label">应付</text><text>¥{{receipt.payAmount}}</text>
      </view>

      <view v-if="receipt.remark" class="r-line r-remark">备注：{{receipt.remark}}</view>

      <view class="r-dash"></view>
      <view class="r-footer">谢谢惠顾，欢迎再次光临！</view>
    </view>

    <!-- 操作 -->
    <view class="ops">
      <view class="op-btn primary" @click="print">🖨 打印小票</view>
      <view class="op-btn" @click="copyText">📋 复制内容</view>
    </view>
  </view>
</template>

<script>
const app = getApp()
import api from '@/api/index'

export default {
  data() {
    return {
      orderId: '',
      receipt: {},
      loading: false
    }
  },
  onLoad(options) {
    if (app.globalData.role !== 'merchant' || !app.globalData.isLogin()) {
      uni.reLaunch({ url: '/pages/role/role' })
      return
    }
    const orderId = options && options.id ? options.id : ''
    this.orderId = orderId
    if (orderId) {
      this.loadReceipt()
    }
  },
  methods: {
    loadReceipt() {
      this.loading = true
      api.getReceipt(this.orderId).then((data) => {
        this.receipt = data || {}
        if (this.receipt.statusText && this.receipt.statusText !== '已完成') {
          uni.showModal({
            title: '提示',
            content: '该订单当前尚未完成，只有已完成的订单才支持打印小票。',
            showCancel: false,
            confirmText: '返回',
            success: () => {
              uni.navigateBack()
            }
          })
        }
      }).catch((err) => {
        const msg = (err && (err.message || err.msg)) || '只有已完成的订单才支持打印小票'
        uni.showModal({
          title: '提示',
          content: msg,
          showCancel: false,
          confirmText: '返回',
          success: () => {
            uni.navigateBack()
          }
        })
      }).then(() => {
        this.loading = false
      })
    },

    /**
     * 打印小票
     * 说明：跨端无法直接驱动热敏打印机，常见方案：
     *  1) 云打印机（如易联云/飞鹅）：后端调用其开放接口下发打印任务
     *  2) 蓝牙打印机：通过 uni.openBluetoothAdapter + writeBLECharacteristicValue 透传 ESC/POS 指令
     * 此处演示蓝牙方案：获取 Base64 指令后按字节下发
     */
    print() {
      uni.showModal({
        title: '打印方式',
        content: '选择打印方式：\n· 云打印：由后端对接云打印机下发任务\n· 蓝牙打印：连接蓝牙热敏打印机透传指令',
        confirmText: '云打印',
        cancelText: '蓝牙打印',
        success: (res) => {
          if (res.confirm) {
            this.cloudPrint()
          } else if (res.cancel) {
            this.bluetoothPrint()
          }
        }
      })
    },

    // 云打印：由后端下发（此处仅请求指令，实际由后端对接打印机）
    cloudPrint() {
      api.getReceiptEscPos(this.orderId).then((data) => {
        if (!data || !data.command) {
          uni.showToast({ title: '获取打印指令失败', icon: 'none' })
          return
        }
        uni.showModal({
          title: '云打印指令已生成',
          content: `指令长度：${data.command.length} 字符（Base64，${data.charset} 编码）\n\n实际项目请在后端对接云打印机（易联云/飞鹅）开放接口下发该指令。`,
          showCancel: false,
          confirmText: '知道了',
          confirmColor: '#2f80ed'
        })
      }).catch(() => {})
    },

    // 蓝牙打印：透传 ESC/POS 指令
    bluetoothPrint() {
      api.getReceiptEscPos(this.orderId).then((data) => {
        if (!data || !data.command) {
          uni.showToast({ title: '获取打印指令失败', icon: 'none' })
          return
        }
        uni.showModal({
          title: '蓝牙打印',
          content: '请在真机上连接蓝牙热敏打印机后，通过 uni.openBluetoothAdapter 与 writeBLECharacteristicValue 下发该 Base64 指令（解码为字节后发送）。',
          showCancel: false,
          confirmText: '知道了',
          confirmColor: '#2f80ed'
        })
      }).catch(() => {})
    },

    copyText() {
      const text = this.receipt.text || ''
      uni.setClipboardData({
        data: text,
        success: () => uni.showToast({ title: '小票内容已复制', icon: 'none' })
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.page {
  padding: 32rpx 24rpx 200rpx;
}

.receipt-paper {
  background: #fff;
  border-radius: 16rpx;
  padding: 40rpx 32rpx;
  font-size: 26rpx;
  color: #1f1f1f;
  box-shadow: 0 6rpx 24rpx rgba(0, 0, 0, 0.08);
  font-family: "Courier New", Consolas, monospace;
}

.r-shop {
  text-align: center;
  font-size: 36rpx;
  font-weight: 700;
  margin-bottom: 12rpx;
}

.r-line {
  text-align: center;
  font-size: 24rpx;
  color: #666;
  margin-top: 6rpx;
}

.r-dash {
  border-top: 2rpx dashed #ddd;
  margin: 24rpx 0;
}

.r-row {
  display: flex;
  justify-content: space-between;
  padding: 8rpx 0;
  font-size: 26rpx;
}

.r-label {
  color: #666;
}

.r-pick {
  font-size: 40rpx;
  font-weight: 700;
  color: #2f80ed;
}

.r-item-head {
  display: flex;
  padding: 8rpx 0;
  font-size: 24rpx;
  color: #999;
}

.r-item {
  display: flex;
  padding: 10rpx 0;
  font-size: 26rpx;
}

.col-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.col-qty {
  width: 100rpx;
  text-align: center;
}

.col-amt {
  width: 160rpx;
  text-align: right;
}

.r-discount {
  color: #ff6b35;
}

.r-total {
  font-size: 32rpx;
  font-weight: 700;
}

.r-remark {
  text-align: left;
  color: #ff9500;
  margin-top: 16rpx;
}

.r-footer {
  text-align: center;
  font-size: 24rpx;
  color: #666;
}

.ops {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  gap: 24rpx;
  padding: 24rpx 32rpx calc(24rpx + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.06);
}

.op-btn {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  border-radius: 999rpx;
  font-size: 29rpx;
  border: 2rpx solid #2f80ed;
  color: #2f80ed;
  background: #fff;
}

.op-btn.primary {
  background: linear-gradient(135deg, #4d95f5, #2f80ed);
  color: #fff;
  border: none;
  font-weight: 600;
}
</style>
