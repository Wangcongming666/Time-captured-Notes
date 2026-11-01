# 拾光笔记开发与架构文档

2026-11-01 · V1.0 · 个人项目 · 设计交付版

用户已确认八模块、三角色、两项创新、Vue3、模块化单体与分层、RabbitMQ异步、本地Docker Compose。本目录完成开发设计；业务代码、部署和实测证据尚未产生。

| 文件 | 用途 |
|---|---|
| 架构设计说明书.pdf | 阅读版，含五视图正文、全量数据字典及12条ADR，目录与页码 |
| 架构设计说明书.md | 主文档可编辑源，含设计依据、边界、五视图、追溯和文献 |
| 数据字典.md | 30张表、逐字段类型/长度/约束/默认/业务含义 |
| 接口设计说明.md | 75个接口索引、权限/错误/幂等/分页约定 |
| 开发实施计划.md | T01–T12实施切片、文件、接口、测试和验收命令；未执行 |
| 课程交付核对表.md | L/D/V/R/P条目到设计位置与执行证据边界 |
| adr/ | ADR-001–012完整决策、权衡和验证条件 |
| images/ | 19幅图的SVG和Mermaid，框图另有DOT/JSON；完整30实体ER与12类领域模型 |
| attachments/schema.sql | 与数据字典同源的DDL设计，未执行 |
| attachments/openapi.yaml | OpenAPI3.0.3契约，75操作、请求/响应结构与鉴权 |
| attachments/compose.design.yaml | 本地设施与未来应用部署设计；不能在当前目录直接启动业务 |
| attachments/validation-report.json | 设计资产结构核对结果，不能替代运行测试 |
| 设计输入与技术核验.md | 用户确认来源、官方资料与设计风险记录 |

SQL、OpenAPI和Compose最终按课程目录迁入03-源码；现在留在设计附件，避免把不存在的业务程序当作可运行交付。正式提交根目录仍须采用“学号_姓名_项目名称”，由本人补充学号姓名后组装；当前工作区保留原始任务书与需求稿。

当前尚缺的是执行事实：真实代码、Git历史、三项Skill和真实使用记录、部署账号、P9访问证据、性能/恢复报告、截图、口播视频、答辩材料、AI修正证据。它们已被开发计划分配任务，不在本轮伪造。

教师是否批准选题、实际截止、学号姓名仍未提供，不阻断本轮设计，但需本人在申报和提交时补充。技术补丁和镜像digest须在T01依赖解析时最终锁定；未声称构建兼容或无漏洞。

## 文档再生成

工具仅用于文档，不是业务工程。`tools/build_design.py`生成字典、SQL和API；`tools/build_support.py`调用它并生成图模型、ADR、计划和Compose；`tools/render_graphs.cjs`将图模型用Graphviz排版；`tools/render_pdf.py`导出PDF；`tools/validate_design.py`核对资产。

顺序：Python build_support → Node render_graphs → Python render_pdf → Python validate_design。工具引用本机已存在的字体与Graphviz包路径，跨机器运行需修改这些文档工具路径；它们不影响未来业务README的10分钟启动目标。主说明书为人工设计源，不由工具覆盖；自动附件如修改，需同步生成模型以避免再生成丢失。

新版Node曾因本机随机数初始化错误无法启动，现用已有Node16执行文档工具，PDF已改用嵌入微软雅黑字体；项目业务前端Node基线仍为24.22.2，不受此文档工具变通影响。
