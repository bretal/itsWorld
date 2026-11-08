# 01 商业服务剩余模块实施计划

> 编制日期：2026-11-07
> 依据：`docs/commerce/00-source-audit.md`、《宠物小程序商业服务剩余模块详细设计实施文档 V2.0》
> 状态：**Phase 0 计划；未修改业务代码、未新建 commerce 表。** 等待用户确认后再进入 Phase 1。

## 1. 目标与不可违背的约束

1. 复用现有用户、宠物、会员、优惠券、钱包、上传、分页、异常、鉴权、管理后台框架。
2. **不重复建用户/会员/钱包/优惠券表**；所有资金操作必须经过钱包领域服务，订单服务不得直接 UPDATE 钱包表。
3. 新建的表、类、方法必须在文档中标注“新增”，不得冒充现有能力。
4. 金额 `BIGINT` 整数分；订单/预约/核销/库存必须服务端重算、事务、幂等、审计。
5. 一期一个订单只允许一种业务类型（`PRODUCT` / `GROOMING` / `PACKAGE`）。
6. 支付成功后才确认履约资源；订单状态与支付状态分离。
7. 未确认的产品规则（V2 第 13 章）不得实现对应资金/履约行为。
8. 新表沿用项目现有 SQL 规范（`BIGINT` 主键、`TIMESTAMP`、`created_at/updated_at`、无 tenant/create_by、配置类用 `status`），不照搬文档的 `DATETIME(3)`/tenant 字段。

## 2. 审计结论摘要

- 可复用：`sys_user`、`pet_profile`、`member_*`、`coupon_template`、`user_coupon`、`wallet_account/fund_batch/transaction/detail`、`UploadController`、`Result/PageResult/PageUtils`、`CurrentUserService`、`ROLE_ADMIN`。
- 必须扩展：
  - 钱包：业务订单支付、支付分摊、退款、原路批次退回；当前**不存在**。
  - 优惠券：锁定/核销/释放/退款恢复；当前**只有发放/查询**；`user_coupon` 需补 `lock_order_no/used_order_no/version` 列。
  - 会员价：等级折扣数据存在，但无服务/SKU 定价方法。
  - 上传：`UploadController` 只允许 `avatar/pet/diary/community`，需扩展 `store/shop/review/aftersale`。
  - 管理端：只有 USER/ADMIN，无 RBAC、无门店数据范围；`commerce_staff` 缺 `user_id` 映射。
- 必须新增：38 张 commerce 候选表 + `user_shipping_address` + `wallet_payment_allocation/wallet_refund/wallet_refund_allocation`，共 **42 张新表**；当前库 38 张，全部落地约 80 张。
- 外部阻塞：无微信支付 SDK/商户号/证书/回调域名、无 `openid/unionid`（微信登录暂缓），真实微信 JSAPI 支付/退款/对账无法在本期完成；可先做钱包支付 + dev mock。

## 3. 建议的模块与包结构

沿用现有单体分包，新增：

```text
com.itsworld.commerce
├── store/        门店、营业时间、员工、班次
├── grooming/     美容分类、服务、门店价、宠物适配
├── booking/      时段容量、预约、签到/完成/取消/改期
├── catalog/      商城分类、SPU、图片、SKU
├── inventory/    库存、预占、流水
├── cart/         购物车
├── packagebiz/   套餐模板、选项、用户套餐、次数核销
├── order/        统一业务订单、订单项、优惠快照、状态日志
├── checkout/     结算试算、优惠编排、支付编排
├── payment/      支付网关抽象、钱包支付、微信支付（待资质）、mock
├── fulfillment/  发货、物流、确认收货
├── aftersale/    售后、退款编排
└── review/       评价与审核
```

- Controller 继续使用 `@RestController` + `Result<T>`；分页使用 `PageResult<T>` + `PageUtils`。
- Entity 使用 `@TableName`、`@TableId(type=IdType.AUTO)`；Mapper 继承 `BaseMapper`，复杂原子更新用 `@Update`/`@Select`。
- DTO 使用 Lombok + `jakarta.validation`；金额字段统一 `Long` 分。
- 不新建第二套用户/钱包/优惠券服务。

## 4. 数据库迁移计划（新增 42 张表，分 4 个脚本）

