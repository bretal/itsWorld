package com.itsworld.commerce.store.controller;

import com.itsworld.commerce.store.dto.BusinessHourSaveRequest;
import com.itsworld.commerce.store.dto.StaffSaveRequest;
import com.itsworld.commerce.store.dto.StaffShiftSaveRequest;
import com.itsworld.commerce.store.dto.StoreSaveRequest;
import com.itsworld.commerce.store.dto.StoreView;
import com.itsworld.commerce.store.entity.CommerceStaff;
import com.itsworld.commerce.store.entity.CommerceStaffShift;
import com.itsworld.commerce.store.entity.CommerceStoreBusinessHour;
import com.itsworld.commerce.store.service.StoreService;
import com.itsworld.common.result.PageResult;
import com.itsworld.common.result.Result;
import com.itsworld.common.util.PageUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 管理端门店控制器
 *
 * @author Claude
 * @date 2026-11-07
 */
@RestController
@RequestMapping("/api/admin/commerce/stores")
@RequiredArgsConstructor
public class StoreAdminController {

    private final StoreService storeService;

    @GetMapping
    public Result<PageResult<StoreView>> pageStores(
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size,
            @RequestParam(required = false) String keyword) {
        return Result.success(storeService.pageAdminStores(
                PageUtils.normalizePageNum(page), PageUtils.normalizePageSize(size), keyword));
    }

    @PostMapping
    public Result<StoreView> createStore(@Valid @RequestBody StoreSaveRequest request) {
        return Result.success(storeService.createStore(request), "门店已创建");
    }

    @PutMapping("/{storeId}")
    public Result<StoreView> updateStore(@PathVariable Long storeId,
                                         @Valid @RequestBody StoreSaveRequest request) {
        return Result.success(storeService.updateStore(storeId, request), "门店已更新");
    }

    @PutMapping("/{storeId}/status")
    public Result<Void> changeStoreStatus(@PathVariable Long storeId,
                                          @RequestParam String businessStatus) {
        storeService.changeStoreStatus(storeId, businessStatus);
        return Result.success(null, "门店状态已更新");
    }

    @GetMapping("/{storeId}/hours")
    public Result<List<CommerceStoreBusinessHour>> listHours(@PathVariable Long storeId) {
        return Result.success(storeService.listHours(storeId));
    }

    @PostMapping("/{storeId}/hours")
    public Result<CommerceStoreBusinessHour> createHour(@PathVariable Long storeId,
                                                        @Valid @RequestBody BusinessHourSaveRequest request) {
        return Result.success(storeService.createHour(storeId, request), "营业时间已创建");
    }

    @PutMapping("/hours/{hourId}")
    public Result<CommerceStoreBusinessHour> updateHour(@PathVariable Long hourId,
                                                        @Valid @RequestBody BusinessHourSaveRequest request) {
        return Result.success(storeService.updateHour(hourId, request), "营业时间已更新");
    }

    @DeleteMapping("/hours/{hourId}")
    public Result<Void> deleteHour(@PathVariable Long hourId) {
        storeService.deleteHour(hourId);
        return Result.success(null, "营业时间已删除");
    }

    @GetMapping("/{storeId}/staff")
    public Result<List<CommerceStaff>> listStaff(@PathVariable Long storeId) {
        return Result.success(storeService.listStaff(storeId));
    }

    @PostMapping("/{storeId}/staff")
    public Result<CommerceStaff> createStaff(@PathVariable Long storeId,
                                             @Valid @RequestBody StaffSaveRequest request) {
        return Result.success(storeService.createStaff(storeId, request), "员工已创建");
    }

    @PutMapping("/staff/{staffId}")
    public Result<CommerceStaff> updateStaff(@PathVariable Long staffId,
                                             @Valid @RequestBody StaffSaveRequest request) {
        return Result.success(storeService.updateStaff(staffId, request), "员工已更新");
    }

    @PutMapping("/staff/{staffId}/status")
    public Result<Void> changeStaffStatus(@PathVariable Long staffId,
                                          @RequestParam String status) {
        storeService.changeStaffStatus(staffId, status);
        return Result.success(null, "员工状态已更新");
    }

    @GetMapping("/staff/{staffId}/shifts")
    public Result<List<CommerceStaffShift>> listShifts(
            @PathVariable Long staffId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(storeService.listShifts(staffId, date));
    }

    @PostMapping("/staff/{staffId}/shifts")
    public Result<CommerceStaffShift> createShift(@PathVariable Long staffId,
                                                  @Valid @RequestBody StaffShiftSaveRequest request) {
        return Result.success(storeService.createShift(staffId, request), "班次已创建");
    }

    @PutMapping("/shifts/{shiftId}")
    public Result<CommerceStaffShift> updateShift(@PathVariable Long shiftId,
                                                  @Valid @RequestBody StaffShiftSaveRequest request) {
        return Result.success(storeService.updateShift(shiftId, request), "班次已更新");
    }

    @DeleteMapping("/shifts/{shiftId}")
    public Result<Void> deleteShift(@PathVariable Long shiftId) {
        storeService.deleteShift(shiftId);
        return Result.success(null, "班次已删除");
    }

}
