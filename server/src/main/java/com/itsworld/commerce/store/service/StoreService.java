package com.itsworld.commerce.store.service;

import com.itsworld.commerce.store.dto.BusinessHourSaveRequest;
import com.itsworld.commerce.store.dto.StaffSaveRequest;
import com.itsworld.commerce.store.dto.StaffShiftSaveRequest;
import com.itsworld.commerce.store.dto.StoreDetailView;
import com.itsworld.commerce.store.dto.StoreSaveRequest;
import com.itsworld.commerce.store.dto.StoreView;
import com.itsworld.commerce.store.entity.CommerceStaff;
import com.itsworld.commerce.store.entity.CommerceStaffShift;
import com.itsworld.commerce.store.entity.CommerceStoreBusinessHour;
import com.itsworld.common.result.PageResult;

import java.time.LocalDate;
import java.util.List;

/**
 * 门店业务接口
 *
 * @author Claude
 * @date 2026-11-07
 */
public interface StoreService {

    /**
     * 用户端门店分页（仅营业中）
     */
    PageResult<StoreView> pageStores(long pageNum, long pageSize, String keyword);

    /**
     * 用户端门店详情
     */
    StoreDetailView getStoreDetail(Long storeId);

    /**
     * 管理端门店分页
     */
    PageResult<StoreView> pageAdminStores(long pageNum, long pageSize, String keyword);

    StoreView createStore(StoreSaveRequest request);

    StoreView updateStore(Long storeId, StoreSaveRequest request);

    void changeStoreStatus(Long storeId, String businessStatus);

    List<CommerceStoreBusinessHour> listHours(Long storeId);

    CommerceStoreBusinessHour createHour(Long storeId, BusinessHourSaveRequest request);

    CommerceStoreBusinessHour updateHour(Long hourId, BusinessHourSaveRequest request);

    void deleteHour(Long hourId);

    List<CommerceStaff> listStaff(Long storeId);

    CommerceStaff createStaff(Long storeId, StaffSaveRequest request);

    CommerceStaff updateStaff(Long staffId, StaffSaveRequest request);

    void changeStaffStatus(Long staffId, String status);

    List<CommerceStaffShift> listShifts(Long staffId, LocalDate shiftDate);

    CommerceStaffShift createShift(Long staffId, StaffShiftSaveRequest request);

    CommerceStaffShift updateShift(Long shiftId, StaffShiftSaveRequest request);

    void deleteShift(Long shiftId);

}
