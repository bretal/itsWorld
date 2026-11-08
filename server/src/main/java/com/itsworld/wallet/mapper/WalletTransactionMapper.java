package com.itsworld.wallet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itsworld.wallet.dto.WalletTransactionView;
import com.itsworld.wallet.entity.WalletTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 钱包资金流水数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface WalletTransactionMapper extends BaseMapper<WalletTransaction> {

    /**
     * 分页查询资金流水（用户端/管理端共用）
     *
     * @param page   分页参数
     * @param userId 用户ID，为空时查询全部（管理端）
     * @param txType 交易类型过滤，可为空
     * @return 分页结果
     */
    @Select("<script>"
            + "SELECT wt.id, wt.tx_no, wt.user_id, u.username, wt.tx_type, wt.business_type, "
            + "wt.business_no, wt.principal_delta, wt.bonus_delta, wt.principal_after, "
            + "wt.bonus_after, wt.remark, wt.created_at "
            + "FROM wallet_transaction wt "
            + "LEFT JOIN sys_user u ON u.id = wt.user_id "
            + "WHERE 1 = 1 "
            + "<if test='userId != null'> AND wt.user_id = #{userId}</if> "
            + "<if test='txType != null and txType != \"\"'> AND wt.tx_type = #{txType}</if> "
            + "ORDER BY wt.id DESC"
            + "</script>")
    Page<WalletTransactionView> selectViewPage(Page<WalletTransactionView> page,
                                               @Param("userId") Long userId,
                                               @Param("txType") String txType);

}
