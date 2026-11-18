package com.itsworld.admin.service;

import com.itsworld.common.exception.BusinessException;
import com.itsworld.common.result.ErrorCode;
import com.itsworld.common.util.MinIOUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Iterator;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MiniProgramAssetService {
    private static final String BUCKET = "itsworld-static";
    private static final String WEBP = "image/webp";
    private static final int MAX_EDGE = 1080;
    private static final int MAX_PIXELS = 16_000_000;
    private static final int MAX_BYTES = 10 * 1024 * 1024;

    private final MinIOUtils minIO;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public Asset upload(MultipartFile file, String purpose) {
        if (!"onboarding".equals(purpose) || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PARAM);
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        BufferedImage source;
        try {
            source = readImage(file);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
        BufferedImage scaled = resize(source);
        byte[] bytes;
        try {
            bytes = encodeWebp(scaled);
        } catch (IOException | RuntimeException | LinkageError ex) {
            log.error("小程序素材 WebP 编码失败", ex);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR.getCode(), "WebP 转换失败");
        }

        String object = "miniapp-v1/" + purpose + "/" + UUID.randomUUID() + ".webp";
        String url = minIO.uploadToExistingBucket(BUCKET, object, new ByteArrayInputStream(bytes), WEBP);
        if (url == null) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR);
        }
        try {
            verifyPublicUrl(url, bytes.length);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            minIO.deleteFile(BUCKET, object);
            throw verificationFailure();
        } catch (IOException | IllegalArgumentException ex) {
            log.warn("小程序素材公开链接验证失败，object={}", object, ex);
            minIO.deleteFile(BUCKET, object);
            throw verificationFailure();
        }

        return new Asset(url, source.getWidth(), source.getHeight(), file.getSize(),
                scaled.getWidth(), scaled.getHeight(), bytes.length);
    }

    private BufferedImage readImage(MultipartFile file) throws IOException {
        try (var input = file.getInputStream(); var stream = new FileCacheImageInputStream(input, null)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName();
                if (!"png".equalsIgnoreCase(format) && !"jpeg".equalsIgnoreCase(format)) {
                    throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED.getCode(), "请选择 PNG 或 JPEG 图片");
                }
                reader.setInput(stream, true, true);
                if ((long) reader.getWidth(0) * reader.getHeight(0) > MAX_PIXELS) {
                    throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "图片尺寸超过 1600 万像素");
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        }
    }

    private BufferedImage resize(BufferedImage source) {
        double ratio = Math.min(1.0, (double) MAX_EDGE / Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * ratio));
        int height = Math.max(1, (int) Math.round(source.getHeight() * ratio));
        int type = source.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage scaled = new BufferedImage(width, height, type);
        Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return scaled;
    }

    private byte[] encodeWebp(BufferedImage image) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByMIMEType(WEBP).next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (var output = new MemoryCacheImageOutputStream(bytes)) {
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionType("Lossy");
            params.setCompressionQuality(0.82f);
            writer.setOutput(output);
            writer.write(null, new IIOImage(image, null, null), params);
        } finally {
            writer.dispose();
        }
        return bytes.toByteArray();
    }

    private void verifyPublicUrl(String url, int size) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<Void> response = http.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() != 200
                || !WEBP.equalsIgnoreCase(response.headers().firstValue("Content-Type").orElse(""))
                || response.headers().firstValueAsLong("Content-Length").orElse(-1) != size) {
            throw new IOException("公开地址校验失败，HTTP " + response.statusCode());
        }
    }

    private BusinessException verificationFailure() {
        return new BusinessException(ErrorCode.FILE_UPLOAD_ERROR.getCode(), "无法通过公开链接读取图片");
    }

    public record Asset(String url, int originalWidth, int originalHeight, long originalBytes,
                        int width, int height, long bytes) {
    }
}
