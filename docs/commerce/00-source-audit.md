# 00 源码审计与复用清单（商业服务剩余模块）

> 审计日期：2026-11-07
> 审计范围：`server/src`、`init-scripts/mysql/01~07`、`frontend/src`、`miniprogram-native/`、`docker-compose.yml`、`server/pom.xml`
> 依据文档：《宠物小程序商业服务剩余模块详细设计实施文档 V2.0》
> 状态：Phase 0 审计完成；**尚未修改任何业务代码或新建 commerce 表**。本文件是后续实施计划的唯一源码依据。

## 1. 真实技术栈与工程结构

| 项目 | 实际情况 | 路径/证据 |
|---|---|---|
| 后端 | Java 17 + Spring Boot 3.3.0 + Spring Security + JWT + MyBatis-Plus 3.5.5 | `server/pom.xml` |
| 数据库 | MySQL 8.0（Docker 13306→3306），库 `its_world` | `docker-compose.yml`、`init-scripts/mysql/01-init.sql` |
| 缓存 | Redis 7 依赖已引入，但后端代码未使用 `RedisTemplate`/分布式锁 | `spring-boot-starter-data-redis`、全仓搜索 |
| 对象存储 | MinIO，`POST /api/upload`，返回 URL 字符串 | `common/controller/UploadController.java`、`common/util/MinIOUtils.java` |
| 前端 | React 19 + Ant Design 6 + Vite 8 + React Router 7 + Zustand；仅管理员可登录 | `frontend/package.json`、`frontend/src/App.tsx`、`frontend/src/components/ProtectedRoute.tsx` |
| 小程序 | 微信原生小程序，WXML/WXSS/TypeScript，16 页，底部 4 Tab+发布 | `miniprogram-native/app.json`、`miniprogram-native/pages/` |
| 迁移 | 纯 SQL 文件，Docker 初始化按文件名顺序执行；无 Flyway/Liquibase | `init-scripts/mysql/` |
| 统一响应 | `Result<T>`（code=0 成功）、`PageResult<T>`（records/total/pageNum/pageSize/totalPages） | `common/result/Result.java`、`common/result/PageResult.java` |
| 分页 | `PageUtils.normalizePageNum/PageSize` + MyBatis-Plus `PaginationInnerInterceptor` | `common/util/PageUtils.java`、`common/config/MybatisPlusConfig.java` |
| 异常 | `BusinessException` + `GlobalExceptionHandler`；业务错误 HTTP 200 + 非零 code | `common/exception/`、`common/result/ErrorCode.java` |
| 鉴权 | JWT 无状态；`/api/admin/**` 要求 `ROLE_ADMIN`；`CurrentUserService` 解析当前用户 | `security/config/SecurityConfig.java`、`security/service/CurrentUserService.java` |
| 角色 | 仅 `USER` / `ADMIN` 两级；无 RBAC 权限表、无门店数据范围 | `sys_user.role`、`CustomUserDetailsService` |
| 审计 | `sys_operation_log` 表存在，但无 Java Entity/Mapper/Service | `01-init.sql` |
| 测试 | `spring-boot-starter-test`、`spring-security-test`、Testcontainers 依赖存在；`server/src/test` 为空 | `server/pom.xml` |
| 定时任务/消息 | 无 MQ、无 Outbox、无 `@Scheduled` 业务任务 | 全仓搜索 |

### 1.1 现有后端包结构

```text
com.itsworld
├── common        Result/PageResult/ErrorCode/BusinessException/PageUtils/Upload/MinIO
├── security      JWT Filter、SecurityConfig、CustomUserDetailsService、CurrentUserService
├── user          sys_user 注册/登录/资料/角色/adminLogin
├── pet           pet_profile CRUD、按用户归属查询
├── diary         pet_diary、点赞
├── community     community_post/comment/like/follow
├── health        pet_weight/feeding/vaccine/checkup
├── member        member_level/account/benefit/task/coupon/gift
├── wallet        wallet_account/fund_batch/transaction/detail/recharge/plan/adjustment
└── admin         AdminOverview 工作台聚合
```

## 2. 现有可复用能力（源码实测）

### 2.1 用户与登录