> 现有脚本 01~07；新增脚本必须按文件名顺序执行。已有数据卷需手动执行；新数据卷自动执行。所有新表金额 `BIGINT`，时间 `TIMESTAMP`，状态用 VARCHAR，历史/资金表不物理删除。

### 4.1 `08-commerce-base.sql`（Phase 1，14 张）

- 门店/美容（8）：`commerce_store`、`commerce_store_business_hour`、`commerce_staff`、`commerce_staff_shift`、`grooming_category`、`grooming_service`、`grooming_service_store`、`grooming_service_pet_rule`。
- 商城基础（6）：`shop_category`、`shop_product`、`shop_product_image`、`shop_sku`、`shop_inventory`、`shop_inventory_log`。
- 关键修正：
  - `shop_inventory.warehouse_id` 使用 `NOT NULL DEFAULT 0`（单仓），避免 MySQL 复合唯一键 NULL 失效。
  - `commerce_staff` 建议补 `user_id BIGINT NULL`（文档候选表没有；无此列无法做门店员工权限映射）→ **需确认**。
  - 门店/商品/服务图片复用 `UploadController`，扩展 type。

### 4.2 `09-commerce-order.sql`（Phase 2，7 张 commerce + 1 地址）

- `shop_cart_item`
- `shop_inventory_reservation`
- `commerce_order`
- `commerce_order_item`
- `commerce_order_discount`
- `commerce_order_status_log`
- `commerce_payment_link`（作为支付记录使用；补 `external_tx_no/paid_time/callback_time/fail_reason` 等字段，因无现有支付单）
- `user_shipping_address`
- 订单类型限定 `PRODUCT/GROOMING/PACKAGE`；订单保存价格/地址/门店/服务时长快照。

### 4.3 `10-wallet-payment-refund.sql`（Phase 2，3 张）

- `wallet_payment_allocation`：`pay_order_id, wallet_tx_id, batch_id, fund_type, paid_amount, refunded_amount`。
- `wallet_refund`：`refund_no, pay_order_id, business_order_no, amount, status, reason, external_refund_no`。
- `wallet_refund_allocation`：`refund_id, original_payment_allocation_id, amount, restored_batch_id`。
- 不新建钱包账户/批次/流水表；复用 `wallet_account`、`wallet_fund_batch`、`wallet_transaction`、`wallet_transaction_detail`。
- 需新增 `WalletService#payForBusinessOrder(...)`、`WalletService#refundBusinessPayment(...)`（**新方法**，不是现有能力），实现赠送优先/到期优先扣款、支付分摊、原批次退款、幂等键。
- 新增 `ALTER TABLE user_coupon ADD lock_order_no/used_order_no/version`（Phase 2 优惠券锁定需要）。
- 若优惠券需要限定服务/SKU 范围，需在 `coupon_template` 补 `scope_type/scope_id` 或新增映射表；当前表没有范围字段，待产品确认。
- 会员价一期按 `member_level.discount_rate` 全店适用；若需按服务/SKU/门店差异化，需补映射表或扩展权益适配；当前没有该表。

### 4.4 `11-commerce-booking-package.sql`（Phase 3，10 张）

- `booking_slot`、`grooming_booking`、`grooming_booking_log`
- `service_package_template`、`service_package_template_item`、`service_package_option`、`service_package_option_item`
- `user_service_package`、`user_service_package_item`、`service_package_usage`
- `booking_slot.staff_id` 使用 `NOT NULL DEFAULT 0`（“不限员工”），避免 NULL 唯一键问题。
- 容量/次数使用条件 UPDATE + 行锁；状态日志追加式。

### 4.5 `12-commerce-fulfillment-aftersale-review.sql`（Phase 4，7 张）

- `commerce_shipment`、`commerce_shipment_item`
- `commerce_after_sale`、`commerce_after_sale_item`、`commerce_refund_link`、`commerce_after_sale_log`
- `commerce_review`
- `commerce_refund_link` 作为退款记录使用；补 `refund_status/reason` 等字段，因无现有退款单。

## 5. API 设计约定（提案）

