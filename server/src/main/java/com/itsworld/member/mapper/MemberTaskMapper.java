package com.itsworld.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.member.entity.MemberTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员任务数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface MemberTaskMapper extends BaseMapper<MemberTask> {
}
