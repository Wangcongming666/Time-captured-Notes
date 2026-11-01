# 真实使用记录

以下均为2026-11-01本次开发中的真实脚本执行。记录中的“修正”由AI执行；**本人尚未逐条审核，不能冒充学生本人修正或反思**。工具失败同样如实保留。

| 次数 | 场景及输入 | 初产物/实际结果 | 修正与理由 | Skill改进 |
|---|---|---|---|---|
| 1 | project-scaffold，`scaffold.py --check` 检查现有工程 | 8模块、环境模板存在，missing=[] | 保留既有代码，只读检查，避免重建覆盖 | 明确检查和生成是两个模式 |
| 2 | project-scaffold，`--output .runtime/scaffold-smoke`，随后对生成目录--check | 实际生成32个分层目录和无密钥环境模板；检查通过 | 输出到ignored临时目录，未污染真实业务模块 | 模板只放空值，不复制本地.env |
| 3 | project-scaffold，对同一输出目录再次生成 | 明确拒绝，Refusing to overwrite | 不删除已有输出强行重试；检查拒绝覆盖保护有效 | 文档强调生成器拒绝已有目录 |
| 4 | module-generator，`--module content --output .runtime/content-template` | 生成Repository参考模板和四层目录 | 参考模板不直接注入真实content，不冒充完整业务实现 | 模板采用ownerId查询边界，不暴露BaseMapper |
| 5 | module-generator，`--check` 扫描真实源码 | 首轮70个Java文件errors=[]，维护任务加入后71个文件errors=[] | 补充零文件输入必须失败，避免空扫描假通过 | 增加web直连Database、domain框架依赖、跨模块实现依赖三类检查 |
| 6 | arch-doc-sync，扫描真实HTTP路由与设计复制件 | 实现77、契约75，覆盖97.40%；缺GET admin/users和admin/topics | 增加两个管理查询契约，分开AdminUser与私人资料DTO；复查77/77，无幽灵接口 | 将源码契约确认为实施权威，保留设计版本并写差异说明 |
| 7 | 按arch-doc-sync运行真实HTTP响应检查 | 初次34/35；测试工具错用GET /reports，暴露不支持方法被映射500 | 工具改为GET /admin/reports；后端补405/404映射；35/35真实样本通过 | 强调路由覆盖不能代表响应体覆盖，保留两份报告 |

| 8 | 完成前检查OpenAPI安全方案 | 两个新增操作误写cookieAuth，实际定义名为sessionCookie | 扫描器先复现2条undefinedSecurity，再改正安全方案名，补引用和operationId唯一性检查 | 路由、schema引用和安全方案必须同时校验 |

Skill认知压力测试的独立受测者未能读取工作区，工具报setup refresh错误。该次**不能作为有效认知基线或Skill遵循成功证据**。上述五次以上记录证明的是实际脚本使用和迭代；后续仍须学生本人使用、审核并补充反思。
