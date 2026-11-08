# 06 变更清单（Phase 1~5）

> 日期：2026-11-07
> 范围：门店/美容/商品/SKU/库存、订单/支付、预约/套餐、物流/售后/评价；管理后台 + 小程序；Phase 5 测试与发布清单。
> 说明：只列本阶段真实新增/修改文件；此前会员/钱包/管理后台任务的文件不重复列入。

## 1. 数据库迁移

- 新增：`init-scripts/mysql/08-commerce-base.sql`
  - 14 张表（见 `02-database-schema.md`）。
  - 已有数据卷需手动执行；新数据卷按 01→08 自动执行。
- 未修改 01~07。

## 2. 后端新增文件（`server/src/main/java/com/itsworld/commerce/`）

```text
commerce/common/CommercePriceUtils.java

commerce/store/
  controller/StoreController.java
  controller/StoreAdminController.java
  dto/StoreView.java
  dto/StoreDetailView.java
  dto/StoreSaveRequest.java
  dto/BusinessHourSaveRequest.java
  dto/StaffSaveRequest.java
  dto/StaffShiftSaveRequest.java
  entity/CommerceStore.java
  entity/CommerceStoreBusinessHour.java
  entity/CommerceStaff.java
  entity/CommerceStaffShift.java
  mapper/CommerceStoreMapper.java
  mapper/CommerceStoreBusinessHourMapper.java
  mapper/CommerceStaffMapper.java
  mapper/CommerceStaffShiftMapper.java
  service/StoreService.java
  service/impl/StoreServiceImpl.java

commerce/grooming/
  controller/GroomingController.java
  controller/GroomingAdminController.java
  dto/GroomingServiceView.java
  dto/GroomingServiceDetailView.java
  dto/GroomingCategorySaveRequest.java
  dto/GroomingServiceSaveRequest.java
  dto/GroomingServiceStoreSaveRequest.java
  dto/GroomingPetRuleSaveRequest.java
  entity/GroomingCategory.java
  entity/GroomingService.java
  entity/GroomingServiceStore.java
  entity/GroomingServicePetRule.java
  mapper/GroomingCategoryMapper.java
  mapper/GroomingServiceMapper.java
  mapper/GroomingServiceStoreMapper.java
  mapper/GroomingServicePetRuleMapper.java
  service/GroomingCatalogService.java
  service/impl/GroomingCatalogServiceImpl.java

commerce/catalog/
  controller/ShopController.java
  controller/ShopAdminController.java
  dto/ShopProductView.java
  dto/ShopProductDetailView.java
  dto/ShopSkuView.java
  dto/ShopCategorySaveRequest.java
  dto/ShopProductSaveRequest.java
  dto/ShopSkuSaveRequest.java
  dto/ShopProductImageSaveRequest.java
  entity/ShopCategory.java
  entity/ShopProduct.java
  entity/ShopProductImage.java
  entity/ShopSku.java
  mapper/ShopCategoryMapper.java
  mapper/ShopProductMapper.java
  mapper/ShopProductImageMapper.java
  mapper/ShopSkuMapper.java
  service/ShopCatalogService.java
  service/impl/ShopCatalogServiceImpl.java

commerce/inventory/
  controller/InventoryAdminController.java
  dto/InventoryView.java
  dto/InventoryLogView.java
  dto/InventoryAdjustRequest.java
  entity/ShopInventory.java
  entity/ShopInventoryLog.java
  mapper/ShopInventoryMapper.java
  mapper/ShopInventoryLogMapper.java
  service/InventoryService.java
  service/impl/InventoryServiceImpl.java
```

## 3. 后端修改文件

| 文件 | 修改 |
|---|---|
| `member/service/MemberService.java` | 新增 `BigDecimal getCurrentDiscountRate(Long userId)`（会员价解析） |
| `member/service/impl/MemberServiceImpl.java` | 实现 `getCurrentDiscountRate`：`getOrCreateAccount` + `member_level.discount_rate` |
| `common/controller/UploadController.java` | 新增上传类型 `store`、`shop`、`review`（映射现有 community bucket） |

## 4. 前端管理后台新增/修改

新增：
```text
frontend/src/types/commerce.ts
frontend/src/services/adminCommerceService.ts
frontend/src/pages/admin/commerce/StoreManagementPage.tsx
frontend/src/pages/admin/commerce/GroomingManagementPage.tsx
frontend/src/pages/admin/commerce/ProductManagementPage.tsx
frontend/src/pages/admin/commerce/InventoryManagementPage.tsx
```

