package com.order.util;

import org.springframework.util.StringUtils;

/**
 * 图片地址公共处理工具（前后端文件路径统一约定）
 *
 * <p>数据库中统一只存「相对路径」，例如 {@code /shop/20261003/xxx.jpg}；
 * 对外返回 / 前端渲染时再拼接 {@code file.base-server}（即 {@code http://host:port/file}）。</p>
 *
 * <p>之所以要统一，是因为历史数据 / 不同上传入口（菜品、店铺、头像）可能写入的是
 * 完整 URL（{@code http://localhost:8082/file/shop/xxx.jpg}）。若不做归一化，
 * 回显时会出现「重复拼接 baseUrl」或「相对路径无法访问」等问题。</p>
 *
 * 本工具同时提供：
 * <ul>
 *   <li>{@link #toRelative(String)}：任意形态 -> 相对路径（入库前调用）</li>
 *   <li>{@link #toAbsolute(String, String)}：相对路径 -> 完整 URL（出参前调用）</li>
 *   <li>{@link #normalizeMulti(String)}：逗号分隔的多图字段批量归一化（入库前调用）</li>
 * </ul>
 */
public final class FileUrlUtil {

    private FileUrlUtil() {
    }

    /**
     * 任意图片地址 -> 相对路径（保证以 / 开头，形如 /shop/20261003/xxx.jpg）。
     * 完整 URL 会剥离 baseServer 与 /file 前缀；空值返回空串。
     */
    public static String toRelative(String path) {
        if (!StringUtils.hasText(path)) {
            return "";
        }
        String p = path.trim();
        if (!StringUtils.hasText(p)) {
            return "";
        }
        // 非 http(s) 地址：本身已是相对路径，仅补前导斜杠
        if (!isAbsoluteUrl(p)) {
            return p.startsWith("/") ? p : "/" + p;
        }
        // 完整 URL 但不是本站文件（外链图片）：原样保留，避免破坏数据
        return p;
    }

    /**
     * 相对路径 -> 完整 URL。
     *
     * @param path       相对路径（或已是完整 URL）
     * @param baseServer 文件访问基础地址，如 http://localhost:8082/file
     */
    public static String toAbsolute(String path, String baseServer) {
        if (!StringUtils.hasText(path)) {
            return "";
        }
        String p = path.trim();
        if (!StringUtils.hasText(p)) {
            return "";
        }
        // 已是完整 URL（含外链）直接返回
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
     * 图片字段出参统一入口：仅当是图片路径（http(s) 开头或 / 开头的相对路径）时才拼接完整地址，
     * emoji 等非图片文本（如菜品占位 🍽️）原样返回，避免被拼成无效的图片 URL。
     */
    public static String toAbsoluteIfImage(String path, String baseServer) {
        if (!StringUtils.hasText(path)) {
            return path;
        }
        String p = path.trim();
        if (!p.startsWith("/") && !isAbsoluteUrl(p)) {
            return path;
        }
        return toAbsolute(path, baseServer);
    }

    /**
     * 归一化逗号分隔的多图字段：逐个转相对路径后用逗号重新拼接。
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
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(toRelative(item));
        }
        return sb.toString();
    }

    private static boolean isAbsoluteUrl(String url) {
        return url.length() > 8 && (url.regionMatches(true, 0, "http://", 0, 7)
                || url.regionMatches(true, 0, "https://", 0, 8));
    }

    private static String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