- 文件：`user/entity/User.java`、`user/mapper/UserMapper.java`、`user/service/UserService.java`、`user/service/impl/UserServiceImpl.java`、`security/service/CurrentUserService.java`。
- 现有方法签名：
  - `Long register(UserRegisterRequest)`
  - `UserLoginResponse login(UserLoginRequest)`
  - `UserLoginResponse adminLogin(UserLoginRequest)`（仅 ADMIN）
  - `User getUserByUsername(String username)`
  - `User getUserById(Long userId)`
  - `UserResponse getCurrentUserInfo()`
  - `UserResponse updateUserInfo(Long userId, UserResponse userRequest)`
  - `void changePassword(Long userId, String oldPassword, String newPassword)`
  - `boolean verifyPassword(String rawPassword, String encodedPassword)`
  - `CurrentUserService#getUserId()` / `#getUser()`
- `sys_user` 实际字段：`id, username, email, phone, password_hash, password_salt, nick_name, avatar_url, bio, gender, birth_date, region, status, role, last_login_at, created_at, updated_at, deleted_at`。
- **结论**：用户、登录、角色、当前用户可直接复用；无 `openid/unionid`，微信登录未实现（`PROGRESS.md` 中暂缓），这会阻塞微信支付 JSAPI 的 `openid` 获取。

### 2.2 宠物与归属校验

- 文件：`pet/service/PetService.java`、`pet/service/impl/PetServiceImpl.java`、`pet/mapper/PetMapper.java`、`pet/entity/Pet.java`。
- 现有方法签名：
  - `Long createPet(Long userId, PetCreateRequest)`
  - `List<PetResponse> getPetListByUserId(Long userId)`
  - `PageResult<PetResponse> getPetPage(Long userId, long pageNum, long pageSize, String keyword)`
  - `PetResponse getPetInfo(Long userId, Long petId)` —— 内部使用 `petMapper.selectByUserIdAndPetId(userId, petId)`，**已做归属校验**
  - `PetResponse updatePet(Long userId, Long petId, PetUpdateRequest)`
  - `void deletePet(Long userId, Long petId)`
  - `Pet getPetById(Long petId)` —— 仅内部使用，不做用户归属校验
- `pet_profile` 字段：`id, user_id, pet_name, pet_type, breed, birth_date, gender, weight_kg, height_cm, avatar_url, bio, is_active, care_notes, created_at, updated_at, deleted_at`。
- **结论**：预约/下单的宠物归属校验直接复用 `PetService#getPetInfo(userId, petId)`；不要新增第二套宠物校验。若需要返回 Pet 实体，可新增 `PetService#getOwnedPet(userId,petId)`（新增方法，不是现有能力）。
- 注意：`Pet` 实体没有声明 `care_notes` 字段（数据库列存在且 `PetMapper` 会查），commerce 若需要该字段需补实体映射；`getPetById(Long)` 无归属校验，禁止在用户侧直接使用。

### 2.3 会员、权益、优惠券、礼包

- 文件：`member/service/MemberService.java`、`MemberTaskService.java`、`CouponService.java`、`GiftService.java` 及 impl。
- 现有方法（与 commerce 相关）：
  - `MemberAccount getOrCreateAccount(Long userId)`
  - `MemberOverviewResponse getOverview(Long userId)`
  - `List<MemberLevelView> getLevels(Long userId)`
  - `MemberAwardResult award(Long userId, int growthDelta, int pointsDelta, String sourceType, String sourceNo, String remark)` —— 方法本身允许负数并检查余额不为负，但**当前所有调用方都传正数**；没有独立的积分扣减/冲回方法。
  - `PageResult<MemberGrowthLog> getGrowthLogs(...)` / `getPointsLogs(...)`
  - `MemberTaskService#recordProgress(Long userId, String taskCode)`（已有日记/社区/宠物埋点）
  - `CouponService#listUserCoupons(Long userId, String status)`
  - `CouponService#issueCoupons(Long userId, Long templateId, Integer quantity)`
  - `CouponService#issueOneCoupon(Long userId, Long templateId)`
  - `GiftService#listGifts(Long userId)` / `claimGift(...)`
