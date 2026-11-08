package com.itsworld.wallet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.wallet.entity.WalletRefundAllocation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 钱包退款分摊数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface WalletRefundAllocationMapper extends BaseMapper<WalletRefundAllocation> {
}
