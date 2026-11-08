package com.itsworld.commerce.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itsworld.commerce.inventory.dto.InventoryLogView;
import com.itsworld.commerce.inventory.entity.ShopInventoryLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 库存流水数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface ShopInventoryLogMapper extends BaseMapper<ShopInventoryLog> {

    /**
     * 库存流水分页
     */
    @Select("<script>"
            + "SELECT l.id, l.sku_id, s.sku_code, p.name AS product_name, l.biz_type, l.biz_id, "
            + "l.change_available, l.change_locked, l.before_available, l.after_available, "
            + "l.idempotency_key, l.remark, l.created_at "
            + "FROM shop_inventory_log l "
            + "JOIN shop_sku s ON s.id = l.sku_id "
            + "JOIN shop_product p ON p.id = s.product_id "
            + "WHERE 1 = 1 "
            + "<if test='skuId != null'> AND l.sku_id = #{skuId}</if> "
            + "ORDER BY l.id DESC"
            + "</script>")
    Page<InventoryLogView> selectViewPage(Page<InventoryLogView> page, @Param("skuId") Long skuId);

}
