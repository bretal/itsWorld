package com.itsworld.wallet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.wallet.entity.WalletAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 钱包账户数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface WalletAccountMapper extends BaseMapper<WalletAccount> {

    /**
     * 按用户ID查询钱包账户
     *
     * @param userId 用户ID
     * @return 钱包账户
     */
    @Select("SELECT * FROM wallet_account WHERE user_id = #{userId} LIMIT 1")
    WalletAccount selectByUserId(@Param("userId") Long userId);

    /**
     * 按用户ID锁定钱包账户
     *
     * @param userId 用户ID
     * @return 钱包账户
     */
    @Select("SELECT * FROM wallet_account WHERE user_id = #{userId} FOR UPDATE")
    WalletAccount selectByUserIdForUpdate(@Param("userId") Long userId);

}
