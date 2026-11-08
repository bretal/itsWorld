package com.itsworld.member.service;

import com.itsworld.member.dto.CouponOptionView;
import com.itsworld.member.dto.CouponQuote;
import com.itsworld.member.dto.CouponTemplateRequest;
import com.itsworld.member.dto.CouponView;
import com.itsworld.member.entity.CouponTemplate;

import java.util.List;

/**
 * 优惠券业务接口
 *
 * @author Claude
 * @date 2026-11-07
 */
public interface CouponService {

    /**
     * 用户券列表
     *
     * @param userId 用户ID
     * @param status 状态过滤，可为空
     * @return 用户券列表
     */
    List<CouponView> listUserCoupons(Long userId, String status);

    /**
     * 发放优惠券
     *
     * @param userId     用户ID
     * @param templateId 券模板ID
     * @param quantity   数量
     */
    void issueCoupons(Long userId, Long templateId, Integer quantity);

    /**
     * 发放一张优惠券并返回用户券ID（供礼包复用）
     *
     * @param userId     用户ID
     * @param templateId 券模板ID
     * @return 用户券ID
     */
    Long issueOneCoupon(Long userId, Long templateId);

    /**
     * 试算优惠券
     *
     * @param userId      用户ID
     * @param couponId    用户券ID
     * @param amountCents 订单金额（分）
     * @return 试算结果
     */
    CouponQuote quoteCoupon(Long userId, Long couponId, long amountCents);

    /**
     * 当前金额可用优惠券
     *
     * @param userId      用户ID
     * @param amountCents 订单金额（分）
     * @return 可用券选项
     */
    List<CouponOptionView> listCouponOptions(Long userId, long amountCents);

    /**
     * 锁定优惠券
     *
     * @param userId   用户ID
     * @param couponId 用户券ID
     * @param orderNo  订单号
     */
    void lockCoupon(Long userId, Long couponId, String orderNo);

    /**
     * 核销优惠券
     */
    void consumeCoupon(Long userId, Long couponId, String orderNo);

    /**
     * 释放锁定
     */
    void releaseCoupon(Long userId, Long couponId, String orderNo);

    /**
     * 退款恢复（Phase 4 使用）
     */
    void restoreCoupon(Long userId, Long couponId, String orderNo);

    /**
     * 管理端券模板列表
     *
     * @return 模板列表
     */
    List<CouponTemplate> getAdminTemplates();

    /**
     * 新建券模板
     *
     * @param request 模板配置
     * @return 模板
     */
    CouponTemplate createTemplate(CouponTemplateRequest request);

    /**
     * 更新券模板
     *
     * @param templateId 模板ID
     * @param request    模板配置
     * @return 模板
     */
    CouponTemplate updateTemplate(Long templateId, CouponTemplateRequest request);

    /**
     * 下架券模板
     *
     * @param templateId 模板ID
     */
    void disableTemplate(Long templateId);

}
