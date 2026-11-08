package com.itsworld.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.member.entity.MemberLevelHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员等级变更历史数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface MemberLevelHistoryMapper extends BaseMapper<MemberLevelHistory> {
}