修改：
| 文件 | 修改 |
|---|---|
| `frontend/src/App.tsx` | 新增 4 个 `/admin/commerce/**` 路由（AdminRoute 包裹） |
| `frontend/src/layouts/MainLayout.tsx` | 新增「商业服务」子菜单（门店/美容/商品/库存），子菜单展开与选中逻辑 |
| `frontend/src/services/uploadService.ts` | `UploadType` 增加 `store/shop/review` |

## 5. 小程序新增/修改

新增：
```text
miniprogram-native/services/commerce.ts
miniprogram-native/pages/commerce/grooming/index.{ts,wxml,wxss,json}
miniprogram-native/pages/commerce/grooming/detail.{ts,wxml,wxss,json}
miniprogram-native/pages/commerce/store/index.{ts,wxml,wxss,json}
miniprogram-native/pages/commerce/store/detail.{ts,wxml,wxss,json}
miniprogram-native/pages/commerce/shop/index.{ts,wxml,wxss,json}
miniprogram-native/pages/commerce/shop/detail.{ts,wxml,wxss,json}
```

修改：
| 文件 | 修改 |
|---|---|
| `miniprogram-native/app.json` | 注册 6 个 commerce 页面 |
| `miniprogram-native/pages/home/index.ts` | 新增 `goGrooming`、`goShop` |
| `miniprogram-native/pages/home/index.wxml` | 首页功能宫格新增「美容服务」「宠物商城」 |
| `miniprogram-native/pages/home/index.wxss` | 新图标背景样式 |
| `miniprogram-native/utils/format.ts` | 新增 `formatMoney` |

## 6. 关键方法/接口

- `StoreService`：`pageStores`、`getStoreDetail`、`pageAdminStores`、`createStore`、`updateStore`、`changeStoreStatus`、`listHours/createHour/updateHour/deleteHour`、`listStaff/createStaff/updateStaff/changeStaffStatus`、`listShifts/createShift/updateShift/deleteShift`。
- `GroomingCatalogService`：`listEnabledCategories`、`pageServices`、`getServiceDetail`、`listAdminCategories`、分类/服务/门店价格/宠物规则 CRUD、`change*Status`。
- `ShopCatalogService`：`listEnabledCategories`、`pageProducts`、`getProductDetail`、`listAdminCategories`、分类/商品/图片/SKU CRUD、`change*Status`。
- `InventoryService`：`pageInventory`、`adjustStock`、`pageLogs`。
- `MemberService#getCurrentDiscountRate`：会员折扣乘数解析（新方法）。
- `UploadController`：`type=store|shop|review`（新白名单）。

## 7. Phase 2 新增/修改文件

新增后端：
```text
commerce/address/
  controller/AddressController.java
  dto/AddressSaveRequest.java
  entity/UserShippingAddress.java
  mapper/UserShippingAddressMapper.java
  service/AddressService.java
  service/impl/AddressServiceImpl.java

commerce/cart/
  controller/CartController.java
  dto/CartItemAddRequest.java
  dto/CartItemUpdateRequest.java
  dto/CartItemView.java
  entity/ShopCartItem.java
  mapper/ShopCartItemMapper.java
  service/CartService.java
  service/impl/CartServiceImpl.java

commerce/order/
  controller/CheckoutController.java
  controller/CommerceOrderController.java
  controller/CommercePaymentController.java
  controller/CommerceOrderAdminController.java
  dto/CheckoutPreviewRequest.java
  dto/CheckoutItemView.java
  dto/CheckoutPreviewResponse.java
  dto/CheckoutSubmitRequest.java
  dto/OrderSubmitResponse.java
  dto/OrderItemView.java
  dto/OrderDetailView.java
  dto/PaymentCreateRequest.java
  dto/PaymentResultView.java
  entity/CommerceOrder.java
  entity/CommerceOrderItem.java
  entity/CommerceOrderDiscount.java
  entity/CommerceOrderStatusLog.java
  entity/CommercePaymentLink.java
  mapper/CommerceOrderMapper.java
  mapper/CommerceOrderItemMapper.java
  mapper/CommerceOrderDiscountMapper.java
  mapper/CommerceOrderStatusLogMapper.java
  mapper/CommercePaymentLinkMapper.java
  service/CheckoutService.java
  service/CommerceOrderService.java
  service/CommercePaymentService.java
  service/impl/CheckoutServiceImpl.java
  service/impl/CommerceOrderServiceImpl.java
  service/impl/CommercePaymentServiceImpl.java
  job/OrderExpireJob.java

commerce/inventory/
  entity/ShopInventoryReservation.java
  mapper/ShopInventoryReservationMapper.java

wallet/
  entity/WalletPaymentAllocation.java
  entity/WalletRefund.java
  entity/WalletRefundAllocation.java
  mapper/WalletPaymentAllocationMapper.java
  mapper/WalletRefundMapper.java
  mapper/WalletRefundAllocationMapper.java
  dto/WalletPaymentResult.java

member/
  dto/CouponOptionView.java
  dto/CouponQuote.java

common/config/SchedulingConfig.java
```