- 小程序/用户接口：`/api/commerce/**`，要求登录，当前用户从 `CurrentUserService` 获取，不接受客户端 userId。
- 管理端接口：`/api/admin/commerce/**`，沿用 `SecurityConfig` 的 `ROLE_ADMIN`；门店级数据权限暂不实现（见风险）。
- 统一返回 `Result<T>`；分页参数 `page/size`，返回 `PageResult<T>`。
- 错误码新增 commerce 段（建议 1900–1999），具体在 `ErrorCode` 扩展。
- 关键接口（按文档 7 映射，路径改为项目前缀）：
  - 基础资料（Phase 1）：`/api/commerce/stores`、`/api/commerce/grooming/categories|services`、`/api/commerce/shop/categories|products`；管理端 `/api/admin/commerce/stores|staff|shifts|grooming|products|skus|inventory`。
  - 订单（Phase 2）：`/api/commerce/cart`、`/api/commerce/checkout/preview|submit`、`/api/commerce/orders`、`/api/commerce/orders/{no}/payments`、`/api/commerce/orders/{no}/cancel`；管理端 `/api/admin/commerce/orders`。
  - 预约/套餐（Phase 3）：`/api/commerce/booking/slots`、`/api/commerce/bookings`、`/api/commerce/packages`、`/api/commerce/my-packages`；管理端 `/api/admin/commerce/bookings|packages`。
  - 物流/售后/评价（Phase 4）：`/api/commerce/orders/{no}/shipments`、`/api/commerce/after-sales`、`/api/commerce/reviews`；管理端 `/api/admin/commerce/shipments|after-sales|reviews`。
- 所有 App 查询按当前用户过滤；宠物/地址/订单/套餐归属校验；管理端按 ADMIN（暂不做门店范围）。

## 6. 分期实施计划

### Phase 0：源码审计与设计冻结（本次）
- 产出 `00-source-audit.md`、`01-implementation-plan.md`。
- 确认产品规则/支付/钱包扩展/权限方案。
- **不写业务代码、不建表。**

### Phase 1：门店、美容、商品、库存基础资料
- **状态：已完成（2026-11-07）**；实际表/接口/页面/测试见 `02`~`06`。
- 新增 `08-commerce-base.sql`（14 表）。
- 后端：store/grooming/catalog/inventory 的 Entity/Mapper/Service/Controller；管理端 CRUD；App 查询。
- 管理后台：门店/员工/排班、美容分类/服务、商品/SPU/SKU/库存页面；`MainLayout` 新增「商业服务」菜单组（门店/美容/商品/库存），`App.tsx` 新增 `/admin/commerce/**` 路由。
- 小程序：美容首页/服务详情/门店列表、商城首页/商品详情页面；入口位置（新增 Tab 或首页/我的入口）需产品确认。
- 复用：`UploadController`（扩展 type）、`Result/PageResult`、`CurrentUserService`、`ROLE_ADMIN`。
- 不涉及订单/支付/预约/套餐。
- 验收：管理端能维护基础资料；小程序能浏览；图片上传正常；无资金行为。

### Phase 2：购物车、结算、订单、支付、钱包扩展
- **状态：核心已完成（2026-11-07）**；购物车/结算/商品订单/钱包+模拟微信支付/优惠券/地址/超时关单已落地。真实微信支付、钱包退款执行留 Phase 4/外部依赖。
- 新增 `09-commerce-order.sql` + `10-wallet-payment-refund.sql` + `user_coupon` 补列。
- 扩展 `CouponService`（锁/核销/释放/恢复）、`MemberService`（会员价解析）、`WalletService`（业务支付/退款）。
- 积分抵扣：当前没有独立扣减方法；若启用，需新增 `consumePoints/refundPoints` 并确认规则；建议一期不做积分抵扣。
- 新增支付编排：`WalletPaymentGateway` 可用；微信支付适配器 **待商户资质/openid**；dev 提供 mock 支付。
- 管理后台：订单列表/详情/关闭；小程序：购物车/结算/订单列表/详情/支付结果。
- 依赖：产品规则确认（预约支付、取消/退款、运费、优惠叠加、混合支付等）。
- 验收：服务端重定价、幂等提交、库存预占/释放、钱包扣款/退款、支付回调幂等、超时关单。

### Phase 3：美容预约与套餐
- **状态：已完成（2026-11-07）**；10 表、预约/时段/签到/完成/取消/改期、套餐模板/购买发放/冻结/核销/释放、管理端 2 页、小程序 6 页已落地；冒烟通过。钱包退款执行仍后置 Phase 4。
- 新增 `11-commerce-booking-package.sql`（10 表）。
- 后端：容量锁定/确认/释放、预约、签到/完成/取消/改期、套餐模板/定制/购买发放/次数冻结/核销/回补。
- 管理后台：预约签到核销、套餐模板/选项。
- 小程序：预约时段/记录/详情、套餐定制/我的套餐/使用记录。
- 依赖：Phase 2 的订单/支付/钱包退款；套餐退款规则确认。

