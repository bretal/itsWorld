# 前后端自动部署

GitHub Actions 监听 `main` 上的 `frontend/` 和 `server/` 变更。修改部署脚本或工作流也会触发。只修改小程序代码的提交不会触发。也可在 Actions 页签手动运行工作流。

首次启用时，在 GitHub 仓库的 Settings → Environments 中创建 `production`，将部署分支限制为 `main`，并填写以下环境配置。

| 类型 | 名称 | 值 |
| --- | --- | --- |
| Variable | `DEPLOY_HOST` | 生产服务器 SSH 主机 |
| Variable | `DEPLOY_USER` | 有权更新 `/opt/itsworld` 并执行 Docker Compose 的 SSH 用户 |
| Secret | `DEPLOY_SSH_KEY` | 已在服务器授权的部署私钥，供非交互式 SSH 使用 |
| Secret | `DEPLOY_KNOWN_HOSTS` | 已核对指纹的服务器 SSH 主机公钥记录 |

服务器需已运行生产 Compose，使用 `/opt/itsworld/app.jar` 和 `/opt/itsworld/dist`，并存在 `/opt/itsworld/releases`。工作流构建前端和后端，校验上传内容，备份旧文件，再更新两项服务。验证检查匿名 API 返回 `401`，同时比较生产首页与新构建文件。失败时尝试恢复备份并保留失败状态。

发布会短暂停止后端和 Web 容器。现有 Compose 配置、环境变量及数据库卷由服务器维护。发布前会备份数据库，再用新 JAR 的 Flyway 执行迁移；迁移失败会中断，旧应用继续运行。发布包和备份分别留在 `releases/` 和 `backups/`，历史前端静态文件保留以支持仍打开旧页面的浏览器。

## 数据库迁移（Flyway）

唯一的结构迁移目录为 `server/src/main/resources/db/migration/`。V18 是 2026-11-19 生产库的 91 张表结构快照，不包含业务数据、账号、密码或样例数据。空库首次启动由 V18 建表；业务初始配置和管理员应通过单独受控流程配置。旧 `init-scripts/mysql/01~18` 留作历史参考，不再挂载到新 MySQL 容器中自动执行。

已经存在的数据库必须先备份、核对结构和索引与 V18 一致，再显式建立基线；禁止直接开启 `baseline-on-migrate`。生产和本地旧卷均适用。生产首次接入示例（先上传同一版本 app.jar，路径按实际填写）：

```bash
cd /opt/itsworld
docker compose run --rm --no-deps \
  -e FLYWAY_VERIFIED_BASELINE=18 -v /绝对路径/app.jar:/migration/app.jar:ro server \
  java -Xmx128m -Dloader.main=com.itsworld.migration.MigrationRunner \
  -cp /migration/app.jar org.springframework.boot.loader.launch.PropertiesLauncher baseline-v18
```

基线只记录版本，不会帮你补齐缺失的表、字段和索引。不要把尚未执行旧迁移的本地库直接标为 V18。迁移工具读取 Compose 服务的 `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` 环境变量，不在命令或仓库中写凭据。将命令末尾换成 `info`、`validate` 或 `migrate` 可检查状态、校验历史或执行待迁移脚本；不提供 clean/repair 入口。

下次结构变更新增 `V19__说明.sql`，以后递增编号，SQL 必须兼容发布期间仍运行的旧应用。已经执行的版本脚本不得修改或删除，校验和不一致时发布失败。禁止自动 repair 掩盖差异。MySQL DDL 失败可能留下部分变更，需人工核对后修复；应用回退只恢复 JAR/前端，不会自动恢复数据库。数据库备份位于每次发布的 `backups/<release>/database.sql.gz`，恢复前必须确认备份时间之后的业务写入如何处理。

CI 通过隔离 MySQL 测试验证空库迁移、重复执行、现有库拒绝自动基线、显式基线以及 clean 禁用。既有业务测试的 test profile 使用自己的 schema fixture，并关闭自动 Flyway，避免两套建表逻辑冲突。
