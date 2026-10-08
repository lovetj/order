// utils/request.js
// 统一请求封装：自动携带 token、统一错误提示、统一解包 Result
import { baseUrl } from './config'

const TOKEN_KEY = 'token'

export function getToken() {
  return uni.getStorageSync(TOKEN_KEY) || ''
}

export function setToken(token) {
  uni.setStorageSync(TOKEN_KEY, token)
}

export function clearToken() {
  uni.removeStorageSync(TOKEN_KEY)
}

let redirecting = false

/**
 * 基础请求
 * @param {Object} options { url, method, data, header, showLoading, loadingText }
 * @returns {Promise<any>} resolve 后端 Result.data
 */
export function request(options) {
  const {
    url,
    method = 'GET',
    data = {},
    header = {},
    showLoading = false,
    loadingText = '加载中'
  } = options

  if (showLoading) {
    uni.showLoading({ title: loadingText, mask: true })
  }

  const token = getToken()
  const finalHeader = {
    'Content-Type': 'application/json',
    ...header
  }
  if (token) {
    finalHeader.Authorization = `Bearer ${token}`
  }
  // 多店铺隔离：携带当前店铺ID，后端据此过滤数据
  const shopId = uni.getStorageSync('shopId') || ''
  if (shopId) {
    finalHeader['X-Shop-Id'] = shopId
  }
  // 顾客三要素登录态：携带用户ID与桌位ID，后端据此校验 (user, shop, table)
  const userId = uni.getStorageSync('userId') || ''
  if (userId) {
    finalHeader['X-User-Id'] = userId
  }
  const tableId = uni.getStorageSync('tableId') || ''
  if (tableId) {
    finalHeader['X-Table-Id'] = tableId
  }

  return new Promise((resolve, reject) => {
    uni.request({
      url: baseUrl + url,
      method,
      data,
      header: finalHeader,
      success(res) {
        const { statusCode, data: body } = res

        if (statusCode !== 200) {
          uni.showToast({ title: `网络错误 ${statusCode}`, icon: 'none' })
          reject(new Error(`HTTP ${statusCode}`))
          return
        }

        // 后端统一 Result 结构 { code, message, data }
        if (body && typeof body === 'object' && 'code' in body) {
          if (body.code === 200) {
            resolve(body.data)
            return
          }
          if (body.code === 401) {
            // 未登录 / 登录已过期：清理登录态并按身份回到登录页
            clearToken()
            uni.showToast({ title: body.message || '请先登录', icon: 'none' })
            const role = uni.getStorageSync('role') === 'merchant' ? 'merchant' : 'customer'
            setTimeout(() => uni.reLaunch({ url: `/pages/login/login?role=${role}` }), 800)
            reject(new Error(body.message || '未登录'))
            return
          }
          if (body.code === 403) {
            // 无权限（如顾客 token 访问店家管理接口）
            uni.showToast({ title: body.message || '无权限操作', icon: 'none' })
            reject(new Error(body.message || '无权限操作'))
            return
          }
          uni.showToast({ title: body.message || '请求失败', icon: 'none' })
          reject(new Error(body.message || '请求失败'))
          return
        }

        // 非标准结构直接返回
        resolve(body)
      },
      fail(err) {
        uni.showToast({ title: '网络连接失败', icon: 'none' })
        reject(err)
      },
      complete() {
        if (showLoading) {
          uni.hideLoading()
        }
      }
    })
  })
}

export const http = {
  get: (url, data, options = {}) => request({ url, method: 'GET', data, ...options }),
  post: (url, data, options = {}) => request({ url, method: 'POST', data, ...options }),
  put: (url, data, options = {}) => request({ url, method: 'PUT', data, ...options }),
  del: (url, data, options = {}) => request({ url, method: 'DELETE', data, ...options })
}

/**
 * 单文件上传（返回后端 data: { url, relativePath, fileName, size, width, height }）
 * 自动携带 Authorization 及业务上下文请求头 (X-Shop-Id, X-User-Id, X-Table-Id) 与 formData
 */
export function uploadFile(filePath, bizType = 'other', extraData = {}) {
  const token = getToken()
  const shopId = uni.getStorageSync('shopId') || ''
  const userId = uni.getStorageSync('userId') || ''
  const tableId = uni.getStorageSync('tableId') || ''

  const header = {}
  if (token) {
    header.Authorization = `Bearer ${token}`
  }
  if (shopId) {
    header['X-Shop-Id'] = String(shopId)
  }
  if (userId) {
    header['X-User-Id'] = String(userId)
  }
  if (tableId) {
    header['X-Table-Id'] = String(tableId)
  }

  const formData = {
    bizType,
    ...(shopId ? { shopId: String(shopId) } : {}),
    ...(userId ? { userId: String(userId) } : {}),
    ...(tableId ? { tableId: String(tableId) } : {}),
    ...extraData
  }

  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${baseUrl}/api/file/upload?bizType=${encodeURIComponent(bizType)}`,
      filePath,
      name: 'file',
      header,
      formData,
      success: (res) => {
        try {
          const body = JSON.parse(res.data)
          if (body && body.code === 200) {
            resolve(body.data)
          } else {
            reject(new Error((body && body.message) || '上传失败'))
          }
        } catch (e) {
          reject(new Error('上传失败'))
        }
      },
      fail: () => reject(new Error('上传失败'))
    })
  })
}

/**
 * 多文件上传（并发调用单文件上传接口，按传入顺序汇总返回）
 */
export function uploadFiles(filePaths, bizType = 'other', extraData = {}) {
  const paths = (filePaths || []).filter(Boolean)
  if (!paths.length) return Promise.resolve([])
  return Promise.all(paths.map((filePath) => uploadFile(filePath, bizType, extraData)))
}

export default {
  request,
  http,
  getToken,
  setToken,
  clearToken,
  uploadFile,
  uploadFiles
}
