# 03 API 契约（Phase 1~4）

> 日期：2026-11-07
> 范围：门店、美容、商品、SKU、库存、订单/支付、预约、套餐、物流、售后、评价。
> 统一响应：`Result<T>`，`code=0` 成功；分页 `PageResult<T>`（`records/total/pageNum/pageSize/totalPages`）。
> 鉴权：App 接口需登录 JWT；管理端 `/api/admin/**` 需 `ROLE_ADMIN`。
> 错误：Phase 1 复用现有 `ErrorCode`（400 参数、404 资源不存在、401/403 鉴权），未新增 commerce 错误码段。

## 1. 用户端接口

### 1.1 门店
`GET /api/commerce/stores?page&size&keyword`
- 返回营业中门店分页：`id, storeCode, name, phone, province, city, district, address, longitude, latitude, businessStatus, description`。

`GET /api/commerce/stores/{storeId}`
- 返回门店详情 + `hours[]`（`dayOfWeek, openTime, closeTime, specialDate, closed`）。

### 1.2 美容
`GET /api/commerce/grooming/categories`
- 返回启用分类：`id, name, iconUrl, sort, status`。

`GET /api/commerce/grooming/services?storeId&categoryId&keyword&page&size`
- 返回启用服务分页；`storeId` 可选，传则只返回该门店已配置且启用的服务，并计算门店价。
- 每条包含：`id, categoryId, categoryName, serviceCode, name, coverUrl, description, durationMin, basePrice, storePrice, memberPrice, status, sort`。
- `memberPrice = storePrice × 当前会员等级 discount_rate`（分，四舍五入）。

`GET /api/commerce/grooming/services/{serviceId}?storeId&petId`
- 返回服务详情 + `petRules[]` + `petEligible` + `ineligibleReason`。
- `petId` 可选；传则用 `PetService#getPetInfo(userId,petId)` 校验归属并检查规则。
- 规则为空=不限制；有规则时任一匹配即通过（Phase 1 语义）。

### 1.3 商城
`GET /api/commerce/shop/categories`
- 返回启用分类。

`GET /api/commerce/shop/products?categoryId&keyword&page&size`
- 返回上架商品分页：`id, productCode, categoryId, categoryName, name, subtitle, coverUrl, saleStatus, minPrice, minMemberPrice, sort`。
- `minPrice` 为启用 SKU 最低销售价；无 SKU 时为 `null`。
- `minMemberPrice = minPrice × 会员折扣`。

`GET /api/commerce/shop/products/{productId}`
- 返回上架商品详情：商品字段、`images[]`（`id/productId/imageUrl/sort`）、`skus[]`（`id, skuCode, specJson, salePrice, marketPrice, memberPrice, weightGram, status, availableQty`）。
- 仅返回启用 SKU。

## 2. 管理端接口

### 2.1 门店 `/api/admin/commerce/stores`
- `GET /?page&size&keyword` → 全部门店分页。
- `POST /` body `{storeCode,name,phone,province,city,district,address,longitude,latitude,businessStatus,description}` → 门店。
- `PUT /{storeId}` → 门店。
- `PUT /{storeId}/status?businessStatus=OPEN|CLOSED|SUSPENDED`。
- `GET /{storeId}/hours` → 营业时间列表。
- `POST /{storeId}/hours` body `{dayOfWeek,openTime,closeTime,specialDate?,closed?}`。
- `PUT /hours/{hourId}`、`DELETE /hours/{hourId}`。
- `GET /{storeId}/staff` → 员工列表。
- `POST /{storeId}/staff` body `{staffName,mobile?,roleCode?,status?}`。
- `PUT /staff/{staffId}`、`PUT /staff/{staffId}/status?status=ACTIVE|INACTIVE`。
- `GET /staff/{staffId}/shifts?date=YYYY-MM-DD`。
- `POST /staff/{staffId}/shifts` body `{shiftDate,startTime,endTime,capacity,status?}`。
- `PUT /shifts/{shiftId}`、`DELETE /shifts/{shiftId}`。

