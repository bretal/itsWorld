# 05 测试报告（Phase 1~4）

> 日期：2026-11-07
> 范围：门店/美容/商品/SKU/库存、订单/支付、预约/套餐、物流/售后/评价。
> 环境：本地 Docker MySQL/Redis/MinIO；Phase 3/4 冒烟使用后端 `localhost:8081`（避免影响运行中的 8080 实例）；前端 Vite 构建；小程序 TypeScript 检查。

## 1. 构建与静态检查

| 检查 | 命令 | 结果 |
|---|---|---|
| 后端编译 | `cd server && mvn clean compile` | 通过（exit 0） |
| 后端测试 | `cd server && mvn clean test` | 通过：12 个测试（7 单测 + 5 集成），0 失败 |
| 前端构建（Phase 4 后） | `cd frontend && npm run build` | 通过（3222 modules；1.61MB / gzip 490KB） |
| 小程序类型检查（Phase 4 后） | `cd miniprogram-native && npm run typecheck` | 通过（exit 0） |

## 2. 数据库迁移

- 已执行 `08-commerce-base.sql`、`09-commerce-order.sql`、`10-wallet-payment-refund.sql`、`11-commerce-booking-package.sql`（均 exit 0）。
- Phase 1 新增 14 张基础表。
- Phase 2 新增 11 张：`user_shipping_address`、`shop_cart_item`、`commerce_order`、`commerce_order_item`、`commerce_order_discount`、`commerce_order_status_log`、`commerce_payment_link`、`shop_inventory_reservation`、`wallet_payment_allocation`、`wallet_refund`、`wallet_refund_allocation`；并给 `user_coupon` 增加 `lock_order_no/used_order_no/version`。
- Phase 3 新增 10 张：`booking_slot`、`grooming_booking`、`grooming_booking_log`、`service_package_template`、`service_package_template_item`、`service_package_option`、`service_package_option_item`、`user_service_package`、`user_service_package_item`、`service_package_usage`。
- Phase 4 新增 7 张：`commerce_shipment`、`commerce_shipment_item`、`commerce_after_sale`、`commerce_after_sale_item`、`commerce_refund_link`、`commerce_after_sale_log`、`commerce_review`。
- 当前总表数 80（原 38 + 42）。

## 3. 接口冒烟（curl）

### 3.1 管理员登录
- `POST /api/admin/auth/login` admin 成功返回 token；`testmember` 返回业务码 403“仅管理员可登录”。

### 3.2 门店
- `POST /api/admin/commerce/stores` 创建门店 `TEST-STORE-1` → id 1。
- `GET /api/admin/commerce/stores?page=1&size=10` 返回门店分页。
- `POST /api/admin/commerce/stores/1/hours` 创建周一 09:00-18:00。
- `POST /api/admin/commerce/stores/1/staff` 创建员工 Alice。
- `POST /api/admin/commerce/stores/staff/1/shifts` 创建 2026-11-10 班次，容量 4。
- `GET /api/commerce/stores/1` 返回门店详情 + 营业时间。
- `PUT /api/admin/commerce/stores/1/status?businessStatus=OPEN` 成功。

### 3.3 美容
- 创建分类 `Bath`、服务 `BATH-001`（基础价 12800 分）、门店价 11800 分、宠物规则 `dog 1000-30000g`。
- `GET /api/commerce/grooming/categories` 返回启用分类。
- `GET /api/commerce/grooming/services?storeId=1` 返回服务，`storePrice=11800`；`testmember`（银卡 0.98）`memberPrice=11564`。
- `GET /api/commerce/grooming/services/1?storeId=1&petId=5` 返回服务详情、`petRules`、`petEligible=true`（testmember 的宠物）。
- `PUT /api/admin/commerce/grooming/services/1/status?status=ENABLED` 成功。

### 3.4 商城/库存
- 创建商品分类 `Snacks`、商品 `SNACK-001`（ON_SALE）、SKU `SNACK-001-500G`（3990 分）。
- `POST /api/admin/commerce/inventory/adjust` `{skuId:1,changeQty:50}` → `availableQty=50`；再 `changeQty:-5` → 45。
- `GET /api/admin/commerce/inventory/logs?skuId=1` 返回 2 条流水，含变动前/后与管理员备注。
- `GET /api/commerce/shop/products` 返回商品，`minPrice=3990`；详情返回 SKU 与 `availableQty=45`。
- `PUT /api/admin/commerce/shop/products/1/sale-status?saleStatus=ON_SALE` 成功。
- `PUT /api/admin/commerce/shop/skus/1` 更新 SKU 成功（version 1）。

