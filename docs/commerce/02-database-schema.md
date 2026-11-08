# 02 数据库实际变更（Phase 1 + Phase 2 + Phase 3 + Phase 4）

> 日期：2026-11-07
> 迁移脚本：`init-scripts/mysql/08-commerce-base.sql`、`09-commerce-order.sql`、`10-wallet-payment-refund.sql`、`11-commerce-booking-package.sql`、`12-commerce-fulfillment-aftersale-review.sql`
> 状态：已在本地 `its_world` 执行；新数据卷按 01→12 自动执行。
> 说明：本文件只记录**实际已创建**的表。

## 1. 总览

| 项 | 值 |
|---|---|
| Phase 1 新增表 | 14 |
| Phase 2 新增表 | 11（8 交易 + 3 钱包支付/退款） |
| Phase 3 新增表 | 10（3 预约 + 7 套餐） |
| Phase 4 新增表 | 7（2 物流 + 4 售后 + 1 评价） |
| 当前库总表数 | 80（原 38 + Phase 1 14 + Phase 2 11 + Phase 3 10 + Phase 4 7） |
| 金额 | `BIGINT` 整数分 |
| 时间 | `TIMESTAMP`（沿用项目规范，不使用文档建议的 `DATETIME(3)`） |
| 主键 | `BIGINT PRIMARY KEY AUTO_INCREMENT` |
| 字符集 | `utf8mb4` / `utf8mb4_unicode_ci` |
| 逻辑删除 | 新表不使用 `deleted_at`；配置类用 `status` 停用/下架 |
| 审计 | `created_at`（全部）、`updated_at`（可变配置）；操作人暂由 `remark`/后续 `sys_operation_log` 记录 |

## 2. 门店域（4 张）

### 2.1 `commerce_store`
- 字段：`id, store_code, name, phone, province, city, district, address, longitude DECIMAL(10,6), latitude DECIMAL(10,6), business_status, description, created_at, updated_at`
- 唯一键：`uk_store_code(store_code)`
- 索引：`idx_business_status`
- 状态：`OPEN/CLOSED/SUSPENDED`，默认 `OPEN`

### 2.2 `commerce_store_business_hour`
- 字段：`id, store_id, day_of_week TINYINT(1-7), open_time TIME, close_time TIME, special_date DATE NULL, closed TINYINT, created_at, updated_at`
- 索引：`idx_store_day(store_id, day_of_week, special_date)`
- 外键：`store_id → commerce_store.id ON DELETE CASCADE`

### 2.3 `commerce_staff`
- 字段：`id, store_id, staff_name, mobile, role_code, status, created_at, updated_at`
- 索引：`idx_store_status(store_id, status)`
- 状态：`ACTIVE/INACTIVE`
- 外键：`store_id → commerce_store.id ON DELETE CASCADE`
- 注意：当前没有 `user_id`，员工尚不能映射到 `sys_user`；门店级后台数据隔离后续阶段补。

### 2.4 `commerce_staff_shift`
- 字段：`id, staff_id, store_id, shift_date DATE, start_time TIME, end_time TIME, capacity INT, status, created_at, updated_at`
- 索引：`idx_staff_date(staff_id, shift_date)`、`idx_store_date(store_id, shift_date)`
- 外键：`staff_id → commerce_staff.id CASCADE`、`store_id → commerce_store.id CASCADE`

## 3. 美容域（4 张）

### 3.1 `grooming_category`
- 字段：`id, name, icon_url, sort, status, created_at, updated_at`
- 索引：`idx_status_sort(status, sort)`
- 状态：`ENABLED/DISABLED`

### 3.2 `grooming_service`
- 字段：`id, category_id, service_code, name, cover_url, description, duration_min, base_price BIGINT, status, sort, created_at, updated_at`
- 唯一键：`uk_service_code(service_code)`
- 索引：`idx_category_status(category_id, status)`
- 外键：`category_id → grooming_category.id ON DELETE SET NULL`