### 2.2 美容 `/api/admin/commerce/grooming`
- `GET /categories`；`POST /categories`；`PUT /categories/{id}`；`PUT /categories/{id}/status?status=ENABLED|DISABLED`。
- `GET /services?categoryId&keyword&page&size`；`GET /services/{id}`；`POST /services`；`PUT /services/{id}`；`PUT /services/{id}/status?status=ENABLED|DISABLED`。
- `GET /services/{serviceId}/stores`；`POST /services/{serviceId}/stores` body `{storeId,salePrice,capacityPerSlot,status?}`；`PUT /service-stores/{id}`；`DELETE /service-stores/{id}`。
- `GET /services/{serviceId}/pet-rules`；`POST /services/{serviceId}/pet-rules` body `{speciesCode,minWeightGram?,maxWeightGram?,minAgeMonth?,maxAgeMonth?,note?}`；`PUT /pet-rules/{id}`；`DELETE /pet-rules/{id}`。

### 2.3 商城 `/api/admin/commerce/shop`
- `GET /categories`；`POST /categories`；`PUT /categories/{id}`；`PUT /categories/{id}/status?status=ENABLED|DISABLED`。
- `GET /products?categoryId&keyword&page&size`；`GET /products/{id}`（含图片与全部 SKU）。
- `POST /products` body `{productCode,categoryId?,name,subtitle?,description?,coverUrl?,saleStatus?,sort?}`；`PUT /products/{id}`；`PUT /products/{id}/sale-status?saleStatus=ON_SALE|OFF_SALE`。
- `GET /products/{productId}/images`；`POST /products/{productId}/images` body `{imageUrl,sort?}`；`DELETE /images/{imageId}`。
- `POST /products/{productId}/skus` body `{skuCode,specJson?,salePrice,marketPrice,weightGram?,status?}`；`PUT /skus/{skuId}`；`PUT /skus/{skuId}/status?status=ENABLED|DISABLED`。

### 2.4 库存 `/api/admin/commerce/inventory`
- `GET /?page&size&keyword` → 库存分页：`id, skuId, productId, productName, productCoverUrl, skuCode, specJson, salePrice, availableQty, lockedQty, soldQty, skuStatus`。
- `POST /adjust` body `{skuId,changeQty,reason}`（正数增加、负数减少）；写 `shop_inventory_log`；可售库存不能为负。
- `GET /logs?skuId&page&size` → 库存流水：`id, skuId, skuCode, productName, bizType, bizId, changeAvailable, changeLocked, beforeAvailable, afterAvailable, idempotencyKey, remark, createdAt`。

## 3. Phase 2 交易接口

### 3.1 收货地址 `/api/commerce/addresses`
- `GET /` → 当前用户地址列表。
- `POST /` body `AddressSaveRequest` → 地址；首个地址自动默认。
- `PUT /{addressId}` → 地址。
- `DELETE /{addressId}`。
- `PUT /{addressId}/default` → 设为默认。

### 3.2 购物车 `/api/commerce/cart`
- `GET /` → `CartItemView[]`（含 productName/coverUrl/skuCode/specJson/salePrice/memberPrice/quantity/selected/availableQty/invalidReason）。
- `POST /items` body `{skuId,quantity}` → 加购；库存不足直接拒绝。
- `PUT /items/{cartItemId}` body `{quantity?,selected?}`。
- `DELETE /items/{cartItemId}`。

### 3.3 结算 `/api/commerce/checkout`
- `POST /preview` body `{couponId?}`：
  - 读取当前用户 `selected=1` 购物车项；
  - 返回 `items[]`（unitPrice/memberUnitPrice/originalTotal/memberDiscount/payableTotal/availableQty/invalidReason）、`originalAmount`、`memberDiscount`、`couponDiscount`、`shippingFee=0`、`payableAmount`、`couponOptions[]`、`walletAvailableBalance`、`valid`。
  - 会员价按当前等级 `discount_rate`；优惠券作用于会员价后金额。
- `POST /submit` body `{idempotencyKey, couponId?, addressId, remark?}`：
  - 服务端重新试算并校验库存/地址归属/优惠券；
  - 创建 `commerce_order`、订单项、优惠明细；预占库存；锁定优惠券；删除已选购物车项；
  - 返回 `{orderId, orderNo, payableAmount, expireTime}`（15 分钟支付超时）。
  - 同一 `userId + idempotencyKey` 重复提交返回原订单。