### Phase 4：物流、售后、评价
- **状态：已完成（2026-11-07）**；7 表、发货/确认收货/7 天自动确认、售后申请/审核/钱包退款执行/库存回补、评价提交/审核/回复、管理端 2 页 + 订单页发货入口、小程序 5 页已落地；22 项冒烟通过。真实微信退款、优惠券/积分追回、套餐退款公式后置。
- 新增 `12-commerce-fulfillment-aftersale-review.sql`（7 表）。
- 后端：发货、物流、确认收货；售后申请/审核/退款编排；评价提交/审核/回复。
- 管理后台：发货/售后/评价管理；小程序：物流/售后/评价。
- 依赖：Phase 2 支付/退款、地址、钱包退款。

### Phase 5：测试与灰度
- **状态：自动化测试与发布清单已完成（2026-11-07）**；12 个 JUnit/Testcontainers 用例通过，开发环境回滚脚本与灰度/上线检查清单已产出。生产灰度执行、7 天自动确认与真实微信支付仍需外部环境。
- 单元/集成测试（T01–T26），并发/幂等/越权测试；支付沙箱/模拟；迁移回滚；灰度门店/商品。
- 产出 `05-test-report.md`、`06-change-log.md`、`07-release-checklist.md`。

## 7. 关键状态机（实施时落为枚举/常量）

- 订单：`WAIT_PAY → PAID → PROCESSING → SHIPPED → COMPLETED`；`WAIT_PAY → CANCELLED/CLOSED`；退款独立 `refund_status`/售后状态。
- 预约：`PENDING_PAYMENT → CONFIRMED → CHECKED_IN → IN_SERVICE → COMPLETED`；取消分支 `PENDING_PAYMENT/CONFIRMED → CANCELLED`；`NO_SHOW` 待产品确认。
- 库存：`available/locked/sold`；预占、确认、释放、退货回补。
- 套餐：`ACTIVE → USED_UP/EXPIRED/CANCELLED`；使用流水 `FROZEN → USED/RELEASED/REVERSED`。
- 售后：`APPLIED → REVIEWING → APPROVED → REFUNDING → REFUNDED`；拒绝 `REJECTED`；异常 `REFUND_FAILED`。
- 支付：`CREATED → PAID/FAILED/CLOSED`；退款独立状态。

## 8. 安全与权限

- App：所有“我的”数据按当前用户过滤；宠物归属复用 `PetService#getPetInfo(userId,petId)`；地址归属新增校验；订单/套餐/售后归属校验。
- 管理端：一期仅 `ROLE_ADMIN`；`/api/admin/commerce/**` 受现有 `SecurityConfig` 保护。
- 门店数据范围：当前无 `commerce_staff.user_id` 映射；建议 Phase 1 先不做门店员工登录/数据隔离，Phase 4 前补 `commerce_staff.user_id` 或 `commerce_staff_user` 映射。**需产品确认。**
- 操作审计：复用 `sys_operation_log` 表，补 Entity/Mapper/Service + AOP/显式日志；价格调整、库存调整、退款审批、核销撤销必须记录。
- 上传：扩展 `UploadController` type，限制商品/门店/评价/售后图片类型与数量。

## 9. 测试计划（对应 V2 T01–T26）

- 订单幂等/超卖/库存释放/支付重复回调。
- 钱包本金+赠送扣款与原路退款；退款累计不超付。
- 预约容量争抢/改期/超时释放。
- 套餐购买发放幂等/次数冻结/核销重复/取消释放/已核销退款审核。
- 越权：A 查 B 订单/宠物/地址；门店员工跨店。
- 历史快照：后台改价后历史订单不变。
- 定时对账/补偿。
- 测试基建：当前 `server/src/test` 为空；需新增 JUnit/Spring Boot Test/Testcontainers 用例。

## 10. 产品/技术决策（2026-11-07 已确认）

> 用户确认：采用本表建议默认；钱包支付/退款扩展批准；微信支付本期只做钱包 + dev mock，真实微信后置；先补齐产品规则后进入 Phase 1。以下默认规则作为 Phase 2~4 的实施基线，后续如产品调整需修订本表。