- 会员表：`member_level(level_code, level_name, growth_threshold, discount_rate, ...)`、`member_account(user_id, level_id, growth_value, points_balance, ...)`、`member_benefit`、`member_level_benefit`、任务/券/礼包表。
- `member_level.discount_rate` 为 `DECIMAL(5,2)`，是**乘数**（1.00/0.98/0.95/0.90，即原价的 100%/98%/95%/90%），**只是等级折扣数据，没有面向服务/SKU 的定价方法**；也没有公开的 `getCurrentLevel(userId)`/`getDiscount(userId)` 方法，commerce 需通过 `getOrCreateAccount` + `MemberLevelMapper` 或新增方法解析。
- `user_coupon` 实际字段：`id, user_id, template_id, coupon_code, status, valid_from, valid_to, used_at, created_at`；状态值支持 `AVAILABLE/LOCKED/USED/EXPIRED`。
  - **注意：当前表没有文档 4.3 提到的 `lock_order_no`、`used_order_no`、`version` 字段**；要实现锁券/核销/释放，需 `ALTER TABLE user_coupon` 补列（或设计等价关联），**不要新建第二套券表**。
  - 当前只有发放和查询方法，**没有锁定、核销、释放、退款恢复方法**。
- `coupon_template` 没有适用范围（服务/SKU）字段；一期若全店通用可直接使用，若需范围限制需补字段/映射表（待产品确认）。
- **结论**：会员等级/权益数据可复用；需要新增定价解析（会员价）、优惠券锁定/核销/释放/恢复方法、`user_coupon` 补列；不得新建第二套会员/优惠券表。

### 2.4 钱包

- 文件：`wallet/service/WalletService.java`、`wallet/service/impl/WalletServiceImpl.java`、`wallet/mapper/*`。
- 现有方法签名：
  - `WalletSummaryResponse getSummary(Long userId)`
  - `List<WalletRechargePlan> getActivePlans()`
  - `PageResult<RechargeOrderView> getUserRecharges(Long userId, long pageNum, long pageSize)`
  - `RechargeOrderView createRecharge(Long userId, Long planId)`
  - `RechargeOrderView getRecharge(Long userId, String rechargeNo)`
  - `RechargeOrderView mockPay(Long userId, String rechargeNo)`（仅 dev `app.payment.mock-enabled=true`）
  - `PageResult<WalletTransactionView> getUserTransactions(Long userId, long pageNum, long pageSize)`
  - 管理端：`getAdminPlans/createPlan/updatePlan/disablePlan/getOfflineRecharges/createOfflineRecharge/approveOfflineRecharge/rejectOfflineRecharge/getAllTransactions`
  - `WalletAdjustmentView adjustBalance(Long operatorId, AdjustBalanceRequest)`（管理员调增/调减）
  - `PageResult<WalletAdjustmentView> getAdjustments(long pageNum, long pageSize, Long userId)`
- 钱包表：`wallet_account(principal_balance, bonus_balance, principal_frozen, bonus_frozen, version)`、`wallet_fund_batch(fund_type, remaining_amount, frozen_amount, expire_at, status)`、`wallet_transaction(tx_type, principal_delta, bonus_delta, before/after, idempotency_key)`、`wallet_transaction_detail`、`wallet_recharge_order`、`wallet_recharge_plan`、`wallet_adjustment`。
- **关键事实（与 V2 文档假设冲突）**：
  - V2 文档第 5 行称“已完成……储值钱包……及其内部扣减退款能力”。
  - 源码实际：**没有业务订单钱包支付、没有支付分摊、没有退款/原路退回、没有 `wallet_payment_allocation`/`wallet_refund`/`wallet_refund_allocation` 表或方法**。
  - 当前 `wallet_service` 只有充值、模拟充值、管理员调账、流水；`wallet_transaction.tx_type` 的注释虽包含 `PAYMENT/REFUND`，但没有对应业务代码。
  - 现有 `principal_frozen/bonus_frozen`、`wallet_fund_batch.frozen_amount/frozen_delta` 字段**从未被设置为非零**；`wallet_fund_batch.scope` 始终是 `ALL`，扣减时不校验范围；`expire_at` 只在管理员负数调账排序时使用，`getSummary` 不排除过期赠送批次，也没有过期任务。
  - 实际写入的 `wallet_transaction.tx_type` 只有 `RECHARGE` 和 `ADJUSTMENT`；DDL 注释中的 `PAYMENT/REFUND` 未被使用。
  - **结论：进入订单支付阶段前，必须先扩展钱包域；这不是“复用已完成能力”，而是新增能力，需用户确认。**

