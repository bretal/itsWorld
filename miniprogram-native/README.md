# 它的世界 · 微信原生小程序

微信原生小程序客户端，按 10 屏设计稿实现。底部 Tab 为「首页 / 相册 / 社区 / 我的」，中间悬浮「+」发布。

> 注意：项目根目录下原有的 `miniprogram/` 是未完成的 Taro 工程，不再使用；本目录才是当前客户端。

## 当前进度

2026-11-05 里程碑 1：工程骨架 + 16 个页面

- 启动页、3 屏引导、账号密码登录
- 自定义 TabBar（含中间发布按钮）
- 首页：宠物卡/切换、四个功能入口、今日动态
- 相册：宠物日记时间轴、分类 Tab、点赞
- 社区：关注/推荐 Tab、帖子流、点赞/取消、帖子详情、发帖
- 我的：资料卡、宠物数/照片数、菜单、退出登录、资料编辑
- 宠物：列表、新建/编辑、详情（成长/健康/档案）
- 健康：体重/饮食/疫苗/体检四 Tab，添加/删除记录，体重趋势柱状图
- 日记：记录日记（照片/心情/公开）、日记详情、点赞、删除
- 请求封装、JWT 存储、MinIO 图片上传工具、格式化工具

暂缓：微信一键登录（2026-11-05 决定，前置条件见 `docs_local/progress/reviews/微信原生小程序.md`）。
待完成：消息/收藏/设置、AI 情绪识别。

## 在微信开发者工具中运行

1. 启动本地后端与中间件：

   ```bash
   docker-compose up -d
   cd server && mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
   ```

2. 微信开发者工具选择「导入项目」，目录选择 `miniprogram-native/`。
   - AppID 可使用测试号，或保留 `project.config.json` 里的 `touristappid`。
   - 基础库建议 3.6.4 及以上。

3. 在「详情 → 本地设置」中勾选 **不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书**。

4. 使用本地开发账号登录：`admin / Admin12345`（见 `docs_local/progress/reviews/开发账号.md`）。

5. 环境地址由 `config/env.ts` 自动切换：
   - 开发版（`develop`）：`http://192.168.31.25:8080/api`，真机需与电脑同一 Wi-Fi；
   - 体验版/正式版（`trial` / `release`）：`https://lvhaorankk.top/api`。
   电脑 IP 变化时只需修改开发环境常量；生产域名需配置微信 request/uploadFile/downloadFile 合法域名。
   生产图片已走同域名 `https://lvhaorankk.top/files`；历史 9000 端口图片 URL 由 `utils/image.ts` 自动改写。

## 类型检查

```bash
cd miniprogram-native
npm install
npm run typecheck
```

## 目录结构

```
miniprogram-native/
├── app.json / app.ts / app.wxss      # 全局配置与样式
├── config/env.ts                     # API 地址
├── custom-tab-bar/                   # 自定义底部 TabBar
├── services/                         # user / pet / diary / community 接口
├── utils/                            # request / auth / upload / format
├── pages/
│   ├── launch/                       # 启动页
│   ├── onboarding/                   # 3 屏引导
│   ├── auth/login                    # 账号密码登录
│   ├── home/                         # 首页
│   ├── album/                        # 相册（宠物日记）
│   ├── community/                    # 宠友圈
│   ├── profile/                      # 我的
│   └── pet/                          # 宠物列表 / 编辑 / 详情
└── assets/                           # logo、Tab 图标、占位图
```

## 已知限制

- 微信一键登录：2026-11-05 决定暂缓；后端 `sys_user` 尚无 `openid/unionid`，`/api/user/wx-login` 未实现，当前先用账号密码登录。`project.config.json` 已配置 AppID，但不代表后端微信登录已接通。
- 相册分类：`pet_diary.category` 已接入；旧日记没有分类，分类 Tab 对旧数据为空。
- 社区「附近」：帖子无定位字段，后置；关注流与评论列表已接入。
- 健康管理：后端四类接口与页面已实现；体重趋势用柱状图，未做折线图和到期提醒。
- 首页/宠物详情的 AI 入口目前为占位提示。
