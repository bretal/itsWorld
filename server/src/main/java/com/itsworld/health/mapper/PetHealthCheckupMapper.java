package com.itsworld.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.health.entity.PetHealthCheckup;
import org.apache.ibatis.annotations.Mapper;

/**
 * 宠物体检记录数据访问接口
 *
 * @author Claude
 * @date 2026-11-05
 */
@Mapper
public interface PetHealthCheckupMapper extends BaseMapper<PetHealthCheckup> {
}