### 3.3 `grooming_service_store`
- 字段：`id, service_id, store_id, sale_price BIGINT, capacity_per_slot INT, status, created_at, updated_at`
- 唯一键：`uk_service_store(service_id, store_id)`
- 索引：`idx_store_status(store_id, status)`
- 外键：service/store 均 `ON DELETE CASCADE`

### 3.4 `grooming_service_pet_rule`
- 字段：`id, service_id, species_code, min_weight_gram, max_weight_gram, min_age_month, max_age_month, note, created_at, updated_at`
- 索引：`idx_service(service_id)`
- 外键：`service_id → grooming_service.id CASCADE`
- 当前业务解释：**没有规则=不限制；有规则时任一规则匹配即可**（Phase 1 实现）。如产品要求“白名单”语义需调整。

## 4. 商城域（6 张）

### 4.1 `shop_category`
- 字段：`id, parent_id, name, icon_url, sort, status, created_at, updated_at`
- 索引：`idx_parent_sort(parent_id, sort)`
- 外键：`parent_id → shop_category.id ON DELETE SET NULL`
- 状态：`ENABLED/DISABLED`

### 4.2 `shop_product`
- 字段：`id, product_code, category_id, name, subtitle, description LONGTEXT, cover_url, sale_status, sort, created_at, updated_at`
- 唯一键：`uk_product_code(product_code)`
- 索引：`idx_category_status(category_id, sale_status)`
- 外键：`category_id → shop_category.id ON DELETE SET NULL`
- 销售状态：`ON_SALE/OFF_SALE`（默认 `OFF_SALE`）

### 4.3 `shop_product_image`
- 字段：`id, product_id, image_url, sort, created_at`
- 索引：`idx_product_sort(product_id, sort)`
- 外键：`product_id → shop_product.id ON DELETE CASCADE`

### 4.4 `shop_sku`
- 字段：`id, product_id, sku_code, spec_json JSON NULL, sale_price BIGINT, market_price BIGINT, weight_gram, status, version, created_at, updated_at`
- 唯一键：`uk_sku_code(sku_code)`
- 索引：`idx_product_status(product_id, status)`
- 外键：`product_id → shop_product.id ON DELETE CASCADE`
- 创建 SKU 时由 Service 自动插入一条 `shop_inventory`（warehouse_id=0，数量 0）。

### 4.5 `shop_inventory`
- 字段：`id, sku_id, warehouse_id BIGINT NOT NULL DEFAULT 0, available_qty, locked_qty, sold_qty, version, created_at, updated_at`
- 唯一键：`uk_sku_warehouse(sku_id, warehouse_id)`（warehouse_id 非空，规避 MySQL NULL 唯一键问题）
- 外键：`sku_id → shop_sku.id ON DELETE CASCADE`
- 当前只调整 `available_qty`；`locked_qty/sold_qty` 留给 Phase 2 订单预占/确认。

### 4.6 `shop_inventory_log`
- 字段：`id, sku_id, biz_type, biz_id, change_available, change_locked, before_available, after_available, idempotency_key, remark, created_at`
- 唯一键：`uk_idempotency(idempotency_key)`
- 索引：`idx_sku_time(sku_id, created_at)`
- 外键：`sku_id → shop_sku.id ON DELETE CASCADE`
- `remark` 为 Phase 1 扩展字段，用于记录管理员调整原因/操作人；订单库存流水 Phase 2 再写入 `biz_type=ORDER_*`。

## 5. 交易域（Phase 2，8 张）

### 5.1 `user_shipping_address`
- 字段：`id, user_id, recipient_name, mobile, province, city, district, address_detail, is_default, created_at, updated_at`
- 索引：`idx_user(user_id)`；外键 user CASCADE。
- 订单提交时保存地址快照到 `commerce_order.address_snapshot`。

### 5.2 `shop_cart_item`
- 字段：`id, user_id, sku_id, quantity, selected, invalid_reason, created_at, updated_at`
- 唯一键：`uk_user_sku(user_id, sku_id)`；外键 user/sku CASCADE。