### 3.5 Phase 2 交易链路（新增）
- 地址：创建/更新/设为默认/删除均成功；首个地址默认。
- 购物车：加购、修改数量/勾选、删除成功；库存不足会拒绝加购。
- 结算预览：商品 3990×1，admin 会员价 3990；testmember 银卡 0.98，3990→3910；优惠券选项按金额过滤。
- 提交订单：生成 `ORD...`，15 分钟超时；同一 `userId + idempotencyKey` 重复提交返回同一订单。
- 钱包支付：admin 3990 分订单从赠送余额扣 3990（bonus 8000→4010），生成 `PAYMENT` 流水、`wallet_payment_allocation`、批次明细；订单 PAID，库存 locked→sold。
- 优惠券 + 模拟微信：testmember 2 件商品原价 7980、会员优惠 160、5 元券 500，应付 7320；提交锁定券、模拟支付后券 USED，`used_order_no` 正确；库存确认。
- 取消：WAIT_PAY 订单取消后券恢复 AVAILABLE、库存释放；重复取消被状态校验拒绝。
- 管理端订单列表/详情正常；管理员关闭 WAIT_PAY 订单后库存释放、状态 CLOSED。
- 重复支付：已 PAID 订单再次调用支付返回原支付记录，不重复扣款。
- 订单超时任务已编码（`OrderExpireJob` 每分钟）；未做真实等待 15 分钟的运行时验证。

### 3.6 Phase 3 预约/套餐（2026-11-07 冒烟脚本）
- 时段：管理员创建时段、App 查询可约时段通过。
- 在线预约：testmember 创建 `PENDING_PAYMENT` 预约 + `GROOMING` 待支付订单；模拟微信支付后预约 `CONFIRMED`、订单 `PAID`；门店签到→开始→完成，预约 `COMPLETED`、订单 `COMPLETED`。
- 取消：用户取消未支付预约后预约 `CANCELLED`，时段 `availableCapacity` 恢复。
- 改期：预约改到新时段；旧时段锁定释放、新时段 `lockedCapacity=1`。
- 已支付取消：管理员取消已支付预约，预约 `CANCELLED`、时段释放，订单仍 `PAID`（退款待 Phase 4）。
- 套餐模板：管理员创建固定项 3 次 + 选项加价 10 元；预览原价 11000、会员优惠 220、应付 10780。
- 套餐购买：模拟微信支付后按订单项发放用户套餐，固定项+选项合计 4 次；订单 `COMPLETED`。
- 套餐预约：使用套餐次数创建 `CONFIRMED` 预约，完成签到/开始/完成后次数 `used=1`、流水 `USED`。
- 套餐取消：另一笔套餐预约管理员取消后冻结次数释放，流水 `RELEASED`。
- 结果：全部断言通过（`ALL PHASE3 SMOKE TESTS PASSED`）。

### 3.7 Phase 4 物流/售后/评价（2026-11-07 冒烟脚本）
- 商品订单：admin 使用钱包支付 3990，订单 PAID。
- 发货：管理员填写物流公司/单号发货，订单 `SHIPPED`，用户可查发货信息。
- 校验：已发货后申请「仅退款」被拒绝。
- 确认收货：用户确认收货，订单 `COMPLETED`、发货单 `DELIVERED`。
- 评价：提交 5 星评价 `PENDING`；重复评价被拒绝；管理员通过并回复；用户查询为 `APPROVED` + 回复内容。
- 售后：对已完成订单申请「退货退款」3990；管理员审核通过后执行钱包退款，退款单 `REFUNDED`、渠道 `WALLET`；订单 `payment_status=REFUNDED`、`refunded_amount=paid_amount`；钱包余额恢复；库存 `sold-1/available+1` 恢复。
- 模拟微信退款：testmember 模拟微信支付 3910，发货前申请「仅退款」；管理员通过并退款，退款渠道 `MOCK_WECHAT`、外部单号 `MOCK-...`，订单退款完成。
- 校验：未发货申请「退货退款」被拒绝；同一订单项重复售后被拒绝。
- 结果：22 项断言全部通过（`ALL PHASE4 SMOKE TESTS PASSED`）。

### 3.8 Phase 5 自动化测试
- 测试框架：JUnit 5 + Mockito（spring-boot-starter-test）+ Testcontainers 2.0.5 + MySQL 8.0。
- 测试库：Testcontainers 启动独立 MySQL，`server/src/test/resources/schema-test.sql` 由 `init-scripts/mysql/01~12` 拼接生成；`mvn clean test` 不依赖本地开发库。
- 单元测试：
  - `CommercePriceUtilsTest`：会员折扣空值、四舍五入、0/1 折，3 个用例。
  - `CommerceRefundUtilsTest`：全额/按比例/上限/非法参数，4 个用例。