修改后端：
| 文件 | 修改 |
|---|---|
| `commerce/inventory/mapper/ShopInventoryMapper.java` | 新增 `reserveStock/confirmReservedStock/releaseReservedStock` 条件更新 |
| `commerce/inventory/service/InventoryService.java` + Impl | 新增订单预占/确认/释放 |
| `wallet/service/WalletService.java` + Impl | 新增 `payForBusinessOrder`；赠送优先/到期优先扣批次，写 `wallet_payment_allocation` |
| `member/entity/UserCoupon.java` | 新增 `lockOrderNo/usedOrderNo/version` |
| `member/mapper/UserCouponMapper.java` | 新增锁券/核销/释放/恢复条件更新 |
| `member/service/CouponService.java` + Impl | 新增 `quoteCoupon/listCouponOptions/lockCoupon/consumeCoupon/releaseCoupon/restoreCoupon` |
| `common/result/ErrorCode.java` | 新增 1718/1719 优惠券错误码 |
| `common/config/SchedulingConfig.java` | `@EnableScheduling` |

新增/修改前端：
- 新增 `frontend/src/pages/admin/commerce/OrderManagementPage.tsx`
- 扩展 `frontend/src/types/commerce.ts`（订单/支付类型）
- 扩展 `frontend/src/services/adminCommerceService.ts`（订单列表/详情/关闭）
- 修改 `frontend/src/App.tsx`、`MainLayout.tsx`（订单管理路由/菜单）

新增/修改小程序：
- 扩展 `miniprogram-native/services/commerce.ts`（地址/购物车/结算/订单/支付）
- 新增 `pages/commerce/cart/index.*`
- 新增 `pages/commerce/checkout/index.*`
- 新增 `pages/commerce/order/index.*`、`pages/commerce/order/detail.*`
- 新增 `pages/commerce/address/index.*`、`pages/commerce/address/edit.*`
- 修改 `pages/commerce/shop/detail.*`（加入购物车/购物车入口）
- 修改 `pages/profile/index.*`（我的订单/购物车/收货地址入口）
- 修改 `app.json`（注册 6 个新页面）

迁移：
- 新增 `init-scripts/mysql/09-commerce-order.sql`、`10-wallet-payment-refund.sql`。

## 8. 迁移步骤（Phase 1 + Phase 2）

1. 备份数据库。
2. 对已有数据卷按顺序执行 `08-commerce-base.sql`、`09-commerce-order.sql`、`10-wallet-payment-refund.sql`。
3. `10` 内含非幂等 `ALTER TABLE user_coupon`，同一库只执行一次。
4. 启动后端，确认 63 张表可访问、`OrderExpireJob` 已注册。
5. 前端 `npm run build`；小程序 `npm run typecheck`；微信开发者工具导入/预览。
6. 如需回滚：开发环境按外键逆序删除 Phase 2 表；生产需单独迁移回滚方案，不得直接删表。

## 9. 本阶段测试数据

- 门店 1、营业时间 1、员工 1、班次 1。
- 美容分类 1、服务 1、门店价格 1、宠物规则 1。
- 商品分类 1、商品 1、SKU 1、库存 42/0/3、库存流水含管理调整与订单预占/确认。
- 订单 4 笔（2 PAID、1 CANCELLED、1 CLOSED）；钱包支付分摊 1 条；testmember 优惠券 2 张 USED、1 张 AVAILABLE。
- 详见 `05-test-report.md`。

## 10. Phase 3 新增/修改文件

迁移：
- 新增 `init-scripts/mysql/11-commerce-booking-package.sql`（10 表）；已在本地库执行，当前 73 表。

新增后端：
```text
commerce/order/event/CommerceOrderStatusChangedEvent.java

commerce/booking/
  controller/BookingController.java
  controller/BookingAdminController.java
  dto/BookingCreateResult.java
  dto/PackageBookingOptionView.java
  service/BookingService.java
  service/impl/BookingServiceImpl.java

commerce/packagebiz/
  controller/PackageController.java
  controller/PackageAdminController.java
  dto/PackagePurchasePreviewRequest.java
  dto/PackagePurchasePreview.java
  dto/PackagePurchaseRequest.java
  dto/PackagePurchaseResult.java
  dto/PackageSelectionItemView.java
  dto/PackageUsageView.java
  dto/UserPackageView.java
  dto/UserPackageItemView.java
  service/PackageService.java
  service/impl/PackageServiceImpl.java
```