### 2.5 文件上传

- 文件：`common/controller/UploadController.java`、`common/util/MinIOUtils.java`。
- 接口：`POST /api/upload`（multipart `file` + `type`）。
- 允许 type：`avatar`、`pet`、`diary`、`community`；**没有** `store`/`shop`/`review`/`aftersale` 类型。
- 返回 URL 字符串；MinIO bucket 公共读；开发/生产 URL 格式见 `application.yml` 与 `MinIOUtils`。
- **结论**：商品/门店/评价图片可复用上传与 MinIO；需要扩展 type 白名单及（可选）新 bucket；不要新建 OSS。

### 2.6 管理端与权限

- 登录：`POST /api/admin/auth/login`（`AdminAuthController` → `UserService#adminLogin`），仅 ADMIN。
- `SecurityConfig`：`/api/admin/**` 要求 `ROLE_ADMIN`；`/api/admin/auth/login` permitAll；其余已认证。
- `CurrentUserService#getUserId/getUser` 可复用。
- 前端 `ProtectedRoute` 要求 ADMIN；`AdminRoute` 二次校验。
- **缺口**：无 `commerce:*` 权限码、无 `PermissionService`、无门店数据范围；`commerce_staff` 候选表没有 `user_id`，无法把门店员工映射到 `sys_user`。**权限/门店隔离需要在实施计划中明确设计。**
- `sys_operation_log` 表已存在（`user_id, operation_type, module_name, resource_id, request_url, request_method, request_params, response_code, ip_address, user_agent, created_at`），但无 Java Entity/Mapper/Service；可直接作为操作审计表补代码，无需新建审计表。

### 2.7 基础设施

- Redis：依赖存在，无实际使用；无分布式锁实现。
- 事务：`@Transactional` 已使用；钱包/订单在同一 MySQL，可在同一事务内完成钱包扣款、订单状态、库存确认。
- 锁：现有钱包使用 `SELECT ... FOR UPDATE` 行锁与条件更新；可复用到库存/预约/套餐次数。
- 消息/Outbox：无 MQ、无 Outbox；可用同一事务 + `@Scheduled` 补偿/对账，需新增定时任务。
- 乐观锁：部分表有 `version` 字段，但未使用 `@Version`；可用条件 UPDATE / 行锁。
- 迁移：新增 SQL 文件需按 `08-...` 继续编号；无 Flyway；已有数据卷需手动执行。
- 测试：无测试代码；可新增 JUnit/Testcontainers。

## 3. 文档假设与源码事实差异（必须确认）

| V2 文档假设 | 源码事实 | 影响 |
|---|---|---|
| 会员等级、成长、积分、权益已完成 | **成立**（`member_*` 表/服务已存在） | 可复用 |
| 储值钱包“内部扣减退款能力”已完成 | **不成立**：只有充值、调账、流水；无业务支付/退款/分摊 | Phase 2 必须先扩展钱包，新增 3 张钱包支付/退款表与 `WalletService` 方法 |
| 优惠券锁定/核销/释放/退回已有 | **不成立**：只有发放/查询；`user_coupon.status` 有状态值但无操作 | 需扩展 `CouponService`，不新建表 |
| 会员价/权益定价服务已有 | **部分成立**：有 `member_level.discount_rate`，无服务/SKU 定价方法 | 需新增会员价解析方法 |
| 统一支付单、微信 JSAPI、回调验签、退款已有 | **不成立**：无支付 SDK、无支付表、无回调、无 `openid` | 需新增支付编排；微信支付需商户资质、证书、回调域名、openid，当前阻塞 |
| 用户地址表已有 | **不成立**：无地址表 | 需新增 `user_shipping_address`（文档 5.39 允许） |
| 门店/员工/排班/商品/订单/售后/评价已有 | **不成立**：全部不存在 | 全部新增 |
| 租户/门店隔离、RBAC 已有 | **不成立**：仅 USER/ADMIN；无门店员工映射 | 一期建议 ADMIN 全量权限；门店范围后续设计 |
| 定时任务/消息机制已有 | **不成立**：无 MQ/Outbox/@Scheduled 业务任务 | 用 DB 事务 + `@Scheduled` 补偿；需新增任务 |
| 建议 `DATETIME(3)` | 项目实际 `TIMESTAMP`（无毫秒） | 新表沿用项目 `TIMESTAMP`；不要引入不一致精度 |
| 建议 `create_by/update_by/tenant_id/del_flag` | 项目实际 `created_at/updated_at/deleted_at`，无 tenant/create_by | 沿用项目规范；操作人用业务表 `operator_id`/日志表记录 |

