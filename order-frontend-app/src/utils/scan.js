// 顾客扫码工具：解析二维码中的店铺ID与桌号ID，并刷新全局上下文 + 三要素登录态
import api from '@/api/index'
// #ifdef H5
import { scanQRCodeInBrowser } from './h5Scanner'
// #endif

/**
 * 解析二维码内容，支持拼入 shopId / tableId / tableNo：
 *   1) 应用内页面路径 pages/role/role?shopId=1&tableId=xxx&tableNo=A01
 *   2) 普通 URL https://xxx?shopId=1&tableId=xxx&tableNo=A01
 * @returns {Promise<{shopId:string, tableId:string, tableNo:string}>}
 */
export function parseQrContent(content) {
  const text = String(content || '').trim()
  const result = { shopId: '', tableId: '', tableNo: '' }
  if (!text) return result

  const queryIndex = text.indexOf('?')
  if (queryIndex < 0) return result

  text.substring(queryIndex + 1).split('&').forEach((pair) => {
    const idx = pair.indexOf('=')
    if (idx < 0) return
    const key = decodeURIComponent(pair.substring(0, idx)).trim()
    const value = decodeURIComponent(pair.substring(idx + 1)).trim()
    if (!key || value === undefined) return
    if (key === 'shopId') result.shopId = value
    else if (key === 'tableId') result.tableId = value
    else if (key === 'tableNo') result.tableNo = value
  })
  return result
}

/** 当前是否已有 店铺+桌位 上下文（三要素登录态所需） */
export function hasCustomerContext(app) {
  return !!(app.globalData.shopId && app.globalData.tableId)
}

/**
 * 按 tableId 二次反查桌位信息（桌号、店铺），补齐 {tableNo, shopId} 并回写全局上下文与缓存。
 * 二维码可能只携带 tableId，桌号 tableNo 必须通过此接口解析获得。
 * @param {object} app getApp()
 * @param {string} tableId 桌位ID
 * @returns {Promise<{shopId:string, tableNo:string}|null>}
 */
export function resolveTableNo(app, tableId) {
  if (!tableId) return Promise.resolve(null)
  return api.getTableByCustomerId(tableId)
    .then((r) => {
      if (!r) return null
      if (r.shopId) app.globalData.setShopId(String(r.shopId))
      if (r.tableNo) app.globalData.setTableNo(String(r.tableNo))
      return { shopId: r.shopId ? String(r.shopId) : '', tableNo: r.tableNo ? String(r.tableNo) : '' }
    })
    .catch(() => null)
}

// 未登录跳登录页的防重入标记（避免 onLoad/onShow 重复触发）
let redirectingToLogin = false

/**
 * 统一的「扫码 / H5 深链 带上下文进入」路由处理（点餐、首页等顾客页均可复用）：
 *   1) 把路由里携带的 shopId/tableId/tableNo 立即写入 globalData 与 storage（供三要素登录态使用）；
 *   2) 顾客端未登录且已具备扫码上下文时，跳转到顾客登录页（自带防重入标记）；
 *   3) 已登录且上下文发生变化时，刷新三要素登录态（回写新 token）。
 * 仅在本函数未阻止（未登录会返回 false）时才允许继续发起需要登录/三要素的接口。
 * @param {object} options 页面 onLoad 的选项参数（{shopId, tableId, tableNo}）
 * @param {boolean} redirectIfNotLogin 未登录时是否跳登录页，默认 true
 * @returns {boolean} true=可继续请求；false=已触发跳登录/未登录，应停止请求
 */