### 5.3 `commerce_order`
- 字段：`id, order_no, user_id, order_type, order_status, payment_status, fulfillment_status, original_amount, discount_amount, payable_amount, paid_amount, refunded_amount, coupon_id, address_snapshot JSON, store_snapshot JSON, expire_time, paid_time, close_time, cancel_reason, remark, idempotency_key, created_at, updated_at`
- 唯一键：`uk_order_no(order_no)`、`uk_user_idempotency(user_id, idempotency_key)`
- 索引：`idx_user_status(user_id, order_status, created_at)`；外键 user CASCADE、coupon SET NULL。
- 状态：`WAIT_PAY/PAID/CANCELLED/CLOSED`（Phase 2 商品订单）；支付状态 `UNPAID/PAID`。

### 5.4 `commerce_order_item`
- 字段：`id, order_id, item_type, target_id, sku_id, pet_id, quantity, title_snapshot, spec_snapshot JSON, unit_price, original_total, discount_allocated, payable_total, fulfilled_qty, refunded_amount, created_at`
- 索引：`idx_order(order_id)`、`idx_target(item_type,target_id)`；外键 order CASCADE、sku SET NULL。

### 5.5 `commerce_order_discount`
- 字段：`id, order_id, order_item_id, discount_type, source_id, discount_amount, rule_snapshot JSON, created_at`
- `discount_type`：Phase 2 写入 `MEMBER`、`COUPON`；外键 order CASCADE、item SET NULL。

### 5.6 `commerce_order_status_log`
- 字段：`id, order_id, from_status, to_status, action, operator_type, operator_id, remark, created_at`
- 索引：`idx_order_time(order_id, created_at)`；外键 order CASCADE。

### 5.7 `commerce_payment_link`
- 字段：`id, order_id, payment_order_id, pay_channel, pay_amount, status, idempotency_key, external_tx_no, paid_time, fail_reason, created_at, updated_at`
- 唯一键：`uk_payment_order(payment_order_id)`、`uk_idempotency(idempotency_key)`；索引 order。
- Phase 2 支付渠道：`WALLET`、`MOCK_WECHAT`；`WECHAT` 未接入。
- 状态：`CREATED/PAID/FAILED/CLOSED`。

### 5.8 `shop_inventory_reservation`
- 字段：`id, reservation_no, order_id, order_item_id, sku_id, qty, status, expire_time, created_at, updated_at`
- 唯一键：`uk_reservation_no`、`uk_order_item(order_item_id)`；索引 `idx_expire(status,expire_time)`。
- 状态：`RESERVED/CONFIRMED/RELEASED`；支付确认后 locked→sold，取消/超时释放回 available。

## 6. 钱包支付/退款扩展（Phase 2，3 张 + 1 个 ALTER）

### 6.1 `wallet_payment_allocation`
- 字段：`id, pay_order_id(commerce_payment_link.id), wallet_tx_id, batch_id, fund_type, paid_amount, refunded_amount, created_at`
- 索引 pay_order/wallet_tx/batch；外键 payment link CASCADE、wallet_transaction、wallet_fund_batch SET NULL。
- 钱包支付按批次记录分摊；赠送优先、按到期时间升序，再本金。

### 6.2 `wallet_refund`
- 字段：`id, refund_no, pay_order_id, business_order_no, amount, status, reason, external_refund_no, created_at, updated_at`
- 唯一键 `uk_refund_no`；外键 payment link。
- **表已建，退款执行方法尚未实现（Phase 4 售后）**。

### 6.3 `wallet_refund_allocation`
- 字段：`id, refund_id, original_payment_allocation_id, amount, restored_batch_id, created_at`
- 用于原支付批次追溯；**Phase 4 使用**。

### 6.4 `user_coupon` ALTER
- 新增：`lock_order_no VARCHAR(64) NULL`、`used_order_no VARCHAR(64) NULL`、`version INT NOT NULL DEFAULT 0`
- 锁定/核销/释放/恢复方法已实现；对应 `user_coupon.status` 的 `LOCKED/USED`。

## 7. 预约与套餐域（Phase 3，10 张）

### 7.1 预约（3 张）