### 3.4 用户订单 `/api/commerce/orders`
- `GET /?status&page&size` → 分页订单详情。
- `GET /{orderNo}` → 订单详情（items/discounts/latestPayment）。
- `POST /{orderNo}/cancel?reason=` → 仅 WAIT_PAY；释放库存、释放优惠券、关闭待支付记录。

### 3.5 支付 `/api/commerce/orders/{orderNo}`
- `POST /payments` body `{channel: WALLET|MOCK_WECHAT|WECHAT}`：
  - `WALLET`：调用 `WalletService#payForBusinessOrder`，按赠送优先/到期优先扣批次，写 `wallet_transaction` + `wallet_payment_allocation`；同一事务内订单置 PAID、确认库存、核销优惠券；
  - `MOCK_WECHAT`：仅 `app.payment.mock-enabled=true`，开发环境模拟微信成功；
  - `WECHAT`：未接入，返回业务错误（需商户资质/openid）。
- `GET /payment-status` → 最新支付记录与订单支付状态。

### 3.6 管理端订单 `/api/admin/commerce/orders`
- `GET /?status&keyword&page&size` → 订单分页（keyword 按订单号）。
- `GET /{orderNo}` → 详情。
- `POST /{orderNo}/close?reason=` → 仅 WAIT_PAY；释放库存/优惠券；写状态日志。

### 3.7 定时任务
- `OrderExpireJob` 每分钟扫描 `WAIT_PAY` 且 `expire_time < now` 的订单，条件更新为 CLOSED，释放库存/优惠券，写 SYSTEM 状态日志。

## 4. Phase 3 预约与套餐接口

### 4.1 预约（用户端）

- `GET /api/commerce/booking/slots?storeId&date=YYYY-MM-DD`
  - 返回该门店当天 `OPEN` 且有余量的时段：`id, storeId, staffId, serviceDate, startTime, endTime, totalCapacity, reservedCapacity, lockedCapacity, availableCapacity, status`。
- `GET /api/commerce/booking/package-options?serviceId&storeId`
  - 返回当前用户可用于该服务（且满足门店限制）的套餐次数项：`userPackageItemId, userPackageId, packageInstanceNo, packageName, petId, petName, storeId, storeName, serviceId, serviceName, totalTimes, usedTimes, frozenTimes, remainingTimes, expireTime`。
- `POST /api/commerce/bookings` body：
  - `{storeId, serviceId, petId, slotId, payType: ONLINE|PACKAGE, userPackageItemId?, couponId?, idempotencyKey?}`
  - `ONLINE`：服务端按门店价 + 会员折扣 + 可选优惠券试算，创建 `GROOMING` 订单（WAIT_PAY，15 分钟），锁定时段容量，预约状态 `PENDING_PAYMENT`；返回 `bookingId, bookingNo, status, orderId, orderNo, payableAmount, expireTime`。
  - `PACKAGE`：校验套餐归属/宠物/门店/次数，冻结 1 次，预约直接 `CONFIRMED`，同时写 `service_package_usage(FROZEN)`；返回 booking 信息，无订单。
  - 同一 `userId + idempotencyKey` 重复提交返回原预约。
- `GET /api/commerce/bookings?status&page&size` → 当前用户预约分页。
- `GET /api/commerce/bookings/{bookingNo}` → 预约详情 + 状态日志。
- `POST /api/commerce/bookings/{bookingNo}/cancel?reason=` → 仅 `PENDING_PAYMENT` 在线预约；取消订单并释放时段/优惠券。
- `POST /api/commerce/bookings/{bookingNo}/reschedule` body `{slotId}` → `PENDING_PAYMENT/CONFIRMED` 可改期；释放旧时段、锁定新时段；已确认预约改期后新时段直接确认。

### 4.2 预约（管理端）

