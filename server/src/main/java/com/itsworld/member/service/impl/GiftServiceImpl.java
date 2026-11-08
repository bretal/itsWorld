package com.itsworld.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itsworld.common.exception.BusinessException;
import com.itsworld.common.result.ErrorCode;
import com.itsworld.member.dto.GiftView;
import com.itsworld.member.dto.MemberGiftRequest;
import com.itsworld.member.entity.CouponTemplate;
import com.itsworld.member.entity.MemberAccount;
import com.itsworld.member.entity.MemberGift;
import com.itsworld.member.entity.MemberGiftRecord;
import com.itsworld.member.entity.MemberLevel;
import com.itsworld.member.mapper.CouponTemplateMapper;
import com.itsworld.member.mapper.MemberGiftMapper;
import com.itsworld.member.mapper.MemberGiftRecordMapper;
import com.itsworld.member.mapper.MemberLevelMapper;
import com.itsworld.member.service.CouponService;
import com.itsworld.member.service.GiftService;
import com.itsworld.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会员礼包业务实现
 *
 * @author Claude
 * @date 2026-11-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GiftServiceImpl implements GiftService {

    private final MemberGiftMapper memberGiftMapper;
    private final MemberGiftRecordMapper memberGiftRecordMapper;
    private final MemberLevelMapper memberLevelMapper;
    private final CouponTemplateMapper couponTemplateMapper;
    private final MemberService memberService;
    private final CouponService couponService;

    @Override
    public List<GiftView> listGifts(Long userId) {
        List<MemberGift> gifts = memberGiftMapper.selectList(new LambdaQueryWrapper<MemberGift>()
                .eq(MemberGift::getStatus, 1)
                .orderByAsc(MemberGift::getSortOrder)
                .orderByAsc(MemberGift::getId));
        if (gifts.isEmpty()) {
            return List.of();
        }

        MemberAccount account = memberService.getOrCreateAccount(userId);
        MemberLevel currentLevel = account.getLevelId() != null
                ? memberLevelMapper.selectById(account.getLevelId()) : null;
        int currentThreshold = account.isActive() && currentLevel != null && currentLevel.getGrowthThreshold() != null
                ? currentLevel.getGrowthThreshold() : -1;

        List<MemberGiftRecord> records = memberGiftRecordMapper.selectList(
                new LambdaQueryWrapper<MemberGiftRecord>().eq(MemberGiftRecord::getUserId, userId));
        Map<Long, MemberGiftRecord> recordMap = new HashMap<>();
        for (MemberGiftRecord record : records) {
            recordMap.put(record.getGiftId(), record);
        }

        LocalDateTime now = LocalDateTime.now();
        List<GiftView> views = new ArrayList<>();
        for (MemberGift gift : gifts) {
            String requiredLevelName = null;
            boolean levelEligible = true;
            if (gift.getRequiredLevelId() != null) {
                MemberLevel requiredLevel = memberLevelMapper.selectById(gift.getRequiredLevelId());
                if (requiredLevel != null) {
                    requiredLevelName = requiredLevel.getLevelName();
                    levelEligible = currentThreshold >= requiredLevel.getGrowthThreshold();
                } else {
                    levelEligible = false;
                }
            }

            String couponName = null;
            if (gift.getCouponTemplateId() != null) {
                CouponTemplate template = couponTemplateMapper.selectById(gift.getCouponTemplateId());
                if (template != null) {
                    couponName = template.getTemplateName();
                }
            }

            boolean inTime = (gift.getStartAt() == null || !now.isBefore(gift.getStartAt()))
                    && (gift.getEndAt() == null || !now.isAfter(gift.getEndAt()));
            MemberGiftRecord record = recordMap.get(gift.getId());
            boolean claimed = record != null;
            boolean claimable = account.isActive() && !claimed && levelEligible && inTime;

            String lockReason = null;
            if (claimed) {
                lockReason = "已领取";
            } else if (!levelEligible) {
                lockReason = "会员等级不足";
            } else if (!inTime) {
                lockReason = "不在领取时间";
            }

            views.add(GiftView.builder()
                    .id(gift.getId())
                    .giftName(gift.getGiftName())
                    .description(gift.getDescription())
                    .requiredLevelId(gift.getRequiredLevelId())
                    .requiredLevelName(requiredLevelName)
                    .growthReward(gift.getGrowthReward())
                    .couponTemplateId(gift.getCouponTemplateId())
                    .couponTemplateName(couponName)
                    .startAt(gift.getStartAt())
                    .endAt(gift.getEndAt())
                    .claimed(claimed)
                    .claimable(claimable)
                    .lockReason(lockReason)
                    .build());
        }
        return views;
    }

    @Override
    @Transactional
    public void claimGift(Long userId, Long giftId) {
        MemberGift gift = memberGiftMapper.selectById(giftId);
        if (gift == null || !Integer.valueOf(1).equals(gift.getStatus())) {
            throw new BusinessException(ErrorCode.GIFT_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now();
        if (gift.getStartAt() != null && now.isBefore(gift.getStartAt())) {
            throw new BusinessException(ErrorCode.GIFT_NOT_AVAILABLE);
        }
        if (gift.getEndAt() != null && now.isAfter(gift.getEndAt())) {
            throw new BusinessException(ErrorCode.GIFT_NOT_AVAILABLE);
        }
        if (!memberService.getOrCreateAccount(userId).isActive()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR.getCode(), "请先开通有效会员");
        }
        checkLevelEligible(userId, gift.getRequiredLevelId());

        Long existing = memberGiftRecordMapper.selectCount(new LambdaQueryWrapper<MemberGiftRecord>()
                .eq(MemberGiftRecord::getUserId, userId)
                .eq(MemberGiftRecord::getGiftId, giftId));
        if (existing != null && existing > 0) {
            throw new BusinessException(ErrorCode.GIFT_ALREADY_CLAIMED);
        }

        MemberGiftRecord record = MemberGiftRecord.builder()
                .userId(userId)
                .giftId(giftId)
                .growthAmount(gift.getGrowthReward() != null ? gift.getGrowthReward() : 0)
                .status("GRANTED")
                .claimedAt(now)
                .createdAt(now)
                .build();
        try {
            memberGiftRecordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.GIFT_ALREADY_CLAIMED);
        }

        int growth = gift.getGrowthReward() != null ? gift.getGrowthReward() : 0;
        if (growth > 0) {
            memberService.award(userId, growth,
                    "GIFT", "GIFT_RECORD:" + record.getId(), "礼包奖励：" + gift.getGiftName());
        }
        if (gift.getCouponTemplateId() != null) {
            Long couponId = couponService.issueOneCoupon(userId, gift.getCouponTemplateId());
            record.setCouponId(couponId);
            memberGiftRecordMapper.updateById(record);
        }

        log.info("礼包领取成功: userId={}, giftId={}, growth={}", userId, giftId, growth);
    }

    @Override
    public List<MemberGift> getAdminGifts() {
        return memberGiftMapper.selectList(new LambdaQueryWrapper<MemberGift>()
                .orderByAsc(MemberGift::getSortOrder)
                .orderByAsc(MemberGift::getId));
    }

    @Override
    @Transactional
    public MemberGift createGift(MemberGiftRequest request) {
        validateReferences(request);
        MemberGift gift = MemberGift.builder()
                .giftName(request.getGiftName().trim())
                .description(request.getDescription())
                .requiredLevelId(request.getRequiredLevelId())
                .growthReward(request.getGrowthReward() != null ? request.getGrowthReward() : 0)
                .couponTemplateId(request.getCouponTemplateId())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .status(request.getStatus() != null ? request.getStatus() : 1)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        memberGiftMapper.insert(gift);
        log.info("会员礼包创建: giftId={}, name={}", gift.getId(), gift.getGiftName());
        return gift;
    }

    @Override
    @Transactional
    public MemberGift updateGift(Long giftId, MemberGiftRequest request) {
        MemberGift gift = memberGiftMapper.selectById(giftId);
        if (gift == null) {
            throw new BusinessException(ErrorCode.GIFT_NOT_FOUND);
        }
        validateReferences(request);

        gift.setGiftName(request.getGiftName().trim());
        gift.setDescription(request.getDescription());
        gift.setRequiredLevelId(request.getRequiredLevelId());
        gift.setGrowthReward(request.getGrowthReward() != null ? request.getGrowthReward() : gift.getGrowthReward());
        gift.setCouponTemplateId(request.getCouponTemplateId());
        gift.setStartAt(request.getStartAt());
        gift.setEndAt(request.getEndAt());
        gift.setStatus(request.getStatus() != null ? request.getStatus() : gift.getStatus());
        gift.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : gift.getSortOrder());
        gift.setUpdatedAt(LocalDateTime.now());
        memberGiftMapper.updateById(gift);
        log.info("会员礼包更新: giftId={}, name={}", giftId, gift.getGiftName());
        return gift;
    }

    @Override
    @Transactional
    public void disableGift(Long giftId) {
        MemberGift gift = memberGiftMapper.selectById(giftId);
        if (gift == null) {
            throw new BusinessException(ErrorCode.GIFT_NOT_FOUND);
        }
        gift.setStatus(0);
        gift.setUpdatedAt(LocalDateTime.now());
        memberGiftMapper.updateById(gift);
        log.info("会员礼包下架: giftId={}", giftId);
    }

    private void checkLevelEligible(Long userId, Long requiredLevelId) {
        if (requiredLevelId == null) {
            return;
        }
        MemberAccount account = memberService.getOrCreateAccount(userId);
        if (account.getLevelId() == null) {
            throw new BusinessException(ErrorCode.GIFT_LEVEL_NOT_ENOUGH);
        }
        MemberLevel currentLevel = memberLevelMapper.selectById(account.getLevelId());
        MemberLevel requiredLevel = memberLevelMapper.selectById(requiredLevelId);
        if (currentLevel == null || requiredLevel == null
                || currentLevel.getGrowthThreshold() < requiredLevel.getGrowthThreshold()) {
            throw new BusinessException(ErrorCode.GIFT_LEVEL_NOT_ENOUGH);
        }
    }

    private void validateReferences(MemberGiftRequest request) {
        if (request.getRequiredLevelId() != null
                && memberLevelMapper.selectById(request.getRequiredLevelId()) == null) {
            throw new BusinessException(ErrorCode.MEMBER_LEVEL_NOT_FOUND);
        }
        if (request.getCouponTemplateId() != null
                && couponTemplateMapper.selectById(request.getCouponTemplateId()) == null) {
            throw new BusinessException(ErrorCode.COUPON_TEMPLATE_NOT_FOUND);
        }
        if (request.getStartAt() != null && request.getEndAt() != null
                && request.getStartAt().isAfter(request.getEndAt())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "礼包开始时间不能晚于结束时间");
        }
    }

}