- `booking_slot`
  - 字段：`id, store_id, staff_id(默认0=不限), service_date DATE, start_time TIME, end_time TIME, total_capacity, reserved_capacity, locked_capacity, status(OPEN/CLOSED), version, created_at, updated_at`
  - 唯一键 `uk_slot(store_id, staff_id, service_date, start_time, end_time)`；`staff_id` 非空默认 0，规避 NULL 唯一键失效。
  - 容量语义：`available = total - reserved - locked`；预约创建锁定 `locked+1`，支付确认 `locked-1 / reserved+1`，取消/超时释放。
- `grooming_booking`
  - 字段：`id, booking_no, user_id, pet_id, store_id, staff_id NULL, slot_id, order_id NULL, service_id, status, reserve_expire_time, checkin_time, completed_time, cancel_reason, created_at, updated_at`
  - 状态：`PENDING_PAYMENT / CONFIRMED / CHECKED_IN / IN_SERVICE / COMPLETED / CANCELLED`。
  - 在线预约生成 `commerce_order.order_type='GROOMING'`；套餐次数预约 `order_id=NULL`。
- `grooming_booking_log`
  - 字段：`id, booking_id, from_status, to_status, action, operator_type(USER/ADMIN/SYSTEM), operator_id, remark, created_at`；状态日志追加式。

### 7.2 套餐（7 张）

- `service_package_template`
  - 字段：`id, package_code, name, cover_url, description, sale_price BIGINT, valid_days NULL(长期), status(ENABLED/DISABLED), allow_cross_store, version, created_at, updated_at`
  - 唯一键 `uk_package_code`。
- `service_package_template_item`：固定服务项，`package_id, service_id, included_times, selectable_group, extra_price, sort`。
- `service_package_option`：选项组，`package_id, option_code, option_name, min_select, max_select, required, sort`；唯一键 `uk_package_option(package_id, option_code)`。
- `service_package_option_item`：选项候选服务，`option_id, service_id, add_price, included_times, status`；唯一键 `uk_option_service(option_id, service_id)`。
- `user_service_package`
  - 字段：`id, package_instance_no, user_id, pet_id NULL, package_id, order_item_id, store_id NULL, name_snapshot, selection_snapshot JSON, start_time, expire_time NULL, status(ACTIVE/USED_UP/EXPIRED/CANCELLED), created_at, updated_at`
  - 唯一键 `uk_instance_no`、`uk_order_item(order_item_id)`；订单支付成功后按订单项幂等发放，绑定单宠物与购买门店。
- `user_service_package_item`
  - 字段：`id, user_package_id, service_id, total_times, used_times, frozen_times, service_snapshot JSON`；唯一键 `uk_instance_service(user_package_id, service_id)`。
  - 次数语义：`remaining = total - used - frozen`；预约冻结、完成核销 `frozen-1/used+1`、取消释放 `frozen-1`。
- `service_package_usage`
  - 字段：`id, usage_no, user_package_id, package_item_id, booking_id NULL, service_id, qty, status(FROZEN/USED/RELEASED/REVERSED), operator_id, used_time, idempotency_key, created_at, updated_at`
  - 唯一键 `uk_usage_no`、`uk_idempotency(idempotency_key)`；预约创建写 `FROZEN`，完成 `USED`，取消 `RELEASED`。

## 8. 物流/售后/评价域（Phase 4，7 张）

### 8.1 物流（2 张）

- `commerce_shipment`
  - 字段：`id, shipment_no, order_id, delivery_company, tracking_no, status(SHIPPED/DELIVERED), shipped_time, received_time, remark, created_at, updated_at`
  - 唯一键 `uk_shipment_no`；索引 `idx_order`、`idx_status_time(status, shipped_time)`；订单 CASCADE。
  - 一个订单当前只允许一张发货单（Service 层校验）；发货后订单 `PAID/PROCESSING → SHIPPED`，确认收货/7 天自动确认后 `SHIPPED → COMPLETED`。
- `commerce_shipment_item`
  - 字段：`id, shipment_id, order_item_id, sku_id NULL, qty, created_at`
  - 唯一键 `uk_shipment_item(shipment_id, order_item_id)`；发货时按订单项剩余数量写明细并回写 `commerce_order_item.fulfilled_qty`。

### 8.2 售后（4 张）