| # | 问题 | 建议默认 | 是否阻塞 |
|---|---|---|---|
| 1 | 美容预约全额预付/定金/到店支付？超时释放多久？ | 全额预付，15 分钟超时；不支持到店支付 | Phase 2/3 |
| 1b | 小程序商业服务入口位置（新增 Tab / 首页入口 / 我的入口）？ | 首页/我的入口，先不改 4 Tab 结构 | Phase 1 |
| 2 | 门店价格是否按体重/品种/毛长分档？超时加价？ | 一期仅基础价+门店价，不按体重/品种 | Phase 3 |
| 3 | 取消/改期/迟到/爽约规则与退款比例？ | 未支付免费取消；已支付未履约走后台审核退款；爽约规则待定 | Phase 3 |
| 4 | 运费/包邮/仓库/配送区域/拆单？ | 一期包邮 0 元、单仓、不拆单 | Phase 2 |
| 5 | 套餐跨店？绑定单宠物还是用户所有宠物？ | 不允许跨店；绑定单宠物 | Phase 3 |
| 6 | 套餐定制选项计价？购买后能否更换？ | 模板价+选项加价；购买后不可更换 | Phase 3 |
| 7 | 套餐过期/已使用部分退款公式？ | 不自动退款，后台审核；公式待定 | Phase 3/4 |
| 8 | 会员价/优惠券/活动价是否叠加？部分退款是否退券？ | 会员价后可用一张券；不叠加；退款退券规则待定 | Phase 2 |
| 9 | 订单是否混合商品+服务？ | 不允许，一单一类型 | Phase 2 |
| 10 | 是否支持微信+钱包混合支付？ | 不支持，二选一 | Phase 2 |
| 11 | 已支付未发货取消/已发货退货？ | 未发货后台退款；已发货走退货售后 | Phase 2/4 |
| 12 | 积分/成长值何时发放、退款如何追回？ | 订单完成后发放；退款调用现有 `MemberService#award` 负向冲回（需确认规则） | Phase 2/4 |
| 13 | 自动确认收货/自动完成美容的时间？ | 收货 7 天自动确认；美容完成由门店操作 | Phase 3/4 |
| 14 | 退款权限/审核阈值/门店与总部数据隔离？ | 一期仅 ADMIN；门店范围后续 | Phase 4 |

## 11. 关键阻塞项与已决事项（2026-11-07）

已决：
1. **钱包支付/退款扩展**：已批准。第二阶段新增 3 张钱包支付/退款表 + `WalletService` 新方法，复用现有账户/批次/流水。
2. **微信支付**：本期只做钱包支付 + dev mock；真实微信 JSAPI 支付/退款/对账等商户资质、证书、回调域名、openid/微信登录方案齐备后再接。
3. **产品规则**：采用第 10 节建议默认；后续变更需修订本表。
4. **一期范围**：Phase 1 基础资料（门店/美容/商品/库存；管理后台 + 小程序浏览；不含订单支付）。

仍待办：
1. **RBAC/门店数据范围缺失**：一期 ADMIN 全量；门店员工跨店隔离需补 `commerce_staff.user_id` 映射（Phase 1 不实现）。
2. **42 张新表规模**：必须分阶段；Phase 1 只建 14 张。
3. **小程序商业入口位置**：默认首页/我的入口，不改 4 Tab 结构；如要新增 Tab 需产品确认。

## 12. 下一步（Phase 1）

1. 新增 `08-commerce-base.sql`（14 张基础表）。
2. 后端 `com.itsworld.commerce.{store,grooming,catalog,inventory}` 的 Entity/Mapper/DTO/Service/Controller。
3. 扩展 `UploadController` 图片类型：`store/shop`（复用现有 MinIO bucket；不新建 OSS）。
4. 管理后台：商业服务菜单 + 门店/员工/排班、美容分类/服务、商品/SKU/库存页面。
5. 小程序：美容首页/服务详情/门店列表、商城首页/商品详情；入口放首页/我的，不改底部 4 Tab。
6. 编译、构建、接口冒烟、迁移校验；产出 Phase 1 变更清单与接口文档。
7. Phase 2 前再实现钱包支付/退款、订单/购物车/结算；微信支付继续后置。
