package com.itsworld.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.member.entity.MemberGiftRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员礼包领取记录数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface MemberGiftRecordMapper extends BaseMapper<MemberGiftRecord> {
}
