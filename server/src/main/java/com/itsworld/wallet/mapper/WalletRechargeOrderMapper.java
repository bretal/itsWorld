package com.itsworld.wallet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itsworld.wallet.dto.RechargeOrderView;
import com.itsworld.wallet.entity.WalletRechargeOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 充值单数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface WalletRechargeOrderMapper extends BaseMapper<WalletRechargeOrder> {

    /**
     * 按充值单号查询
     *
     * @param rechargeNo 充值单号
     * @return 充值单
     */
    @Select("SELECT * FROM wallet_recharge_order WHERE recharge_no = #{rechargeNo} LIMIT 1")
    WalletRechargeOrder selectByRechargeNo(@Param("rechargeNo") String rechargeNo);

    /**
     * 按充值单号锁定充值单
     *
     * @param rechargeNo 充值单号
     * @return 充值单
     */
    @Select("SELECT * FROM wallet_recharge_order WHERE recharge_no = #{rechargeNo} FOR UPDATE")
    WalletRechargeOrder selectByRechargeNoForUpdate(@Param("rechargeNo") String rechargeNo);

    /**
     * 充值单展示查询 SQL 片段（不含 WHERE）
     */
    String VIEW_COLUMNS = "SELECT o.id, o.recharge_no, o.user_id, u.username, o.plan_id, p.plan_name, "
            + "o.principal_amount, o.bonus_amount, o.channel, o.payment_method, o.store_name, "
            + "o.operator_id, o.approver_id, o.approved_at, o.reject_reason, o.external_tx_no, "
            + "o.payment_status, o.credit_status, o.plan_snapshot, o.remark, o.paid_at, "
            + "o.credited_at, o.created_at "
            + "FROM wallet_recharge_order o "
            + "LEFT JOIN sys_user u ON u.id = o.user_id "
            + "LEFT JOIN wallet_recharge_plan p ON p.id = o.plan_id ";

    /**
     * 分页查询充值单
     *
     * @param page          分页参数
     * @param userId        用户ID，为空时查询全部（管理端）
     * @param channel       渠道过滤，可为空
     * @param paymentStatus 支付状态过滤，可为空
     * @return 分页结果
     */
    @Select("<script>"
            + VIEW_COLUMNS
            + "WHERE 1 = 1 "
            + "<if test='userId != null'> AND o.user_id = #{userId}</if> "
            + "<if test='channel != null and channel != \"\"'> AND o.channel = #{channel}</if> "
            + "<if test='paymentStatus != null and paymentStatus != \"\"'> AND o.payment_status = #{paymentStatus}</if> "
            + "ORDER BY o.id DESC"
            + "</script>")
    Page<RechargeOrderView> selectViewPage(Page<RechargeOrderView> page,
                                           @Param("userId") Long userId,
                                           @Param("channel") String channel,
                                           @Param("paymentStatus") String paymentStatus);

    /**
     * 按充值单号查询展示信息
     *
     * @param rechargeNo 充值单号
     * @return 充值单展示信息
     */
    @Select(VIEW_COLUMNS + "WHERE o.recharge_no = #{rechargeNo} LIMIT 1")
    RechargeOrderView selectViewByNo(@Param("rechargeNo") String rechargeNo);

}
