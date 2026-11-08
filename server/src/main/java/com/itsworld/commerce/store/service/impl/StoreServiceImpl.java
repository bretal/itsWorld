package com.itsworld.commerce.store.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itsworld.commerce.store.dto.BusinessHourSaveRequest;
import com.itsworld.commerce.store.dto.StaffSaveRequest;
import com.itsworld.commerce.store.dto.StaffShiftSaveRequest;
import com.itsworld.commerce.store.dto.StoreDetailView;
import com.itsworld.commerce.store.dto.StoreSaveRequest;
import com.itsworld.commerce.store.dto.StoreView;
import com.itsworld.commerce.store.entity.CommerceStaff;
import com.itsworld.commerce.store.entity.CommerceStaffShift;
import com.itsworld.commerce.store.entity.CommerceStore;
import com.itsworld.commerce.store.entity.CommerceStoreBusinessHour;
import com.itsworld.commerce.store.mapper.CommerceStaffMapper;
import com.itsworld.commerce.store.mapper.CommerceStaffShiftMapper;
import com.itsworld.commerce.store.mapper.CommerceStoreBusinessHourMapper;
import com.itsworld.commerce.store.mapper.CommerceStoreMapper;
import com.itsworld.commerce.store.service.StoreService;
import com.itsworld.common.exception.BusinessException;
import com.itsworld.common.result.ErrorCode;
import com.itsworld.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 门店业务实现
 *
 * @author Claude
 * @date 2026-11-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private static final String STORE_OPEN = "OPEN";

    private final CommerceStoreMapper commerceStoreMapper;
    private final CommerceStoreBusinessHourMapper businessHourMapper;
    private final CommerceStaffMapper staffMapper;
    private final CommerceStaffShiftMapper shiftMapper;

    @Override
    public PageResult<StoreView> pageStores(long pageNum, long pageSize, String keyword) {
        Page<CommerceStore> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CommerceStore> wrapper = new LambdaQueryWrapper<CommerceStore>()
                .eq(CommerceStore::getBusinessStatus, STORE_OPEN)
                .orderByAsc(CommerceStore::getId);
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(w -> w.like(CommerceStore::getName, value)
                    .or().like(CommerceStore::getAddress, value)
                    .or().like(CommerceStore::getCity, value));
        }
        Page<CommerceStore> result = commerceStoreMapper.selectPage(page, wrapper);
        List<StoreView> records = result.getRecords().stream().map(this::toView).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public StoreDetailView getStoreDetail(Long storeId) {
        CommerceStore store = requireStore(storeId);
        List<CommerceStoreBusinessHour> hours = businessHourMapper.selectList(
                new LambdaQueryWrapper<CommerceStoreBusinessHour>()
                        .eq(CommerceStoreBusinessHour::getStoreId, storeId)
                        .orderByAsc(CommerceStoreBusinessHour::getDayOfWeek)
                        .orderByAsc(CommerceStoreBusinessHour::getId));
        return StoreDetailView.builder()
                .id(store.getId())
                .storeCode(store.getStoreCode())
                .name(store.getName())
                .phone(store.getPhone())
                .province(store.getProvince())
                .city(store.getCity())
                .district(store.getDistrict())
                .address(store.getAddress())
                .longitude(store.getLongitude())
                .latitude(store.getLatitude())
                .businessStatus(store.getBusinessStatus())
                .description(store.getDescription())
                .hours(hours)
                .build();
    }

    @Override
    public PageResult<StoreView> pageAdminStores(long pageNum, long pageSize, String keyword) {
        Page<CommerceStore> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CommerceStore> wrapper = new LambdaQueryWrapper<CommerceStore>()
                .orderByAsc(CommerceStore::getId);
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(w -> w.like(CommerceStore::getName, value)
                    .or().like(CommerceStore::getStoreCode, value)
                    .or().like(CommerceStore::getCity, value));
        }
        Page<CommerceStore> result = commerceStoreMapper.selectPage(page, wrapper);
        List<StoreView> records = result.getRecords().stream().map(this::toView).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public StoreView createStore(StoreSaveRequest request) {
        validateStoreCode(request.getStoreCode(), null);
        CommerceStore store = CommerceStore.builder()
                .storeCode(request.getStoreCode().trim())
                .name(request.getName().trim())
                .phone(request.getPhone())
                .province(request.getProvince())
                .city(request.getCity())
                .district(request.getDistrict())
                .address(request.getAddress())
                .longitude(request.getLongitude())
                .latitude(request.getLatitude())
                .businessStatus(normalizeStoreStatus(request.getBusinessStatus()))
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        commerceStoreMapper.insert(store);
        log.info("门店创建: storeId={}, code={}", store.getId(), store.getStoreCode());
        return toView(store);
    }

    @Override
    @Transactional
    public StoreView updateStore(Long storeId, StoreSaveRequest request) {
        CommerceStore store = requireStore(storeId);
        validateStoreCode(request.getStoreCode(), storeId);
        store.setStoreCode(request.getStoreCode().trim());
        store.setName(request.getName().trim());
        store.setPhone(request.getPhone());
        store.setProvince(request.getProvince());
        store.setCity(request.getCity());
        store.setDistrict(request.getDistrict());
        store.setAddress(request.getAddress());
        store.setLongitude(request.getLongitude());
        store.setLatitude(request.getLatitude());
        store.setBusinessStatus(normalizeStoreStatus(request.getBusinessStatus()));
        store.setDescription(request.getDescription());
        store.setUpdatedAt(LocalDateTime.now());
        commerceStoreMapper.updateById(store);
        log.info("门店更新: storeId={}", storeId);
        return toView(store);
    }

    @Override
    @Transactional
    public void changeStoreStatus(Long storeId, String businessStatus) {
        CommerceStore store = requireStore(storeId);
        store.setBusinessStatus(normalizeStoreStatus(businessStatus));
        store.setUpdatedAt(LocalDateTime.now());
        commerceStoreMapper.updateById(store);
    }

    @Override
    public List<CommerceStoreBusinessHour> listHours(Long storeId) {
        requireStore(storeId);
        return businessHourMapper.selectList(new LambdaQueryWrapper<CommerceStoreBusinessHour>()
                .eq(CommerceStoreBusinessHour::getStoreId, storeId)
                .orderByAsc(CommerceStoreBusinessHour::getDayOfWeek)
                .orderByAsc(CommerceStoreBusinessHour::getId));
    }

    @Override
    @Transactional
    public CommerceStoreBusinessHour createHour(Long storeId, BusinessHourSaveRequest request) {
        requireStore(storeId);
        validateTimeRange(request);
        CommerceStoreBusinessHour hour = CommerceStoreBusinessHour.builder()
                .storeId(storeId)
                .dayOfWeek(request.getDayOfWeek())
                .openTime(request.getOpenTime())
                .closeTime(request.getCloseTime())
                .specialDate(request.getSpecialDate())
                .closed(request.getClosed() != null ? request.getClosed() : 0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        businessHourMapper.insert(hour);
        return hour;
    }

    @Override
    @Transactional
    public CommerceStoreBusinessHour updateHour(Long hourId, BusinessHourSaveRequest request) {
        CommerceStoreBusinessHour hour = businessHourMapper.selectById(hourId);
        if (hour == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "营业时间不存在");
        }
        validateTimeRange(request);
        hour.setDayOfWeek(request.getDayOfWeek());
        hour.setOpenTime(request.getOpenTime());
        hour.setCloseTime(request.getCloseTime());
        hour.setSpecialDate(request.getSpecialDate());
        hour.setClosed(request.getClosed() != null ? request.getClosed() : 0);
        hour.setUpdatedAt(LocalDateTime.now());
        businessHourMapper.updateById(hour);
        return hour;
    }

    @Override
    @Transactional
    public void deleteHour(Long hourId) {
        businessHourMapper.deleteById(hourId);
    }

    @Override
    public List<CommerceStaff> listStaff(Long storeId) {
        requireStore(storeId);
        return staffMapper.selectList(new LambdaQueryWrapper<CommerceStaff>()
                .eq(CommerceStaff::getStoreId, storeId)
                .orderByAsc(CommerceStaff::getId));
    }

    @Override
    @Transactional
    public CommerceStaff createStaff(Long storeId, StaffSaveRequest request) {
        requireStore(storeId);
        CommerceStaff staff = CommerceStaff.builder()
                .storeId(storeId)
                .staffName(request.getStaffName().trim())
                .mobile(request.getMobile())
                .roleCode(request.getRoleCode())
                .status(normalizeStaffStatus(request.getStatus()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        staffMapper.insert(staff);
        return staff;
    }

    @Override
    @Transactional
    public CommerceStaff updateStaff(Long staffId, StaffSaveRequest request) {
        CommerceStaff staff = requireStaff(staffId);
        staff.setStaffName(request.getStaffName().trim());
        staff.setMobile(request.getMobile());
        staff.setRoleCode(request.getRoleCode());
        staff.setStatus(normalizeStaffStatus(request.getStatus()));
        staff.setUpdatedAt(LocalDateTime.now());
        staffMapper.updateById(staff);
        return staff;
    }

    @Override
    @Transactional
    public void changeStaffStatus(Long staffId, String status) {
        CommerceStaff staff = requireStaff(staffId);
        staff.setStatus(normalizeStaffStatus(status));
        staff.setUpdatedAt(LocalDateTime.now());
        staffMapper.updateById(staff);
    }

    @Override
    public List<CommerceStaffShift> listShifts(Long staffId, java.time.LocalDate shiftDate) {
        LambdaQueryWrapper<CommerceStaffShift> wrapper = new LambdaQueryWrapper<CommerceStaffShift>()
                .orderByAsc(CommerceStaffShift::getShiftDate)
                .orderByAsc(CommerceStaffShift::getStartTime);
        if (staffId != null) {
            wrapper.eq(CommerceStaffShift::getStaffId, staffId);
        }
        if (shiftDate != null) {
            wrapper.eq(CommerceStaffShift::getShiftDate, shiftDate);
        }
        return shiftMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public CommerceStaffShift createShift(Long staffId, StaffShiftSaveRequest request) {
        CommerceStaff staff = requireStaff(staffId);
        validateShiftTime(request);
        CommerceStaffShift shift = CommerceStaffShift.builder()
                .staffId(staffId)
                .storeId(staff.getStoreId())
                .shiftDate(request.getShiftDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .capacity(request.getCapacity())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        shiftMapper.insert(shift);
        return shift;
    }

    @Override
    @Transactional
    public CommerceStaffShift updateShift(Long shiftId, StaffShiftSaveRequest request) {
        CommerceStaffShift shift = shiftMapper.selectById(shiftId);
        if (shift == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "班次不存在");
        }
        validateShiftTime(request);
        shift.setShiftDate(request.getShiftDate());
        shift.setStartTime(request.getStartTime());
        shift.setEndTime(request.getEndTime());
        shift.setCapacity(request.getCapacity());
        shift.setStatus(request.getStatus() != null ? request.getStatus() : shift.getStatus());
        shift.setUpdatedAt(LocalDateTime.now());
        shiftMapper.updateById(shift);
        return shift;
    }

    @Override
    @Transactional
    public void deleteShift(Long shiftId) {
        shiftMapper.deleteById(shiftId);
    }

    private CommerceStore requireStore(Long storeId) {
        CommerceStore store = commerceStoreMapper.selectById(storeId);
        if (store == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "门店不存在");
        }
        return store;
    }

    private CommerceStaff requireStaff(Long staffId) {
        CommerceStaff staff = staffMapper.selectById(staffId);
        if (staff == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "员工不存在");
        }
        return staff;
    }

    private void validateStoreCode(String storeCode, Long excludeId) {
        LambdaQueryWrapper<CommerceStore> wrapper = new LambdaQueryWrapper<CommerceStore>()
                .eq(CommerceStore::getStoreCode, storeCode.trim());
        if (excludeId != null) {
            wrapper.ne(CommerceStore::getId, excludeId);
        }
        Long count = commerceStoreMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "门店编码已存在");
        }
    }

    private void validateTimeRange(BusinessHourSaveRequest request) {
        if (!request.getOpenTime().isBefore(request.getCloseTime())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "营业开始时间必须早于结束时间");
        }
    }

    private void validateShiftTime(StaffShiftSaveRequest request) {
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "班次开始时间必须早于结束时间");
        }
    }

    private String normalizeStoreStatus(String status) {
        if (status == null || status.isBlank()) {
            return STORE_OPEN;
        }
        String value = status.trim().toUpperCase();
        if (!"OPEN".equals(value) && !"CLOSED".equals(value) && !"SUSPENDED".equals(value)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "门店状态仅支持 OPEN/CLOSED/SUSPENDED");
        }
        return value;
    }

    private String normalizeStaffStatus(String status) {
        if (status == null || status.isBlank()) {
            return "ACTIVE";
        }
        String value = status.trim().toUpperCase();
        if (!"ACTIVE".equals(value) && !"INACTIVE".equals(value)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "员工状态仅支持 ACTIVE/INACTIVE");
        }
        return value;
    }

    private StoreView toView(CommerceStore store) {
        return StoreView.builder()
                .id(store.getId())
                .storeCode(store.getStoreCode())
                .name(store.getName())
                .phone(store.getPhone())
                .province(store.getProvince())
                .city(store.getCity())
                .district(store.getDistrict())
                .address(store.getAddress())
                .longitude(store.getLongitude())
                .latitude(store.getLatitude())
                .businessStatus(store.getBusinessStatus())
                .description(store.getDescription())
                .build();
    }

}
