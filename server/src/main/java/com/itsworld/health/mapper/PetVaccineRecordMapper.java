package com.itsworld.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.health.entity.PetVaccineRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 宠物疫苗记录数据访问接口
 *
 * @author Claude
 * @date 2026-11-05
 */
@Mapper
public interface PetVaccineRecordMapper extends BaseMapper<PetVaccineRecord> {
}
