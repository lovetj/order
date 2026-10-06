// H5（浏览器）端摄像头扫码：全屏遮罩 + 实时解码二维码
import { Html5Qrcode } from 'html5-qrcode'

/**
 * 浏览器摄像头扫码，返回 Promise<string>（解码出的二维码文本）。
 * - 动态创建全屏遮罩（含取消按钮、提示、<video> 预览），z-index 置顶阻断后台交互
 * - 优先使用后置摄像头实时循环解码
 * - 解码成功 / 取消 / 权限被拒 / 失败 均会停流并清理 DOM
 */
export function scanQRCodeInBrowser() {
  return new Promise((resolve, reject) => {
    if (
      typeof document === 'undefined' ||
      !navigator.mediaDevices ||
      !navigator.mediaDevices.getUserMedia
    ) {
      reject(new Error('当前浏览器不支持摄像头扫码'))
      return
    }

    let overlay = null
    let html5Qr = null
    let stopped = false

    const cleanup = () => {
      if (stopped) return
      stopped = true
      if (html5Qr) {
        try {
          if (html5Qr.isScanning) html5Qr.stop().catch(() => {})
        } catch (e) {
          /* ignore */
        }
      }
      if (overlay && overlay.parentNode) {
        overlay.parentNode.removeChild(overlay)
      }
      overlay = null
      html5Qr = null
    }

    // 构建遮罩层
    overlay = document.createElement('div')
    overlay.style.cssText =
      'position:fixed;top:0;left:0;width:100vw;height:100vh;z-index:999999;' +
      'background:rgba(0,0,0,0.92);display:flex;flex-direction:column;' +
      'align-items:center;justify-content:center;box-sizing:border-box;margin:0;padding:0;'

    const cancelBtn = document.createElement('div')
    cancelBtn.textContent = '取消'
    cancelBtn.style.cssText =
      'color:#fff;font-size:17px;line-height:1;padding:10px 26px;border:1px solid #fff;' +
      'border-radius:22px;position:absolute;top:44px;right:18px;cursor:pointer;z-index:20;' +
      'user-select:none;background:rgba(255,255,255,0.12);'
    cancelBtn.addEventListener('click', () => {
      cleanup()
      reject(new Error('cancelled'))
    })

    const hint = document.createElement('div')
    hint.textContent = '请将桌位二维码对准取景框'
    hint.style.cssText = 'color:#eee;font-size:16px;margin-bottom:18px;z-index:10;'

    const container = document.createElement('div')
    container.id = 'h5-qrcode-reader'
    container.style.cssText =
      'width:80vw;max-width:340px;height:80vw;max-height:340px;position:relative;' +
      'overflow:hidden;background:#000;border-radius:12px;z-index:10;'

    overlay.appendChild(cancelBtn)
    overlay.appendChild(hint)
    overlay.appendChild(container)
    document.body.appendChild(overlay)

    html5Qr = new Html5Qrcode('h5-qrcode-reader', { verbose: false })

    html5Qr
      .start(
        { facingMode: { exact: 'environment' } },
        { fps: 10, qrbox: { width: 220, height: 220 }, aspectRatio: 1.0 },
        (decodedText) => {
          // 解码成功
          const text = decodedText || ''
          cleanup()
          resolve(text)
        },
        () => {
          /* 每帧未识别到，忽略 */
        }
      )
      .catch((err) => {
        if (stopped) return
        cleanup()
        reject(err)
      })
  })
}