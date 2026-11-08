package com.itsworld.commerce.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itsworld.commerce.inventory.dto.InventoryView;
import com.itsworld.commerce.inventory.entity.ShopInventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * SKU库存数据访问接口
 *
 * @author Claude
 * @date 2026-11-07
 */
@Mapper
public interface ShopInventoryMapper extends BaseMapper<ShopInventory> {

    /**
     * 按 SKU 与仓库查询库存
     */
    @Select("SELECT * FROM shop_inventory WHERE sku_id = #{skuId} AND warehouse_id = #{warehouseId} LIMIT 1")
    ShopInventory selectBySkuIdAndWarehouse(@Param("skuId") Long skuId,
                                            @Param("warehouseId") Long warehouseId);

    /**
     * 按 SKU 与仓库锁定库存行
     */
    @Select("SELECT * FROM shop_inventory WHERE sku_id = #{skuId} AND warehouse_id = #{warehouseId} FOR UPDATE")
    ShopInventory selectBySkuIdAndWarehouseForUpdate(@Param("skuId") Long skuId,
                                                     @Param("warehouseId") Long warehouseId);

    /**
     * 管理端库存分页
     */
    @Select("<script>"
            + "SELECT i.id, i.sku_id, s.product_id, p.name AS product_name, p.cover_url AS product_cover_url, "
            + "s.sku_code, s.spec_json, s.sale_price, i.available_qty, i.locked_qty, i.sold_qty, s.status AS sku_status "
            + "FROM shop_inventory i "
            + "JOIN shop_sku s ON s.id = i.sku_id "
            + "JOIN shop_product p ON p.id = s.product_id "
            + "WHERE i.warehouse_id = 0 "
            + "<if test='keyword != null and keyword != \"\"'>"
            + "AND (p.name LIKE CONCAT('%', #{keyword}, '%') OR s.sku_code LIKE CONCAT('%', #{keyword}, '%'))"
            + "</if> "
            + "ORDER BY i.id DESC"
            + "</script>")
    Page<InventoryView> selectViewPage(Page<InventoryView> page, @Param("keyword") String keyword);

    /**
     * 按 SKU 查询库存展示
     */
    @Select("SELECT i.id, i.sku_id, s.product_id, p.name AS product_name, p.cover_url AS product_cover_url, "
            + "s.sku_code, s.spec_json, s.sale_price, i.available_qty, i.locked_qty, i.sold_qty, s.status AS sku_status "
            + "FROM shop_inventory i "
            + "JOIN shop_sku s ON s.id = i.sku_id "
            + "JOIN shop_product p ON p.id = s.product_id "
            + "WHERE i.sku_id = #{skuId} AND i.warehouse_id = 0 LIMIT 1")
    InventoryView selectViewBySkuId(@Param("skuId") Long skuId);

    /**
     * 原子预占：available -= qty, locked += qty
     */
    @Update("UPDATE shop_inventory SET available_qty = available_qty - #{qty}, "
            + "locked_qty = locked_qty + #{qty}, version = version + 1, updated_at = NOW() "
            + "WHERE sku_id = #{skuId} AND warehouse_id = 0 AND available_qty >= #{qty}")
    int reserveStock(@Param("skuId") Long skuId, @Param("qty") int qty);

    /**
     * 支付确认：locked -= qty, sold += qty
     */
    @Update("UPDATE shop_inventory SET locked_qty = locked_qty - #{qty}, "
            + "sold_qty = sold_qty + #{qty}, version = version + 1, updated_at = NOW() "
            + "WHERE sku_id = #{skuId} AND warehouse_id = 0 AND locked_qty >= #{qty}")
    int confirmReservedStock(@Param("skuId") Long skuId, @Param("qty") int qty);

    /**
     * 取消释放：locked -= qty, available += qty
     */
    @Update("UPDATE shop_inventory SET locked_qty = locked_qty - #{qty}, "
            + "available_qty = available_qty + #{qty}, version = version + 1, updated_at = NOW() "
            + "WHERE sku_id = #{skuId} AND warehouse_id = 0 AND locked_qty >= #{qty}")
    int releaseReservedStock(@Param("skuId") Long skuId, @Param("qty") int qty);

    /**
     * 售后退货回补：sold -= qty, available += qty
     */
    @Update("UPDATE shop_inventory SET sold_qty = sold_qty - #{qty}, "
            + "available_qty = available_qty + #{qty}, version = version + 1, updated_at = NOW() "
            + "WHERE sku_id = #{skuId} AND warehouse_id = 0 AND sold_qty >= #{qty}")
    int restoreSoldStock(@Param("skuId") Long skuId, @Param("qty") int qty);

}
