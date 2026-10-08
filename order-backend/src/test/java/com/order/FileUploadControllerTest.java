package com.order;

import com.order.common.Result;
import com.order.config.FileConfigProperties;
import com.order.controller.FileUploadController;
import com.order.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FileUploadControllerTest {

    private FileUploadController controller;
    private FileConfigProperties properties;
    private JwtUtil jwtUtil;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setup() {
        controller = new FileUploadController();
        properties = new FileConfigProperties();
        properties.setBasePath(tempDir.toAbsolutePath().toString());
        properties.setBaseServer("http://localhost:8082/file");

        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "0123456789abcdef0123456789abcdef0123456789abcdef");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);
        jwtUtil.init();

        ReflectionTestUtils.setField(controller, "fileConfigProperties", properties);
        ReflectionTestUtils.setField(controller, "jwtUtil", jwtUtil);
    }

    private byte[] createTestImage(int width, int height, String format) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.WHITE);
        g.drawString("ORDER_TEST_IMAGE", 50, 50);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }

    @Test
    public void testScaleLargeImageTo750WidthAndCompress() throws IOException {
        // 创建 1800x1200 宽图
        byte[] imageBytes = createTestImage(1800, 1200, "jpg");
        MockMultipartFile file = new MockMultipartFile("file", "large_dish.jpg", "image/jpeg", imageBytes);

        Result<Map<String, String>> res = controller.upload(
                file, "dish", "1", null, null, null, null, null, null);

        assertNotNull(res);
        assertEquals(200, res.getCode());
        Map<String, String> data = res.getData();
        assertNotNull(data);
        assertEquals("750", data.get("width"));
        assertEquals("500", data.get("height"));
        assertTrue(data.get("relativePath").startsWith("/dish/1/"));
        assertTrue(data.get("url").startsWith("http://localhost:8082/file/dish/1/"));

        // 验证磁盘文件与尺寸
        String rel = data.get("relativePath");
        File diskFile = tempDir.resolve(rel.substring(1)).toFile();
        assertTrue(diskFile.exists());

        BufferedImage readBack = ImageIO.read(diskFile);
        assertEquals(750, readBack.getWidth());
        assertEquals(500, readBack.getHeight());

        // 验证文件体积得到有效压缩
        int compressedSize = Integer.parseInt(data.get("size"));
        assertTrue(compressedSize > 0);
        System.out.println("Original size: " + imageBytes.length + " bytes, Compressed size: " + compressedSize + " bytes");
    }

    @Test
    public void testKeepSmallImageOriginalDimensions() throws IOException {
        // 创建 400x300 小图
        byte[] imageBytes = createTestImage(400, 300, "jpg");
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", imageBytes);

        Result<Map<String, String>> res = controller.upload(
                file, "avatar", null, "888", null, null, null, "888", null);

        assertNotNull(res);
        assertEquals(200, res.getCode());
        Map<String, String> data = res.getData();
        assertEquals("400", data.get("width"));
        assertEquals("300", data.get("height"));
        assertTrue(data.get("relativePath").startsWith("/avatar/user_888/"));
    }

    @Test
    public void testMerchantAvatarPath() throws IOException {
        String token = jwtUtil.generateToken("admin1", "admin", "merchant", "12");
        byte[] imageBytes = createTestImage(300, 300, "jpg");
        MockMultipartFile file = new MockMultipartFile("file", "merchant_avatar.jpg", "image/jpeg", imageBytes);

        Result<Map<String, String>> res = controller.upload(
                file, "avatar", null, null, null, "Bearer " + token, null, null, null);

        assertNotNull(res);
        assertEquals(200, res.getCode());
        Map<String, String> data = res.getData();
        assertTrue(data.get("relativePath").startsWith("/avatar/shop_12/"));
    }

    @Test
    public void testTableDirectoryPath() throws IOException {
        byte[] imageBytes = createTestImage(600, 400, "jpg");
        MockMultipartFile file = new MockMultipartFile("file", "table_qr.jpg", "image/jpeg", imageBytes);

        Result<Map<String, String>> res = controller.upload(
                file, "table", "10", null, "T101", null, null, null, null);

        assertNotNull(res);
        Map<String, String> data = res.getData();
        assertTrue(data.get("relativePath").startsWith("/table/10/T101/"));
    }

    @Test
    public void testDeleteFile() throws IOException {
        byte[] imageBytes = createTestImage(600, 400, "jpg");
        MockMultipartFile file = new MockMultipartFile("file", "del.jpg", "image/jpeg", imageBytes);

        Result<Map<String, String>> res = controller.upload(
                file, "shop", "5", null, null, null, null, null, null);
        String rel = res.getData().get("relativePath");

        File diskFile = tempDir.resolve(rel.substring(1)).toFile();
        assertTrue(diskFile.exists());

        Result<Void> delRes = controller.delete(rel);
        assertEquals(200, delRes.getCode());
        assertFalse(diskFile.exists());
    }
}