## 4. 候选表复用/新增矩阵

> 说明：以下 38 张为 V2 文档候选表；**当前库没有任何一张 commerce 候选表**。分类“需新增”均指按文档候选名新建，不是现有表。当前库共 38 张表（18 基础 + 20 会员/钱包/调账），与这 38 张候选表**没有重叠**。

| # | 文档候选表 | 源码现状 | 分类 | 关联/风险 |
|---|---|---|---|---|
| 5.1 | `commerce_store` | 不存在 | 需新增 | 门店主数据；无租户字段 |
| 5.2 | `commerce_store_business_hour` | 不存在 | 需新增 | 营业时间/例外日 |
| 5.3 | `commerce_staff` | 不存在 | 需新增 | 员工；**缺 user_id 映射**，门店权限需补 |
| 5.4 | `commerce_staff_shift` | 不存在 | 需新增 | 班次/容量 |
| 5.5 | `grooming_category` | 不存在 | 需新增 | 美容分类 |
| 5.6 | `grooming_service` | 不存在 | 需新增 | 服务项目 |
| 5.7 | `grooming_service_store` | 不存在 | 需新增 | 门店服务/差异价 |
| 5.8 | `grooming_service_pet_rule` | 不存在 | 需新增 | 宠物适配规则 |
| 5.9 | `booking_slot` | 不存在 | 需新增 | `staff_id` 可空唯一键风险，建议默认 0 |
| 5.10 | `grooming_booking` | 不存在 | 需新增 | 关联 slot/order/pet |
| 5.11 | `grooming_booking_log` | 不存在 | 需新增 | 状态审计 |
| 5.12 | `shop_category` | 不存在 | 需新增 | 商品分类 |
| 5.13 | `shop_product` | 不存在 | 需新增 | SPU |
| 5.14 | `shop_product_image` | 不存在 | 需新增 | 图片 |
| 5.15 | `shop_sku` | 不存在 | 需新增 | SKU |
| 5.16 | `shop_inventory` | 不存在 | 需新增 | `warehouse_id` 可空唯一键风险，建议默认 0 |
| 5.17 | `shop_inventory_reservation` | 不存在 | 需新增 | 库存预占 |
| 5.18 | `shop_inventory_log` | 不存在 | 需新增 | 库存流水 |
| 5.19 | `shop_cart_item` | 不存在 | 需新增 | 购物车 |
| 5.20 | `commerce_order` | 不存在 | 需新增 | 统一订单 |
| 5.21 | `commerce_order_item` | 不存在 | 需新增 | 订单项快照 |
| 5.22 | `commerce_order_discount` | 不存在 | 需新增 | 优惠分摊 |
| 5.23 | `commerce_order_status_log` | 不存在 | 需新增 | 订单状态审计 |
| 5.24 | `commerce_payment_link` | 不存在 | 需新增 | 无现有支付单；可复用为支付记录，需补 `external_tx_no/paid_time` 等字段 |
| 5.25 | `service_package_template` | 不存在 | 需新增 | 套餐模板 |
| 5.26 | `service_package_template_item` | 不存在 | 需新增 | 固定服务/次数 |
| 5.27 | `service_package_option` | 不存在 | 需新增 | 选项组 |
| 5.28 | `service_package_option_item` | 不存在 | 需新增 | 选项候选 |
| 5.29 | `user_service_package` | 不存在 | 需新增 | 用户套餐实例 |
| 5.30 | `user_service_package_item` | 不存在 | 需新增 | 次数 |
| 5.31 | `service_package_usage` | 不存在 | 需新增 | 核销流水 |
| 5.32 | `commerce_shipment` | 不存在 | 需新增 | 发货单 |
| 5.33 | `commerce_shipment_item` | 不存在 | 需新增 | 发货明细 |
| 5.34 | `commerce_after_sale` | 不存在 | 需新增 | 售后 |
| 5.35 | `commerce_after_sale_item` | 不存在 | 需新增 | 售后明细 |
| 5.36 | `commerce_refund_link` | 不存在 | 需新增 | 退款记录；无现有退款单 |
| 5.37 | `commerce_after_sale_log` | 不存在 | 需新增 | 售后审计 |
| 5.38 | `commerce_review` | 不存在 | 需新增 | 评价 |
| 5.39 | `user_shipping_address` | 不存在 | 需新增 | 文档允许补建；地址归属校验 |
| 原会员文档 4.2 | `wallet_payment_allocation` | 不存在 | 需新增 | V2 未列，但钱包支付分摊必需 |
| 原会员文档 4.2 | `wallet_refund` | 不存在 | 需新增 | V2 未列，但钱包退款必需 |
| 原会员文档 4.2 | `wallet_refund_allocation` | 不存在 | 需新增 | V2 未列，但原路批次退款必需 |

