package com.itsworld.common.controller;

import com.itsworld.common.exception.BusinessException;
import com.itsworld.common.result.ErrorCode;
import com.itsworld.common.result.Result;
import com.itsworld.common.util.MinIOUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 文件上传控制器
 * 提供头像、宠物头像、日记照片、社区图片的统一上传入口
 *
 * @author Claude
 * @date 2026-11-04
 */
@Slf4j
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private static final String TYPE_AVATAR = "avatar";
    private static final String TYPE_PET = "pet";
    private static final String TYPE_DIARY = "diary";
    private static final String TYPE_COMMUNITY = "community";
    private static final String TYPE_STORE = "store";
    private static final String TYPE_SHOP = "shop";
    private static final String TYPE_REVIEW = "review";

    private final MinIOUtils minIOUtils;
    private final com.itsworld.member.service.MemberTaskService memberTaskService;
    private final com.itsworld.security.service.CurrentUserService currentUserService;

    @Value("${minio.bucket.avatars}")
    private String avatarBucket;

    @Value("${minio.bucket.diary}")
    private String diaryBucket;

    @Value("${minio.bucket.community}")
    private String communityBucket;

    @Value("${app.upload.max-file-size:52428800}")
    private long maxFileSize;

    @Value("${app.upload.allowed-extensions:jpg,jpeg,png,gif,webp}")
    private String allowedExtensions;

    /**
     * 上传图片
     *
     * @param file 图片文件
     * @param type 上传类型：avatar / pet / diary / community / store / shop / review
     * @return 图片访问 URL
     */
    @PostMapping
    public Result<String> upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam(value = "type", defaultValue = TYPE_AVATAR) String type) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "文件不能为空");
        }
        if (file.getSize() > maxFileSize) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        String extension = getExtension(file.getOriginalFilename());
        List<String> allowed = Arrays.asList(allowedExtensions.split(","));
        if (!allowed.contains(extension)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }

        String bucketName = resolveBucket(type);
        String objectName = buildObjectName(type, extension);

        try (InputStream inputStream = file.getInputStream()) {
            String url = minIOUtils.uploadFile(bucketName, objectName, inputStream, file.getContentType());
            if (url == null) {
                throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR);
            }
            log.info("图片上传成功：type={}, object={}", type, objectName);
            if (TYPE_PET.equals(type) || TYPE_DIARY.equals(type)) {
                memberTaskService.recordProgress(currentUserService.getUserId(), "PHOTO_UPLOAD");
            }
            return Result.success(url, "上传成功");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("图片上传异常：type={}, error={}", type, e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR);
        }
    }

    private String resolveBucket(String type) {
        return switch (type) {
            case TYPE_AVATAR, TYPE_PET -> avatarBucket;
            case TYPE_DIARY -> diaryBucket;
            case TYPE_COMMUNITY, TYPE_STORE, TYPE_SHOP, TYPE_REVIEW -> communityBucket;
            default -> throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "不支持的上传类型");
        };
    }

    private String buildObjectName(String type, String extension) {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return type + "/" + date + "/" + uuid + "." + extension;
    }

    private String getExtension(String fileName) {
        if (fileName == null) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
        return fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

}
