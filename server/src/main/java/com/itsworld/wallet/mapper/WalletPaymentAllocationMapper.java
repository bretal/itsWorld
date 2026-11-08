package com.itsworld.wallet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.wallet.entity.WalletPaymentAllocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 钱包支付分摊数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface WalletPaymentAllocationMapper extends BaseMapper<WalletPaymentAllocation> {

    @Select("SELECT * FROM wallet_payment_allocation WHERE pay_order_id = #{payOrderId} ORDER BY id")
    List<WalletPaymentAllocation> selectByPayOrderId(@Param("payOrderId") Long payOrderId);

}
