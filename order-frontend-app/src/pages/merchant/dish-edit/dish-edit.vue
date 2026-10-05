<template>
  <view class="page" @click="clearActive">
    <!-- 图片上传 -->
    <view class="card">
      <view class="label">菜品图片 <text class="required">*</text></view>
      <view class="img-area">
        <image
          v-if="isImageUrl"
          class="preview-img"
          :src="imageUrl"
          mode="aspectFill"
          @click="previewImage"
        ></image>
        <view v-else class="preview">{{form.image}}</view>
        <view class="img-ops">
          <view class="img-btn primary" @click="chooseImage">
            {{uploading ? '上传中...' : (isImageUrl ? '更换图片' : '上传图片')}}
          </view>
          <view v-if="isImageUrl" class="img-btn" @click="removeImage">移除</view>
        </view>
      </view>

      
    </view>

    <!-- 基本信息 -->
    <view class="card">
      <view class="form-row">
        <text class="label">所属分类</text>
        <picker
          class="picker"
          mode="selector"
          :range="categories"
          range-key="name"
          :value="categoryIndex"
          @change="onCategoryChange"
        >
          <view class="picker-value">
            {{(categories[categoryIndex] && categories[categoryIndex].name) || '请选择'}} <text class="arrow">›</text>
          </view>
        </picker>
      </view>

      <view class="form-row">
        <text class="label">菜品名称 <text class="required">*</text></text>
        <input
          class="input"
          placeholder="如：招牌红烧肉"
          :value="form.name"
          @input="onInput('name', $event)"
        />
      </view>

      <view class="form-row">
        <text class="label">菜品描述 <text class="required">*</text></text>
        <input
          class="input"
          placeholder="如：肥而不腻，入口即化"
          :value="form.description"
          @input="onInput('description', $event)"
        />
      </view>

      <view class="form-row">
        <text class="label">价格（元） <text class="required">*</text></text>
        <input
          class="input"
          type="digit"
          placeholder="请输入价格"
          :value="form.price"
          @input="onInput('price', $event)"
        />
      </view>

      <view class="form-row">
        <text class="label">起购份数 <text class="required">*</text></text>
        <input
          class="input"
          type="number"
          placeholder="1 表示一份起购"
          :value="form.minBuy"
          @input="onInput('minBuy', $event)"
        />
      </view>

      <view class="form-row">
        <text class="label">库存</text>
        <input
          class="input"
          type="number"
          placeholder="默认 999"
          :value="form.stock"
          @input="onInput('stock', $event)"
        />
      </view>

      <view class="form-row">
        <text class="label">排序</text>
        <input
          class="input"
          type="number"
          placeholder="数字越小越靠前"
          :value="form.sort"
          @input="onInput('sort', $event)"
        />
      </view>
    </view>

    <!-- 开关 -->
    <view class="card">
      <view class="form-row">
        <text class="label">热销推荐</text>
        <switch :checked="form.isHot === 1" color="#ff6b35" @change="onHotChange" />
      </view>
      <view class="form-row">
        <text class="label">上架销售</text>
        <switch :checked="form.status === 1" color="#2f80ed" @change="onStatusChange" />
      </view>
    </view>

    <!-- 规格/口味配置 -->
    <view class="card">
      <view class="spec-head">
        <text class="section-title">规格 / 口味（可选）</text>
        <text class="spec-add" @click="addSpecGroup">＋ 添加分组</text>
      </view>

      <view class="spec-group" v-for="(group, gi) in specGroups" :key="group.groupName">
        <view class="spec-group-head">
          <!-- 左移分组按钮：仅激活分组显示，第一个禁用 -->
          <view
            v-if="activeGroupGi === gi"
            class="move-btn"
            :class="{ disabled: gi === 0 }"
            @click.stop="moveGroup(gi, -1)"
          >‹</view>
          <view
            class="spec-group-name"
            :class="{ active: activeGroupGi === gi }"
            @click.stop="toggleGroup(gi)"
          >{{group.groupName}}</view>
          <!-- 右移分组按钮：仅激活分组显示，最后一个禁用 -->
          <view
            v-if="activeGroupGi === gi"
            class="move-btn"
            :class="{ disabled: gi === specGroups.length - 1 }"
            @click.stop="moveGroup(gi, 1)"
          >›</view>
          <view class="spec-tags" @click.stop="noop">
            <view class="spec-tag" :class="{ on: group.selectType === 2 }" @click.stop="toggleSelectType(gi)">
              {{group.selectType === 2 ? '多选' : '单选'}}
            </view>
            <view class="spec-tag" :class="{ on: group.required === 1 }" @click.stop="toggleRequired(gi)">
              {{group.required === 1 ? '必选' : '可选'}}
            </view>
            <view class="spec-del" @click.stop="removeSpecGroup(gi)">删除分组</view>
          </view>
        </view>

        <view class="spec-options">
          <view class="opt-cell" v-for="(opt, oi) in group.options" :key="opt.name">
            <!-- 左移按钮：仅激活项显示，第一项禁用 -->
            <view
              v-if="activeGi === gi && activeOi === oi"
              class="move-btn"
              :class="{ disabled: oi === 0 }"
              @click.stop="moveOption(gi, oi, -1)"
            >‹</view>
            <view
              class="spec-option"
              :class="{ default: opt.isDefault, active: activeGi === gi && activeOi === oi }"
              @click.stop="toggleOption(gi, oi)"
            >
              <text class="opt-name">{{opt.name}}</text>
              <text v-if="opt.extraPrice > 0" class="opt-price">+{{opt.extraPrice}}</text>
              <text v-if="opt.isDefault" class="opt-default">默认</text>
              <!-- 激活时提示：再点一次切换默认 -->
              <text v-if="activeGi === gi && activeOi === oi" class="opt-hint">
                {{opt.isDefault ? '点一次取消默认' : '点一次设为默认'}}
              </text>
              <text class="opt-del" @click.stop="removeSpecOption(gi, oi)">×</text>
            </view>
            <!-- 右移按钮：仅激活项显示，最后一项禁用 -->
            <view
              v-if="activeGi === gi && activeOi === oi"
              class="move-btn"
              :class="{ disabled: oi === group.options.length - 1 }"
              @click.stop="moveOption(gi, oi, 1)"
            >›</view>
          </view>
          <view class="spec-option add" @click="addSpecOption(gi)">＋ 添加选项</view>
        </view>
      </view>

      <view v-if="specGroups.length === 0" class="spec-empty">
        暂无规格，如「辣度」「加料」可点击右上角添加
      </view>
    </view>

    <!-- 操作按钮（固定悬浮底部）：删除在前，保存在后，横向平排 -->
    <view class="btn-wrap">
      <view v-if="isEdit" class="delete-btn" :class="{ disabled: deleting }" @click="remove">
        <view v-if="deleting" class="btn-spinner danger"></view>
        <text>{{deleting ? '删除中...' : '删除菜品'}}</text>
      </view>
      <view class="btn-primary submit-btn" :class="{ disabled: submitting }" @click="submit">
        <view v-if="submitting" class="btn-spinner"></view>
        <text>{{submitting ? '保存中...' : (isEdit ? '保存修改' : '确认新增')}}</text>
      </view>
    </view>

    <!-- 保存/删除 全局加载蒙版 -->
    <view class="loading-mask" v-if="submitting || deleting">
      <view class="loading-box">
        <view class="loading-spinner"></view>
        <text class="loading-text">{{deleting ? '正在删除…' : '正在保存…'}}</text>
      </view>
    </view>
  </view>