修改后端：
| 文件 | 修改 |
|---|---|
| `commerce/booking/dto/BookingCreateRequest.java` | `payType` 改为 `ONLINE/PACKAGE`；新增 `couponId`、`idempotencyKey` |
| `commerce/booking/mapper/GroomingBookingMapper.java` | 新增确认/取消/签到/开始/完成/改期条件更新 |
| `commerce/packagebiz/mapper/ServicePackageUsageMapper.java` | 新增按预约查冻结流水、按用户套餐查流水、标记 USED/RELEASED、幂等查询 |
| `commerce/grooming/service/GroomingCatalogService.java` + Impl | 新增 `requireEnabledService`、`requireEnabledServiceStore`、`resolveStorePrice`、`validatePetEligible` |
| `commerce/order/mapper/CommerceOrderMapper.java` | 新增 `completeIfPaid` |
| `commerce/order/service/impl/CommerceOrderServiceImpl.java` | 支付/取消/关闭发布 `CommerceOrderStatusChangedEvent`，由预约/套餐监听履约 |
| `common/result/ErrorCode.java` | 新增 1901~1914 预约/套餐错误码 |

新增前端管理后台：
```text
frontend/src/pages/admin/commerce/BookingManagementPage.tsx
frontend/src/pages/admin/commerce/PackageManagementPage.tsx
```

修改前端管理后台：
- `frontend/src/types/commerce.ts`：新增预约/套餐类型。
- `frontend/src/services/adminCommerceService.ts`：新增时段/预约/套餐接口。
- `frontend/src/App.tsx`：新增 `/admin/commerce/bookings`、`/admin/commerce/packages` 路由。
- `frontend/src/layouts/MainLayout.tsx`：商业服务菜单新增「预约管理」「套餐管理」。

新增/修改小程序：
- 扩展 `miniprogram-native/services/commerce.ts`（预约/套餐类型与接口）。
- 新增 `pages/commerce/booking/index|list|detail.*`（预约创建/列表/详情，含支付、取消、改期）。
- 新增 `pages/commerce/package/index|detail|mine.*`（套餐列表/定制购买/我的套餐与使用记录）。
- 修改 `app.json` 注册 6 个页面。
- 修改 `pages/commerce/grooming/detail.*`：「立即预约」入口。
- 修改 `pages/home/index.*`：新增「宠物套餐」「附近门店」入口。
- 修改 `pages/profile/index.ts`：新增「我的预约」「我的套餐」入口。

## 11. Phase 4 新增/修改文件

迁移：
- 新增 `init-scripts/mysql/12-commerce-fulfillment-aftersale-review.sql`（7 表）；已在本地库执行，当前 80 表。

新增后端：
```text
commerce/fulfillment/
  controller/ShipmentController.java
  controller/ShipmentAdminController.java
  dto/ShipmentCreateRequest.java
  dto/ShipmentItemView.java
  dto/ShipmentView.java
  entity/CommerceShipment.java
  entity/CommerceShipmentItem.java
  mapper/CommerceShipmentMapper.java
  mapper/CommerceShipmentItemMapper.java
  service/ShipmentService.java
  service/impl/ShipmentServiceImpl.java
  job/ShipmentAutoConfirmJob.java

commerce/aftersale/
  controller/AfterSaleController.java
  controller/AfterSaleAdminController.java
  dto/AfterSaleApplyItemRequest.java
  dto/AfterSaleApplyRequest.java
  dto/AfterSaleItemView.java
  dto/AfterSaleReviewRequest.java
  dto/AfterSaleView.java
  entity/CommerceAfterSale.java
  entity/CommerceAfterSaleItem.java
  entity/CommerceAfterSaleLog.java
  entity/CommerceRefundLink.java
  mapper/CommerceAfterSaleMapper.java
  mapper/CommerceAfterSaleItemMapper.java
  mapper/CommerceAfterSaleLogMapper.java
  mapper/CommerceRefundLinkMapper.java
  service/AfterSaleService.java
  service/impl/AfterSaleServiceImpl.java

commerce/review/
  controller/ReviewController.java
  controller/ReviewAdminController.java
  dto/ReviewReplyRequest.java
  dto/ReviewSubmitRequest.java
  dto/ReviewView.java
  entity/CommerceReview.java
  mapper/CommerceReviewMapper.java
  service/ReviewService.java
  service/impl/ReviewServiceImpl.java

wallet/dto/WalletRefundResult.java
```

