package com.itsworld.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itsworld.common.exception.BusinessException;
import com.itsworld.common.result.ErrorCode;
import com.itsworld.common.result.PageResult;
import com.itsworld.member.dto.AdminMemberAccountView;
import com.itsworld.member.dto.MemberAwardResult;
import com.itsworld.member.dto.MemberBenefitRequest;
import com.itsworld.member.dto.MemberBenefitView;
import com.itsworld.member.dto.MemberLevelRequest;
import com.itsworld.member.dto.MemberLevelView;
import com.itsworld.member.dto.MemberOverviewResponse;
import com.itsworld.member.entity.MemberAccount;
import com.itsworld.member.entity.MemberBenefit;
import com.itsworld.member.entity.MemberGrowthLog;
import com.itsworld.member.entity.MemberLevel;
import com.itsworld.member.entity.MemberLevelBenefit;
import com.itsworld.member.entity.MemberLevelHistory;
import com.itsworld.member.mapper.MemberAccountMapper;
import com.itsworld.member.mapper.MemberBenefitMapper;
import com.itsworld.member.mapper.MemberGrowthLogMapper;
import com.itsworld.member.mapper.MemberLevelBenefitMapper;
import com.itsworld.member.mapper.MemberLevelHistoryMapper;
import com.itsworld.member.mapper.MemberLevelMapper;
import com.itsworld.member.service.MemberService;
import com.itsworld.user.entity.User;
import com.itsworld.user.mapper.UserMapper;
import com.itsworld.wallet.dto.WalletSummaryResponse;
import com.itsworld.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 会员账户与等级业务实现
 *
 * @author Claude
 * @date 2026-11-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberLevelMapper memberLevelMapper;
    private final MemberBenefitMapper memberBenefitMapper;
    private final MemberLevelBenefitMapper memberLevelBenefitMapper;
    private final MemberAccountMapper memberAccountMapper;
    private final MemberLevelHistoryMapper memberLevelHistoryMapper;
    private final MemberGrowthLogMapper memberGrowthLogMapper;
    private final UserMapper userMapper;
    private final WalletService walletService;

    @Override
    @Transactional
    public MemberAccount getOrCreateAccount(Long userId) {
        MemberAccount account = memberAccountMapper.selectByUserId(userId);
        if (account != null) {
            return account;
        }

        MemberLevel defaultLevel = findLowestActiveLevel();
        account = MemberAccount.builder()
                .userId(userId)
                .levelId(defaultLevel != null ? defaultLevel.getId() : null)
                .growthValue(0)
                .version(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        try {
            memberAccountMapper.insert(account);
        } catch (DuplicateKeyException e) {
            log.info("会员账户已由其他请求创建: userId={}", userId);
            account = memberAccountMapper.selectByUserId(userId);
        }
        if (account == null) {
            throw new BusinessException(ErrorCode.MEMBER_ACCOUNT_NOT_FOUND);
        }
        return account;
    }

    @Override
    public BigDecimal getCurrentDiscountRate(Long userId) {
        MemberAccount account = getOrCreateAccount(userId);
        if (!account.isActive() || account.getLevelId() == null) {
            return BigDecimal.ONE;
        }
        MemberLevel level = memberLevelMapper.selectById(account.getLevelId());
        return level != null && level.getDiscountRate() != null
                ? level.getDiscountRate() : BigDecimal.ONE;
    }

    @Override
    public MemberOverviewResponse getOverview(Long userId) {
        MemberAccount account = getOrCreateAccount(userId);
        MemberLevel level = account.isActive() && account.getLevelId() != null
                ? memberLevelMapper.selectById(account.getLevelId()) : null;
        List<MemberBenefitView> benefits = level != null
                ? memberLevelBenefitMapper.selectBenefitsByLevelId(level.getId()) : List.of();
        int growth = account.getGrowthValue() != null ? account.getGrowthValue() : 0;

        WalletSummaryResponse wallet = walletService.getSummary(userId);

        return MemberOverviewResponse.builder()
                .userId(userId)
                .levelId(level != null ? level.getId() : null)
                .levelCode(level != null ? level.getLevelCode() : null)
                .levelName(level != null ? level.getLevelName() : "未开通会员")
                .membershipStatus(account.getMemberExpiresAt() == null ? "NOT_OPEN" : account.isActive() ? "ACTIVE" : "EXPIRED")
                .memberExpiresAt(account.getMemberExpiresAt())
                .growthValue(growth)
                .benefits(benefits)
                .wallet(wallet)
                .build();
    }

    @Override
    public List<MemberLevelView> getLevels(Long userId) {
        MemberAccount account = memberAccountMapper.selectByUserId(userId);
        Long currentLevelId = account != null && account.isActive() ? account.getLevelId() : null;

        List<MemberLevel> levels = memberLevelMapper.selectList(new LambdaQueryWrapper<MemberLevel>()
                .eq(MemberLevel::getStatus, 1)
                .orderByAsc(MemberLevel::getGrowthThreshold)
                .orderByAsc(MemberLevel::getSortOrder));
        Set<Long> levelIds = new HashSet<>();
        for (MemberLevel level : levels) {
            levelIds.add(level.getId());
        }
        if (currentLevelId != null && !levelIds.contains(currentLevelId)) {
            MemberLevel currentLevel = memberLevelMapper.selectById(currentLevelId);
            if (currentLevel != null) {
                levels.add(currentLevel);
                levels.sort(Comparator.comparing(MemberLevel::getGrowthThreshold));
            }
        }

        List<MemberLevelView> views = new ArrayList<>();
        for (MemberLevel level : levels) {
            views.add(buildLevelView(level, currentLevelId));
        }
        return views;
    }

    @Override
    public PageResult<MemberGrowthLog> getGrowthLogs(Long userId, long pageNum, long pageSize) {
        Page<MemberGrowthLog> page = new Page<>(pageNum, pageSize);
        Page<MemberGrowthLog> result = memberGrowthLogMapper.selectPage(page,
                new LambdaQueryWrapper<MemberGrowthLog>()
                        .eq(MemberGrowthLog::getUserId, userId)
                        .orderByDesc(MemberGrowthLog::getCreatedAt)
                        .orderByDesc(MemberGrowthLog::getId));
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public List<MemberLevelHistory> getLevelHistory(Long userId) {
        return memberLevelHistoryMapper.selectList(new LambdaQueryWrapper<MemberLevelHistory>()
                .eq(MemberLevelHistory::getUserId, userId)
                .orderByDesc(MemberLevelHistory::getCreatedAt)
                .orderByDesc(MemberLevelHistory::getId)
                .last("LIMIT 50"));
    }

    @Override
    @Transactional
    public MemberAwardResult award(Long userId, int growthDelta,
                                   String sourceType, String sourceNo, String remark) {
        getOrCreateAccount(userId);
        MemberAccount account = memberAccountMapper.selectByUserIdForUpdate(userId);
        if (account == null) {
            throw new BusinessException(ErrorCode.MEMBER_ACCOUNT_NOT_FOUND);
        }

        int oldGrowth = account.getGrowthValue() != null ? account.getGrowthValue() : 0;
        int newGrowth = Math.addExact(oldGrowth, growthDelta);
        if (newGrowth < 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR.getCode(), "成长值不足");
        }
        LocalDateTime now = LocalDateTime.now();
        MemberGrowthLog growthLog = MemberGrowthLog.builder().userId(userId)
                .changeValue(growthDelta).balanceAfter(newGrowth).sourceType(sourceType)
                .sourceNo(sourceNo).remark(remark).createdAt(now).build();
        try {
            memberGrowthLogMapper.insert(growthLog);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.MEMBER_AWARD_DUPLICATE);
        }
        account.setGrowthValue(newGrowth);
        account.setVersion(account.getVersion() + 1);
        account.setUpdatedAt(now);
        memberAccountMapper.updateById(account);
        return MemberAwardResult.builder().growthAwarded(growthDelta).levelUp(false).build();
    }

    @Override
    public PageResult<AdminMemberAccountView> getAdminAccounts(long pageNum, long pageSize,
                                                               String keyword, Integer status, String role) {
        Page<AdminMemberAccountView> page = new Page<>(pageNum, pageSize);
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String normalizedRole = StringUtils.hasText(role) ? role.trim().toUpperCase() : null;
        Page<AdminMemberAccountView> result = memberAccountMapper.selectAdminPage(
                page, normalizedKeyword, status, normalizedRole);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public void updateUserStatus(Long operatorId, Long userId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM.getCode(), "状态只能为0或1");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (status == 0 && userId.equals(operatorId)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR.getCode(), "不能禁用当前登录账号");
        }
        if (status == 0 && "ADMIN".equalsIgnoreCase(user.getRole())) {
            Long activeAdmins = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getRole, "ADMIN")
                    .eq(User::getStatus, 1));
            if (activeAdmins != null && activeAdmins <= 1) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR.getCode(), "至少保留一个启用状态的管理员");
            }
        }
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        log.info("用户状态更新: operatorId={}, userId={}, status={}", operatorId, userId, status);
    }

    @Override
    public List<MemberLevelView> getAdminLevels() {
        List<MemberLevel> levels = memberLevelMapper.selectList(new LambdaQueryWrapper<MemberLevel>()
                .orderByAsc(MemberLevel::getGrowthThreshold)
                .orderByAsc(MemberLevel::getSortOrder));
        List<MemberLevelView> views = new ArrayList<>();
        for (MemberLevel level : levels) {
            views.add(buildLevelView(level, null));
        }
        return views;
    }

    @Override
    @Transactional
    public MemberLevelView createLevel(MemberLevelRequest request) {
        validateLevelUnique(request, null);
        MemberLevel level = MemberLevel.builder()
                .levelCode(request.getLevelCode().trim())
                .levelName(request.getLevelName().trim())
                .growthThreshold(request.getGrowthThreshold())
                .iconUrl(request.getIconUrl())
                .discountRate(request.getDiscountRate() != null ? request.getDiscountRate() : BigDecimal.ONE)
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : 1)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        memberLevelMapper.insert(level);
        saveBenefits(level.getId(), request.getBenefits());
        log.info("会员等级创建: levelId={}, code={}", level.getId(), level.getLevelCode());
        return buildLevelView(level, null);
    }

    @Override
    @Transactional
    public MemberLevelView updateLevel(Long levelId, MemberLevelRequest request) {
        MemberLevel level = memberLevelMapper.selectById(levelId);
        if (level == null) {
            throw new BusinessException(ErrorCode.MEMBER_LEVEL_NOT_FOUND);
        }
        validateLevelUnique(request, levelId);

        // 保证系统始终保留一个启用的 0 门槛等级，避免新用户无法初始化等级
        Integer newStatus = request.getStatus() != null ? request.getStatus() : level.getStatus();
        boolean remainsNormal = Integer.valueOf(1).equals(newStatus)
                && request.getGrowthThreshold() != null
                && request.getGrowthThreshold() == 0;
        if (!remainsNormal) {
            Long otherNormalCount = memberLevelMapper.selectCount(new LambdaQueryWrapper<MemberLevel>()
                    .eq(MemberLevel::getStatus, 1)
                    .eq(MemberLevel::getGrowthThreshold, 0)
                    .ne(MemberLevel::getId, levelId));
            if (otherNormalCount == null || otherNormalCount == 0) {
                throw new BusinessException(ErrorCode.MEMBER_LEVEL_LAST_NORMAL);
            }
        }

        level.setLevelCode(request.getLevelCode().trim());
        level.setLevelName(request.getLevelName().trim());
        level.setGrowthThreshold(request.getGrowthThreshold());
        level.setIconUrl(request.getIconUrl());
        level.setDiscountRate(request.getDiscountRate() != null ? request.getDiscountRate() : BigDecimal.ONE);
        level.setDescription(request.getDescription());
        level.setStatus(request.getStatus() != null ? request.getStatus() : level.getStatus());
        level.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : level.getSortOrder());
        level.setUpdatedAt(LocalDateTime.now());
        memberLevelMapper.updateById(level);

        // 权益采用“整组重写”：删除映射后重新创建；仅清理不再被任何等级引用的权益定义
        replaceBenefits(levelId, request.getBenefits());

        log.info("会员等级更新: levelId={}, code={}", levelId, level.getLevelCode());
        return buildLevelView(level, null);
    }

    @Override
    @Transactional
    public void disableLevel(Long levelId) {
        MemberLevel level = memberLevelMapper.selectById(levelId);
        if (level == null) {
            throw new BusinessException(ErrorCode.MEMBER_LEVEL_NOT_FOUND);
        }
        if (level.getGrowthThreshold() != null && level.getGrowthThreshold() == 0) {
            Long count = memberLevelMapper.selectCount(new LambdaQueryWrapper<MemberLevel>()
                    .eq(MemberLevel::getStatus, 1)
                    .eq(MemberLevel::getGrowthThreshold, 0));
            if (count != null && count <= 1) {
                throw new BusinessException(ErrorCode.MEMBER_LEVEL_LAST_NORMAL);
            }
        }
        level.setStatus(0);
        level.setUpdatedAt(LocalDateTime.now());
        memberLevelMapper.updateById(level);
        log.info("会员等级下架: levelId={}", levelId);
    }

    private MemberLevelView buildLevelView(MemberLevel level, Long currentLevelId) {
        List<MemberBenefitView> benefits = memberLevelBenefitMapper.selectBenefitsByLevelId(level.getId());
        return MemberLevelView.builder()
                .id(level.getId())
                .levelCode(level.getLevelCode())
                .levelName(level.getLevelName())
                .growthThreshold(level.getGrowthThreshold())
                .iconUrl(level.getIconUrl())
                .discountRate(level.getDiscountRate())
                .description(level.getDescription())
                .status(level.getStatus())
                .sortOrder(level.getSortOrder())
                .current(currentLevelId != null && Objects.equals(currentLevelId, level.getId()))
                .benefits(benefits)
                .build();
    }

    private void replaceBenefits(Long levelId, List<MemberBenefitRequest> benefits) {
        List<MemberLevelBenefit> oldMappings = memberLevelBenefitMapper.selectList(
                new LambdaQueryWrapper<MemberLevelBenefit>()
                        .eq(MemberLevelBenefit::getLevelId, levelId));
        Set<Long> oldBenefitIds = new HashSet<>();
        for (MemberLevelBenefit mapping : oldMappings) {
            oldBenefitIds.add(mapping.getBenefitId());
        }
        memberLevelBenefitMapper.delete(new LambdaQueryWrapper<MemberLevelBenefit>()
                .eq(MemberLevelBenefit::getLevelId, levelId));
        for (Long benefitId : oldBenefitIds) {
            Long referenced = memberLevelBenefitMapper.selectCount(
                    new LambdaQueryWrapper<MemberLevelBenefit>()
                            .eq(MemberLevelBenefit::getBenefitId, benefitId));
            if (referenced == null || referenced == 0) {
                memberBenefitMapper.deleteById(benefitId);
            }
        }
        saveBenefits(levelId, benefits);
    }

    private void saveBenefits(Long levelId, List<MemberBenefitRequest> benefits) {
        if (benefits == null || benefits.isEmpty()) {
            return;
        }
        for (MemberBenefitRequest request : benefits) {
            MemberBenefit benefit = MemberBenefit.builder()
                    .benefitName(request.getBenefitName().trim())
                    .benefitType(request.getBenefitType())
                    .benefitValue(request.getBenefitValue())
                    .description(request.getDescription())
                    .createdAt(LocalDateTime.now())
                    .build();
            memberBenefitMapper.insert(benefit);
            memberLevelBenefitMapper.insert(MemberLevelBenefit.builder()
                    .levelId(levelId)
                    .benefitId(benefit.getId())
                    .createdAt(LocalDateTime.now())
                    .build());
        }
    }

    private void validateLevelUnique(MemberLevelRequest request, Long excludeLevelId) {
        LambdaQueryWrapper<MemberLevel> codeWrapper = new LambdaQueryWrapper<MemberLevel>()
                .eq(MemberLevel::getLevelCode, request.getLevelCode().trim());
        LambdaQueryWrapper<MemberLevel> thresholdWrapper = new LambdaQueryWrapper<MemberLevel>()
                .eq(MemberLevel::getGrowthThreshold, request.getGrowthThreshold());
        if (excludeLevelId != null) {
            codeWrapper.ne(MemberLevel::getId, excludeLevelId);
            thresholdWrapper.ne(MemberLevel::getId, excludeLevelId);
        }
        Long codeCount = memberLevelMapper.selectCount(codeWrapper);
        if (codeCount != null && codeCount > 0) {
            throw new BusinessException(ErrorCode.MEMBER_LEVEL_CODE_EXISTS);
        }
        Long thresholdCount = memberLevelMapper.selectCount(thresholdWrapper);
        if (thresholdCount != null && thresholdCount > 0) {
            throw new BusinessException(ErrorCode.MEMBER_LEVEL_THRESHOLD_EXISTS);
        }
    }

    private MemberLevel findLowestActiveLevel() {
        return memberLevelMapper.selectOne(new LambdaQueryWrapper<MemberLevel>()
                .eq(MemberLevel::getStatus, 1)
                .orderByAsc(MemberLevel::getGrowthThreshold)
                .last("LIMIT 1"));
    }

}
