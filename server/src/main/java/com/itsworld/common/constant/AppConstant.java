package com.itsworld.common.constant;

/**
 * 系统全局常量
 * 包括JWT、Redis、MinIO、分页等配置
 *
 * @author Claude
 * @date 2026-10-30
 */
public class AppConstant {

    // ==================== JWT 相关 ====================

    /**
     * JWT请求头名称
     */
    public static final String JWT_HEADER = "Authorization";

    /**
     * JWT令牌前缀
     */
    public static final String JWT_PREFIX = "Bearer ";

    /**
     * JWT过期时间（秒）：24小时
     */
    public static final long JWT_EXPIRATION = 86400;

    /**
     * JWT刷新令牌过期时间（秒）：7天
     */
    public static final long JWT_REFRESH_EXPIRATION = 604800;

    // ==================== Redis 相关 ====================

    /**
     * Redis Key前缀：token黑名单
     */
    public static final String REDIS_PREFIX_TOKEN_BLACKLIST = "token:blacklist:";

    /**
     * Redis Key前缀：用户信息缓存
     */
    public static final String REDIS_PREFIX_USER_INFO = "user:info:";

    /**
     * Redis Key前缀：登录尝试计数（防暴力破解）
     */
    public static final String REDIS_PREFIX_LOGIN_ATTEMPT = "login:attempt:";

    // ==================== MinIO 相关 ====================

    /**
     * MinIO Bucket：用户头像
     */
    public static final String MINIO_BUCKET_AVATARS = "itsworld-avatars";

    /**
     * MinIO Bucket：日记图片
     */
    public static final String MINIO_BUCKET_DIARY = "itsworld-diary";

    /**
     * MinIO Bucket：社区内容
     */
    public static final String MINIO_BUCKET_COMMUNITY = "itsworld-community";

    /**
     * MinIO Bucket：临时文件
     */
    public static final String MINIO_BUCKET_TEMP = "itsworld-temp";

    /**
     * MinIO预签名URL过期时间（秒）：1小时
     */
    public static final int MINIO_PRESIGNED_URL_EXPIRATION = 3600;

    // ==================== 分页相关 ====================

    /**
     * 默认每页条数
     */
    public static final int PAGE_SIZE_DEFAULT = 20;

    /**
     * 默认第一页
     */
    public static final int PAGE_NUM_DEFAULT = 1;

    /**
     * 最大每页条数（防止过大查询）
     */
    public static final int PAGE_SIZE_MAX = 100;

    // ==================== 文件上传相关 ====================

    /**
     * 最大文件大小：50MB
     */
    public static final long MAX_FILE_SIZE = 52428800L;

    /**
     * 单次上传最多照片数
     */
    public static final int MAX_PHOTOS_PER_UPLOAD = 10;

    /**
     * 允许的文件扩展名
     */
    public static final String[] ALLOWED_EXTENSIONS = {"jpg", "jpeg", "png", "gif", "webp"};

    // ==================== 密码相关 ====================

    /**
     * 密码最小长度：8个字符
     */
    public static final int PASSWORD_MIN_LENGTH = 8;

    /**
     * 密码最大长度：128个字符
     */
    public static final int PASSWORD_MAX_LENGTH = 128;

    // ==================== 用户相关 ====================

    /**
     * 用户名最小长度
     */
    public static final int USERNAME_MIN_LENGTH = 3;

    /**
     * 用户名最大长度
     */
    public static final int USERNAME_MAX_LENGTH = 50;

    /**
     * 用户启用状态
     */
    public static final int USER_STATUS_ENABLED = 1;

    /**
     * 用户禁用状态
     */
    public static final int USER_STATUS_DISABLED = 0;

    // ==================== 宠物相关 ====================

    /**
     * 用户最多可拥有的宠物数
     */
    public static final int MAX_PETS_PER_USER = 10;

    // ==================== 日记相关 ====================

    /**
     * 每个宠物一天最多一条日记
     */
    public static final int MAX_DIARY_PER_DAY = 1;

}
