// utils/util.js
// 通用工具方法
import { baseUrl } from './config'

/**
 * 将图片地址转换为可直接渲染的完整 URL。
 *
 * 约定：后端所有图片字段出参（url / logo / images / avatar / image 等）已由
 * file.base-server 拼成完整可访问地址（如 https://host/order_backend/file/xxx.jpg），
 * 前端展示时直接采用接口字段即可，无需再拼接。
 *
 * 本函数对完整地址一律透传；仅对极少数历史相对路径（如 /shop/xxx.jpg）做兜底：
 * 拼上后端静态资源前缀 /file（与后端 WebConfig 的 /file/** 映射一致）。
 */
export function formatImageUrl(path) {
  if (!path) return ''
  const p = String(path).trim()
  if (!p) return ''
  if (/^https?:\/\//i.test(p)) return p
  if (p.startsWith('data:') || p.startsWith('blob:') || p.startsWith('/static/')) return p
  // 已是 /file 前缀的路径不再重复拼接（兜底兼容历史数据）
  if (p.startsWith('/file/')) return baseUrl + p
  // 统一补上前导斜杠，避免出现 baseUrl + 'shop/xxx.jpg'
  return baseUrl + '/file' + (p.startsWith('/') ? p : '/' + p)
}

/**
 * 批量转换图片地址（逗号分隔或数组）
 */
export function formatImageUrls(images) {
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
 */
export function toRelativePath(url, relativePath) {
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

export default {
  formatImageUrl,
  formatImageUrls,
  toRelativePath
}
