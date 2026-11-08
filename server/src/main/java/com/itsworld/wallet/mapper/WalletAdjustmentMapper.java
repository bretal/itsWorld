package com.itsworld.wallet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itsworld.wallet.dto.WalletAdjustmentView;
import com.itsworld.wallet.entity.WalletAdjustment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 钱包余额调账数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface WalletAdjustmentMapper extends BaseMapper<WalletAdjustment> {

    /**
     * 调账记录分页
     *
     * @param page   分页参数
     * @param userId 用户ID，可为空
     * @return 分页结果
     */
    @Select("<script>"
            + "SELECT a.id, a.adjustment_no, a.user_id, u.username, a.principal_delta, a.bonus_delta, "
            + "a.principal_before, a.principal_after, a.bonus_before, a.bonus_after, "
            + "a.reason, a.operator_id, op.username AS operator_name, a.status, a.created_at "
            + "FROM wallet_adjustment a "
            + "LEFT JOIN sys_user u ON u.id = a.user_id "
            + "LEFT JOIN sys_user op ON op.id = a.operator_id "
            + "WHERE 1 = 1 "
            + "<if test='userId != null'> AND a.user_id = #{userId}</if> "
            + "ORDER BY a.id DESC"
            + "</script>")
    Page<WalletAdjustmentView> selectViewPage(Page<WalletAdjustmentView> page,
                                              @Param("userId") Long userId);

    /**
     * 按调账单号查询展示信息
     *
     * @param adjustmentNo 调账单号
     * @return 调账展示信息
     */
    @Select("SELECT a.id, a.adjustment_no, a.user_id, u.username, a.principal_delta, a.bonus_delta, "
            + "a.principal_before, a.principal_after, a.bonus_before, a.bonus_after, "
            + "a.reason, a.operator_id, op.username AS operator_name, a.status, a.created_at "
            + "FROM wallet_adjustment a "
            + "LEFT JOIN sys_user u ON u.id = a.user_id "
            + "LEFT JOIN sys_user op ON op.id = a.operator_id "
            + "WHERE a.adjustment_no = #{adjustmentNo} LIMIT 1")
    WalletAdjustmentView selectViewByNo(@Param("adjustmentNo") String adjustmentNo);

}