</template>

<script>
import api from '@/api/index'
import { formatImageUrl, toRelativePath } from '@/utils/util'

export default {
  data() {
    return {
      id: '',
      isEdit: false,
      categories: [],
      categoryIndex: 0,
      // 是否为真实图片 URL
      isImageUrl: false,
      // 图片完整展示地址（form.image 存相对路径，此处存拼好的完整地址用于渲染）
      imageUrl: '',
      // 规格分组：[{ groupName, selectType, required, options:[{id,name,extraPrice,isDefault}] }]
      specGroups: [],
      form: {
        categoryId: '',
        name: '',
        description: '',
        image: '',
        price: '',
        minBuy: '1',
        stock: '999',
        isHot: 0,
        status: 1,
        sort: 0
      },
      submitting: false,
      deleting: false,
      uploading: false,
      // 当前激活的选项（显示左右移动按钮）
      activeGi: -1,
      activeOi: -1,
      // 当前激活的分组（显示左右移动按钮）
      activeGroupGi: -1
    }
  },
  onLoad(options) {
    const id = options && options.id ? options.id : ''
    this.id = id
    this.isEdit = !!id
    uni.setNavigationBarTitle({ title: id ? '编辑菜品' : '新增菜品' })
    this.loadCategories(id)
  },
  methods: {
    // 选择并上传图片
    chooseImage() {
      if (this.uploading) return
      uni.chooseImage({
        count: 1,
        sourceType: ['album', 'camera'],
        sizeType: ['compressed'],
        success: (res) => {
          const filePath = res.tempFilePaths[0]
          this.uploadImage(filePath)
        }
      })
    },

    uploadImage(filePath) {
      this.uploading = true
      uni.showLoading({ title: '上传中', mask: true })
      api.uploadFile(filePath, 'dish').then((data) => {
        // 表单仍存相对路径（提交/回显约定），展示直接用上传接口返回的完整 url 字段
        if (!data.url) {
          uni.showToast({ title: '上传结果异常：未获取到地址', icon: 'none' })
          return
        }
        this.form.image = toRelativePath(data.url, data.relativePath)
        this.imageUrl = data.url
        this.isImageUrl = true
        uni.showToast({ title: '上传成功', icon: 'success' })
      }).catch((e) => {
        uni.showToast({ title: (e && e.message) || '上传失败', icon: 'none' })
      }).then(() => {
        uni.hideLoading()
        this.uploading = false
      })
    },

    // 移除图片
    removeImage() {
      this.form.image = ''
      this.imageUrl = ''
      this.isImageUrl = false
    },

    previewImage() {
      if (!this.isImageUrl || !this.imageUrl) return
      uni.previewImage({ urls: [this.imageUrl] })
    },

    loadCategories(id) {
      api.getAllCategories().then((categories) => {
        const list = categories || []
        this.categories = list
        if (!id && list.length) {
          this.form.categoryId = list[0].id
          this.categoryIndex = 0
        }
        if (id) {
          this.loadDish(id)
        }
      }).catch(() => {})
    },

    loadDish(id) {
      api.getDishDetail(id).then((dish) => {
        if (!dish) return
        const index = this.categories.findIndex((c) => c.id === dish.categoryId)
        const image = dish.image || ''
        // 后端出参已是完整可访问地址（http 开头）或相对路径（历史数据），其余视为 emoji
        const isImageUrl = /^https?:\/\//.test(image) || image.startsWith('/')
        this.categoryIndex = index < 0 ? 0 : index
        this.isImageUrl = isImageUrl
        // 展示用接口返回字段（完整地址直接渲染）；表单仍存相对路径，保持提交约定
        this.imageUrl = isImageUrl ? formatImageUrl(image) : ''
        this.form = {
          categoryId: dish.categoryId,
          name: dish.name || '',
          description: dish.desc || '',
          image: isImageUrl ? toRelativePath(image) : image,
          price: dish.price != null ? String(dish.price) : '',
          minBuy: dish.minBuy != null && dish.minBuy > 0 ? String(dish.minBuy) : '1',
          stock: dish.stock != null ? String(dish.stock) : '999',
          isHot: dish.isHot || 0,
          status: dish.status ? 1 : 0,
          sort: dish.sort || 0
        }
        // 回填已有规格分组
        this.specGroups = (dish.specs || []).map((group) => ({
          groupName: group.groupName,
          selectType: group.selectType == null ? 1 : group.selectType,
          required: group.required == null ? 0 : group.required,
          options: (group.options || []).map((o) => ({
            id: o.id,
            name: o.name,
            extraPrice: Number(o.extraPrice) || 0,
            isDefault: !!o.isDefault
          }))
        }))
      }).catch(() => {})
    },

    onInput(field, e) {
      this.form[field] = e.detail.value
    },

    // 切换分类
    onCategoryChange(e) {
      const index = Number(e.detail.value)
      const category = this.categories[index]
      this.categoryIndex = index
      this.form.categoryId = category ? category.id : ''
    },

    // 是否热销
    onHotChange(e) {
      this.form.isHot = e.detail.value ? 1 : 0
    },

    // 是否上架
    onStatusChange(e) {
      this.form.status = e.detail.value ? 1 : 0
    },

    // ==================== 规格管理 ====================

    // 新增一个规格分组（如「辣度」「加料」）
    addSpecGroup() {
      uni.showModal({
        title: '新增规格分组',
        editable: true,
        placeholderText: '如：辣度 / 加料',
        confirmColor: '#2f80ed',
        success: (res) => {
          if (!res.confirm) return
          const name = (res.content || '').trim()
          if (!name) {
            uni.showToast({ title: '分组名称不能为空', icon: 'none' })
            return
          }
          const specGroups = [...this.specGroups]
          if (specGroups.some((g) => g.groupName === name)) {
            uni.showToast({ title: '分组已存在', icon: 'none' })
            return
          }
          specGroups.push({
            groupName: name,
            selectType: 1,
            required: 1,
            options: []
          })
          this.specGroups = specGroups
        }
      })
    },

    // 删除规格分组（带确认提示）
    removeSpecGroup(gi) {
      const group = this.specGroups[gi]
      if (!group) return
      uni.showModal({
        title: '删除确认',
        content: `确定删除分组「${group.groupName}」及其全部选项吗？`,
        confirmText: '删除',
        confirmColor: '#ff3b30',
        success: (res) => {
          if (!res.confirm) return
          const specGroups = [...this.specGroups]
          specGroups.splice(gi, 1)
          // 分组被删除，重置激活态
          this.specGroups = specGroups
          this.activeGi = -1
          this.activeOi = -1
          this.activeGroupGi = -1
          uni.showToast({ title: '已删除', icon: 'none' })
        }
      })
    },

    // 切换分组选择类型：单选 / 多选
    toggleSelectType(gi) {
      const specGroups = [...this.specGroups]
      specGroups[gi].selectType = specGroups[gi].selectType === 1 ? 2 : 1
      this.specGroups = specGroups
    },

    // 切换分组是否必选
    toggleRequired(gi) {
      const specGroups = [...this.specGroups]
      specGroups[gi].required = specGroups[gi].required === 1 ? 0 : 1
      this.specGroups = specGroups
    },

    // 新增分组下的选项
    addSpecOption(gi) {
      const specGroups = [...this.specGroups]
      const group = specGroups[gi]
      uni.showModal({
        title: `新增选项（${group.groupName}）`,
        editable: true,
        placeholderText: '如：微辣 / 加蛋',
        confirmColor: '#2f80ed',
        success: (res) => {
          if (!res.confirm) return
          const name = (res.content || '').trim()
          if (!name) {
            uni.showToast({ title: '选项名称不能为空', icon: 'none' })
            return
          }
          // 输入加价
          uni.showModal({
            title: `选项「${name}」加价`,
            editable: true,
            placeholderText: '加价金额，不需要加价填 0',
            confirmColor: '#2f80ed',
            success: (r2) => {
              if (!r2.confirm) return
              const extra = Number(r2.content) || 0
              group.options.push({
                id: '',
                name,
                extraPrice: extra,
                isDefault: false
              })
              this.specGroups = specGroups
            }
          })
        }
      })
    },

    // 删除选项（带确认提示）
    removeSpecOption(gi, oi) {
      const option = this.specGroups[gi].options[oi]
      uni.showModal({
        title: '删除确认',
        content: `确定删除选项「${option.name}」吗？`,
        confirmText: '删除',
        confirmColor: '#ff3b30',
        success: (res) => {
          if (!res.confirm) return
          const specGroups = this.specGroups.map((g) => ({
            ...g,
            options: [...g.options]
          }))
          specGroups[gi].options.splice(oi, 1)
          // 选项被删除，重置激活态，避免移动按钮残留
          this.specGroups = specGroups
          this.activeGi = -1
          this.activeOi = -1
          uni.showToast({ title: '已删除', icon: 'none' })
        }
      })
    },

    // ==================== 选项排序（左右移动按钮） ====================

    /**
     * 点击选项：
     *  - 第一次点击：仅激活该项（左右显示移动按钮），不改变默认状态
     *  - 再次点击已激活项：切换其「默认」状态（单选互斥 / 多选可多选）
     */
    toggleOption(gi, oi) {
      const isActive = this.activeGi === gi && this.activeOi === oi

      // 未激活：仅激活，不切换默认
      if (!isActive) {
        this.activeGi = gi
        this.activeOi = oi
        this.activeGroupGi = -1
        return
      }

      // 已激活：再次点击切换默认状态
      const specGroups = this.specGroups.map((g) => ({
        ...g,
        options: [...g.options]
      }))
      const group = specGroups[gi]
      group.options.forEach((o, idx) => {
        if (group.selectType === 1) {
          // 单选：互斥
          o.isDefault = idx === oi ? !o.isDefault : false
        } else {
          // 多选：可多默认
          if (idx === oi) o.isDefault = !o.isDefault
        }
      })
      this.specGroups = specGroups
    },

    // 左移 / 右移选项（dir: -1 左移，1 右移）
    moveOption(gi, from, dir) {
      const target = from + Number(dir)
      const specGroups = this.specGroups.map((g) => ({
        ...g,
        options: [...g.options]
      }))
      const options = specGroups[gi].options
      if (target < 0 || target >= options.length) return
      const [moved] = options.splice(from, 1)
      options.splice(target, 0, moved)
      // 激活项跟随移动后的新位置
      this.specGroups = specGroups
      this.activeGi = gi
      this.activeOi = target
    },

    // 取消激活（点击空白处收起所有移动按钮）
    clearActive() {
      if (this.activeGi === -1 && this.activeGroupGi === -1) return
      this.activeGi = -1
      this.activeOi = -1
      this.activeGroupGi = -1
    },

    // 空操作：用于阻止子元素点击冒泡到根节点（避免误收起）
    noop() {},

    // ==================== 分组排序（左右移动按钮） ====================

    // 点击分组名：激活该分组
    toggleGroup(gi) {
      const activeGroupGi = this.activeGroupGi === gi ? -1 : gi
      this.activeGroupGi = activeGroupGi
      this.activeGi = -1
      this.activeOi = -1
    },

    // 左移 / 右移分组（dir: -1 左移，1 右移）
    moveGroup(gi, dir) {
      const from = Number(gi)
      const target = from + Number(dir)
      const specGroups = [...this.specGroups]
      if (target < 0 || target >= specGroups.length) return
      const [moved] = specGroups.splice(from, 1)
      specGroups.splice(target, 0, moved)
      // 激活分组跟随移动到新位置
      this.specGroups = specGroups
      this.activeGroupGi = target
    },

    // 保存规格（新增菜品时需先保存菜品拿到 id）
    saveSpecs(dishId) {
      const specs = []
      this.specGroups.forEach((group) => {
        group.options.forEach((opt) => {
          specs.push({
            groupName: group.groupName,
            name: opt.name,
            extraPrice: Number(opt.extraPrice) || 0,
            isDefault: opt.isDefault ? 1 : 0,
            selectType: group.selectType,
            required: group.required,
            status: 1
          })
        })
      })
      return api.saveDishSpecs(dishId, specs)
    },

    submit() {
      if (this.submitting) return
      const { form, isEdit, id } = this

      if (!form.categoryId) {
        uni.showToast({ title: '请选择分类', icon: 'none' })
        return
      }
      // 菜品图片必填：需为真实上传的图片
      if (!this.isImageUrl || !form.image || !form.image.startsWith('/')) {
        uni.showToast({ title: '请上传菜品图片', icon: 'none' })
        return
      }
      if (!form.name || !form.name.trim()) {
        uni.showToast({ title: '请输入菜品名称', icon: 'none' })
        return
      }
      if (!form.description || !form.description.trim()) {
        uni.showToast({ title: '请输入菜品描述', icon: 'none' })
        return
      }
      const price = Number(form.price)
      if (!price || price <= 0) {
        uni.showToast({ title: '请输入有效价格', icon: 'none' })
        return
      }
      const minBuy = form.minBuy === '' ? 1 : Number(form.minBuy)
      if (isNaN(minBuy) || minBuy < 1 || !Number.isInteger(minBuy)) {
        uni.showToast({ title: '起购份数需为不小于1的整数', icon: 'none' })
        return
      }
      const stock = form.stock === '' ? 999 : Number(form.stock)
      if (isNaN(stock) || stock < 0) {
        uni.showToast({ title: '请输入有效库存', icon: 'none' })
        return
      }

      const payload = {
        categoryId: form.categoryId,
        name: form.name.trim(),
        description: form.description.trim(),
        image: form.image,
        price,
        minBuy,
        stock,
        isHot: form.isHot,
        status: form.status,
        sort: Number(form.sort) || 0
      }

      this.submitting = true
      const request = isEdit
        ? api.updateDish({ ...payload, id })
        : api.addDish(payload)

      request.then((result) => {
        // 新增接口返回新菜品ID；编辑沿用当前 id
        const dishId = isEdit ? id : result
        // 菜品保存成功后再保存规格（规格需 dishId）
        return this.saveSpecs(dishId).then(() => {
          uni.showToast({ title: isEdit ? '保存成功' : '新增成功', icon: 'success' })
          setTimeout(() => uni.navigateBack(), 700)
        })
      }).catch(() => {
        this.submitting = false
      })
    },

    // 删除（仅编辑模式）
    remove() {
      if (this.deleting || this.submitting) return
      uni.showModal({
        title: '删除确认',
        content: '确定删除该菜品吗？',
        confirmText: '删除',
        confirmColor: '#ff3b30',
        success: (res) => {
          if (!res.confirm) return
          this.deleting = true
          api.deleteDish(this.id).then(() => {
            uni.showToast({ title: '已删除', icon: 'none' })
            setTimeout(() => uni.navigateBack(), 700)
          }).catch(() => {}).then(() => {
            this.deleting = false
          })
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.page {
  padding-bottom: 260rpx;
}

.label {
  font-size: 28rpx;
  color: #333;
  min-width: 160rpx;
}

.required {
  color: #ff3b30;
}

.preview {
  width: 200rpx;
  height: 200rpx;
  border-radius: 20rpx;
  background: #f7f7f7;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 110rpx;
  margin: 20rpx auto;
}

.img-area {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.preview-img {
  width: 200rpx;
  height: 200rpx;
  border-radius: 20rpx;
  margin: 20rpx auto;
  background: #f7f7f7;
}

.img-ops {
  display: flex;
  gap: 20rpx;
  margin-top: 12rpx;
}

.img-btn {
  padding: 14rpx 36rpx;
  border-radius: 999rpx;
  font-size: 26rpx;
  border: 2rpx solid #2f80ed;
  color: #2f80ed;
}

.img-btn.primary {
  background: #2f80ed;
  color: #fff;
}

.form-row {
  display: flex;
  align-items: center;
  padding: 26rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}

.form-row:last-child {
  border-bottom: none;
}

.picker {
  flex: 1;
}

.picker-value {
  text-align: right;
  font-size: 28rpx;
  color: #2f80ed;
}

.arrow {
  color: #c8c8c8;
  margin-left: 8rpx;
}

.input {
  flex: 1;
  text-align: right;
  font-size: 28rpx;
}

/* 底部操作栏：固定悬浮在页面下方，删除与保存横向平排 */
.btn-wrap {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 20rpx 24rpx calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.06);
}

/* 规格配置 */
.spec-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.spec-add {
  font-size: 26rpx;
  color: #2f80ed;
}

.spec-group {
  border: 1rpx solid #eef0f3;
  border-radius: 16rpx;
  padding: 20rpx;
  margin-bottom: 20rpx;
  background: #fafbfc;
}

.spec-group-head {
  display: flex;
  align-items: center;
  margin-bottom: 18rpx;
}

.spec-group-name {
  font-size: 28rpx;
  font-weight: 600;
  padding: 4rpx 10rpx;
  border-radius: 10rpx;
}

/* 激活的分组名：蓝色高亮，提示可左右移动 */
.spec-group-name.active {
  color: #2f80ed;
  background: #eaf3ff;
}

.spec-tags {
  display: flex;
  align-items: center;
  margin-left: auto;
}

.spec-tag {
  flex-shrink: 0;
  height: 44rpx;
  line-height: 44rpx;
  padding: 0 18rpx;
  box-sizing: border-box;
  font-size: 22rpx;
  border-radius: 999rpx;
  background: #eef0f3;
  color: #666;
  margin-left: 12rpx;
}

.spec-tag.on {
  background: #eaf3ff;
  color: #2f80ed;
}

.spec-del {
  flex-shrink: 0;
  height: 44rpx;
  line-height: 44rpx;
  font-size: 22rpx;
  color: #ff3b30;
  margin-left: 16rpx;
}

.spec-options {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.spec-option {
  position: relative;
  padding: 14rpx 22rpx;
  border-radius: 12rpx;
  background: #fff;
  border: 2rpx solid #e8eaee;
  font-size: 25rpx;
  display: flex;
  align-items: center;
  /* 重排时其它选项平滑让位 */
  transition: transform 0.18s ease, box-shadow 0.18s ease, opacity 0.18s ease;
}

.spec-option.default {
  border-color: #2f80ed;
  background: #eaf3ff;
}

.spec-option.add {
  color: #2f80ed;
  border-style: dashed;
}

/* 激活的选项：加粗描边，提示可左右移动 */
.spec-option.active {
  border-color: #2f80ed;
  box-shadow: 0 6rpx 16rpx rgba(47, 128, 237, 0.18);
}

/* 选项单元：选项本体 + 左右移动按钮，横向排列 */
.opt-cell {
  display: flex;
  align-items: center;
}

/* 左右移动按钮：flex 居中文本箭头，避免字体基线偏移 */
.move-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 44rpx;
  height: 44rpx;
  margin: 0 8rpx;
  border-radius: 50%;
  background: #2f80ed;
  color: #fff;
  font-size: 34rpx;
  line-height: 1;
  padding-bottom: 4rpx;
  box-sizing: border-box;
}

.move-btn.disabled {
  background: #d8dce3;
  opacity: 0.6;
}

.opt-name {
  color: #333;
}

.opt-price {
  color: #ff6b35;
  font-size: 22rpx;
  margin-left: 8rpx;
}

.opt-default {
  color: #2f80ed;
  font-size: 20rpx;
  margin-left: 8rpx;
}

/* 激活项的提示文字：再点一次可切换默认 */
.opt-hint {
  color: #2f80ed;
  font-size: 18rpx;
  margin-left: 8rpx;
  opacity: 0.75;
}

.opt-del {
  color: #bbb;
  font-size: 30rpx;
  margin-left: 12rpx;
  padding: 0 6rpx;
}

.spec-empty {
  font-size: 24rpx;
  color: #a0a0a0;
  text-align: center;
  padding: 20rpx 0;
}

.submit-btn {
  flex: 1;
  height: 92rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  font-size: 32rpx;
}

.submit-btn.disabled {
  opacity: 0.6;
}

.delete-btn {
  flex-shrink: 0;
  padding: 0 40rpx;
  height: 92rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  border-radius: 999rpx;
  color: #ff3b30;
  font-size: 30rpx;
  background: #fff;
  border: 2rpx solid #ffd6d2;
  box-sizing: border-box;
}

.delete-btn.disabled {
  opacity: 0.6;
}

/* 按钮内加载动画：白色旋转圆环 */
.btn-spinner {
  width: 30rpx;
  height: 30rpx;
  margin-right: 12rpx;
  border: 4rpx solid rgba(255, 255, 255, 0.45);
  border-top-color: #fff;
  border-radius: 50%;
  animation: btn-spinner-rotate 0.7s linear infinite;
}

.btn-spinner.danger {
  border-color: rgba(255, 59, 48, 0.25);
  border-top-color: #ff3b30;
}

@keyframes btn-spinner-rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* ---------- 保存/删除 全局加载蒙版 ---------- */
.loading-mask {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  z-index: 999;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-box {
  min-width: 220rpx;
  padding: 40rpx 36rpx;
  background: rgba(0, 0, 0, 0.78);
  border-radius: 20rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* 旋转圆环 */
.loading-spinner {
  width: 56rpx;
  height: 56rpx;
  border: 6rpx solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: btn-spinner-rotate 0.7s linear infinite;
}

.loading-text {
  margin-top: 24rpx;
  font-size: 26rpx;
  color: #fff;
}
</style>
