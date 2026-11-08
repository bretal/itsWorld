# 04 状态机（Phase 1~4 实际）

> 日期：2026-11-07
> Phase 1 基础资料、Phase 2 订单/支付、Phase 3 预约/套餐、Phase 4 物流/售后/评价状态机均已实现；真实微信支付/退款待外部资质。

## 1. 已实现（Phase 1）

### 门店 `commerce_store.business_status`
- `OPEN`（营业中，默认）
- `CLOSED`（关闭）
- `SUSPENDED`（暂停营业）
- 管理端通过 `PUT /api/admin/commerce/stores/{id}/status?businessStatus=` 切换；App 门店列表只返回 `OPEN`。

### 员工 `commerce_staff.status`
- `ACTIVE`（在职，默认）
- `INACTIVE`（停用）
- 管理端通过 `PUT /api/admin/commerce/stores/staff/{id}/status?status=` 切换。

### 班次 `commerce_staff_shift.status`
- `ACTIVE`（默认）
- `CLOSED`
- Phase 1 仅维护字段；未做自动排班/容量计算。班次可删除（配置数据）。

### 美容分类/服务/门店价格/商品分类/SKU
- `ENABLED` / `DISABLED`（默认 `ENABLED`；管理端下架用 DISABLED）。
- 商品销售状态单独用 `shop_product.sale_status`：`ON_SALE` / `OFF_SALE`（默认 `OFF_SALE`）。
- App 只返回启用的分类/服务/门店价格/商品/SKU 和上架商品。

### 库存 `shop_inventory`
- `available_qty`（可售）、`locked_qty`（锁定）、`sold_qty`（已售）。
- Phase 1 只通过管理端 `adjustStock` 修改 `available_qty`，写 `shop_inventory_log`；`locked/sold` 保持 0。
- 约束：`available_qty >= 0`；库存流水 `idempotency_key` 唯一。
- Phase 2 订单流程将使用：预占 `available -= qty, locked += qty`；支付确认 `locked -= qty, sold += qty`；取消 `locked -= qty, available += qty`（尚未实现）。

## 2. 已实现（Phase 2）

### 订单
- 已实现商品订单状态：`WAIT_PAY → PAID`；`WAIT_PAY → CANCELLED`（用户取消）；`WAIT_PAY → CLOSED`（管理员关闭/超时任务）。
- 订单类型当前为 `PRODUCT`；`GROOMING/PACKAGE` 留给 Phase 3。
- 支付状态：`UNPAID → PAID`；退款状态字段已建，未使用。
- 订单状态更新使用条件 UPDATE（`WHERE order_status='WAIT_PAY'`），并写 `commerce_order_status_log`。
- 超时：创建订单时 `expire_time = now + 15 分钟`；`OrderExpireJob` 每分钟关闭超时未支付订单并释放库存/优惠券。

### 支付
- 已实现 `commerce_payment_link` 支付记录：`CREATED → PAID`；关闭时 `CREATED → CLOSED`。
- 支付渠道：
  - `WALLET`：调用钱包业务支付，扣本金/赠送批次，写 `wallet_transaction`（PAYMENT）+ `wallet_payment_allocation`；同一事务内订单置 PAID、库存确认、优惠券核销。
  - `MOCK_WECHAT`：仅 dev（`app.payment.mock-enabled=true`），模拟支付成功，不产生钱包资金流水。
  - `WECHAT`：未接入。
- 钱包支付扣减顺序：赠送批次优先（按到期时间升序，无到期最后），再本金批次；冻结金额不可用。
- 退款：`wallet_refund`/`wallet_refund_allocation` 表已建，**退款执行方法/售后流程尚未实现（Phase 4）**。

### 退款/售后
- 建议：售后 `APPLIED → REVIEWING → APPROVED → REFUNDING → REFUNDED`，拒绝 `REJECTED`，异常 `REFUND_FAILED`。
- Phase 2 未实现售后/退款接口；仅建了钱包退款追溯表。

## 3. 已实现（Phase 3）

