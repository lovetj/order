package com.order.util;

import org.springframework.util.StringUtils;

/**
 * 文件与图片访问地址公共处理工具（前后端文件路径统一规范）
 *
 * <p>规范约定：
 * 1. 数据库存储：统一只存储相对路径（如 {@code /dish/1/xxx.webp}、{@code /shop/1/xxx.webp}、{@code /avatar/user_1/xxx.webp}），
 *    绝不将域名、IP、端口或上下文前缀（如 {@code http://localhost:8081/order_file}、{@code /order_file}、{@code /file}）硬编码写入数据库；
 * 2. 接口出参：后端在返回给前端时，动态拼接 {@code file.base-server} 配置（如 {@code http://localhost:8081/order_file}）；
 * 3. 容错与兼容：
 *    - 若前端传入带 baseServer 或带 /order_file、/file 的绝对/相对 URL，入库前自动剥离，还原为纯相对路径；
 *    - emoji 或非图片文字（如 🍔、🍽️）保持原样，不进行错误拼接；
 *    - 外链图片（非本项目业务目录或外部网络图片）在必要时保留原样或提取相对路径。
 * </p>
 */
public final class FileUrlUtil {

    private FileUrlUtil() {
    }

    /**
     * 将任意形态的文件/图片地址转换为纯相对路径（用于落库保存）。
     */
    public static String toRelative(String path) {
        if (!StringUtils.hasText(path)) {
            return "";
        }
        String p = path.trim();
        if (!StringUtils.hasText(p)) {
            return "";
        }

        // 如果是绝对 URL (http:// 或 https://)
        if (isAbsoluteUrl(p)) {
            // 提取协议与域名之后的路径部分
            int slashIndex = p.indexOf("://");
            if (slashIndex > 0) {
                int pathIndex = p.indexOf('/', slashIndex + 3);
                if (pathIndex > 0) {
                    p = p.substring(pathIndex);
                } else {
                    return "";
                }
            }
        }

        // 统一斜杠
        p = p.replace('\\', '/');

        // 剥离可能存在的上下文前缀（如 /order_file 或 /file）
        if (p.startsWith("/order_file/")) {
            p = p.substring("/order_file".length());
        } else if (p.equals("/order_file")) {
            return "";
        } else if (p.startsWith("/file/")) {
            p = p.substring("/file".length());
        } else if (p.equals("/file")) {
            return "";
        }

        // 判断是否是 emoji 或非文件路径（不含斜杠且不含常见图片后缀）
        if (!p.contains("/") && !p.contains(".")) {
            return p;
        }

        return p.startsWith("/") ? p : "/" + p;
    }

    /**
     * 相对路径 -> 完整访问 URL（用于出参给前端）。
     *
     * @param path       相对路径（如 /dish/1/xxx.webp）或已是完整 URL
     * @param baseServer 文件访问基础地址，如 http://localhost:8081/order_file
     */
    public static String toAbsolute(String path, String baseServer) {
        if (!StringUtils.hasText(path)) {
            return "";
        }
        String p = path.trim();
        if (!StringUtils.hasText(p)) {
            return "";
        }
        // 如果已经是绝对 URL，直接返回
        if (isAbsoluteUrl(p)) {
            return p;
        }
        String base = StringUtils.hasText(baseServer) ? trimSlash(baseServer.trim()) : "";
        if (!StringUtils.hasText(base)) {
            return p.startsWith("/") ? p : "/" + p;
        }
        return base + (p.startsWith("/") ? p : "/" + p);
    }

    /**
     * 图片字段出参统一入口：仅当是有效图片/文件路径时才拼接完整地址，
     * emoji 等非图片文本（如 🍽️、🍔）原样返回，避免拼成无效 URL。
     */
    public static String toAbsoluteIfImage(String path, String baseServer) {
        if (!StringUtils.hasText(path)) {
            return path;
        }
        String p = path.trim();
        if (!p.startsWith("/") && !isAbsoluteUrl(p) && !p.contains("/")) {
            return path;
        }
        return toAbsolute(p, baseServer);
    }

    /**
     * 归一化逗号分隔的多图字段：逐个转相对路径后用逗号重新拼接（入库前调用）。
     * 入参兼容 JSON 数组字符串（[ "a", "b" ]）与普通逗号分隔。
     */
    public static String normalizeMulti(String images) {
        if (!StringUtils.hasText(images)) {
            return "";
        }
        String str = images.trim();
        if (str.startsWith("[") && str.endsWith("]")) {
            str = str.substring(1, str.length() - 1);
        }
        StringBuilder sb = new StringBuilder();
        for (String part : str.split(",")) {
            String item = part.trim().replaceAll("^[\"']|[\"']$", "");
            if (!StringUtils.hasText(item)) {
                continue;
            }
            String rel = toRelative(item);
            if (!StringUtils.hasText(rel)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(rel);
        }
        return sb.toString();
    }

    /**
     * 逗号分隔的多图字段：逐个转换为完整 URL 后用逗号拼接（出参前调用）。
     */
    public static String toAbsoluteMulti(String images, String baseServer) {
        if (!StringUtils.hasText(images)) {
            return "";
        }
        String normalized = normalizeMulti(images);
        if (!StringUtils.hasText(normalized)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String item : normalized.split(",")) {
            if (!StringUtils.hasText(item)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(toAbsolute(item, baseServer));
        }
        return sb.toString();
    }

    private static boolean isAbsoluteUrl(String url) {
        return url != null && url.length() > 8 && (url.regionMatches(true, 0, "http://", 0, 7)
                || url.regionMatches(true, 0, "https://", 0, 8));
    }

    private static String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
