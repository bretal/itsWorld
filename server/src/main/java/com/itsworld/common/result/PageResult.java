package com.itsworld.common.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页数据载荷
 * 由 Result 包装后返回，例如 Result&lt;PageResult&lt;PetResponse&gt;&gt;
 *
 * @param <T> 列表元素类型
 * @author Claude
 * @date 2026-11-05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResult<T> {

    /**
     * 当前页数据
     */
    private List<T> records;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码（从 1 开始）
     */
    private Long pageNum;

    /**
     * 每页条数
     */
    private Long pageSize;

    /**
     * 总页数
     */
    private Long totalPages;

    /**
     * 构造分页结果
     *
     * @param records 当前页数据
     * @param total 总记录数
     * @param pageNum 当前页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    public static <T> PageResult<T> of(List<T> records, long total, long pageNum, long pageSize) {
        long safePageSize = pageSize <= 0 ? 20L : pageSize;
        long totalPages = (total + safePageSize - 1) / safePageSize;
        return PageResult.<T>builder()
                .records(records)
                .total(total)
                .pageNum(pageNum)
                .pageSize(safePageSize)
                .totalPages(totalPages)
                .build();
    }

}
