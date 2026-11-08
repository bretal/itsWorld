package com.itsworld.wallet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.wallet.entity.WalletRechargePlan;
import org.apache.ibatis.annotations.Mapper;

/**
 * 充值方案数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface WalletRechargePlanMapper extends BaseMapper<WalletRechargePlan> {
}
