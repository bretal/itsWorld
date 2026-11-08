package com.itsworld.admin.mapper;

import com.itsworld.admin.dto.AdminOverviewResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 管理后台总览数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface AdminOverviewMapper {

    /**
     * 聚合用户、会员、钱包与线下充值数据
     *
     * @return 总览数据
     */
    @Select("SELECT "
            + "(SELECT COUNT(*) FROM sys_user WHERE deleted_at IS NULL) AS total_users, "
            + "(SELECT COUNT(*) FROM sys_user WHERE deleted_at IS NULL AND status = 1) AS active_users, "
            + "(SELECT COUNT(*) FROM sys_user WHERE deleted_at IS NULL AND status = 0) AS disabled_users, "
            + "(SELECT COUNT(*) FROM sys_user WHERE deleted_at IS NULL AND role = 'ADMIN') AS admin_users, "
            + "(SELECT COUNT(*) FROM member_account) AS member_count, "
            + "(SELECT COALESCE(SUM(principal_balance), 0) FROM wallet_account) AS total_principal_balance, "
            + "(SELECT COALESCE(SUM(bonus_balance), 0) FROM wallet_account) AS total_bonus_balance, "
            + "(SELECT COALESCE(SUM(principal_balance + bonus_balance - principal_frozen - bonus_frozen), 0) "
            + " FROM wallet_account) AS total_available_balance, "
            + "(SELECT COALESCE(SUM(principal_delta + bonus_delta), 0) FROM wallet_transaction "
            + " WHERE tx_type = 'RECHARGE' AND created_at >= CURDATE()) AS today_recharge_amount, "
            + "(SELECT COUNT(*) FROM wallet_recharge_order "
            + " WHERE channel = 'OFFLINE' AND payment_status = 'CREATED') AS pending_offline_recharges")
    AdminOverviewResponse selectOverview();

}
