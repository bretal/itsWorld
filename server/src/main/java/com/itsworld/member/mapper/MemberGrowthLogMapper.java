package com.itsworld.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.member.entity.MemberGrowthLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员成长值流水数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface MemberGrowthLogMapper extends BaseMapper<MemberGrowthLog> {
}
