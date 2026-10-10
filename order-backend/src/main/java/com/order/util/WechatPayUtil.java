package com.order.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class WechatPayUtil {

    private static final String SCHEMA = "WECHATPAY2-SHA256-RSA2048";
    private static final Map<String, PrivateKey> PRIVATE_KEY_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, PublicKey> PUBLIC_KEY_CACHE = new ConcurrentHashMap<>();

    public static PrivateKey loadPrivateKey(String privateKeyPath) throws Exception {
        if (!StringUtils.hasText(privateKeyPath)) {
            throw new IllegalArgumentException("微信支付私钥路径 privateKeyPath 不能为空");
        }
        PrivateKey cached = PRIVATE_KEY_CACHE.get(privateKeyPath);
        if (cached != null) {
            return cached;
        }
        String keyStr = readCertContent(privateKeyPath, "私钥");
        String cleanKey = keyStr
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(cleanKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

        PRIVATE_KEY_CACHE.put(privateKeyPath, privateKey);
        return privateKey;
    }

    public static PublicKey loadPublicKey(String publicKeyPath) throws Exception {
        if (!StringUtils.hasText(publicKeyPath)) {
            throw new IllegalArgumentException("平台公钥路径 platformCertPath 不能为空");
        }
        PublicKey cached = PUBLIC_KEY_CACHE.get(publicKeyPath);
        if (cached != null) {
            return cached;
        }
        String keyStr = readCertContent(publicKeyPath, "平台公钥");
        String cleanKey = keyStr
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(cleanKey);
        PublicKey publicKey;
        try {
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
            publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec);
        } catch (Exception e) {
            java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
            java.security.cert.Certificate cert = cf.generateCertificate(new java.io.ByteArrayInputStream(keyStr.getBytes(StandardCharsets.UTF_8)));
            publicKey = cert.getPublicKey();
        }

        PUBLIC_KEY_CACHE.put(publicKeyPath, publicKey);
        return publicKey;
    }

    private static String readCertContent(String path, String desc) throws Exception {
        byte[] bytes;
        if (path.startsWith("classpath:")) {
            String cp = path.substring("classpath:".length()).trim();
            if (cp.startsWith("/")) {
                cp = cp.substring(1);
            }
            ClassPathResource res = new ClassPathResource(cp);
            if (!res.exists()) {
                throw new IllegalStateException("classpath 下未找到" + desc + "文件: " + cp);
            }
            try (InputStream in = res.getInputStream()) {
                bytes = FileCopyUtils.copyToByteArray(in);
            }
        } else {
            File f = new File(path);
            if (!f.exists() || !f.isFile()) {
                throw new IllegalStateException("磁盘路径未找到" + desc + "文件: " + path);
            }
            try (InputStream in = new FileInputStream(f)) {
                bytes = FileCopyUtils.copyToByteArray(in);
            }
        }
        return new String(bytes, StandardCharsets.UTF_8).trim();
    }

    public static String buildAuthorization(String mchId, String certSerialNo, PrivateKey privateKey,
                                            String method, String urlPath, String body) throws Exception {
        long timestamp = System.currentTimeMillis() / 1000;
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String message = method + "\n" + urlPath + "\n" + timestamp + "\n" + nonce + "\n" + (body == null ? "" : body) + "\n";
        String signature = signRsa(privateKey, message);
        return SCHEMA + " mchid=\"" + mchId + "\",nonce_str=\"" + nonce + "\",timestamp=\"" + timestamp + "\",serial_no=\"" + certSerialNo + "\",signature=\"" + signature + "\"";
    }

    public static String signRsa(PrivateKey privateKey, String message) throws Exception {
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(privateKey);
        signer.update(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signer.sign());
    }

    public static boolean verifyRsa(PublicKey publicKey, String message, String signature) {
        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);
            verifier.update(message.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getDecoder().decode(signature));
        } catch (Exception e) {
            log.warn("[微信验签] 验签异常: {}", e.getMessage());
            return false;
        }
    }

    public static String decryptAesGcm(String apiV3Key, String associatedData, String nonce, String ciphertext) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(apiV3Key.getBytes(StandardCharsets.UTF_8), "AES");
        GCMParameterSpec spec = new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8));
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);
        if (StringUtils.hasText(associatedData)) {
            cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
        }
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
