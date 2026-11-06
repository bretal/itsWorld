package com.itsworld.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.health.entity.PetFeedingRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 宠物喂食记录数据访问接口
 *
 * @author Claude
 * @date 2026-11-05
 */
@Mapper
public interface PetFeedingRecordMapper extends BaseMapper<PetFeedingRecord> {
}
