# 拾光笔记 · Time-captured Notes

小红书风格的图文与视频社区，个人软件工程课程项目。前后端分离，采用经典 Spring Boot 分层架构，支持图文和视频发布、内容审核、互动通知，以及“收藏→实践→反馈”闭环。

## 功能

- 账号注册、登录、个人资料和头像；普通用户、审核员、管理员三类角色。
- 用户自主选择图文（1–9张）或视频（单段MP4），草稿保存、上传预览、自动视频封面、提交审核、发布状态管理及已发布内容编辑重审。
- 分类瀑布流、关键词搜索、关注流及展示推荐理由的兴趣推荐。
- 点赞、二级评论、关注、私有收藏夹和跨夹收藏。
- 实践计划、状态时间线、体验反馈；反馈默认私有，公开需单独审核。
- 异步互动通知、审核结果通知、举报处理和操作审计。
- 创作者与平台统计、内容排行、热门话题、治理和实践完成情况。

## 技术栈

| 层次 | 技术 |
|---|---|
| 后端 | Java 21、Spring Boot、Spring Security、MyBatis-Plus |
| 前端 | Vue 3、TypeScript、Vite、Pinia、Vue Router、ECharts |
| 数据 | MySQL 8.4、Flyway、Redis |
| 媒体 | 本地文件（开发测试） / MinIO 私有桶（生产） |
| 视频 | FFmpeg / FFprobe，真实解码校验、封面生成、MP4快速起播与Range拖动播放 |
| 消息 | RabbitMQ、事务 Outbox / Inbox |
| 部署 | Docker Compose、Nginx |
| 验证 | JUnit、ArchUnit、Testcontainers、Vitest、Playwright、GitHub Actions |

后端按经典 Spring Boot 技术层分包：`controller` 接收请求，`service` 定义业务接口及应用服务，`service.impl` 实现业务逻辑，`repository` 封装持久化操作与查询投影，`mapper` 和 `entity` 放置 MyBatis-Plus Mapper 与数据库实体。账号、内容、发现、互动、收藏实践、通知、治理、统计八项业务能力保持不变，事务由业务服务协调。MySQL 保存业务事实，Redis 用于缓存与限流，RabbitMQ 处理异步通知和统计。

后端 Java 目录位于 `03-源码/backend/src/main/java/cn/shiguang/notes`：

```text
cn.shiguang.notes/
├── NotesApplication.java       Spring Boot 启动类
├── controller/                 REST 控制器、advice 全局异常处理
├── service/                    业务接口和应用服务
│   ├── application/            按业务职责拆分的应用服务、事务协调
│   ├── impl/                   XxxServiceImpl 实现
│   └── support/                幂等命令、分页等服务支持
├── repository/                 SQL 数据访问接口
│   ├── impl/                   调用 Mapper 的持久化实现
│   ├── query/                  只读聚合查询
│   └── support/                数据库访问支持
├── mapper/                     30 个 BaseMapper 与明确查询方法
├── entity/                     BaseEntity 与 30 张表对应的实体
├── dto/                        请求 DTO、校验、请求上下文
├── vo/                         明确字段的响应 VO、分页结果
├── converter/                  请求与响应转换
├── config/                     Spring 配置
├── security/                   认证、CSRF、限流
├── cache/                      Redis 缓存
├── storage/                    本地/MinIO 存储及视频处理
├── messaging/                  消息、Outbox/Inbox
├── schedule/                   定时任务
├── bootstrap/                  演示数据初始化
├── validation/                 纯业务规则校验
└── common/                     公共契约、工具、业务异常
```

全部 30 张表都有完整实体和对应 `BaseMapper<XxxEntity>`，包含字段映射、主键、时间与数值类型。生产读写经 Mapper 执行，复杂查询位于 `src/main/resources/mapper` 的 XML 中，使用绑定参数和受控动态条件；生产代码不再使用通用 JDBC 执行器。数据库迁移脚本和现有 HTTP 路径保持兼容。

控制器按业务职责拆分，经 `service/application` 中的应用服务协调业务 Service 与 Repository。请求使用具名 DTO 和 Bean Validation，响应使用明确字段的 VO，实体不直接对外返回。内部持久化投影、幂等结果快照与消息载荷仍使用受控 Map，并在 HTTP 边界转换为 VO。

事务保留行锁、显式版本比较、幂等命令及 Outbox/Inbox 机制。架构测试约束层间依赖，并禁止生产代码依赖 JDBC 执行器；实体契约测试检查所有表字段与 Mapper 泛型，真实 MySQL 集成测试验证全部实体的插入、查询和更新。

## 快速启动

本地开发使用已安装的 MySQL（`127.0.0.1:3306`、数据库 `shiguang`，本地账号密码默认 `root / root`）、Java 21、Maven、Node.js，以及 Docker 中的 Redis、RabbitMQ。MySQL 中需先创建空库 `shiguang`，表结构由后端首次启动时通过 Flyway 初始化。

