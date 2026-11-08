package com.itsworld.commerce.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.commerce.store.entity.CommerceStore;
import org.apache.ibatis.annotations.Mapper;

/**
 * 门店数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface CommerceStoreMapper extends BaseMapper<CommerceStore> {
}