export function ensureCustomerContext(options, redirectIfNotLogin = true) {
  const app = getApp()
  const query = options || {}
  let hasNew = false
  if (query.shopId) {
    const before = app.globalData.shopId || ''
    const isShopChange = before && String(query.shopId) !== before
    if (isShopChange) {
      // 切换到不同店铺：清空旧店铺的内存缓存，避免串店展示
      app.globalData.clearShopScopedCache()
    }
    app.globalData.setShopId(String(query.shopId))
    hasNew = true
  }
  if (query.tableId) {
    const beforeTableId = app.globalData.tableId || ''
    const isTableChange = beforeTableId && String(query.tableId) !== beforeTableId
    if (isTableChange) {
      // 换店或换桌：清掉残留桌号，交由页面按新 tableId 反查
      app.globalData.setTableNo('')
    }
    app.globalData.setTableId(String(query.tableId))
    hasNew = true
  }
  if (query.tableNo) {
    app.globalData.setTableNo(String(query.tableNo))
  }

  // 二维码可能只携带 tableId：异步二次反查桌位补齐桌号 tableNo（首次扫码/未登录也先缓存）
  if (query.tableId && !app.globalData.tableNo) {
    resolveTableNo(app, String(query.tableId))
  }

  // 未登录 + 顾客 + 已具备扫码上下文 -> 跳登录页（防重入）
  if (redirectIfNotLogin && !app.globalData.isLogin() &&
      app.globalData.role !== 'merchant' &&
      (app.globalData.shopId || app.globalData.tableId)) {
    if (!redirectingToLogin) {
      redirectingToLogin = true
      // 记录来源页，登录成功后回点餐/首页并携带上下文
      const pages = getCurrentPages()
      const page = pages.length ? pages[pages.length - 1] : null
      const route = (page && page.route) || 'pages/menu/menu'
      app.globalData.__loginBack = `/pages/${route}?shopId=${encodeURIComponent(app.globalData.shopId || '')}&tableId=${encodeURIComponent(app.globalData.tableId || '')}&tableNo=${encodeURIComponent(app.globalData.tableNo || '')}`
      setTimeout(() => {
        redirectingToLogin = false
        uni.reLaunch({ url: `/pages/login/login?role=customer&back=${encodeURIComponent(app.globalData.__loginBack || '')}` })
      }, 0)
    }
    return false
  }

  // 已登录且上下文变化 -> 刷新三要素登录态
  if (hasNew && app.globalData.isLogin()) {
    app.globalData.rebindCustomerSession()
  }
  return true
}

/**
 * 调起摄像头扫码 -> 解析店铺/桌号 -> (切换店铺时清空旧店缓存) -> 更新全局上下文与缓存 -> 重新绑定三要素登录态回写新 token
 * @param {object} app getApp()
 * @returns {Promise<{ok:boolean, changed:boolean, shopChanged:boolean, tableNo:string}>}
 */
export async function scanOrderContext(app) {
  let content = ''

  // H5（浏览器）走真实摄像头扫码
  // #ifdef H5
  const isWeb =
    typeof document !== 'undefined' &&
    uni.getSystemInfoSync().uniPlatform === 'web'
  if (isWeb) {
    content = await scanQRCodeInBrowser().catch(() => '')
    if (!content) {
      uni.showToast({ title: '扫码已取消', icon: 'none' })
      return { ok: false, changed: false, tableNo: '' }
    }
  }
  // #endif

  // 非浏览器（小程序/App等）走原生 uni.scanCode
  if (!content) {
    content = await new Promise((res) => {
      uni.scanCode({
        success: (r) => res(r.result || ''),
        fail: () => res('')
      })
    })
    if (!content) {
      uni.showToast({ title: '扫码已取消', icon: 'none' })
      return { ok: false, changed: false, tableNo: '' }
    }
  }

  const parsed = parseQrContent(content)
  if (!parsed.tableId) {
    uni.showToast({ title: '二维码不完整，请扫描桌位二维码', icon: 'none' })
    return { ok: false, changed: false, tableNo: '' }
  }
  // 二维码只带 tableId、或缺少 tableNo 时：二次反查桌位接口补齐 shopId 与桌号 tableNo
  if (!parsed.shopId || !parsed.tableNo) {
    try {
      const r = await api.getTableByCustomerId(parsed.tableId)
      if (r) {
        if (r.shopId) parsed.shopId = String(r.shopId)
        if (r.tableNo) parsed.tableNo = String(r.tableNo)
      }
    } catch (e) {
      uni.showToast({ title: '识别桌位失败，请重试', icon: 'none' })
      return { ok: false, changed: false, tableNo: '' }
    }
  }
  if (!parsed.shopId) {
    uni.showToast({ title: '无法识别所属店铺', icon: 'none' })
    return { ok: false, changed: false, tableNo: '' }
  }
  const shopChanged = Boolean(app.globalData.shopId && parsed.shopId !== app.globalData.shopId)
  const changed = shopChanged || parsed.tableId !== app.globalData.tableId
  // 切换店铺：先清空旧店铺的内存缓存（购物车/会员信息），避免串店展示
  if (shopChanged) {
    app.globalData.clearShopScopedCache()
  }
  app.globalData.setShopId(parsed.shopId)
  app.globalData.setTableId(parsed.tableId)
  if (parsed.tableNo) {
    app.globalData.setTableNo(parsed.tableNo)
  }
  // 刷新三要素登录态（Redis），服务端可能回写新 token
  if (changed && app.globalData.isLogin()) {
    app.globalData.rebindCustomerSession()
  }
  uni.showToast({
    title: parsed.tableNo ? `已识别桌号 ${parsed.tableNo}` : '已识别桌位，正在刷新…',
    icon: 'none'
  })
  return { ok: true, changed, shopChanged, tableNo: parsed.tableNo }
}