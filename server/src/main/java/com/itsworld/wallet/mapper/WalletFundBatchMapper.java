package com.itsworld.wallet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.wallet.entity.WalletFundBatch;
import org.apache.ibatis.annotations.Mapper;

/**
 * 钱包资金批次数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface WalletFundBatchMapper extends BaseMapper<WalletFundBatch> {
}
