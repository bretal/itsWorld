package com.itsworld.member.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.itsworld.common.exception.BusinessException;
import com.itsworld.common.result.ErrorCode;
import com.itsworld.member.dto.CouponOptionView;
import com.itsworld.member.dto.CouponQuote;
import com.itsworld.member.dto.CouponTemplateRequest;
import com.itsworld.member.dto.CouponView;
import com.itsworld.member.entity.CouponTemplate;
import com.itsworld.member.entity.UserCoupon;
import com.itsworld.member.mapper.CouponTemplateMapper;
import com.itsworld.member.mapper.UserCouponMapper;
import com.itsworld.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 优惠券业务实现
 *
 * @author Claude
 * @date 2026-11-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private static final String TYPE_CASH = "CASH";
    private static final String TYPE_DISCOUNT = "DISCOUNT";
    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_EXPIRED = "EXPIRED";

    private final CouponTemplateMapper couponTemplateMapper;
    private final UserCouponMapper userCouponMapper;
    private final UserService userService;

    public List<CouponView> listUserCoupons(Long userId, String status) {
        userCouponMapper.update(null, new LambdaUpdateWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, STATUS_AVAILABLE)
                .lt(UserCoupon::getValidTo, LocalDateTime.now())
                .set(UserCoupon::getStatus, STATUS_EXPIRED));
        return userCouponMapper.selectUserCoupons(userId, status);
    }

    @Transactional
    public void issueCoupons(Long userId, Long templateId, Integer quantity) {
        int count = quantity != null ? quantity : 1;
        if (count < 1 || count > 100) {
            throw new BusinessException(ErrorCode.COUPON_ISSUE_INVALID);
        }
        for (int i = 0; i < count; i++) {
            issueOneCoupon(userId, templateId);
        }
        log.info("优惠券发放: userId={}, templateId={}, quantity={}", userId, templateId, count);
    }

    @Transactional
    public Long issueOneCoupon(Long userId, Long templateId) {
        userService.getUserById(userId);
        CouponTemplate template = couponTemplateMapper.selectById(templateId);
        if (template == null || !Integer.valueOf(1).equals(template.getStatus())) {
            throw new BusinessException(ErrorCode.COUPON_TEMPLATE_NOT_FOUND);
        }

        int updated = couponTemplateMapper.update(null, new LambdaUpdateWrapper<CouponTemplate>()
                .eq(CouponTemplate::getId, templateId)
                .eq(CouponTemplate::getStatus, 1)
                .apply("issued_count < total_count")
                .setSql("issued_count = issued_count + 1"));
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.COUPON_STOCK_EMPTY);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime validTo = null;
        if (template.getValidDays() != null && template.getValidDays() > 0) {
            validTo = now.plusDays(template.getValidDays());
        }

        UserCoupon coupon = UserCoupon.builder()
                .userId(userId)
                .templateId(templateId)
                .couponCode(generateCouponCode())
                .status(STATUS_AVAILABLE)
                .validFrom(now)
                .validTo(validTo)
                .createdAt(now)
                .build();
        userCouponMapper.insert(coupon);
        return coupon.getId();
    }

    public CouponQuote quoteCoupon(Long userId, Long couponId, long amountCents) {
        UserCoupon coupon = requireUsableCoupon(userId, couponId);
        CouponTemplate template = requireTemplate(coupon.getTemplateId());
        long discount = calculateDiscount(template, amountCents);
        if (discount <= 0) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
        }
        return CouponQuote.builder()
                .couponId(couponId)
                .couponName(template.getTemplateName())
                .couponType(template.getCouponType())
                .discountAmount(discount)
                .amountBefore(amountCents)
                .amountAfter(amountCents - discount)
                .discountRate(template.getDiscountRate())
                .build();
    }

    public List<CouponOptionView> listCouponOptions(Long userId, long amountCents) {
        LocalDateTime now = LocalDateTime.now();
        List<UserCoupon> coupons = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, STATUS_AVAILABLE)
                .le(UserCoupon::getValidFrom, now)
                .and(w -> w.isNull(UserCoupon::getValidTo).or().ge(UserCoupon::getValidTo, now))
                .orderByDesc(UserCoupon::getId));
        List<CouponOptionView> options = new ArrayList<>();
        for (UserCoupon coupon : coupons) {
            CouponTemplate template = couponTemplateMapper.selectById(coupon.getTemplateId());
            if (template == null) {
                continue;
            }
            long minAmount = template.getMinAmountCents() != null ? template.getMinAmountCents() : 0L;
            if (amountCents < minAmount) {
                continue;
            }
            long discount;
            try {
                discount = calculateDiscount(template, amountCents);
            } catch (BusinessException e) {
                continue;
            }
            if (discount <= 0) {
                continue;
            }
            options.add(CouponOptionView.builder()
                    .id(coupon.getId())
                    .templateName(template.getTemplateName())
                    .couponType(template.getCouponType())
                    .amountCents(template.getAmountCents())
                    .discountRate(template.getDiscountRate())
                    .minAmountCents(template.getMinAmountCents())
                    .discountAmount(discount)
                    .description(template.getDescription())
                    .build());
        }
        options.sort(Comparator.comparing(CouponOptionView::getDiscountAmount).reversed());
        return options;
    }

    @Transactional
    public void lockCoupon(Long userId, Long couponId, String orderNo) {
        int updated = userCouponMapper.lockCoupon(userId, couponId, orderNo);
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.COUPON_LOCK_FAILED);
        }
        log.info("优惠券锁定: userId={}, couponId={}, orderNo={}", userId, couponId, orderNo);
    }

    @Transactional
    public void consumeCoupon(Long userId, Long couponId, String orderNo) {
        int updated = userCouponMapper.consumeCoupon(userId, couponId, orderNo);
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
        }
        log.info("优惠券核销: userId={}, couponId={}, orderNo={}", userId, couponId, orderNo);
    }

    @Transactional
    public void releaseCoupon(Long userId, Long couponId, String orderNo) {
        int updated = userCouponMapper.releaseCoupon(userId, couponId, orderNo);
        if (updated <= 0) {
            log.info("优惠券无需释放或已释放: couponId={}, orderNo={}", couponId, orderNo);
        }
    }

    @Transactional
    public void restoreCoupon(Long userId, Long couponId, String orderNo) {
        int updated = userCouponMapper.restoreCoupon(userId, couponId, orderNo);
        if (updated <= 0) {
            log.info("优惠券无需恢复或已恢复: couponId={}, orderNo={}", couponId, orderNo);
        }
    }

    public List<CouponTemplate> getAdminTemplates() {
        return couponTemplateMapper.selectList(new LambdaQueryWrapper<CouponTemplate>()
                .orderByDesc(CouponTemplate::getId));
    }

    @Transactional
    public CouponTemplate createTemplate(CouponTemplateRequest request) {
        validateTemplate(request, 0);
        CouponTemplate template = CouponTemplate.builder()
                .templateName(request.getTemplateName().trim())
                .couponType(request.getCouponType())
                .amountCents(request.getAmountCents())
                .discountRate(request.getDiscountRate())
                .minAmountCents(request.getMinAmountCents() != null ? request.getMinAmountCents() : 0L)
                .totalCount(request.getTotalCount())
                .issuedCount(0)
                .validDays(request.getValidDays())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : 1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        couponTemplateMapper.insert(template);
        log.info("优惠券模板创建: templateId={}, name={}", template.getId(), template.getTemplateName());
        return template;
    }

    @Transactional
    public CouponTemplate updateTemplate(Long templateId, CouponTemplateRequest request) {
        CouponTemplate template = couponTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException(ErrorCode.COUPON_TEMPLATE_NOT_FOUND);
        }
        validateTemplate(request, template.getIssuedCount() != null ? template.getIssuedCount() : 0);

        template.setTemplateName(request.getTemplateName().trim());
        template.setCouponType(request.getCouponType());
        template.setAmountCents(request.getAmountCents());
        template.setDiscountRate(request.getDiscountRate());
        template.setMinAmountCents(request.getMinAmountCents() != null ? request.getMinAmountCents() : 0L);
        template.setTotalCount(request.getTotalCount());
        template.setValidDays(request.getValidDays());
        template.setDescription(request.getDescription());
        template.setStatus(request.getStatus() != null ? request.getStatus() : template.getStatus());
        template.setUpdatedAt(LocalDateTime.now());
        couponTemplateMapper.updateById(template);
        log.info("优惠券模板更新: templateId={}, name={}", templateId, template.getTemplateName());
        return template;
    }

    @Transactional
    public void disableTemplate(Long templateId) {
        CouponTemplate template = couponTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException(ErrorCode.COUPON_TEMPLATE_NOT_FOUND);
        }
        template.setStatus(0);
        template.setUpdatedAt(LocalDateTime.now());
        couponTemplateMapper.updateById(template);
        log.info("优惠券模板下架: templateId={}", templateId);
    }

    private void validateTemplate(CouponTemplateRequest request, int issuedCount) {
        if (request.getTotalCount() != null && request.getTotalCount() < issuedCount) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "发行总量不能小于已发行数量");
        }
        if (TYPE_CASH.equals(request.getCouponType())) {
            if (request.getAmountCents() == null || request.getAmountCents() <= 0) {
                throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "现金券面额必须大于0");
            }
        } else if (TYPE_DISCOUNT.equals(request.getCouponType())) {
            if (request.getDiscountRate() == null) {
                throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "折扣券必须填写折扣率");
            }
        } else {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "券类型仅支持 CASH/DISCOUNT");
        }
    }

    private UserCoupon requireUsableCoupon(Long userId, Long couponId) {
        UserCoupon coupon = userCouponMapper.selectById(couponId);
        if (coupon == null || !coupon.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
        }
        if (!STATUS_AVAILABLE.equals(coupon.getStatus())) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
        }
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getValidFrom() != null && coupon.getValidFrom().isAfter(now)) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
        }
        if (coupon.getValidTo() != null && coupon.getValidTo().isBefore(now)) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
        }
        return coupon;
    }

    private CouponTemplate requireTemplate(Long templateId) {
        CouponTemplate template = couponTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException(ErrorCode.COUPON_TEMPLATE_NOT_FOUND);
        }
        return template;
    }

    private long calculateDiscount(CouponTemplate template, long amountCents) {
        long minAmount = template.getMinAmountCents() != null ? template.getMinAmountCents() : 0L;
        if (amountCents < minAmount) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
        }
        if (TYPE_CASH.equals(template.getCouponType())) {
            Long amount = template.getAmountCents();
            if (amount == null || amount <= 0) {
                throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
            }
            return Math.min(amount, amountCents);
        }
        if (TYPE_DISCOUNT.equals(template.getCouponType())) {
            BigDecimal rate = template.getDiscountRate();
            if (rate == null) {
                throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
            }
            long after = BigDecimal.valueOf(amountCents)
                    .multiply(rate)
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValue();
            return Math.max(0L, amountCents - after);
        }
        throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
    }

    private String generateCouponCode() {
        return "CP" + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase();
    }

}
