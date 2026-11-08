package com.itsworld.commerce.packagebiz.service;

import com.itsworld.commerce.booking.dto.PackageBookingOptionView;
import com.itsworld.commerce.packagebiz.dto.PackagePurchasePreview;
import com.itsworld.commerce.packagebiz.dto.PackagePurchasePreviewRequest;
import com.itsworld.commerce.packagebiz.dto.PackagePurchaseRequest;
import com.itsworld.commerce.packagebiz.dto.PackagePurchaseResult;
import com.itsworld.commerce.packagebiz.dto.PackageTemplateSaveRequest;
import com.itsworld.commerce.packagebiz.dto.PackageTemplateView;
import com.itsworld.commerce.packagebiz.dto.PackageUsageView;
import com.itsworld.commerce.packagebiz.dto.UserPackageView;
import com.itsworld.common.result.PageResult;

import java.util.List;

/**
 * 服务套餐业务接口
 * 覆盖套餐模板维护、用户购买、已购套餐查询与预约核销联动。
 *
 * @author Claude
 * @date 2026-11-07
 */
public interface PackageService {

    /**
     * 用户端套餐模板分页（仅启用）
     */
    PageResult<PackageTemplateView> pageTemplates(String keyword, long pageNum, long pageSize);

    /**
     * 用户端套餐模板详情
     */
    PackageTemplateView getTemplateDetail(Long templateId);

    /**
     * 用户端启用套餐列表
     */
    List<PackageTemplateView> listEnabledTemplates();

    /**
     * 套餐购买试算
     */
    PackagePurchasePreview previewPurchase(Long userId, PackagePurchasePreviewRequest request);

    /**
     * 提交套餐购买，生成 PACKAGE 订单
     */
    PackagePurchaseResult purchasePackage(Long userId, PackagePurchaseRequest request);

    /**
     * 我的套餐分页
     */
    PageResult<UserPackageView> pageMyPackages(Long userId, String status,
                                               long pageNum, long pageSize);

    /**
     * 我的套餐详情
     */
    UserPackageView getMyPackage(Long userId, Long userPackageId);

    /**
     * 套餐核销流水
     */
    List<PackageUsageView> listUsages(Long userId, Long userPackageId);

    // ---------- 管理端 ----------

    PageResult<PackageTemplateView> pageAdminTemplates(String status, String keyword,
                                                       long pageNum, long pageSize);

    PackageTemplateView getAdminTemplate(Long templateId);

    PackageTemplateView createTemplate(PackageTemplateSaveRequest request);

    PackageTemplateView updateTemplate(Long templateId, PackageTemplateSaveRequest request);

    void changeTemplateStatus(Long templateId, String status);

    // ---------- 预约联动 ----------

    /**
     * 指定服务下当前用户可用的套餐次数项
     */
    List<PackageBookingOptionView> listBookingOptions(Long userId, Long serviceId, Long storeId);

    /**
     * 预约时冻结套餐次数并写核销流水（FROZEN）
     */
    void freezeForBooking(Long userId, Long userPackageItemId, Long bookingId,
                          Long serviceId, Long storeId, Long petId);

    /**
     * 预约完成时确认核销（FROZEN -> USED）
     */
    void consumeForBooking(Long bookingId, Long operatorId);

    /**
     * 预约取消时释放冻结次数（FROZEN -> RELEASED）
     */
    void releaseForBooking(Long bookingId);

    /**
     * 套餐订单支付成功后发放用户套餐
     */
    void issueForPaidOrder(Long orderId);

}
