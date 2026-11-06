package com.itsworld.health.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itsworld.common.exception.BusinessException;
import com.itsworld.common.result.ErrorCode;
import com.itsworld.common.result.PageResult;
import com.itsworld.health.dto.CheckupRecordCreateRequest;
import com.itsworld.health.dto.CheckupRecordResponse;
import com.itsworld.health.dto.FeedingRecordCreateRequest;
import com.itsworld.health.dto.FeedingRecordResponse;
import com.itsworld.health.dto.VaccineRecordCreateRequest;
import com.itsworld.health.dto.VaccineRecordResponse;
import com.itsworld.health.dto.WeightRecordCreateRequest;
import com.itsworld.health.dto.WeightRecordResponse;
import com.itsworld.health.entity.PetFeedingRecord;
import com.itsworld.health.entity.PetHealthCheckup;
import com.itsworld.health.entity.PetVaccineRecord;
import com.itsworld.health.entity.PetWeightRecord;
import com.itsworld.health.mapper.PetFeedingRecordMapper;
import com.itsworld.health.mapper.PetHealthCheckupMapper;
import com.itsworld.health.mapper.PetVaccineRecordMapper;
import com.itsworld.health.mapper.PetWeightRecordMapper;
import com.itsworld.health.service.HealthService;
import com.itsworld.pet.dto.PetUpdateRequest;
import com.itsworld.pet.service.PetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 健康管理业务实现
 *
 * @author Claude
 * @date 2026-11-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthServiceImpl implements HealthService {

    private final PetWeightRecordMapper weightRecordMapper;
    private final PetFeedingRecordMapper feedingRecordMapper;
    private final PetVaccineRecordMapper vaccineRecordMapper;
    private final PetHealthCheckupMapper checkupMapper;
    private final PetService petService;

    @Override
    public List<WeightRecordResponse> getWeightRecords(Long userId, Long petId) {
        validatePetOwnership(userId, petId);

        List<PetWeightRecord> records = weightRecordMapper.selectList(
                new LambdaQueryWrapper<PetWeightRecord>()
                        .eq(PetWeightRecord::getPetId, petId)
                        .orderByDesc(PetWeightRecord::getRecordedAt)
                        .orderByDesc(PetWeightRecord::getId));
        return records.stream()
                .map(this::convertToWeightResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Long addWeightRecord(Long userId, WeightRecordCreateRequest request) {
        validatePetOwnership(userId, request.getPetId());

        LocalDateTime recordedAt = request.getRecordedAt() == null
                ? LocalDateTime.now()
                : request.getRecordedAt();

        PetWeightRecord latestRecord = weightRecordMapper.selectOne(
                new LambdaQueryWrapper<PetWeightRecord>()
                        .eq(PetWeightRecord::getPetId, request.getPetId())
                        .orderByDesc(PetWeightRecord::getRecordedAt)
                        .orderByDesc(PetWeightRecord::getId)
                        .last("LIMIT 1"));

        PetWeightRecord record = PetWeightRecord.builder()
                .petId(request.getPetId())
                .weightKg(request.getWeightKg())
                .recordedAt(recordedAt)
                .createdAt(LocalDateTime.now())
                .build();

        if (weightRecordMapper.insert(record) <= 0) {
            throw new BusinessException(ErrorCode.SERVER_ERROR);
        }

        // 只有最新一条记录才回写宠物档案的当前体重
        if (latestRecord == null || !recordedAt.isBefore(latestRecord.getRecordedAt())) {
            PetUpdateRequest updateRequest = PetUpdateRequest.builder()
                    .weight(request.getWeightKg())
                    .build();
            petService.updatePet(userId, request.getPetId(), updateRequest);
        }

        log.info("体重记录创建成功: userId={}, petId={}, recordId={}",
                userId, request.getPetId(), record.getId());
        return record.getId();
    }

    @Override
    @Transactional
    public void deleteWeightRecord(Long userId, Long recordId) {
        PetWeightRecord record = weightRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCode.HEALTH_RECORD_NOT_FOUND);
        }
        validatePetOwnership(userId, record.getPetId());

        weightRecordMapper.deleteById(recordId);
        log.info("体重记录删除成功: userId={}, recordId={}", userId, recordId);
    }

    @Override
    public List<FeedingRecordResponse> getFeedingRecords(Long userId, Long petId, LocalDate date) {
        validatePetOwnership(userId, petId);

        LambdaQueryWrapper<PetFeedingRecord> wrapper = new LambdaQueryWrapper<PetFeedingRecord>()
                .eq(PetFeedingRecord::getPetId, petId);
        if (date != null) {
            LocalDateTime start = date.atStartOfDay();
            wrapper.ge(PetFeedingRecord::getMealTime, start)
                    .lt(PetFeedingRecord::getMealTime, start.plusDays(1));
        }
        wrapper.orderByDesc(PetFeedingRecord::getMealTime)
                .orderByDesc(PetFeedingRecord::getId);

        List<PetFeedingRecord> records = feedingRecordMapper.selectList(wrapper);
        return records.stream()
                .map(this::convertToFeedingResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Long addFeedingRecord(Long userId, FeedingRecordCreateRequest request) {
        validatePetOwnership(userId, request.getPetId());

        LocalDateTime mealTime = request.getMealTime() == null
                ? LocalDateTime.now()
                : request.getMealTime();

        PetFeedingRecord record = PetFeedingRecord.builder()
                .petId(request.getPetId())
                .feedType(request.getFeedType())
                .amountG(request.getAmountG())
                .mealTime(mealTime)
                .notes(request.getNotes())
                .createdAt(LocalDateTime.now())
                .build();

        if (feedingRecordMapper.insert(record) <= 0) {
            throw new BusinessException(ErrorCode.SERVER_ERROR);
        }

        log.info("喂食记录创建成功: userId={}, petId={}, recordId={}",
                userId, request.getPetId(), record.getId());
        return record.getId();
    }

    @Override
    @Transactional
    public void deleteFeedingRecord(Long userId, Long recordId) {
        PetFeedingRecord record = feedingRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCode.HEALTH_RECORD_NOT_FOUND);
        }
        validatePetOwnership(userId, record.getPetId());

        feedingRecordMapper.deleteById(recordId);
        log.info("喂食记录删除成功: userId={}, recordId={}", userId, recordId);
    }

    @Override
    public List<VaccineRecordResponse> getVaccineRecords(Long userId, Long petId) {
        validatePetOwnership(userId, petId);

        List<PetVaccineRecord> records = vaccineRecordMapper.selectList(
                new LambdaQueryWrapper<PetVaccineRecord>()
                        .eq(PetVaccineRecord::getPetId, petId)
                        .orderByDesc(PetVaccineRecord::getVaccinatedAt)
                        .orderByDesc(PetVaccineRecord::getId));
        return records.stream()
                .map(this::convertToVaccineResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Long addVaccineRecord(Long userId, VaccineRecordCreateRequest request) {
        validatePetOwnership(userId, request.getPetId());

        PetVaccineRecord record = PetVaccineRecord.builder()
                .petId(request.getPetId())
                .vaccineName(request.getVaccineName())
                .vaccinatedAt(request.getVaccinatedAt())
                .nextDueAt(request.getNextDueAt())
                .vetClinic(request.getVetClinic())
                .notes(request.getNotes())
                .createdAt(LocalDateTime.now())
                .build();

        if (vaccineRecordMapper.insert(record) <= 0) {
            throw new BusinessException(ErrorCode.SERVER_ERROR);
        }

        log.info("疫苗记录创建成功: userId={}, petId={}, recordId={}",
                userId, request.getPetId(), record.getId());
        return record.getId();
    }

    @Override
    @Transactional
    public void deleteVaccineRecord(Long userId, Long recordId) {
        PetVaccineRecord record = vaccineRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCode.HEALTH_RECORD_NOT_FOUND);
        }
        validatePetOwnership(userId, record.getPetId());

        vaccineRecordMapper.deleteById(recordId);
        log.info("疫苗记录删除成功: userId={}, recordId={}", userId, recordId);
    }

    @Override
    public List<CheckupRecordResponse> getCheckupRecords(Long userId, Long petId) {
        validatePetOwnership(userId, petId);

        List<PetHealthCheckup> records = checkupMapper.selectList(
                new LambdaQueryWrapper<PetHealthCheckup>()
                        .eq(PetHealthCheckup::getPetId, petId)
                        .orderByDesc(PetHealthCheckup::getCheckupDate)
                        .orderByDesc(PetHealthCheckup::getId));
        return records.stream()
                .map(this::convertToCheckupResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Long addCheckupRecord(Long userId, CheckupRecordCreateRequest request) {
        validatePetOwnership(userId, request.getPetId());

        PetHealthCheckup record = PetHealthCheckup.builder()
                .petId(request.getPetId())
                .checkupDate(request.getCheckupDate())
                .vetClinic(request.getVetClinic())
                .findings(request.getFindings())
                .recommendations(request.getRecommendations())
                .createdAt(LocalDateTime.now())
                .build();

        if (checkupMapper.insert(record) <= 0) {
            throw new BusinessException(ErrorCode.SERVER_ERROR);
        }

        log.info("体检记录创建成功: userId={}, petId={}, recordId={}",
                userId, request.getPetId(), record.getId());
        return record.getId();
    }

    @Override
    @Transactional
    public void deleteCheckupRecord(Long userId, Long recordId) {
        PetHealthCheckup record = checkupMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCode.HEALTH_RECORD_NOT_FOUND);
        }
        validatePetOwnership(userId, record.getPetId());

        checkupMapper.deleteById(recordId);
        log.info("体检记录删除成功: userId={}, recordId={}", userId, recordId);
    }

    @Override
    public PageResult<WeightRecordResponse> getWeightRecordPage(Long userId, Long petId,
                                                                long pageNum, long pageSize) {
        validatePetOwnership(userId, petId);

        Page<PetWeightRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PetWeightRecord> wrapper = new LambdaQueryWrapper<PetWeightRecord>()
                .eq(PetWeightRecord::getPetId, petId)
                .orderByDesc(PetWeightRecord::getRecordedAt)
                .orderByDesc(PetWeightRecord::getId);
        Page<PetWeightRecord> resultPage = weightRecordMapper.selectPage(page, wrapper);

        List<WeightRecordResponse> records = resultPage.getRecords().stream()
                .map(this::convertToWeightResponse)
                .collect(Collectors.toList());
        return PageResult.of(records, resultPage.getTotal(),
                resultPage.getCurrent(), resultPage.getSize());
    }

    @Override
    public PageResult<FeedingRecordResponse> getFeedingRecordPage(Long userId, Long petId,
                                                                  LocalDate date, long pageNum, long pageSize) {
        validatePetOwnership(userId, petId);

        Page<PetFeedingRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PetFeedingRecord> wrapper = new LambdaQueryWrapper<PetFeedingRecord>()
                .eq(PetFeedingRecord::getPetId, petId);
        if (date != null) {
            LocalDateTime start = date.atStartOfDay();
            wrapper.ge(PetFeedingRecord::getMealTime, start)
                    .lt(PetFeedingRecord::getMealTime, start.plusDays(1));
        }
        wrapper.orderByDesc(PetFeedingRecord::getMealTime)
                .orderByDesc(PetFeedingRecord::getId);

        Page<PetFeedingRecord> resultPage = feedingRecordMapper.selectPage(page, wrapper);
        List<FeedingRecordResponse> records = resultPage.getRecords().stream()
                .map(this::convertToFeedingResponse)
                .collect(Collectors.toList());
        return PageResult.of(records, resultPage.getTotal(),
                resultPage.getCurrent(), resultPage.getSize());
    }

    @Override
    public PageResult<VaccineRecordResponse> getVaccineRecordPage(Long userId, Long petId,
                                                                  long pageNum, long pageSize) {
        validatePetOwnership(userId, petId);

        Page<PetVaccineRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PetVaccineRecord> wrapper = new LambdaQueryWrapper<PetVaccineRecord>()
                .eq(PetVaccineRecord::getPetId, petId)
                .orderByDesc(PetVaccineRecord::getVaccinatedAt)
                .orderByDesc(PetVaccineRecord::getId);
        Page<PetVaccineRecord> resultPage = vaccineRecordMapper.selectPage(page, wrapper);

        List<VaccineRecordResponse> records = resultPage.getRecords().stream()
                .map(this::convertToVaccineResponse)
                .collect(Collectors.toList());
        return PageResult.of(records, resultPage.getTotal(),
                resultPage.getCurrent(), resultPage.getSize());
    }

    @Override
    public PageResult<CheckupRecordResponse> getCheckupRecordPage(Long userId, Long petId,
                                                                  long pageNum, long pageSize) {
        validatePetOwnership(userId, petId);

        Page<PetHealthCheckup> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PetHealthCheckup> wrapper = new LambdaQueryWrapper<PetHealthCheckup>()
                .eq(PetHealthCheckup::getPetId, petId)
                .orderByDesc(PetHealthCheckup::getCheckupDate)
                .orderByDesc(PetHealthCheckup::getId);
        Page<PetHealthCheckup> resultPage = checkupMapper.selectPage(page, wrapper);

        List<CheckupRecordResponse> records = resultPage.getRecords().stream()
                .map(this::convertToCheckupResponse)
                .collect(Collectors.toList());
        return PageResult.of(records, resultPage.getTotal(),
                resultPage.getCurrent(), resultPage.getSize());
    }

    /**
     * 校验宠物归属，非本人宠物会抛出 PET_NOT_FOUND
     */
    private void validatePetOwnership(Long userId, Long petId) {
        petService.getPetInfo(userId, petId);
    }

    private WeightRecordResponse convertToWeightResponse(PetWeightRecord record) {
        return WeightRecordResponse.builder()
                .id(record.getId())
                .petId(record.getPetId())
                .weightKg(record.getWeightKg())
                .recordedAt(record.getRecordedAt())
                .createdAt(record.getCreatedAt())
                .build();
    }

    private FeedingRecordResponse convertToFeedingResponse(PetFeedingRecord record) {
        return FeedingRecordResponse.builder()
                .id(record.getId())
                .petId(record.getPetId())
                .feedType(record.getFeedType())
                .amountG(record.getAmountG())
                .mealTime(record.getMealTime())
                .notes(record.getNotes())
                .createdAt(record.getCreatedAt())
                .build();
    }

    private VaccineRecordResponse convertToVaccineResponse(PetVaccineRecord record) {
        return VaccineRecordResponse.builder()
                .id(record.getId())
                .petId(record.getPetId())
                .vaccineName(record.getVaccineName())
                .vaccinatedAt(record.getVaccinatedAt())
                .nextDueAt(record.getNextDueAt())
                .vetClinic(record.getVetClinic())
                .notes(record.getNotes())
                .createdAt(record.getCreatedAt())
                .build();
    }

    private CheckupRecordResponse convertToCheckupResponse(PetHealthCheckup record) {
        return CheckupRecordResponse.builder()
                .id(record.getId())
                .petId(record.getPetId())
                .checkupDate(record.getCheckupDate())
                .vetClinic(record.getVetClinic())
                .findings(record.getFindings())
                .recommendations(record.getRecommendations())
                .createdAt(record.getCreatedAt())
                .build();
    }

}