- `GET /api/admin/commerce/bookings/slots?storeId&date&status&page&size`
- `POST /api/admin/commerce/bookings/slots` body `BookingSlotSaveRequest`
- `PUT /api/admin/commerce/bookings/slots/{slotId}`；`DELETE /api/admin/commerce/bookings/slots/{slotId}`
- `POST /api/admin/commerce/bookings/slots/generate` body `{storeId, staffId?, serviceDate, startTime, endTime, intervalMinutes(>=15), totalCapacity}` → 批量生成，已存在时段跳过。
- `GET /api/admin/commerce/bookings?status&date&storeId&keyword&page&size`
- `GET /api/admin/commerce/bookings/{bookingNo}`
- `POST /api/admin/commerce/bookings/{bookingNo}/checkin|start|complete|cancel|reschedule`
  - 状态流转：`CONFIRMED→CHECKED_IN→IN_SERVICE→COMPLETED`；取消允许 `PENDING_PAYMENT/CONFIRMED/CHECKED_IN`。
  - 完成：套餐预约核销次数（FROZEN→USED），在线预约订单置 COMPLETED。
  - 取消：释放容量；套餐预约释放冻结次数；已支付在线预约订单保持 PAID，退款待 Phase 4 售后处理。

### 4.3 套餐（用户端）

- `GET /api/commerce/packages?keyword&page&size` → 启用套餐分页（含固定项/选项组）。
- `GET /api/commerce/packages/{templateId}` → 套餐详情。
- `POST /api/commerce/packages/preview` body `{templateId, optionItemIds?, couponId?}`
  - 返回 `originalAmount, memberDiscount, couponDiscount, payableAmount, walletAvailableBalance, items[], couponOptions[]`；预览不强制选项必选，提交时强校验。
- `POST /api/commerce/packages/purchase` body `{templateId, petId, storeId, optionItemIds?, couponId?, idempotencyKey}`
  - 原价 = 模板售价 + 选中选项加价；会员折扣后可用一张券；创建 `PACKAGE` 订单（WAIT_PAY，15 分钟）；同幂等键返回原订单。
- `GET /api/commerce/my-packages?status&page&size`；`GET /api/commerce/my-packages/{userPackageId}`；`GET /api/commerce/my-packages/{userPackageId}/usages`。
- 套餐订单支付成功后自动发放 `user_service_package` + `user_service_package_item`（按订单快照，幂等），订单置 COMPLETED。

### 4.4 套餐（管理端）

- `GET /api/admin/commerce/packages?status&keyword&page&size`
- `GET /api/admin/commerce/packages/{templateId}`
- `POST /api/admin/commerce/packages`、`PUT /api/admin/commerce/packages/{templateId}`
- `PUT /api/admin/commerce/packages/{templateId}/status?status=ENABLED|DISABLED`
- 保存请求含 `packageCode, name, coverUrl, description, salePrice, validDays?, status?, allowCrossStore?, items[], options[]`；选项组含 `optionCode, optionName, minSelect, maxSelect, required, items[]`。

## 5. Phase 4 物流/售后/评价接口

### 5.1 物流

- `GET /api/commerce/orders/{orderNo}/shipment` → 当前用户订单发货信息：`shipmentNo, orderNo, deliveryCompany, trackingNo, status(SHIPPED/DELIVERED), shippedTime, receivedTime, items[]`。
- `POST /api/commerce/orders/{orderNo}/confirm-receipt` → 确认收货；发货单 `SHIPPED → DELIVERED`，订单 `SHIPPED → COMPLETED`。
- 管理端：
  - `GET /api/admin/commerce/shipments?status&keyword&page&size`
  - `GET /api/admin/commerce/shipments/order/{orderNo}`
  - `POST /api/admin/commerce/shipments/order/{orderNo}` body `{deliveryCompany, trackingNo, remark?}` → 仅 `PRODUCT` 且 `PAID/PROCESSING` 订单可发货；写发货单/明细，订单置 `SHIPPED`。
- 自动确认：`ShipmentAutoConfirmJob` 每小时扫描发货超过 7 天的 `SHIPPED` 发货单，自动置 `DELIVERED` 并完成订单。

### 5.2 售后

- `POST /api/commerce/after-sales` body：
  - `{orderNo, type: REFUND_ONLY|RETURN_REFUND, reason, description?, evidenceUrls?, items: [{orderItemId, qty}]}`
  - 未发货仅允许 `REFUND_ONLY`；已发货/已完成仅允许 `RETURN_REFUND`；只支持 `PRODUCT` 订单项；同一订单项存在处理中/已退款售后时拒绝。
  - 金额按订单项实付金额比例计算，受已退金额上限约束。
