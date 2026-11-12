# 后端三层架构整改技术实施文档

日期：2026-11-10

## 1. 目标与本次范围

统一调用关系：Controller → Service → Mapper → 数据库。Service 采用具体类，不再为单一实现建立同名接口与 `impl/*Impl`。Mapper 仍采用 MyBatis 接口。

本次只整改 `commerce/address`；admin 已在上一轮完成整改。其余模块由后续实施者逐模块执行本文件，不纳入本次修改。

注意：存在 Service 接口与 Impl 本身不代表违反三层架构。本项目将“取消单一实现的接口拆分”作为额外命名约定；不得把 Service 改名为 Mapper，也不得把业务逻辑全部下沉到 Mapper。

## 2. 目标目录与职责

```text
<module>/
  controller/XxxController.java
  service/XxxService.java
  mapper/XxxMapper.java
  dto/...
  entity/...
```

- Controller：路由、请求参数校验、身份上下文获取、响应封装；只调用 Service，不注入 Mapper、JdbcTemplate 或数据库连接。
- Service：业务规则、权限及数据归属校验、跨操作编排、事务边界；通过 Mapper 完成持久化。
- Mapper：MyBatis-Plus BaseMapper 通用 CRUD，或注解/XML 自定义 SQL。复杂动态 SQL 可使用 mapper 包内的 SqlProvider。
- DTO、Entity 是数据模型，不是额外业务层；保留现有目录。
- Service 可以使用 LambdaQueryWrapper/LambdaUpdateWrapper 调用 Mapper；不直接执行 JDBC，也不持有原始 SQL。可复用的复杂查询再封装为 Mapper 方法。
- 跨模块调用业务能力时调用目标模块的 Service，不为消除 impl 而绕过其业务规则。

## 3. address 参考整改

原结构为 AddressService 接口 + service/impl/AddressServiceImpl 实现；实现原本已通过 UserShippingAddressMapper 访问数据库。

整改后：

1. 将原实现迁至 `service/AddressService.java`，类名改为 AddressService。
2. 删除原接口和原实现文件，移除空 impl 目录。
3. 保留 `@Service`、构造器注入和全部业务方法签名；移除接口实现声明与失效的 `@Override`。
4. 保留创建、修改、删除、设置默认地址的方法事务。
5. 保留归属校验、默认地址更新、字段清理和 Mapper 调用行为。
6. 保持原 AddressService 包名，Controller 及其他模块通过该类型注入，无需改动业务调用。

## 4. 其余模块逐项实施步骤

### 4.1 建立清单

先执行 `git status --short`，保留已有修改。使用 `rg` 定位目标模块的 Service、Mapper 和调用点，再定向查阅相关文件。

扫描并记录：Service 接口及所有实现、注入点、直接引用 Impl 的生产代码和测试、Bean 名称及 Qualifier、继承关系、JdbcTemplate/原始 SQL 使用、事务、Mapper/XML namespace、自定义 SQL Provider。

遇到真正的多实现策略、对外接口契约、动态代理等情况，先给出单独迁移方案；不要直接覆盖接口导致实现丢失。

### 4.2 合并 Service

对确认只有一个实现的 XxxService：

1. 以实现内容替换原接口文件，修改 package 和类名。
2. 删除 `implements XxxService`，移除仅用于该接口的方法 `@Override`；保留实际父类或其他接口所需声明。
3. 保留 Spring 注解、构造器、可见性、方法签名和业务实现。
4. 更新直接引用 XxxServiceImpl 的 import、测试、配置、工厂及扫描条件。
5. 检查显式 Bean 名、`@Qualifier`、`@Resource(name=...)`、SpEL 等：默认 Bean 名由 xxxServiceImpl 变成 xxxService，所有按名称引用必须同步处理。
6. 删除旧实现文件；确认空目录后删除 impl 目录，不递归删除未知内容。

### 4.3 修复数据访问分层

Controller 中的数据操作移到 Service；Service 中 JDBC 和原始 SQL 移到 Mapper。普通单表 CRUD 优先复用 BaseMapper，复杂关联或聚合查询使用明确的 Mapper 方法及自定义 SQL。

所有请求值通过 `#{...}` 绑定；动态表名、列名、排序字段必须由服务端固定白名单选择，不能直接拼接请求值或使用未校验的 `${...}`。迁移 JDBC 的 `?` 时必须核对参数名、空值、分页及生成主键。

数据库更新与对应审计日志、计数维护等必须保留在同一个 Service 事务内。不要将 `@Transactional` 迁到 Controller。核对自调用、事务传播及异常类型，避免代理方式变化导致事务失效。

### 4.4 保持接口行为

路由、HTTP 方法、校验约束、返回字段、错误码、分页顺序、逻辑删除、租户或用户隔离、乐观锁与审计行为均需保持。特别注意 JDBC 与 MyBatis 返回 Map 时的字段名及 null 字段差异。采用强类型 DTO 的改动应单独评估兼容性，不混入纯架构迁移。

## 5. 验证与验收

在 PowerShell 中将 Maven 的 -D 参数加引号：

```powershell
mvn -f server/pom.xml '-Dcheckstyle.skip=true' '-DskipTests' clean compile
mvn -f server/pom.xml '-Dcheckstyle.skip=true' '-Dtest=CommerceFlowIntegrationTest' test
# admin 回归需要时执行
mvn -f server/pom.xml '-Dcheckstyle.skip=true' '-Dtest=AdminManagementIntegrationTest' test
git diff --check
```

接口改为具体类后必须执行一次 clean，清除旧生产/测试 class；否则增量编译可能出现 IncompatibleClassChangeError（旧字节码仍按接口调用新类）。

集成测试依赖 Docker/MySQL Testcontainers。运行失败时区分环境错误和业务断言失败，不能将“编译通过”描述成“业务回归通过”。每个模块选取实际相关测试，不用无关测试替代。

逐模块验收：

- 目标模块不存在 service/impl 和 *ServiceImpl；保留 Service 与 Mapper 两类明确职责。
- Controller、Service 没有直接 JDBC 或原始 SQL；Controller 不直接访问 Mapper。
- 应用上下文可启动，注入唯一，无遗留实现类引用及 Bean 名错误。
- 正常 CRUD、归属校验、异常分支、分页及事务回滚经过相关验证。
- 地址类场景需覆盖跨用户拒绝访问、默认地址切换；资金/库存类场景需额外验证并发、幂等、事务回滚。
- 已执行编译、相关测试、diff 检查，并记录命令及结果。

## 6. 交付与回退

每次按一个业务模块交付，列出迁移文件、调用点、事务变化、测试结果及未解决问题。其余模块保留待执行状态，不声称已完成。

回退按本次模块的具体变更反向还原；保留用户已有及其他模块修改，禁止使用全仓库 reset/clean 进行回退。无需数据库迁移时不要新增表结构变更。