### 预约 `grooming_booking.status`
- 在线预约：`PENDING_PAYMENT → CONFIRMED → CHECKED_IN → IN_SERVICE → COMPLETED`；`PENDING_PAYMENT → CANCELLED`（用户取消/订单关闭/超时）。
- 套餐预约：创建即 `CONFIRMED`，后续 `CHECKED_IN → IN_SERVICE → COMPLETED`；`CONFIRMED/CHECKED_IN → CANCELLED`（门店取消）。
- 改期：`PENDING_PAYMENT/CONFIRMED` 可改期，状态不变；先锁定新时段容量，再释放旧时段。
- 门店取消已支付在线预约：预约 `CANCELLED`，订单保持 `PAID`，退款待 Phase 4 售后；取消套餐预约会释放冻结次数。

### 时段 `booking_slot`
- `status`: `OPEN/CLOSED`；容量 `available = total - reserved - locked`。
- 创建预约：`locked += 1`（在线）/ 立即 `locked-1, reserved+1`（套餐）。
- 支付成功：`locked-1, reserved+1`；取消/关单/超时：`locked-1`；已确认预约取消/改期：`reserved-1`。
- 被预约或存在预约记录的时段不可删除；容量不可改到小于 `reserved+locked`；存在预约记录时不能修改门店/日期/时间/员工；不能新建或修改到过去日期。

### 套餐模板 `service_package_template.status`
- `ENABLED / DISABLED`；管理端下架不物理删除。

### 用户套餐 `user_service_package.status`
- `ACTIVE`：生效；次数用尽自动 `USED_UP`；过期按 `expire_time` 惰性置 `EXPIRED`；`CANCELLED` 预留。
- 同一订单项幂等发放（`uk_order_item`）；有效期取订单快照 `validDays`。

### 套餐次数 `user_service_package_item`
- `remaining = total_times - used_times - frozen_times`。
- 预约创建：`frozen+1`；预约完成：`frozen-1, used+1`；预约取消：`frozen-1`。

### 套餐核销流水 `service_package_usage.status`
- `FROZEN`（预约创建）→ `USED`（预约完成）/ `RELEASED`（预约取消）；`REVERSED` 预留。
- `idempotency_key = BOOKING_PACKAGE:{bookingId}` 唯一，防止重复冻结。

## 4. 已实现（Phase 4）

### 订单履约 `commerce_order.order_status`
- 商品订单：`PAID/PROCESSING → SHIPPED → COMPLETED`；发货写 `fulfillment_status=SHIPPED`，确认收货/自动确认写 `COMPLETED`。
- 退款不改变订单主状态，只改 `payment_status`：部分退款 `PARTIAL_REFUND`，全额退款 `REFUNDED`。

### 发货单 `commerce_shipment.status`
- `SHIPPED → DELIVERED`；用户确认收货或发货 7 天自动确认后置 `DELIVERED`。
- 一个订单当前只允许一张发货单；仅 `PRODUCT` 订单且 `PAID/PROCESSING` 可发货。

### 售后 `commerce_after_sale.status`
- `APPLIED → APPROVED / REJECTED`；`APPROVED → REFUNDING → REFUNDED`；失败预留 `REFUND_FAILED`（当前失败回滚保留 APPROVED，供重试）。
- 申请规则：未发货 `REFUND_ONLY`；已发货/已完成 `RETURN_REFUND`；同一订单项存在处理中/已退款售后时禁止重复申请。
- 退款成功：库存 `sold → available` 回补；订单项/订单 `refunded_amount` 累加；订单支付状态 `PARTIAL_REFUND/REFUNDED`。
- 退款渠道：`WALLET` 原路退回原批次；`MOCK_WECHAT` 模拟成功；`WECHAT` 未接入。

### 评价 `commerce_review.status`
- `PENDING → APPROVED / HIDDEN`；管理员可回复；一个订单一条评价，订单完成后才可提交。

## 5. Phase 1~4 迁移/回滚注意

- 门店/分类/服务/商品/套餐等配置通过 `status` 下架，不物理删除；营业时间/班次/图片/宠物规则允许删除。
- 时段被预约引用时不可删除；用户套餐/核销流水为履约历史，不物理删除。
- 开发环境回滚可删除 Phase 3 表；生产不得直接删表，需迁移回滚计划。