修改后端：
| 文件 | 修改 |
|---|---|
| `wallet/service/WalletService.java` + Impl | 新增 `refundBusinessPayment`：按原支付分摊退回原批次、写退款/分摊/流水/明细 |
| `wallet/mapper/WalletRefundMapper.java` | 新增按退款单号查询（幂等） |
| `commerce/order/mapper/CommerceOrderMapper.java` | 新增 `shipIfPaid`、`completeIfShipped`、`addRefundedAmount` |
| `commerce/order/mapper/CommerceOrderItemMapper.java` | 新增 `updateFulfilledQty`、`addRefundedAmount` |
| `commerce/order/mapper/CommercePaymentLinkMapper.java` | 新增 `selectLatestPaidByOrderId` |
| `commerce/inventory/mapper/ShopInventoryMapper.java` | 新增 `restoreSoldStock` |
| `commerce/inventory/service/InventoryService.java` + Impl | 新增 `restoreRefundedStock`，写 `ORDER_REFUND` 库存流水 |
| `common/result/ErrorCode.java` | 新增 1961~1974 物流/售后/评价错误码 |

新增前端管理后台：
```text
frontend/src/pages/admin/commerce/AfterSaleManagementPage.tsx
frontend/src/pages/admin/commerce/ReviewManagementPage.tsx
```

修改前端管理后台：
- `frontend/src/pages/admin/commerce/OrderManagementPage.tsx`：新增发货/物流操作与弹窗。
- `frontend/src/types/commerce.ts`：新增物流/售后/评价类型。
- `frontend/src/services/adminCommerceService.ts`：新增物流/售后/评价接口。
- `frontend/src/App.tsx`：新增 `/admin/commerce/after-sales`、`/admin/commerce/reviews` 路由。
- `frontend/src/layouts/MainLayout.tsx`：商业服务菜单新增「售后管理」「评价管理」。

新增/修改小程序：
- 扩展 `miniprogram-native/services/commerce.ts`（物流/售后/评价类型与接口）。
- 新增 `pages/commerce/shipment/index.*`（物流与确认收货）。
- 新增 `pages/commerce/after-sale/apply|list|detail.*`（售后申请/列表/详情）。
- 新增 `pages/commerce/review/index.*`（订单评价）。
- 修改 `pages/commerce/order/detail.*`：查看物流、确认收货、申请售后、评价入口。
- 修改 `pages/profile/index.ts`：新增「我的售后」入口。
- 修改 `app.json` 注册 5 个页面。
- 修改 `utils/upload.ts`：上传类型扩展 `store/shop/review`。

## 12. Phase 5 测试与发布

新增测试：
```text
server/src/test/java/com/itsworld/commerce/CommerceFlowIntegrationTest.java
server/src/test/java/com/itsworld/commerce/common/CommercePriceUtilsTest.java
server/src/test/java/com/itsworld/commerce/common/CommerceRefundUtilsTest.java
server/src/test/resources/schema-test.sql（由 01~12 迁移拼接的测试库快照）
```

修改：
| 文件 | 修改 |
|---|---|
| `server/pom.xml` | Testcontainers 2.0.5（core/mysql/junit-jupiter）+ commons-io 测试依赖 |
| `commerce/common/CommerceRefundUtils.java` | 新增售后金额计算工具，售后 Service 复用 |
| `commerce/aftersale/service/impl/AfterSaleServiceImpl.java` | 金额计算改为调用 `CommerceRefundUtils` |

新增发布资料：
```text
docs/commerce/07-release-checklist.md
init-scripts/mysql/rollback/99-commerce-dev-down.sql
```

测试范围：
- 单元：会员折扣、售后金额计算。
- 集成（Testcontainers MySQL）：商品订单幂等/支付/发货/收货/评价/退货退款、预约容量/改期/取消、套餐购买/发放/核销、时段并发锁定、跨用户订单访问拒绝。
- 发布清单：T01~T26 测试矩阵、灰度门店/商品策略、上线观察指标、开发环境回滚脚本。

## 13. 未完成（外部依赖/后续）

- 真实微信支付/退款/回调/对账。
- 优惠券、积分、成长值退款追回；套餐退款公式；预约爽约规则。
- 管理端物流列表独立页面（当前发货/物流入口在订单管理页）。
- 门店员工数据权限、RBAC、支付对账/补偿任务。
- 套餐次数/支付/退款并发压测；7 天自动确认收货运行时验证；生产灰度执行。
