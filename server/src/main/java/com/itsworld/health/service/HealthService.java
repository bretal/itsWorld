package com.itsworld.health.service;

import com.itsworld.common.result.PageResult;
import com.itsworld.health.dto.CheckupRecordCreateRequest;
import com.itsworld.health.dto.CheckupRecordResponse;
import com.itsworld.health.dto.FeedingRecordCreateRequest;
import com.itsworld.health.dto.FeedingRecordResponse;
import com.itsworld.health.dto.VaccineRecordCreateRequest;
import com.itsworld.health.dto.VaccineRecordResponse;
import com.itsworld.health.dto.WeightRecordCreateRequest;
import com.itsworld.health.dto.WeightRecordResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * 健康管理业务接口
 *
 * @author Claude
 * @date 2026-11-05
 */
public interface HealthService {

    /**
     * 查询宠物体重记录
     *
     * @param userId 当前用户ID
     * @param petId 宠物ID
     * @return 体重记录列表
     */
    List<WeightRecordResponse> getWeightRecords(Long userId, Long petId);

    /**
     * 分页查询体重记录
     *
     * @param userId 当前用户ID
     * @param petId 宠物ID
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    PageResult<WeightRecordResponse> getWeightRecordPage(Long userId, Long petId,
                                                         long pageNum, long pageSize);

    /**
     * 新增体重记录
     *
     * @param userId 当前用户ID
     * @param request 创建请求
     * @return 记录ID
     */
    Long addWeightRecord(Long userId, WeightRecordCreateRequest request);

    /**
     * 删除体重记录
     *
     * @param userId 当前用户ID
     * @param recordId 记录ID
     */
    void deleteWeightRecord(Long userId, Long recordId);

    /**
     * 查询喂食记录
     *
     * @param userId 当前用户ID
     * @param petId 宠物ID
     * @param date 指定日期，可为空
     * @return 喂食记录列表
     */
    List<FeedingRecordResponse> getFeedingRecords(Long userId, Long petId, LocalDate date);

    /**
     * 分页查询喂食记录
     *
     * @param userId 当前用户ID
     * @param petId 宠物ID
     * @param date 指定日期，可为空
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    PageResult<FeedingRecordResponse> getFeedingRecordPage(Long userId, Long petId,
                                                           LocalDate date, long pageNum, long pageSize);

    /**
     * 新增喂食记录
     *
     * @param userId 当前用户ID
     * @param request 创建请求
     * @return 记录ID
     */
    Long addFeedingRecord(Long userId, FeedingRecordCreateRequest request);

    /**
     * 删除喂食记录
     *
     * @param userId 当前用户ID
     * @param recordId 记录ID
     */
    void deleteFeedingRecord(Long userId, Long recordId);

    /**
     * 查询疫苗记录
     *
     * @param userId 当前用户ID
     * @param petId 宠物ID
     * @return 疫苗记录列表
     */
    List<VaccineRecordResponse> getVaccineRecords(Long userId, Long petId);

    /**
     * 分页查询疫苗记录
     *
     * @param userId 当前用户ID
     * @param petId 宠物ID
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    PageResult<VaccineRecordResponse> getVaccineRecordPage(Long userId, Long petId,
                                                           long pageNum, long pageSize);

    /**
     * 新增疫苗记录
     *
     * @param userId 当前用户ID
     * @param request 创建请求
     * @return 记录ID
     */
    Long addVaccineRecord(Long userId, VaccineRecordCreateRequest request);

    /**
     * 删除疫苗记录
     *
     * @param userId 当前用户ID
     * @param recordId 记录ID
     */
    void deleteVaccineRecord(Long userId, Long recordId);

    /**
     * 查询体检记录
     *
     * @param userId 当前用户ID
     * @param petId 宠物ID
     * @return 体检记录列表
     */
    List<CheckupRecordResponse> getCheckupRecords(Long userId, Long petId);

    /**
     * 分页查询体检记录
     *
     * @param userId 当前用户ID
     * @param petId 宠物ID
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    PageResult<CheckupRecordResponse> getCheckupRecordPage(Long userId, Long petId,
                                                           long pageNum, long pageSize);

    /**
     * 新增体检记录
     *
     * @param userId 当前用户ID
     * @param request 创建请求
     * @return 记录ID
     */
    Long addCheckupRecord(Long userId, CheckupRecordCreateRequest request);

    /**
     * 删除体检记录
     *
     * @param userId 当前用户ID
     * @param recordId 记录ID
     */
    void deleteCheckupRecord(Long userId, Long recordId);

}
