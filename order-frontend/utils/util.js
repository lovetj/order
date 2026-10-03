// utils/util.js
// 通用工具方法
const { baseUrl } = require('./config')

/**
 * 将后端返回的图片地址转换为可直接渲染的完整 URL。
 *
 * 后端返回的图片地址有两种形态：
 *   1. 完整 URL（http/https 开头，如 http://localhost:8082/file/shop/xxx.jpg）——直接使用；
 *   2. 相对路径（如 /shop/20261003/xxx.jpg）——拼上后端静态资源前缀 /file。
 *
 * @param {string} path 相对路径或完整 URL
 * @returns {string} 可直接用于 image src 的地址
 */
function formatImageUrl(path) {
  if (!path) return ''
  const p = String(path).trim()
  if (!p) return ''
  if (/^https?:\/\//i.test(p)) return p
  // 已是 /file 前缀的路径不再重复拼接
  if (p.startsWith('/file/')) return baseUrl + p
  // 统一补上前导斜杠，避免出现 baseUrl + 'shop/xxx.jpg'
  return baseUrl + '/file' + (p.startsWith('/') ? p : '/' + p)
}

/**
 * 批量转换图片地址（逗号分隔或数组）
 * @param {string|string[]} images
 * @returns {string[]} 完整地址数组
 */
function formatImageUrls(images) {
  if (!images) return []
  let list = images
  if (typeof images === 'string') {
    let str = images.trim()
    if (str.startsWith('[') && str.endsWith(']')) {
      str = str.slice(1, -1)
    }
    list = str.split(',')
  }
  if (!Array.isArray(list)) return []
  return list
    .map((s) => String(s).trim().replace(/^["']|["']$/g, ''))
    .filter(Boolean)
    .map(formatImageUrl)
}

/**
 * 将上传接口返回的地址统一转为「相对路径」用于入库。
 *
 * 后端可能返回：
 *   - url:          http://localhost:8082/file/shop/xxx.jpg
 *   - relativePath: /shop/xxx.jpg
 * 为保证数据库只存相对路径，这里优先取 relativePath；
 * 若只有完整 URL，则剥离 baseUrl 与 /file 前缀。
 *
 * @param {string} url          完整 URL
 * @param {string} relativePath 相对路径
 * @returns {string} 相对路径（如 /shop/20261003/xxx.jpg）
 */
function toRelativePath(url, relativePath) {
  if (relativePath) {
    const rp = String(relativePath).trim()
    if (rp) return rp.startsWith('/') ? rp : '/' + rp
  }
  if (!url) return ''
  const u = String(url).trim()
  if (!u) return ''
  // 去掉 baseUrl 前缀
  if (u.startsWith(baseUrl)) {
    const rest = u.slice(baseUrl.length)
    // 去掉 /file 前缀
    if (rest.startsWith('/file/')) return rest.slice('/file'.length)
    return rest
  }
  // 兜底：截取 /file/ 之后的部分
  const idx = u.indexOf('/file/')
  if (idx >= 0) return u.slice(idx + '/file'.length)
  // 已是相对路径
  if (!/^https?:\/\//i.test(u)) return u.startsWith('/') ? u : '/' + u
  return u
}

module.exports = {
  formatImageUrl,
  formatImageUrls,
  toRelativePath
}
