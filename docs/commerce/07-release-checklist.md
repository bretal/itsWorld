# 07 发布与灰度检查清单（Phase 5）

> 日期：2026-11-07
> 范围：商业服务 Phase 1~4（门店/美容/商品/库存、订单/支付、预约/套餐、物流/售后/评价）。
> 结论：功能与本地冒烟通过；上线前仍需真实微信支付/退款资质、人工走查与并发压测。

## 1. 上线前检查

### 1.1 配置

- [ ] `app.payment.mock-enabled=false`（生产禁止模拟支付）。
- [ ] `jwt.secret`、`DB_PASSWORD`、`MINIO_ACCESS_KEY/SECRET_KEY` 使用环境变量，不落库。
- [ ] MinIO bucket：`itsworld-avatars`、`itsworld-diary`、`itsworld-community`、`itsworld-temp` 已创建并配置访问策略。
- [ ] `minio.public-endpoint` 指向生产图片域名（当前 `https://lvhaorankk.top/files`）。
- [ ] CORS 仅允许管理后台域名与小程序合法来源。
- [ ] `app.payment.mock-enabled` 在 dev 才可开启。

### 1.2 数据库

- [ ] 备份 `its_world`。
- [ ] 按顺序执行 `08-commerce-base.sql` → `12-commerce-fulfillment-aftersale-review.sql`。
- [ ] 校验表数 80，重点表存在：`commerce_order`、`booking_slot`、`user_service_package`、`commerce_shipment`、`commerce_after_sale`、`commerce_review`。
- [ ] 确认 `user_coupon.lock_order_no/used_order_no/version` 已存在。
- [ ] 回滚脚本 `rollback/99-commerce-dev-down.sql` 仅开发环境可用，生产不得执行。

### 1.3 外部依赖

- [ ] 真实微信支付：商户号、API v3 证书、回调域名、openid/微信登录方案（当前未就绪，生产只能使用钱包支付）。
- [ ] 短信/订阅消息、物流查询服务（当前仅人工录入物流单号）。
- [ ] 监控：支付成功率、超时关单数、退款失败数、库存差异、预约超时释放。

## 2. 灰度策略

当前没有代码级灰度开关，建议按「门店/商品」运营灰度：

1. 只保留试点门店 `business_status=OPEN`，其他门店 `CLOSED`；App 门店/服务/商品只展示启用数据。
2. 非试点商品 `sale_status=OFF_SALE` / SKU `DISABLED`；非试点套餐 `status=DISABLED`。
3. 只给试点门店配置 `booking_slot`；未配置门店的 App 预约页无时段可选。
4. 观察 1~3 天：下单、支付、预约、核销、退款、评价链路与资金对账。
5. 逐步开放更多门店/商品；每次变更保留操作记录。

## 3. 测试矩阵（对应原 T01~T26）

| 编号 | 场景 | 覆盖方式 | 状态 |
|---|---|---|---|
| T01 | 商品订单重复提交幂等 | 接口冒烟 / 订单幂等键 | 已覆盖 |
| T02 | 库存不足/超卖 | 库存条件 UPDATE + 冒烟 | 部分覆盖（未并发压测） |
| T03 | 取消/超时释放库存与优惠券 | 冒烟 + `OrderExpireJob` | 已覆盖（超时未等待 15 分钟） |
| T04 | 重复支付幂等 | 支付记录幂等键 | 已覆盖 |
| T05 | 钱包本金/赠送扣款顺序 | 钱包支付冒烟 | 已覆盖 |
| T06 | 钱包退款原路退回 | Phase 4 冒烟 | 已覆盖 |
| T07 | 优惠券锁定/核销/释放 | Phase 2 冒烟 | 已覆盖 |
| T08 | 预约时段容量锁定 | Phase 3 冒烟 | 已覆盖 |
| T09 | 预约超时释放 | `OrderExpireJob` 事件联动 | 未等待验证 |
| T10 | 预约改期容量释放/占用 | Phase 3 冒烟 | 已覆盖 |
| T11 | 套餐购买发放幂等 | `uk_order_item` + 冒烟 | 已覆盖 |
| T12 | 套餐次数冻结/核销/释放 | Phase 3 冒烟 | 已覆盖 |
| T13 | 套餐次数并发扣减 | 条件 UPDATE | 未并发压测 |
| T14 | 已支付预约取消退款 | 预约取消 + 售后规则 | 退款走售后，未做组合用例 |
| T15 | 越权访问订单/预约/套餐/售后 | 归属校验代码审查 | 待补充自动化用例 |
| T16 | 发货与确认收货 | Phase 4 冒烟 | 已覆盖 |
| T17 | 7 天自动确认收货 | `ShipmentAutoConfirmJob` | 未等待验证 |
| T18 | 售后申请规则（仅退款/退货退款） | Phase 4 冒烟 | 已覆盖 |
| T19 | 售后重复申请 | Phase 4 冒烟 | 已覆盖 |
| T20 | 退款金额不超过实付 | `CommerceRefundUtils` 单测 + 条件更新 | 已覆盖 |
| T21 | 退款库存回补 | Phase 4 冒烟 | 已覆盖 |
| T22 | 评价唯一/审核/回复 | Phase 4 冒烟 | 已覆盖 |
| T23 | 价格快照（后台改价不影响历史订单） | 订单项快照 | 待补充用例 |
| T24 | 定时任务重复执行 | `@Scheduled` + 条件更新 | 代码审查 |
| T25 | 迁移脚本顺序执行 | 本地 01→12 | 已覆盖 |
| T26 | 回滚脚本 | `rollback/99-commerce-dev-down.sql` | 开发环境可执行 |

## 4. 发布后观察

- 订单：`WAIT_PAY` 超时未关闭、重复支付、库存锁定未释放。
- 预约：时段容量负数、预约完成未核销套餐次数、取消未释放容量。
- 退款：`REFUND_FAILED`、钱包退款分摊与原支付批次不一致、订单退款金额超过实付。
- 评价：重复评价、审核积压。
- 资金：钱包流水与订单支付/退款对账、优惠券状态与订单状态一致性。

## 5. 回滚

1. 停止新订单/预约写入（关闭门店或下架商品/套餐）。
2. 备份数据库。
3. 开发环境可执行 `init-scripts/mysql/rollback/99-commerce-dev-down.sql`；生产必须按备份与单独迁移方案回滚，不得直接删表。
4. 回滚后验证核心用户/宠物/日记/社区功能不受影响。
