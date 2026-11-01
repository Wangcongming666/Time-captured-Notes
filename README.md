# 拾光笔记 · Time-captured Notes

小红书图文社区的个人软件工程课程实现。技术栈为Java21、Spring Boot、MyBatis-Plus、MySQL、Vue3、Redis、RabbitMQ。采用八模块单体、分层依赖和事务Outbox/Inbox；创新功能为可解释推荐及“收藏→实践→反馈”闭环。

## 本地运行

前提：Docker Desktop已启动，建议为Docker分配至少6GB内存并预留10GB以上磁盘；首次构建需要访问Maven、npm和镜像仓库。已验证环境为Windows Docker Desktop，Docker可用内存约7.7GB。

```powershell
# 新克隆仓库时：
cd 03-源码
python scripts/init-env.py
# 已有.env时保留原文件，不重新生成数据库密码。
docker compose up -d --build --wait --wait-timeout 240
```

打开 [http://localhost:8080](http://localhost:8080)。当前工作区已经生成.env并启动服务，无需再次初始化。

| 账号 | 角色 | 用途 |
|---|---|---|
| demo_user | USER | 发布、收藏、实践、通知、个人统计 |
| demo_reviewer | USER + REVIEWER | 内容审核、举报、治理统计 |
| demo_admin | USER + ADMIN | 账号角色、话题、审计、平台统计 |

三个演示账号的初始密码均为本机`03-源码/.env`中的DEMO_PASSWORD。密码随机生成，不在源码或聊天中公布。管理员不自动拥有审核员权限。演示笔记均明确标注“合成演示内容”，图片来源与许可见[素材声明](03-源码/backend/src/main/resources/demo/ATTRIBUTION.md)。

```powershell
docker compose ps
docker compose logs --tail 100 app
docker compose stop
# 再次启动，不删除数据卷：
docker compose up -d --wait
```

仅Nginx发布127.0.0.1:8080。MySQL、Redis、RabbitMQ及后端管理端口只在Docker网络内访问。默认使用具名卷保存数据库、RabbitMQ及媒体。`docker compose down`保留卷；不要随意使用会清空卷的`down -v`。

若依赖下载受网络限制，可在本机明确配置Docker构建代理。后端Dockerfile支持可选MAVEN_OPTS构建参数；不得把本地代理地址或认证信息硬编码进仓库。首次镜像拉取耗时受网络影响，未承诺所有网络条件下均能10分钟完成。

## 开发与验证

宿主机开发需要Java21、Maven3.9+、Node22.12+和Python3。全量后端测试必须有Docker，使用真实临时MySQL、Redis、RabbitMQ，不能跳过后宣称通过。

```powershell
# 从仓库根目录运行：
mvn -B -ntp -f 03-源码/backend/pom.xml verify
python -m pip install -r 03-源码/scripts/requirements.txt
python 02-开发Skill/module-generator/scripts/module.py --check
python 02-开发Skill/arch-doc-sync/scripts/sync.py
python 03-源码/scripts/check-http-contract.py

cd 03-源码/frontend
npm ci
npm test
npm run build
npx playwright install chromium
npm run test:e2e
# Docker服务运行时，本地Vite通过8080代理API：
npm run dev
```

JaCoCo报告位于`03-源码/backend/target/site/jacoco/index.html`。Windows中文路径若导致Java agent输出路径乱码，可额外指定一个本机ASCII路径，例如`-Djacoco.data.path=E:/xiaohongshu/.runtime/coverage.exec`；这是本机环境问题的可选覆盖，不是提交的硬编码路径。

GitHub Actions配置见[CI工作流](.github/workflows/ci.yml)，包含架构扫描、真实容器测试、前端测试、构建、全栈启动、HTTP契约和桌面/手机浏览器验收。远程执行结果以GitHub Actions实际状态为准。

## 数据与文档

- [数据库SQL及执行顺序](03-源码/sql/README.md)：30张业务表，另有Flyway历史表；默认自动迁移。
- [实施OpenAPI契约](03-源码/docs/api/openapi.yaml)：77项HTTP操作；设计基线保留在架构目录。
- [架构设计与附图](01-架构设计文档/README.md)、[实施差异说明](01-架构设计文档/实施差异说明.md)。
- [三个项目Skill及索引](02-开发Skill/README.md)、[真实使用记录](02-开发Skill/usage-log.md)。
- [验证报告](docs/验证报告.md)、[执行记录](docs/执行记录.md)、[功能追溯](docs/需求实现追溯.md)。

数据库备份：`python 03-源码/scripts/backup-db.py`。快照保存在ignored的`.runtime/backups`；恢复脚本仅允许新建`shiguang_restore_*`验证库，拒绝覆盖现有库。媒体卷需在暂停写入时另行备份，完整RPO/RTO和媒体恢复不能仅凭数据库恢复演练宣称达标。

## 提交边界

源代码和文档按真实功能阶段提交main并推送GitHub，不伪造提交时间。课程要求的六周提交跨度需持续开发自然积累；正式封面姓名学号、教师选题批准、最终截止、本人Skill反思及现场答辩材料仍须本人确认。性能目标、故障恢复时限与完整媒体备份恢复需另行取得对应实测证据，不能用功能测试冒充。