- 集成测试 `CommerceFlowIntegrationTest`（5 个用例）：
  - 商品订单：购物车→结算→同幂等键重复提交返回同单→模拟支付→发货→确认收货→评价提交/通过/回复→退货退款；校验订单退款金额、库存 `available+1/sold-1`、跨用户查单被拒。
  - 预约：时段容量 1 时第二笔预约失败；支付后 `locked→reserved`；改期释放旧时段/占用新时段；门店取消已支付预约（订单保持 PAID）；用户取消未支付预约（订单 CANCELLED、容量释放）。
  - 套餐：模板创建→购买→支付发放→套餐次数预约→签到/开始/完成→次数核销 `used+1`、流水 `USED`。
  - 并发：两个线程同时锁定容量 1 的时段，仅 1 个成功（条件 UPDATE 原子性）。
- 运行结果：`Tests run: 5, Failures: 0, Errors: 0`（集成）+ `3 + 4`（单元），总计 12 个测试通过。
- 说明：`schema-test.sql` 是测试用快照；修改 `01~12` 迁移后需同步重新生成（后续可改为 Testcontainers 启动后执行脚本）。

## 4. 未覆盖/未验证

- **浏览器人工走查**：管理后台新增「预约管理」「套餐管理」页面已构建通过，但未在浏览器逐页点击。
- **微信开发者工具**：小程序新增预约 3 页 + 套餐 3 页已注册、TypeScript 检查通过；未在开发者工具/真机走查。
- **图片上传**：`type=store|shop` 已加入后端白名单，未实际上传文件验证 MinIO URL。
- **并发/压测**：集成测试覆盖「两线程抢 1 个时段容量」；套餐次数并发、订单支付并发、退款并发未做完整压测。
- **订单/预约超时**：`OrderExpireJob` 已实现；未等待 15 分钟验证预约超时释放。
- **自动确认收货**：`ShipmentAutoConfirmJob` 每小时扫描发货超过 7 天订单；未等待 7 天/修改 shipped_time 做运行时验证。
- **越权**：集成测试覆盖「用户 A 读取用户 B 订单」；预约/套餐/售后/评价的跨用户访问未逐一自动化。
- **灰度**：发布清单与 T01~T26 矩阵见 `07-release-checklist.md`；实际门店/商品灰度未执行（需运维环境）。
- **钱包退款**：已覆盖钱包原路退款与模拟微信退款；真实微信退款未接入；部分退款、多次部分退款未做专项用例。
- **退款规则待定**：优惠券、积分、成长值退款恢复未实现；套餐退款公式未实现。
- **售后并发/幂等**：条件更新与状态门控已实现，未做并发压测。
- **微信真实支付**：无商户资质/证书/openid，未接入；仅 dev mock。
- **历史测试数据**：第一次冒烟运行（修复前）留下 1 条 `service_package_usage.status=FROZEN` 的旧测试数据，不影响新逻辑，可随本地数据卷重置清理。

## 5. 本地测试数据（Phase 1 + Phase 2）

- 门店：id 1，`TEST-STORE-1` / Test Store；1 条营业时间；员工 Alice；1 个班次。
- 美容：分类 Bath；服务 BATH-001（12800 分）；门店价 11800 分；宠物规则 dog 1000-30000g。
- 商城：分类 Snacks；商品 SNACK-001；SKU SNACK-001-500G（3990 分）；库存 42/锁定 0/已售 3；库存流水含管理调整与订单预占/确认。
- 商品订单：4 笔（2 笔 PAID：admin 钱包 3990、testmember 券+模拟微信 7320；1 笔 CANCELLED；1 笔 CLOSED）。
- 钱包：admin 本金 50000、赠送 4010；`wallet_payment_allocation` 1 条（BONUS 3990）。
- 优惠券：testmember 2 张已使用、1 张已释放（AVAILABLE）。
- 地址：admin 1 条、testmember 1 条。
- Phase 3 预约：10 笔（在线支付 4 笔、套餐 3 笔、取消/改期若干）；2 个用户套餐（各 4 次）；4 条核销流水（1 条旧运行遗留 FROZEN、2 条 USED、2 条 RELEASED）；10 个时段；2 个套餐模板；GROOMING/PACKAGE 订单 8 笔。
- Phase 4：发货单 1 张（DELIVERED）；售后 2 单（RETURN_REFUND 3990 钱包退款、REFUND_ONLY 3910 模拟微信退款）；退款记录 2 条（WALLET/MOCK_WECHAT 均 REFUNDED）；评价 1 条（5 星 APPROVED + 回复）；商品订单 17/18 已全额退款。
- 这些为本地冒烟数据，可删除或重置数据卷。