- `commerce_after_sale`
  - 字段：`id, after_sale_no, order_id, user_id, type(REFUND_ONLY/RETURN_REFUND), reason, description, evidence_urls JSON, amount, status, review_remark, reviewed_by, reviewed_at, refunded_at, created_at, updated_at`
  - 状态：`APPLIED → APPROVED / REJECTED`；`APPROVED → REFUNDING → REFUNDED`；失败预留 `REFUND_FAILED`。
  - 规则：未发货只能 `REFUND_ONLY`；已发货/已完成只能 `RETURN_REFUND`；同一订单项存在处理中/已退款售后时拒绝重复申请。
- `commerce_after_sale_item`
  - 字段：`id, after_sale_id, order_item_id, sku_id NULL, qty, amount`
  - 唯一键 `uk_after_sale_item(after_sale_id, order_item_id)`；金额按订单项实付金额与数量比例计算，并受已退金额上限约束。
- `commerce_refund_link`
  - 字段：`id, refund_no, after_sale_id, payment_link_id, channel(WALLET/MOCK_WECHAT/WECHAT), amount, status(CREATED/REFUNDED/FAILED), external_refund_no, fail_reason, created_at, updated_at`
  - 唯一键 `uk_refund_no`；钱包退款同时写 `wallet_refund`/`wallet_refund_allocation`，模拟微信退款写 `MOCK-` 外部单号。
- `commerce_after_sale_log`
  - 字段：`id, after_sale_id, from_status, to_status, action, operator_type(USER/ADMIN/SYSTEM), operator_id, remark, created_at`；追加式状态日志。

### 8.3 评价（1 张）

- `commerce_review`
  - 字段：`id, order_id, user_id, rating TINYINT(1-5), content, images JSON, status(PENDING/APPROVED/HIDDEN), reply, replied_at, created_at, updated_at`
  - 唯一键 `uk_order(order_id)`：一个订单一条评价；订单完成后才可提交；管理端审核通过/隐藏/回复。

## 9. 迁移与回滚

- 已有数据卷按顺序执行：
  ```bash
  docker exec -i its-world-mysql sh -c 'mysql -uitsworld -p"$MYSQL_PASSWORD" --default-character-set=utf8mb4 its_world' < init-scripts/mysql/08-commerce-base.sql
  docker exec -i its-world-mysql sh -c 'mysql -uitsworld -p"$MYSQL_PASSWORD" --default-character-set=utf8mb4 its_world' < init-scripts/mysql/09-commerce-order.sql
  docker exec -i its-world-mysql sh -c 'mysql -uitsworld -p"$MYSQL_PASSWORD" --default-character-set=utf8mb4 its_world' < init-scripts/mysql/10-wallet-payment-refund.sql
  docker exec -i its-world-mysql sh -c 'mysql -uitsworld -p"$MYSQL_PASSWORD" --default-character-set=utf8mb4 its_world' < init-scripts/mysql/11-commerce-booking-package.sql
  docker exec -i its-world-mysql sh -c 'mysql -uitsworld -p"$MYSQL_PASSWORD" --default-character-set=utf8mb4 its_world' < init-scripts/mysql/12-commerce-fulfillment-aftersale-review.sql
  ```
- 新数据卷：Docker 按 01→12 自动执行。
- `10-wallet-payment-refund.sql` 包含非幂等 `ALTER TABLE user_coupon`，同一库只执行一次。
- 回滚（仅开发环境）：`init-scripts/mysql/rollback/99-commerce-dev-down.sql` 按外键依赖逆序删除 Phase 1~4 共 42 张 commerce 表；生产不得直接删表，需迁移回滚计划。
- 当前本地测试数据：门店/服务/商品/SKU/库存；商品订单 4 笔；Phase 3 预约 10 笔（含在线支付/套餐核销/取消/改期）、2 个用户套餐、4 条核销流水、若干时段与 2 个套餐模板；Phase 4 发货 1 单、售后 2 单（钱包退款 3990、模拟微信退款 3910）、评价 1 条；钱包支付分摊 1 条；优惠券 2 张已用、1 张已释放。
