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

发布会短暂停止后端和 Web 容器。现有 Compose 配置、环境变量及数据库卷由服务器维护。涉及数据库结构的改动应先按项目流程执行对应迁移。发布包和备份分别留在 `releases/` 和 `backups/`，历史前端静态文件保留以支持仍打开旧页面的浏览器。
