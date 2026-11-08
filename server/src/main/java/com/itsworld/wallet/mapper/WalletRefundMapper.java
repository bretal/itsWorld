package com.itsworld.wallet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.wallet.entity.WalletRefund;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 钱包退款单数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface WalletRefundMapper extends BaseMapper<WalletRefund> {

    @Select("SELECT * FROM wallet_refund WHERE refund_no = #{refundNo} LIMIT 1")
    WalletRefund selectByRefundNo(@Param("refundNo") String refundNo);

}
