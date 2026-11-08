-- ========================================
-- 商业服务开发环境回滚脚本（仅开发/测试库）
-- ========================================
-- 警告：
-- 1. 本脚本会删除 Phase 1~4 全部 commerce 表及数据，仅用于开发环境重置。
-- 2. 生产环境禁止直接执行；生产回滚必须基于备份与单独迁移方案。
-- 3. 不删除用户、会员、钱包账户、优惠券等核心表；user_coupon 的锁券列保留。
-- 4. 执行前必须确认库名为 its_world，且已完成备份。
-- ========================================

USE its_world;

SET FOREIGN_KEY_CHECKS = 0;

-- Phase 4
DROP TABLE IF EXISTS commerce_review;
DROP TABLE IF EXISTS commerce_after_sale_log;
DROP TABLE IF EXISTS commerce_refund_link;
DROP TABLE IF EXISTS commerce_after_sale_item;
DROP TABLE IF EXISTS commerce_after_sale;
DROP TABLE IF EXISTS commerce_shipment_item;
DROP TABLE IF EXISTS commerce_shipment;

-- Phase 3
DROP TABLE IF EXISTS service_package_usage;
DROP TABLE IF EXISTS user_service_package_item;
DROP TABLE IF EXISTS user_service_package;
DROP TABLE IF EXISTS service_package_option_item;
DROP TABLE IF EXISTS service_package_option;
DROP TABLE IF EXISTS service_package_template_item;
DROP TABLE IF EXISTS service_package_template;
DROP TABLE IF EXISTS grooming_booking_log;
DROP TABLE IF EXISTS grooming_booking;
DROP TABLE IF EXISTS booking_slot;

-- Phase 2
DROP TABLE IF EXISTS wallet_refund_allocation;
DROP TABLE IF EXISTS wallet_refund;
DROP TABLE IF EXISTS wallet_payment_allocation;
DROP TABLE IF EXISTS shop_inventory_reservation;
DROP TABLE IF EXISTS commerce_payment_link;
DROP TABLE IF EXISTS commerce_order_status_log;
DROP TABLE IF EXISTS commerce_order_discount;
DROP TABLE IF EXISTS commerce_order_item;
DROP TABLE IF EXISTS commerce_order;
DROP TABLE IF EXISTS shop_cart_item;
DROP TABLE IF EXISTS user_shipping_address;

-- Phase 1
DROP TABLE IF EXISTS shop_inventory_log;
DROP TABLE IF EXISTS shop_inventory;
DROP TABLE IF EXISTS shop_sku;
DROP TABLE IF EXISTS shop_product_image;
DROP TABLE IF EXISTS shop_product;
DROP TABLE IF EXISTS shop_category;
DROP TABLE IF EXISTS grooming_service_pet_rule;
DROP TABLE IF EXISTS grooming_service_store;
DROP TABLE IF EXISTS grooming_service;
DROP TABLE IF EXISTS grooming_category;
DROP TABLE IF EXISTS commerce_staff_shift;
DROP TABLE IF EXISTS commerce_staff;
DROP TABLE IF EXISTS commerce_store_business_hour;
DROP TABLE IF EXISTS commerce_store;

SET FOREIGN_KEY_CHECKS = 1;
