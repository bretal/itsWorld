package com.itsworld.commerce.aftersale.service;

import com.itsworld.commerce.aftersale.dto.AfterSaleApplyRequest;
import com.itsworld.commerce.aftersale.dto.AfterSaleReviewRequest;
import com.itsworld.commerce.aftersale.dto.AfterSaleView;
import com.itsworld.common.result.PageResult;

/**
 * 售后服务接口
 *
 * @author Claude
 * @date 2026-11-07
 */
public interface AfterSaleService {

    /**
     * 用户申请售后
     */
    AfterSaleView apply(Long userId, AfterSaleApplyRequest request);

    PageResult<AfterSaleView> pageUserAfterSales(Long userId, String status,
                                                 long pageNum, long pageSize);

    AfterSaleView getUserAfterSale(Long userId, String afterSaleNo);

    PageResult<AfterSaleView> pageAdminAfterSales(String status, String keyword,
                                                  long pageNum, long pageSize);

    AfterSaleView getAdminAfterSale(String afterSaleNo);

    /**
     * 管理端审核：通过或拒绝；通过后需再执行退款
     */
    AfterSaleView review(String afterSaleNo, AfterSaleReviewRequest request, Long adminId);

    /**
     * 执行退款：钱包原路退回 / 模拟微信退款；回补库存与订单退款金额
     */
    AfterSaleView executeRefund(String afterSaleNo, Long adminId);

}
