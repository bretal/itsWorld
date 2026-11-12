# 宠物助手 V1 接入与验收记录

日期：2026-11-10。实施依据：`pet-assistant-v1-implementation.md`。

## 已实现

- `/api/assistant/sessions` 新建/最近30个会话，按所有者读取/发送；历史按20个轮次游标分页。
- 三表与 Mapper；唯一请求ID、问题摘要冲突校验；成功重放、失败显式重试、全会话用户互斥、120秒租约、小时20次尝试、日30个新会话。
- 短事务申请/完成/失败；模型 HTTP 不持有数据库事务；旧租约不能覆盖新结果。
- DeepSeek 独立客户端：非思考、900 tokens、5秒连接/20秒响应/75秒整轮截止，不自动重试收费调用，最多3次模型请求/2轮工具/4次工具。
- 只读搜索/详情、参数白名单、分类祖先状态校验、有货 SKU 普通售价预算、会员价、最终候选白名单与重新核验。复用商城增加只读定价分支，避免查询工具自动创建会员账户；原有调用保留原行为。
- 原生小程序商城入口、聊天、快捷问题、最近会话、新建、历史分页、商品卡片、复制、图片失败占位、失败重试和待确认状态查询；90秒仅作用于助手发送，旧接口仍15秒。
- 页面离开/账号切换使用代次与 token 校验丢弃旧异步结果，无本地共享会话缓存。
- 入口第三方处理说明、医疗和商品事实边界提示词。助手 Mapper 日志单独 WARN，避免 DEBUG SQL 参数打印问题和答案。

## 本地接入

1. 核对目标为本地开发库 `its_world`，备份后在数据库客户端执行 `init-scripts/mysql/15-assistant.sql`。已有数据卷不会自动再次运行 init 脚本。此迁移仅新增表，可重复执行，不执行 DROP。
2. 当前进程环境中检测到 `DEEPSEEK_API_KEY` 存在；没有将其写入源码、SQL、文档或回复。新启动的 IDE/容器进程需自行确认继承了环境。
3. 设置 `DEEPSEEK_ENABLED=true`、`DEEPSEEK_API_KEY` 和可选的 `DEEPSEEK_MODEL=deepseek-flash`。Spring Boot 不自动读取根目录 `.env`；IDE 可设置环境变量，或在已忽略的 `application-local.yml` 中设置 `ai.deepseek`。不要覆盖既有数据库与 MinIO 配置。检查本地配置是否覆盖了开关/模型。默认保持关闭，缺少配置仅助手调用失败。
4. 重启本地后端，微信开发者工具重新编译 `miniprogram-native`，登录后从商城进入“宠物助手”。API 基址沿用现有配置，服务层不重复添加 `/api`。
5. 生产 Compose 已增加环境变量映射，只修改文件，未运行部署。生产发布须另行授权。回退先关闭开关并隐藏入口，保留会话表。

## 自动验证

```powershell
mvn -f server/pom.xml '-Dcheckstyle.skip=true' '-Dtest=AssistantIntegrationTest,AssistantProductServiceTest,DeepSeekClientTest,CommerceFlowIntegrationTest' test
npm --prefix miniprogram-native run typecheck
npm --prefix miniprogram-native run test:assistant
git diff --check
```

常规测试不会调用付费 API。集成测试在 Testcontainers 临时 MySQL 中初始化既有测试 schema，然后直接执行15号真实迁移两次，验证重复执行。不会连接本地已有业务库。

真实冒烟使用上述 Maven 命令额外添加 `-Dassistant.liveSmoke=true`：只在显式开启时使用环境中的密钥，以一条非敏感查询验证真实模型与临时商品工具；同一问题最多3次模型请求，不自动重试。不要在没有确认费用与目标时反复运行。

## 验证结果与缺口

- 后端27项非联网测试通过：助手集成8项、商品工具3项、HTTP客户端5项、既有商城回归11项。
- 小程序 typecheck 与2项请求契约测试通过；git diff --check 通过。
- 最终 Maven BUILD SUCCESS（28项，0失败/0错误）。测试容器退出后，既有定时任务仍尝试连接已关闭容器，并出现 Surefire 30秒终止提示；不影响本轮断言结果，但测试上下文/定时任务的退出清理仍需完善。
- 首轮幂等测试发现首次返回时间精度与数据库重放不同，已改为写入后统一读取数据库记录；后续回归通过。
- 真实模型冒烟通过：使用已有环境密钥、deepseek-flash 与一条非敏感测试商品查询，返回成功文字与真实测试库商品卡片，ID=990002、价格3990分断言通过；模型调用由客户端限制最多3次。最终本轮共28项后端测试（含1项显式开启的联网冒烟）通过。
- 迁移仅在临时测试容器执行；未向已有本地库或生产库执行迁移，未部署。
- 微信页面视觉、键盘/安全区、长历史滚动、断网/退出重进/账号切换的真实设备操作尚未验收，由用户提供截图与操作结果；没有操控电脑。
- 自动化覆盖主路径与关键安全/并发边界，并非原方案所有故障组合已逐项覆盖；保存事务故障注入、完整上下文断言、全部恶意工具/模型轮次组合及真实设备端到端回归仍可补充。
- 检索仅名称/副标题/编码关键词，没有语义召回、宠物档案或知识库；缺少适用年龄等字段时不保证精准推荐。卡片为历史快照，以详情实时价格库存为准。

官方接口按实施时的 [DeepSeek工具文档](https://api-docs.deepseek.com/guides/tool_calls/) 与 [首次调用文档](https://api-docs.deepseek.com/quick_start/agent_integrations/) 核对。

## 后续本地接入修复

2026-11-10 用户报告会话列表500，日志确认为assistant_session缺表。已核对开发库localhost:13306/its_world，对本机its-world-mysql执行15号新增三表迁移，并验证三表存在。此前“已有开发库尚未迁移”的记录由本条更新。未操作生产；待用户登录态刷新验证接口。