**复用表**：`sys_user`、`pet_profile`、`member_level`、`member_account`、`member_benefit`、`member_level_benefit`、`member_growth_log`、`member_points_log`、`member_task`、`member_task_record`、`coupon_template`、`user_coupon`、`member_gift`、`member_gift_record`、`wallet_account`、`wallet_fund_batch`、`wallet_transaction`、`wallet_transaction_detail`、`wallet_recharge_order`、`wallet_recharge_plan`、`wallet_adjustment`、`sys_config`、`sys_operation_log`。

## 5. 候选 Controller/Service 与源码映射

| V2 候选类 | 源码现状 | 处理建议 | 风险 |
|---|---|---|---|
| `StoreController/Service` | 不存在 | 新建 `com.itsworld.commerce.store` | 门店数据范围 |
| `GroomingServiceController/Service` | 不存在 | 新建 `commerce.grooming` | 会员价解析 |
| `StoreScheduleController`/`BookingSlotService` | 不存在 | 新建 `commerce.booking` | 容量并发 |
| `GroomingBookingController/Service` | 不存在 | 新建 `commerce.booking` | 订单/容量联动 |
| `ShopProductController/ShopCatalogService` | 不存在 | 新建 `commerce.catalog` | 会员价 |
| `ShopCartController/ShopCartService` | 不存在 | 新建 `commerce.cart` | 失效校验 |
| `InventoryService` | 不存在 | 新建 `commerce.inventory` | 防超卖 |
| `CheckoutController/Service` | 不存在 | 新建 `commerce.checkout` | 服务端重定价 |
| `CommerceOrderController/Service` | 不存在 | 新建 `commerce.order` | 状态机/幂等 |
| `CommercePaymentController/Service` | 不存在 | 新建 `commerce.checkout/payment` | **无支付网关/openid** |
| `ServicePackageController`/`UserPackageService`/`PackageUsageService` | 不存在 | 新建 `commerce.packagebiz` | 次数并发/退款 |
| `ShipmentService` | 不存在 | 新建 `commerce.fulfillment` | 地址 |
| `AfterSaleService`/`RefundOrchestrator` | 不存在 | 新建 `commerce.aftersale` | **钱包退款缺失** |
| `ReviewService` | 不存在 | 新建 `commerce.review` | 审核 |
| `PetService` | 存在 | 复用 `getPetInfo(userId,petId)` 做归属校验；不新增平行宠物表 | 无 |
| `MemberBenefitService` | **不存在** | 扩展 `MemberService` 或新增 `CommerceMemberPricingService`（新方法） | 会员价适用范围 |
| `CouponService` | 存在 | 扩展 `lockCoupon/consumeCoupon/releaseCoupon/restoreCouponOnRefund`（新方法） | 状态机 |
| `WalletPaymentService` | **不存在** | 扩展 `WalletService`（新方法），新增 3 张钱包支付/退款表 | 文档假设错误 |
| `PaymentService/PaymentCallbackHandler` | **不存在** | 新建支付编排；微信支付依赖商户资质/openid | 外部阻塞 |
| `FileUploadService` | 不存在（实际 `UploadController`） | 扩展 `UploadController` type 白名单 | 无 |
| `PermissionService` | **不存在** | 一期用 `ROLE_ADMIN`；门店范围后续设计 | RBAC 缺失 |

## 6. 前端与小程序现状

### 6.1 Web 管理后台