```powershell
git clone https://github.com/Wangcongming666/Time-captured-Notes.git
cd Time-captured-Notes/03-源码
python scripts/init-env.py
docker compose up -d redis rabbitmq
```

在 IDE 启动 `NotesApplication`，默认启用 `dev`；前端在 `frontend` 目录执行 `npm ci`、`npm run dev`，打开 [http://localhost:5173](http://localhost:5173)。

默认 Compose 只启动 Redis、RabbitMQ，不启动数据库和前后端。需要完整容器环境时，可执行 `docker compose --profile full-stack up -d --build --wait --wait-timeout 240`：该模式使用独立 Docker 数据库（宿主机端口 `13306`，容器账号为 `.env` 对应的 `shiguang`），网页地址为 [http://localhost:8080](http://localhost:8080)。避免同时运行 IDE 后端和容器后端。

开发环境配置集中在 `03-源码/backend/src/main/resources/application-dev.yml`，包含 MySQL、Redis、RabbitMQ、媒体、MinIO、上传限制及演示账号配置，默认启用 `dev`。初始化脚本从已有 `.env` 生成此文件；重复运行保留已有凭据及手动修改。该文件含真实本地密码，不上传 GitHub。`application.yml` 只放公共配置，生产配置使用 `application-prod.yml`。

`.env` 供 Docker 创建数据库和消息服务时使用。已有 `.env` 也可以执行初始化命令，只补齐缺失的开发配置，不更换数据库密码。开发配置不打包进 JAR 或镜像，Compose 以只读文件挂载到容器，IDE 直接加载资源目录中的配置。首次镜像下载时间取决于网络。

### 演示账号

| 账号 | 角色 | 用途 |
|---|---|---|
| `demo_user` | USER | 发布、收藏、实践、个人统计 |
| `demo_reviewer` | USER + REVIEWER | 内容审核、举报与治理统计 |
| `demo_admin` | USER + ADMIN | 账号角色、话题与平台统计 |

三个账号的初始密码均取自本机 `03-源码/.env` 中的 `DEMO_PASSWORD`，由初始化脚本随机生成。不要提交 `.env`。管理员不自动拥有审核员权限。

Compose 显式启用演示数据，笔记均标注为合成演示内容。图片来源及许可见[素材声明](03-源码/backend/src/main/resources/demo/ATTRIBUTION.txt)。

### 常用命令

在 `03-源码` 目录执行：

```powershell
docker compose ps
docker compose logs --tail 100 app
docker compose stop
docker compose up -d --wait
```

开发连接端口：本地 MySQL `3306`、Docker Redis `6379`、RabbitMQ `5672`、RabbitMQ 管理界面 `15672`，Docker 端口仅绑定本机。可选全容器模式的数据库、消息及媒体使用具名卷持久化；`docker compose --profile full-stack down` 保留数据卷，`down -v` 会清空数据卷。本地数据库与 Docker 数据库各自独立，配置切换不会自动迁移数据。

## 图片与视频存储：测试本地，上线 MinIO

默认 `local` 模式，上传文件保存在后端的 `MEDIA_ROOT`。使用 Compose 时对应容器 `/data/media` 和具名卷 `shiguang_media_data`，现有本地图片可以继续使用。

生产环境启用 `prod` 配置会**强制使用 MinIO**；缺少凭据、桶不可访问或允许匿名访问时启动失败，不会回退写本地文件。图片和视频仍通过后端 `/api/v1/media/{id}/content` 鉴权后流式读取，前端不保存 MinIO 密钥，也不使用可绕过实时权限的永久图片直链。

先在已有 MinIO 服务创建私有桶，为应用配置对应桶的检查、策略读取、对象读写和删除权限。将以下配置补入本机 `03-源码/.env`，填写自己的实际值：

```dotenv
MINIO_ENDPOINT=https://minio.example.com
MINIO_ACCESS_KEY=your-access-key
MINIO_SECRET_KEY=your-secret-key
MINIO_BUCKET=shiguang-media
```

在 `03-源码` 目录启动 MinIO 存储模式：

```powershell
docker compose --profile full-stack -f docker-compose.yml -f docker-compose.minio.yml up -d --build --wait --wait-timeout 240
```

覆盖配置启用 `prod` 并关闭演示数据；桶须提前创建，默认不允许应用自动建桶。该文件连接现有 MinIO，不负责创建线上 MinIO 服务器或配置公网 HTTPS。

直接部署 Java 服务时，设置 `SPRING_PROFILES_ACTIVE=prod` 和上述四个 MinIO 环境变量即可。视频封面、正常读取、上传失败后的回滚补偿、孤立媒体清理都使用同一存储实现；MySQL只保存对象key及媒体元数据。

切换配置不会自动搬迁已有本地图片。若保留原数据库，需要先把媒体文件按数据库中原有的 `storage_key` 和视频的 `poster_storage_key` 复制到目标私有桶；新生产环境可使用独立数据库和桶。数据库与对象存储应配套备份。数据库提交结果不确定时会保留对象，避免误删；若回滚补偿删除本身失败，会记录对象key，当前需人工核查清理。

## 视频发布

发布页由用户选择“上传图文”或“上传视频”。视频使用 MP4 容器、H.264 画面和可选 AAC 音频；不兼容的编码会给出错误提示。服务端用 FFprobe 校验真实格式和时长，FFmpeg 整理 MP4 以便快速起播、清除容器元数据并生成 JPEG 封面。视频和封面共用同一媒体权限，审核前仅作者及有权限的审核员可见；修改已发布内容后重新审核。播放接口支持 HEAD、单段 Range 和拖动定位。

默认视频上限为 100MB、5分钟，图片仍为每张 10MB。这是可配置的工程默认值。可在 `.env` 设置 `VIDEO_MAX_BYTES`、`VIDEO_MAX_SECONDS`；提高大小时需同时调整 `UPLOAD_MAX_FILE_SIZE`、`UPLOAD_MAX_REQUEST_SIZE` 和 `NGINX_CLIENT_MAX_BODY_SIZE`，请求上限应略大于文件上限以容纳 multipart 边界。重新执行 `docker compose up -d --build --wait` 后生效。直接运行 Java 时可用 `FFMPEG_PATH`、`FFPROBE_PATH` 指定工具路径。

当前同步处理一段视频，处理繁忙时提示重试；暂不提供视频剪辑、多码率转码或混合图文视频。素材默认落在同一个本地媒体卷，生产环境的原视频与封面均写入 MinIO 私有桶。

## 本地开发与测试

需要 Java 21、Maven 3.9+、Node.js 22.12+，以及 PATH 中可用的 FFmpeg / FFprobe（含 H.264 编码器，用于生成测试夹具）。Docker 镜像和 CI 会安装 FFmpeg。后端集成测试使用真实临时 MySQL、Redis、RabbitMQ 和 MinIO 容器，分别验证本地与 MinIO 存储，必须保持 Docker 可用。MinIO 测试服务从官方固定源码版本构建，首次运行还需要下载 Go 工具链镜像及模块依赖；先构建下面的测试镜像。

从仓库根目录执行后端验证：

```powershell
docker build -t shiguang-minio-test:2025-10-15 -f 03-源码/backend/src/test/resources/minio/Dockerfile 03-源码/backend/src/test/resources/minio
mvn -B -ntp -f 03-源码/backend/pom.xml verify
```

前端开发与验证：

```powershell
cd 03-源码/frontend
npm ci
npm test
npm run build
npm run dev
```

Vite 开发服务器默认在 `127.0.0.1:5173`，API 代理到已启动的 Compose 服务 `localhost:8080`。

浏览器验收需要先启动完整 Compose 服务，并安装 Chromium：

```powershell
npx playwright install chromium
npm run test:e2e
```

JaCoCo 报告输出到 `03-源码/backend/target/site/jacoco/index.html`。Windows 中文路径若出现 agent 输出路径乱码，可通过 `-Djacoco.data.path=<本机ASCII绝对路径>/coverage.exec` 指定输出文件。

[GitHub Actions](https://github.com/Wangcongming666/Time-captured-Notes/actions) 执行后端单元与架构测试、真实容器集成测试、前端测试及构建、全栈启动和桌面/手机浏览器验收，失败时保存测试产物。

## 目录与 SQL

```text
.github/workflows/ci.yml       自动验证
03-源码/
├── backend/                  Java 后端、迁移脚本与测试
├── frontend/                 Vue 前端与测试
├── sql/                      可独立查看的数据库 SQL
├── scripts/                  环境初始化与数据库备份工具
├── docker-compose.yml        本地服务编排
└── .env.example              环境变量模板
```

[01-schema.sql](03-源码/sql/01-schema.sql) 包含30张业务表；[02-roles-and-search.sql](03-源码/sql/02-roles-and-search.sql) 初始化角色并补充搜索索引。[03-video-media.sql](03-源码/sql/03-video-media.sql) 增加视频类型、时长与封面元数据。它们对应后端 Flyway V1、V2、V3；正常启动采用自动迁移，不要在已有数据库中重复执行建表脚本。

从仓库根目录运行 `python 03-源码/scripts/backup-db.py` 可生成数据库快照，保存在本地 `.runtime/backups`。恢复工具仅接受新的 `shiguang_restore_*` 验证库，不覆盖现有库。媒体卷需要另外备份。

## 当前边界

当前默认提供本地部署，并提供生产 MinIO 存储配置，未配置公网域名和 TLS。Redis 推荐快照失效时需刷新列表；正式规模压测、完整媒体恢复演练以及长期故障恢复指标仍需进一步验证。

本仓库上传前后端源码、SQL、必要运行配置和本 README；课程需求、架构设计、Skill 与验收报告保留在本地。
