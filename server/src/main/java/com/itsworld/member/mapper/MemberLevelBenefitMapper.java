package com.itsworld.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.member.dto.MemberBenefitView;
import com.itsworld.member.entity.MemberLevelBenefit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 会员等级权益映射数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface MemberLevelBenefitMapper extends BaseMapper<MemberLevelBenefit> {

    /**
     * 查询某等级的权益列表
     *
     * @param levelId 等级ID
     * @return 权益列表
     */
    @Select("SELECT b.id, b.benefit_name, b.benefit_type, b.benefit_value, b.description "
            + "FROM member_level_benefit lb "
            + "JOIN member_benefit b ON b.id = lb.benefit_id "
            + "WHERE lb.level_id = #{levelId} ORDER BY b.id")
    List<MemberBenefitView> selectBenefitsByLevelId(@Param("levelId") Long levelId);

}
