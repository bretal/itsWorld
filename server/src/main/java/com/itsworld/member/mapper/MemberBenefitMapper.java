package com.itsworld.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.member.entity.MemberBenefit;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员权益数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface MemberBenefitMapper extends BaseMapper<MemberBenefit> {
}
