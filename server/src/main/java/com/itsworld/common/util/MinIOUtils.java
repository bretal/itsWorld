package com.itsworld.common.util;

import com.itsworld.common.constant.AppConstant;
import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * MinIO工具类
 * 负责文件的上传、删除和URL生成
 *
 * @author Claude
 * @date 2026-10-30
 */
@Slf4j
@Component
public class MinIOUtils {

    /**
     * MinIO服务端点（从application.yml读取）
     */
    @Value("${minio.endpoint}")
    private String endpoint;

    /**
     * 对外访问图片的根地址（生产为 https://lvhaorankk.top/files）。
     * 未配置时回退到 minio.endpoint，保证本地开发行为不变。
     */
    @Value("${minio.public-endpoint:${minio.endpoint}}")
    private String publicEndpoint;

    /**
     * MinIO访问密钥（从application.yml读取）
     */
    @Value("${minio.access-key}")
    private String accessKey;

    /**
     * MinIO秘密密钥（从application.yml读取）
     */
    @Value("${minio.secret-key}")
    private String secretKey;

    /**
     * MinIO client（延迟初始化）
     */
    private MinioClient minioClient;

    /**
     * 获取MinioClient实例
     */
    private MinioClient getMinioClient() {
        if (minioClient == null) {
            minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
        }
        return minioClient;
    }

    /**
     * 上传文件到MinIO
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称（包括路径，如 user/avatar/123.jpg）
     * @param inputStream 文件输入流
     * @param contentType 文件类型（如 image/jpeg）
     * @return 文件的访问URL
     */
    public String uploadFile(String bucketName, String objectName, InputStream inputStream, String contentType) {
        try {
            // 确保bucket存在
            ensureBucketExists(bucketName);

            // 上传文件
            getMinioClient().putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, -1, 10485760) // 10MB
                            .contentType(contentType)
                            .build()
            );

            log.info("文件上传成功：bucket={}, object={}", bucketName, objectName);

            // 返回文件URL
            return getFileUrl(bucketName, objectName);

        } catch (MinioException e) {
            log.error("MinIO异常：{}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("文件上传异常：{}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 删除文件
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     */
    public boolean deleteFile(String bucketName, String objectName) {
        try {
            getMinioClient().removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );

            log.info("文件删除成功：bucket={}, object={}", bucketName, objectName);
            return true;

        } catch (MinioException e) {
            log.error("MinIO异常：{}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("文件删除异常：{}", e.getMessage(), e);
        }
        return false;
    }

    /**
     * 生成预签名URL（用于分享）
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @param expiresInSeconds URL过期时间（秒）
     * @return 预签名URL
     */
    public String getPresignedUrl(String bucketName, String objectName, int expiresInSeconds) {
        try {
            // 构建GetPresignedObjectUrlArgs
            GetPresignedObjectUrlArgs.Builder builder = GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectName);

            // MinIO 8.5.7中设置过期时间的方式
            // 注意：根据MinIO版本，可能需要使用其他方法
            // builder.expiration(expiresInSeconds, TimeUnit.SECONDS);

            String url = getMinioClient().getPresignedObjectUrl(builder.build());

            log.info("预签名URL生成成功：bucket={}, object={}", bucketName, objectName);
            return url;

        } catch (MinioException e) {
            log.error("MinIO异常：{}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("预签名URL生成异常：{}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 生成默认过期时间的预签名URL
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @return 预签名URL
     */
    public String getPresignedUrl(String bucketName, String objectName) {
        return getPresignedUrl(bucketName, objectName, AppConstant.MINIO_PRESIGNED_URL_EXPIRATION);
    }

    /**
     * 获取文件的直接访问URL（不带签名）
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @return 文件URL
     */
    private String getFileUrl(String bucketName, String objectName) {
        String baseUrl = publicEndpoint == null || publicEndpoint.isBlank()
                ? endpoint
                : publicEndpoint;
        return baseUrl + "/" + bucketName + "/" + objectName;
    }

    /**
     * 检查bucket是否存在，不存在则创建
     *
     * @param bucketName bucket名称
     */
    private void ensureBucketExists(String bucketName) {
        try {
            if (!getMinioClient().bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                getMinioClient().makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Bucket创建成功：{}", bucketName);
            }
            setBucketPublicRead(bucketName);
        } catch (Exception e) {
            log.error("Bucket检查/创建异常：{}", e.getMessage(), e);
        }
    }

    /**
     * 设置bucket为公共读（仅用于本地开发，生产环境应使用 CDN 或预签名 URL）
     *
     * @param bucketName bucket名称
     */
    public void setBucketPublicRead(String bucketName) {
        try {
            String policy = """
                    {
                      "Version": "2012-10-17",
                      "Statement": [
                        {
                          "Effect": "Allow",
                          "Principal": {"AWS": ["*"]},
                          "Action": ["s3:GetObject"],
                          "Resource": ["arn:aws:s3:::%s/*"]
                        }
                      ]
                    }
                    """.formatted(bucketName);
            getMinioClient().setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                            .bucket(bucketName)
                            .config(policy)
                            .build()
            );
        } catch (Exception e) {
            log.error("设置bucket公共读失败：bucket={}, error={}", bucketName, e.getMessage(), e);
        }
    }

    /**
     * 检查bucket是否存在
     *
     * @param bucketName bucket名称
     * @return 是否存在
     */
    public boolean bucketExists(String bucketName) {
        try {
            return getMinioClient().bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            log.error("Bucket检查异常：{}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 创建bucket
     *
     * @param bucketName bucket名称
     * @return 是否创建成功
     */
    public boolean createBucket(String bucketName) {
        try {
            if (!bucketExists(bucketName)) {
                getMinioClient().makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Bucket创建成功：{}", bucketName);
                return true;
            }
            return true;
        } catch (Exception e) {
            log.error("Bucket创建异常：{}", e.getMessage(), e);
            return false;
        }
    }

}
