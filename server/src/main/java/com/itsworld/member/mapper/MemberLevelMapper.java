package com.itsworld.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.member.entity.MemberLevel;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员等级数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface MemberLevelMapper extends BaseMapper<MemberLevel> {
}
