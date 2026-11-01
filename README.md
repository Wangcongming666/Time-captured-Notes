# 拾光笔记 · Time-captured Notes

小红书风格的图文社区，个人软件工程课程项目。前后端分离，采用模块化单体架构，支持图文发布、内容审核、互动通知，以及“收藏→实践→反馈”闭环。

## 功能

- 账号注册、登录、个人资料和头像；普通用户、审核员、管理员三类角色。
- 图文草稿、图片上传、提交审核、发布状态管理及已发布内容编辑重审。
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
| 图片 | 本地文件（开发测试） / MinIO 私有桶（生产） |
| 消息 | RabbitMQ、事务 Outbox / Inbox |
| 部署 | Docker Compose、Nginx |
| 验证 | JUnit、ArchUnit、Testcontainers、Vitest、Playwright、GitHub Actions |

后端分为 identity、content、discovery、social、collection、notification、governance、statistics 八个业务模块；跨模块事务由 workflows 编排。MySQL 保存业务事实，Redis 用于缓存与限流，RabbitMQ 处理异步通知和统计。

## 快速启动

需要 Git、Python 3、Docker Desktop（或 Docker Engine 与 Compose 插件）。先启动 Docker，建议分配至少 6GB 内存并预留 10GB 以上磁盘；首次构建需要下载镜像、Maven 和 npm 依赖。

```powershell
git clone https://github.com/Wangcongming666/Time-captured-Notes.git
cd Time-captured-Notes/03-源码
python scripts/init-env.py
docker compose up -d --build --wait --wait-timeout 240
```

打开 [http://localhost:8080](http://localhost:8080)。数据库由 Flyway 自动初始化，无需手工执行 SQL。

如果本机已有 `.env`，直接启动 Compose，保留原文件。初始化脚本拒绝覆盖已有配置，避免随机密码变化导致已有数据库无法连接。首次镜像下载时间取决于网络。

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

默认仅发布 `127.0.0.1:8080`。MySQL、Redis、RabbitMQ和后端管理端口通过 Docker 网络访问。数据库、消息及媒体使用具名卷持久化；`docker compose down` 保留数据卷，`down -v` 会清空数据卷。

## 图片存储：测试本地，上线 MinIO

默认 `local` 模式，上传文件保存在后端的 `MEDIA_ROOT`。使用 Compose 时对应容器 `/data/media` 和具名卷 `shiguang_media_data`，现有本地图片可以继续使用。

生产环境启用 `prod` 配置会**强制使用 MinIO**；缺少凭据、桶不可访问或允许匿名访问时启动失败，不会回退写本地文件。图片仍通过后端 `/api/v1/media/{id}/content` 鉴权后流式读取，前端不保存 MinIO 密钥，也不使用可绕过实时权限的永久图片直链。

先在已有 MinIO 服务创建私有桶，为应用配置对应桶的检查、策略读取、对象读写和删除权限。将以下配置补入本机 `03-源码/.env`，填写自己的实际值：

```dotenv
MINIO_ENDPOINT=https://minio.example.com
MINIO_ACCESS_KEY=your-access-key
MINIO_SECRET_KEY=your-secret-key
MINIO_BUCKET=shiguang-media
```

在 `03-源码` 目录启动 MinIO 存储模式：

```powershell
docker compose -f docker-compose.yml -f docker-compose.minio.yml up -d --build --wait --wait-timeout 240
```

覆盖配置启用 `prod` 并关闭演示数据；桶须提前创建，默认不允许应用自动建桶。该文件连接现有 MinIO，不负责创建线上 MinIO 服务器或配置公网 HTTPS。

直接部署 Java 服务时，设置 `SPRING_PROFILES_ACTIVE=prod` 和上述四个 MinIO 环境变量即可。正常读取、上传失败后的回滚补偿、孤立图片清理都使用同一存储实现；MySQL只保存对象key及图片元数据。

切换配置不会自动搬迁已有本地图片。若保留原数据库，需要先把媒体文件按数据库中原有的 `storage_key` 复制到目标私有桶；新生产环境可使用独立数据库和桶。数据库与对象存储应配套备份。数据库提交结果不确定时会保留对象，避免误删；若回滚补偿删除本身失败，会记录对象key，当前需人工核查清理。

## 本地开发与测试

需要 Java 21、Maven 3.9+、Node.js 22.12+。后端集成测试使用真实临时 MySQL、Redis、RabbitMQ 和 MinIO 容器，分别验证本地与 MinIO 存储，必须保持 Docker 可用。MinIO 测试服务从官方固定源码版本构建，首次运行还需要下载 Go 工具链镜像及模块依赖；先构建下面的测试镜像。

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

[01-schema.sql](03-源码/sql/01-schema.sql) 包含30张业务表；[02-roles-and-search.sql](03-源码/sql/02-roles-and-search.sql) 初始化角色并补充搜索索引。它们对应后端 Flyway V1、V2；正常启动采用自动迁移，不要在已有数据库中重复执行建表脚本。

从仓库根目录运行 `python 03-源码/scripts/backup-db.py` 可生成数据库快照，保存在本地 `.runtime/backups`。恢复工具仅接受新的 `shiguang_restore_*` 验证库，不覆盖现有库。媒体卷需要另外备份。

## 当前边界

当前默认提供本地部署，并提供生产 MinIO 存储配置，未配置公网域名和 TLS。Redis 推荐快照失效时需刷新列表；正式规模压测、完整媒体恢复演练以及长期故障恢复指标仍需进一步验证。

本仓库上传前后端源码、SQL、必要运行配置和本 README；课程需求、架构设计、Skill 与验收报告保留在本地。
