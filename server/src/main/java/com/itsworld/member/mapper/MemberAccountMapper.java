package com.itsworld.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itsworld.member.dto.AdminMemberAccountView;
import com.itsworld.member.entity.MemberAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 会员账户数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface MemberAccountMapper extends BaseMapper<MemberAccount> {

    /**
     * 按用户ID查询会员账户
     *
     * @param userId 用户ID
     * @return 会员账户
     */
    @Select("SELECT * FROM member_account WHERE user_id = #{userId} LIMIT 1")
    MemberAccount selectByUserId(@Param("userId") Long userId);

    /**
     * 按用户ID锁定会员账户
     *
     * @param userId 用户ID
     * @return 会员账户
     */
    @Select("SELECT * FROM member_account WHERE user_id = #{userId} FOR UPDATE")
    MemberAccount selectByUserIdForUpdate(@Param("userId") Long userId);

    /**
     * 管理端会员账户分页（以 sys_user 为主表，未开通会员的用户也可见）
     *
     * @param page    分页参数
     * @param keyword 用户名/昵称/手机号关键字
     * @param status  账号状态，可为空
     * @param role    角色，可为空
     * @return 分页结果
     */
    @Select("<script>"
            + "SELECT u.id AS user_id, u.username, u.nick_name, u.phone, u.email, u.role, "
            + "u.status, u.last_login_at, u.created_at, "
            + "ma.level_id, ml.level_name, "
            + "COALESCE(ma.growth_value, 0) AS growth_value, "
            + "COALESCE(wa.principal_balance, 0) AS principal_balance, "
            + "COALESCE(wa.bonus_balance, 0) AS bonus_balance, "
            + "COALESCE(wa.principal_balance, 0) + COALESCE(wa.bonus_balance, 0) "
            + "- COALESCE(wa.principal_frozen, 0) - COALESCE(wa.bonus_frozen, 0) AS available_balance "
            + "FROM sys_user u "
            + "LEFT JOIN member_account ma ON ma.user_id = u.id "
            + "LEFT JOIN member_level ml ON ml.id = ma.level_id "
            + "LEFT JOIN wallet_account wa ON wa.user_id = u.id "
            + "WHERE u.deleted_at IS NULL "
            + "<if test='keyword != null and keyword != \"\"'>"
            + "AND (u.username LIKE CONCAT('%', #{keyword}, '%') "
            + "OR u.phone LIKE CONCAT('%', #{keyword}, '%') "
            + "OR u.nick_name LIKE CONCAT('%', #{keyword}, '%'))"
            + "</if> "
            + "<if test='status != null'> AND u.status = #{status}</if> "
            + "<if test='role != null and role != \"\"'> AND u.role = #{role}</if> "
            + "ORDER BY u.id DESC"
            + "</script>")
    Page<AdminMemberAccountView> selectAdminPage(Page<AdminMemberAccountView> page,
                                                 @Param("keyword") String keyword,
                                                 @Param("status") Integer status,
                                                 @Param("role") String role);

}
