package com.itsworld.commerce.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itsworld.commerce.order.entity.CommerceOrderStatusLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商业订单状态日志数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface CommerceOrderStatusLogMapper extends BaseMapper<CommerceOrderStatusLog> {
}
