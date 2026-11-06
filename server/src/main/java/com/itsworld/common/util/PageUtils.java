package com.itsworld.common.util;

/**
 * 分页参数工具
 * 统一页码、每页条数的默认值与上限，避免客户端传入过大 size。
 *
 * @author Claude
 * @date 2026-11-05
 */
public final class PageUtils {

    /**
     * 默认页码
     */
    public static final long DEFAULT_PAGE_NUM = 1L;

    /**
     * 默认每页条数
     */
    public static final long DEFAULT_PAGE_SIZE = 20L;

    /**
     * 每页条数上限
     */
    public static final long MAX_PAGE_SIZE = 100L;

    private PageUtils() {
    }

    /**
     * 归一化页码
     *
     * @param pageNum 原始页码
     * @return 合法页码
     */
    public static long normalizePageNum(Long pageNum) {
        if (pageNum == null || pageNum < DEFAULT_PAGE_NUM) {
            return DEFAULT_PAGE_NUM;
        }
        return pageNum;
    }

    /**
     * 归一化每页条数
     *
     * @param pageSize 原始每页条数
     * @return 合法每页条数
     */
    public static long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

}
