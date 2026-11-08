package com.itsworld.wallet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.wallet.entity.WalletTransactionDetail;
import org.apache.ibatis.annotations.Mapper;

/**
 * 钱包资金交易批次明细数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface WalletTransactionDetailMapper extends BaseMapper<WalletTransactionDetail> {
}
