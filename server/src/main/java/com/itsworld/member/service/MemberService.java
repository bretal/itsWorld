package com.itsworld.member.service;

import com.itsworld.common.result.PageResult;
import com.itsworld.member.dto.AdminMemberAccountView;
import com.itsworld.member.dto.MemberAwardResult;
import com.itsworld.member.dto.MemberLevelRequest;
import com.itsworld.member.dto.MemberLevelView;
import com.itsworld.member.dto.MemberOverviewResponse;
import com.itsworld.member.entity.MemberAccount;
import com.itsworld.member.entity.MemberGrowthLog;
import com.itsworld.member.entity.MemberLevelHistory;

import java.math.BigDecimal;
import java.util.List;

/**
 * 会员账户与等级业务接口
 *
 * @author Claude
 * @date 2026-11-07
 */
public interface MemberService {

    /**
     * 获取或初始化会员账户
     *
     * @param userId 用户ID
     * @return 会员账户
     */
    MemberAccount getOrCreateAccount(Long userId);

    /**
     * 获取当前会员折扣乘数（无等级时返回 1.00）
     *
     * @param userId 用户ID
     * @return 折扣乘数
     */
    BigDecimal getCurrentDiscountRate(Long userId);

    /**
     * 会员中心总览
     *
     * @param userId 用户ID
     * @return 总览数据
     */
    MemberOverviewResponse getOverview(Long userId);

    /**
     * 查询全部启用等级（含当前等级标记）
     *
     * @param userId 用户ID
     * @return 等级列表
     */
    List<MemberLevelView> getLevels(Long userId);

    /**
     * 成长值流水
     *
     * @param userId   用户ID
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    PageResult<MemberGrowthLog> getGrowthLogs(Long userId, long pageNum, long pageSize);



    /**
     * 等级变更历史
     *
     * @param userId 用户ID
     * @return 历史列表
     */
    List<MemberLevelHistory> getLevelHistory(Long userId);

    /**
     * 记账成长值，不改变付费会员资格
     *
     * @param userId      用户ID
     * @param growthDelta 成长值变动

     * @param sourceType  来源类型
     * @param sourceNo    来源单号（幂等）
     * @param remark      备注
     * @return 发放结果
     */
    MemberAwardResult award(Long userId, int growthDelta,
                            String sourceType, String sourceNo, String remark);

    /**
     * 管理端会员账户分页
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param keyword  关键字
     * @param status   账号状态（0/1），可为空
     * @param role     角色（USER/ADMIN），可为空
     * @return 分页结果
     */
    PageResult<AdminMemberAccountView> getAdminAccounts(long pageNum, long pageSize,
                                                        String keyword, Integer status, String role);

    /**
     * 管理端启用/禁用用户
     *
     * @param operatorId 当前管理员ID
     * @param userId     目标用户ID
     * @param status     1-启用 0-禁用
     */
    void updateUserStatus(Long operatorId, Long userId, Integer status);

    /**
     * 管理端全部等级（含停用）
     *
     * @return 等级列表
     */
    List<MemberLevelView> getAdminLevels();

    /**
     * 新建等级
     *
     * @param request 等级配置
     * @return 等级视图
     */
    MemberLevelView createLevel(MemberLevelRequest request);

    /**
     * 更新等级
     *
     * @param levelId 等级ID
     * @param request 等级配置
     * @return 等级视图
     */
    MemberLevelView updateLevel(Long levelId, MemberLevelRequest request);

    /**
     * 下架等级
     *
     * @param levelId 等级ID
     */
    void disableLevel(Long levelId);

}

