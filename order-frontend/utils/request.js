// utils/request.js
// 统一请求封装：自动携带 token、统一错误提示、统一解包 Result
const { baseUrl } = require('./config')

const TOKEN_KEY = 'token'

function getToken() {
  return wx.getStorageSync(TOKEN_KEY) || ''
}

function setToken(token) {
  wx.setStorageSync(TOKEN_KEY, token)
}

function clearToken() {
  wx.removeStorageSync(TOKEN_KEY)
}

/**
 * 基础请求
 * @param {Object} options { url, method, data, header, showLoading, loadingText }
 * @returns {Promise<any>} resolve 后端 Result.data
 */
function request(options) {
  const {
    url,
    method = 'GET',
    data = {},
    header = {},
    showLoading = false,
    loadingText = '加载中'
  } = options

  if (showLoading) {
    wx.showLoading({ title: loadingText, mask: true })
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
  const shopId = wx.getStorageSync('shopId') || ''
  if (shopId) {
    finalHeader['X-Shop-Id'] = shopId
  }

  return new Promise((resolve, reject) => {
    wx.request({
      url: baseUrl + url,
      method,
      data,
      header: finalHeader,
      success(res) {
        const { statusCode, data: body } = res

        if (statusCode !== 200) {
          wx.showToast({ title: `网络错误 ${statusCode}`, icon: 'none' })
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
            wx.showToast({ title: body.message || '请先登录', icon: 'none' })
            const role = wx.getStorageSync('role') === 'merchant' ? 'merchant' : 'customer'
            setTimeout(() => wx.reLaunch({ url: `/pages/login/login?role=${role}` }), 800)
            reject(new Error(body.message || '未登录'))
            return
          }
          if (body.code === 403) {
            // 无权限（如顾客 token 访问店家管理接口）
            wx.showToast({ title: body.message || '无权限操作', icon: 'none' })
            reject(new Error(body.message || '无权限操作'))
            return
          }
          wx.showToast({ title: body.message || '请求失败', icon: 'none' })
          reject(new Error(body.message || '请求失败'))
          return
        }

        // 非标准结构直接返回
        resolve(body)
      },
      fail(err) {
        wx.showToast({ title: '网络连接失败', icon: 'none' })
        reject(err)
      },
      complete() {
        if (showLoading) {
          wx.hideLoading()
        }
      }
    })
  })
}

const http = {
  get: (url, data, options = {}) => request({ url, method: 'GET', data, ...options }),
  post: (url, data, options = {}) => request({ url, method: 'POST', data, ...options }),
  put: (url, data, options = {}) => request({ url, method: 'PUT', data, ...options }),
  del: (url, data, options = {}) => request({ url, method: 'DELETE', data, ...options }),
}

module.exports = {
  request,
  http,
  getToken,
  setToken,
  clearToken,
}
