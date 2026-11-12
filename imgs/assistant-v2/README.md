# 助手页面 V2 素材

依据用户第二张参考图生成。icon-atlas.png 为9格透明图标原稿；paw/bowl/shower/care/heart/pill/shield/bug/comb.png 为拆分后的独立图标。hero-source.png 为猫咪插画原稿，hero.webp 为页面使用的压缩版本。

10个页面素材已上传本地 MinIO 的 `itsworld-static/miniapp-v1/assistant-v2/`，并逐个执行匿名GET与SHA256对比。对象列表与校验值见 manifest.json。源码通过现有 assetUrl + resolveImage 加载；不将大图放进小程序包。

复用上传：`python tools/upload_assistant_assets.py`。凭证仅从环境或被忽略的根目录 .env 读取，不写入清单或脚本。默认本机9000端口，可通过 ASSISTANT_ASSET_ENDPOINT 指定其他已授权目标。

页面保留原生导航，欢迎区为猫咪+六个问题气泡，下方为日常养护与健康问题两列共六个问题；12处点击均直接发送，发送中禁用，未有会话时先创建。普通输入同样支持发送。保留最近会话、历史、商品卡片、失败重试与隐私提示。没有增加语音能力，输入提示使用“点击输入”。

验证：typecheck、test:assistant（3项，其中1项逐个覆盖全部12个问题与忙碌状态）通过。视觉与键盘/安全区仍需微信开发者工具及真机截图确认；未操控用户电脑。若页面提示加载失败，须另查本地后端是否已启动并执行助手迁移，与静态素材是否加载是不同环节。