- `GET /api/commerce/after-sales?status&page&size`；`GET /api/commerce/after-sales/{afterSaleNo}`。
- 管理端：
  - `GET /api/admin/commerce/after-sales?status&keyword&page&size`
  - `GET /api/admin/commerce/after-sales/{afterSaleNo}`
  - `POST /api/admin/commerce/after-sales/{afterSaleNo}/review` body `{approved: true|false, remark?}` → `APPLIED → APPROVED/REJECTED`
  - `POST /api/admin/commerce/after-sales/{afterSaleNo}/refund` → `APPROVED → REFUNDING → REFUNDED`
    - `WALLET`：`WalletService#refundBusinessPayment` 按原支付分摊原路退回原批次，写 `wallet_refund`/`wallet_refund_allocation`；
    - `MOCK_WECHAT`：模拟退款成功，写 `MOCK-` 外部退款单号；
    - `WECHAT`：真实微信退款未接入，返回业务错误；
    - 退款成功后回补库存（sold→available）、回写订单项/订单退款金额，订单 `payment_status` 置 `PARTIAL_REFUND` 或 `REFUNDED`。

### 5.3 评价

- `POST /api/commerce/reviews` body `{orderNo, rating(1-5), content?, images?}` → 订单 `COMPLETED` 且未评价时可提交，状态 `PENDING`。
- `GET /api/commerce/reviews/order/{orderNo}` → 当前用户订单评价，未评价返回 `null`。
- 管理端：
  - `GET /api/admin/commerce/reviews?status&page&size`
  - `POST /api/admin/commerce/reviews/{reviewId}/approve` → `PENDING → APPROVED`
  - `POST /api/admin/commerce/reviews/{reviewId}/hide` → `HIDDEN`
  - `POST /api/admin/commerce/reviews/{reviewId}/reply` body `{reply}` → 写商家回复。

## 6. 关键实现约束

- 所有 App 接口从 `CurrentUserService` 取当前用户，不接受客户端 userId。
- 宠物归属复用 `PetService#getPetInfo(userId,petId)`；套餐绑定的宠物必须与预约宠物一致。
- 会员价由 `MemberService#getCurrentDiscountRate(userId)` 解析 `member_level.discount_rate`；优惠券通过 `CouponService#quoteCoupon` 试算。
- 商品/服务/门店/员工/套餐配置均为管理端 CRUD；下架用 status，不物理删除（营业时间/班次/图片/宠物规则允许硬删除；被预约引用的时段不可删）。
- 库存：管理端调整 `available_qty`；订单预占 `available→locked`，支付确认 `locked→sold`，取消/超时 `locked→available`；均条件 UPDATE + 行锁。
- 预约容量：`booking_slot` 条件 UPDATE；创建锁定、支付确认、取消/超时释放；改期先锁新时段再释放旧时段。
- 套餐次数：`user_service_package_item` 条件 UPDATE；冻结/核销/释放均校验剩余次数。
- 图片上传复用 `POST /api/upload`，新增 `type=store|shop`（映射到现有 community bucket）。
- 物流：仅商品订单发货；发货单状态 `SHIPPED/DELIVERED`；发货 7 天自动确认收货；售后/退款不改变发货单状态，退款回补库存。
- 售后：钱包退款按原 `wallet_payment_allocation` 原路退回原批次；模拟微信退款仅标记成功；真实微信退款后置；优惠券/积分/成长值退款规则待定，当前不恢复。
- 评价：一个订单一条评价，`PENDING` 状态仅管理端可见。
- 已实现：地址、购物车、结算、商品订单、钱包支付、dev 模拟微信支付、优惠券锁定/核销/释放、超时关单、预约时段/预约/签到/完成/取消/改期、套餐模板/购买/发放/核销、发货/确认收货、售后申请/审核/钱包退款、评价提交/审核/回复。
- 未实现：真实微信支付/退款、优惠券与积分退款追回、自动对账/补偿、RBAC/门店数据权限。
