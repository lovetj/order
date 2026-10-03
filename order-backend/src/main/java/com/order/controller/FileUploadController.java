package com.order.controller;

import com.order.common.Result;
import com.order.config.FileConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 文件上传 / 删除 —— 菜品图片 / 店铺图片等
 *
 * 图片按原图保存，不做任何压缩/转码，仅做两项校验：
 *   1. 扩展名必须属于 {@link #ALLOWED_EXT}；
 *   2. 单张大小不能超过 {@link #MAX_SIZE}（10MB）。
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
public class FileUploadController {

    private static final DateTimeFormatter DATE_DIR = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final String[] ALLOWED_EXT = { "jpg", "jpeg", "png", "gif", "webp", "bmp" };

    /** 单张图片大小上限：10MB */
    private static final long MAX_SIZE = 10L * 1024 * 1024;

    /**
     * 允许删除的路径格式：/{bizType}/{yyyyMMdd}/{32位UUID}.{ext}
     * 只允许删除本服务自己上传的文件，杜绝 ../ 路径穿越删到系统其它文件。
     */
    private static final Pattern STORED_PATH = Pattern.compile(
            "^/(dish|shop|avatar|other)/\\d{8}/[0-9a-f]{32}\\.(jpg|jpeg|png|gif|webp|bmp)$");

    @Autowired
    private FileConfigProperties fileConfigProperties;

    /**
     * 通用文件上传（单文件）
     *
     * @param file    文件
     * @param bizType 业务类型：dish / shop / avatar / other
     * @return { url, relativePath, fileName }
     */
    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file,
                                              @RequestParam(defaultValue = "other") String bizType) {
        if (file == null || file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }
        try {
            Map<String, String> data = storeOne(file, bizType);
            return Result.success(data);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 多文件上传
     *
     * @param files   文件列表
     * @param bizType 业务类型：dish / shop / avatar / other
     * @return 每个文件的结果列表：{ url, relativePath, fileName }
     */
    @PostMapping("/upload-batch")
    public Result<List<Map<String, String>>> uploadBatch(@RequestParam("files") MultipartFile[] files,
                                                         @RequestParam(defaultValue = "other") String bizType) {
        if (files == null || files.length == 0) {
            return Result.error("上传文件不能为空");
        }
        List<Map<String, String>> list = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            try {
                list.add(storeOne(file, bizType));
            } catch (IllegalArgumentException e) {
                // 校验类错误（格式不支持 / 超过 10MB）直接反馈给前端，避免整批静默丢失
                return Result.error(e.getMessage());
            } catch (IOException e) {
                log.error("批量上传失败: {}", file.getOriginalFilename(), e);
                return Result.error("文件上传失败：" + e.getMessage());
            }
        }
        if (list.isEmpty()) {
            return Result.error("上传文件不能为空");
        }
        return Result.success(list);
    }

    /**
     * 删除已上传的图片（真实删除磁盘文件）
     *
     * @param path 相对路径，如 /shop/20261003/xxx.jpg
     */
    @DeleteMapping("/delete")
    public Result<Void> delete(@RequestParam("path") String path) {
        if (!StringUtils.hasText(path)) {
            return Result.error("文件地址不能为空");
        }
        String relative = path.trim();
        if (!STORED_PATH.matcher(relative).matches()) {
            return Result.error("非法的文件地址");
        }

        String basePath = fileConfigProperties.getBasePath();
        if (!StringUtils.hasText(basePath)) {
            return Result.error("文件存储目录未配置");
        }

        // 二次校验：解析后的绝对路径必须仍在存储目录内
        Path root = Paths.get(basePath).toAbsolutePath().normalize();
        Path target = root.resolve(relative.substring(1)).normalize();
        if (!target.startsWith(root)) {
            return Result.error("非法的文件地址");
        }

        try {
            boolean deleted = Files.deleteIfExists(target);
            log.info("[file:delete] path={}, deleted={}", relative, deleted);
        } catch (IOException e) {
            log.error("文件删除失败: {}", relative, e);
            return Result.error("文件删除失败");
        }
        return Result.success();
    }

    /** 校验并落盘单个文件（原图保存，不做压缩） */
    private Map<String, String> storeOne(MultipartFile file, String bizType) throws IOException {
        String originalName = file.getOriginalFilename();
        String ext = getExtension(originalName);
        if (!isAllowed(ext)) {
            throw new IllegalArgumentException("仅支持 " + String.join("/", ALLOWED_EXT) + " 格式的图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("图片大小不能超过 10MB");
        }

        String basePath = fileConfigProperties.getBasePath();
        if (!StringUtils.hasText(basePath)) {
            throw new IOException("文件存储目录未配置");
        }

        // 按业务类型 + 日期分目录
        String dateDir = LocalDate.now().format(DATE_DIR);
        String subDir = sanitizeBizType(bizType) + File.separator + dateDir;
        Path targetDir = Paths.get(basePath, subDir);
        Files.createDirectories(targetDir);

        // 原样保存：不压缩、不转码，保留原始格式与画质
        byte[] content = file.getBytes();
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + normalizeExt(ext);
        File dest = targetDir.resolve(fileName).toFile();
        Files.write(dest.toPath(), content);

        // 统一存相对路径，前端通过 baseUrl 拼接访问（后端静态资源映射 /file/**）
        String relativePath = "/" + subDir.replace(File.separatorChar, '/') + "/" + fileName;
        String baseServer = fileConfigProperties.getBaseServer();
        String url = StringUtils.hasText(baseServer) ? trimSlash(baseServer) + relativePath : relativePath;

        Map<String, String> data = new HashMap<>();
        data.put("relativePath", relativePath);
        data.put("url", url);
        data.put("fileName", fileName);
        data.put("originalName", originalName);
        data.put("size", String.valueOf(content.length));
        return data;
    }

    /** 规范化扩展名：jpeg -> jpg，其余保持原样 */
    private String normalizeExt(String ext) {
        return "jpeg".equals(ext) ? "jpg" : ext;
    }

    private String getExtension(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    private boolean isAllowed(String ext) {
        for (String allow : ALLOWED_EXT) {
            if (allow.equals(ext)) {
                return true;
            }
        }
        return false;
    }

    /** 白名单业务类型，防止路径穿越 */
    private String sanitizeBizType(String bizType) {
        if (!StringUtils.hasText(bizType)) {
            return "other";
        }
        String type = bizType.trim().toLowerCase();
        if (type.equals("dish") || type.equals("shop") || type.equals("avatar")) {
            return type;
        }
        return "other";
    }

    private String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
