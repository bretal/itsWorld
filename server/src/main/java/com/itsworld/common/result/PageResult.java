package com.itsworld.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页返回结果
 * 包含分页相关信息和数据列表
 *
 * @param <T> 列表元素类型
 * @author Claude
 * @date 2026-10-30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResult<T> {

    /**
     * 响应码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 数据列表
     */
    private List<T> data;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码
     */
    private Long pageNum;

    /**
     * 每页条数
     */
    private Long pageSize = 20L;

    /**
     * 总页数
     */
    private Long totalPages;

    /**
     * 构造函数：成功的分页响应
     */
    public static <T> PageResult<T> success(List<T> data, Long total, Long pageNum, Long pageSize) {
        long totalPages = (total + pageSize - 1) / pageSize;
        PageResult<T> result = new PageResult<>();
        result.setCode(0);
        result.setMessage("success");
        result.setData(data);
        result.setTimestamp(System.currentTimeMillis());
        result.setTotal(total);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setTotalPages(totalPages);
        return result;
    }

    /**
     * 构造函数：失败的分页响应
     */
    public static <T> PageResult<T> fail(Integer code, String message) {
        PageResult<T> result = new PageResult<>();
        result.setCode(code);
        result.setMessage(message);
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }

}