- 技术栈：React 19 + AntD 6 + Vite 8 + React Router 7 + Zustand + Axios。
- 现状：管理员专用登录；菜单为工作台、用户管理、会员配置、钱包管理；用户侧路由隐藏但保留。
- 现有页面：`pages/admin/AdminDashboardPage.tsx`、`pages/admin/users/UserManagementPage.tsx`、`pages/admin/member/*`、`pages/admin/wallet/*`。
- 服务层：`adminMemberService`、`adminWalletService`、`adminOverviewService`、`memberService`、`walletService`。
- 公共工具：`Result` 解包、`PageData<T>`、`formatMoney/yuanToCents`、`utils/format.ts`。
- **没有**门店/商品/购物车/订单/预约/套餐/售后/评价页面或服务。
- 新 commerce 管理页可挂在 `MainLayout` 菜单，路由用 `/admin/commerce/*` 或 `/admin/stores|products|orders|...`；需要新增菜单项和 `AdminRoute` 包裹。

### 6.2 微信原生小程序

- 结构：`miniprogram-native/app.json`、`pages/`、`services/`、`utils/`、`config/env.ts`。
- 现状：16 页（首页、宠物、日记、健康、社区、我的、登录等），底部 4 Tab + 发布；分页类型 `types/page.ts`；API base 默认 `http://localhost:8080/api`，按 envVersion 切生产。
- **没有**门店/美容/预约/商城/购物车/订单/套餐/售后/评价/地址页面或服务。
- 新增页面需要注册到 `app.json`；tabBar 页面数量有限，commerce 入口可放在首页/我的或新增 Tab（需产品确认）。
- 现有 UI 模式：WXML/WXSS + TypeScript，服务层封装请求；无支付/地址/订单代码。

## 7. 关键风险与未决事项

1. **钱包支付/退款缺失**：V2 文档的错误假设；订单支付/退款无法直接实现。必须先扩展钱包域（新增 3 表 + 方法），或调整一期范围只做充值/调账。
2. **微信支付阻塞**：无支付 SDK、无商户号/证书/回调域名、无 `openid/unionid`、微信登录暂缓。真实 JSAPI 支付/退款/对账无法在本期完成；可先做钱包支付 + dev mock，微信支付待资质与登录方案。
3. **优惠券锁定/核销缺失**：需扩展 `CouponService`，复用 `user_coupon` 状态；不新建券表。
4. **会员价解析缺失**：只有等级折扣字段；需新增服务/SKU 定价方法。适用范围/叠加规则待产品确认。
5. **地址表缺失**：需新增 `user_shipping_address`。
6. **RBAC/门店数据范围缺失**：只有 ADMIN；`commerce_staff` 无 user 映射。一期建议 ADMIN 全量；门店范围后续。
7. **无 MQ/Outbox/定时任务**：用同库事务 + `@Scheduled` + 条件更新；需明确补偿/对账策略。
8. **表/字段规范差异**：文档建议 `DATETIME(3)`/create_by/tenant；项目实际 `TIMESTAMP`/created_at/updated_at/deleted_at，无 tenant。新表沿用项目规范。
9. **38 张候选表 + 地址 + 钱包 3 表 = 42 张新表**；当前库 38 张，全部落地约 80 张。必须分阶段，不可一次性建表。
10. **产品规则未确认**：预约支付模式、取消/改期/退款、运费、套餐退款公式、优惠叠加、混合支付、积分发放/追回、自动确认、退款权限等（V2 第 13 章）。未确认前不得实现资金/履约行为。

## 8. 审计结论

- 可复用：用户、登录、角色、当前用户、宠物归属、会员等级/权益数据、钱包充值/调账/流水、上传、分页/异常/响应、前端管理后台框架。
- 需扩展：钱包支付/退款、优惠券锁定/核销/释放、会员价解析、上传类型、管理端权限/门店范围。
- 需新增：全部 38 张 commerce 候选表、地址表、3 张钱包支付/退款表、commerce 后端包、Web 管理页、小程序页面。
- 不可直接开始：Phase 2（订单/支付/退款）依赖钱包扩展、微信支付资质/openid 与产品规则；建议先做 Phase 1（门店/美容/商品/库存基础资料 + 管理后台 + 小程序浏览），Phase 2 待确认后实施。
- 本审计未修改业务代码；下一步为 `01-implementation-plan.md` 的确认。
