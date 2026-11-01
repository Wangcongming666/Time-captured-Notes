# 附录 2：开发用 AI Skill 编写指南

> 本附录配合任务书第五章使用，供学生上手。**示例项目为虚构的"校园圈"内容社区 App**（Spring Boot 3 + Vue 3 + MySQL + Redis + RabbitMQ），你需替换为自己的项目。

---

## 1. 什么是 Skill

Skill 是一个**给 AI 编程助手看的项目规范文档**。当你向 AI 说"帮我加一个评论功能"时，通用 AI 只知道通用的最佳实践，不知道你的项目：

- 你的分层是 `controller / application / domain / infrastructure` 还是 `controller / service / dao`？
- 事务注解该加在哪一层？
- 统一响应包装类叫什么名字？
- 哪些依赖方向是被禁止的？

Skill 把这些约定写死，AI 在生成代码前先加载它，产出的代码才不会破坏你的架构。**这也是本作业考察的重点：你能不能把自己的架构约束，表达成 AI 能执行的规则。**

### 工作原理（一句话）

AI 工具读取你的 `description` 判断"这个任务要不要用这个 Skill"，命中后加载 `SKILL.md` 正文，按其中的步骤执行。

> **因此 `description` 是本 Skill 最关键的一行。** 它必须写清"什么时候用"，包含用户可能说出的关键词。

---

## 2. 目录结构与规范

```
02-开发Skill/
├── README.md                   # 清单与索引
├── project-scaffold/
│   ├── SKILL.md
│   └── scripts/
│       └── init-module.sh
├── module-generator/
│   ├── SKILL.md
│   ├── templates/
│   │   ├── Controller.java.tpl
│   │   ├── Service.java.tpl
│   │   └── Repository.java.tpl
│   └── scripts/
│       └── check-layering.py   # 校验生成代码是否违反分层依赖
├── arch-doc-sync/
│   ├── SKILL.md
│   └── scripts/
│       └── scan-structure.py   # 扫描源码目录，输出模块清单供文档使用
├── usage-log.md                # 使用记录（≥5 次，要有反思）
└── reflection.md               # （加分）Skill 迭代反思
```

### frontmatter 规范

```yaml
---
name: module-generator          # 小写字母 + 连字符，全局唯一
description: 在校园圈项目中按四层架构生成新的业务模块。当用户要求"新增/创建/添加 XX 功能、模块、接口"，
  或需要生成 Controller、Service、Repository、DTO、数据库迁移脚本时使用。生成代码严格遵守
  controller→application→domain→infrastructure 单向依赖约束，统一返回 Result<T>。
---
```

| 字段 | 要求 |
|------|------|
| `name` | 小写 + 连字符，≤64 字符，与目录名一致 |
| `description` | **必填且最关键**。≤1024 字符。写清：适用项目 + 触发场景关键词 + 产出物 + 关键约束 |

### SKILL.md 正文七要素（缺一项扣 0.5 分）

| 序号 | 要素 | 说明 |
|------|------|------|
| 1 | 适用场景 | 什么情况下该调用它 |
| 2 | 前置条件 | 调用前必须确认的信息 |
| 3 | 执行步骤 | 有序、可执行的步骤，不写"合理设计一下"这类空话 |
| 4 | 强制约束 | **本项目特有**的硬性规则，写死，不留 AI 发挥空间 |
| 5 | 输出物清单 | 明确会生成哪些文件 |
| 6 | 自检清单 | 生成后必须核对什么（最好能被脚本验证） |
| 7 | 常见错误与反例 | 把 AI 常犯的错写出来，附错误代码对比 |

---

## 3. 三个必需 Skill 的设计要点

| Skill | 要解决什么问题 | 关键内容 |
|-------|--------------|---------|
| `project-scaffold` | 新同学/新环境能一键还原项目结构与规范 | 目录初始化、依赖清单、配置文件模板、规范检查脚本 |
| `module-generator` | 新增业务模块时代码风格统一、不破坏分层 | 四层代码模板、命名规范、依赖方向校验脚本、迁移脚本命名 |
| `arch-doc-sync` | 代码改了文档没改 → 文档与实现脱节 | 扫描源码输出模块清单/接口清单，回填到设计文档对应章节 |

加分 Skill：`test-and-quality`（生成单测与覆盖率检查）、`deploy`（构建镜像与部署脚本生成）。

---

## 4. 完整示例：`module-generator`

以下是**合格样例**，你可以参照结构改写（内容必须换成你自己的项目约定）。

```markdown
---
name: campus-module-generator
description: 在"校园圈"内容社区项目中按四层架构生成新的业务模块。当用户要求"新增/创建/添加 XX
  功能、模块、接口"，或需要生成 Controller、Service、Repository、DTO、数据库迁移脚本时使用。
  生成代码严格遵守 controller→application→domain→infrastructure 单向依赖，统一返回 Result<T>。
---

# 校园圈 · 业务模块生成器

## 1. 适用场景
- 新增一个业务模块（如"话题""私信""收藏夹"）
- 为已有模块补充接口
- 生成对应的数据库迁移脚本与单元测试

## 2. 前置条件
生成前必须确认（缺少则先向用户询问，不得臆造）：
1. 模块名（大驼峰，如 `Topic`）、所属上下文
2. 涉及的领域实体与关键字段
3. 涉及的接口清单（方法、路径、请求/响应字段）
4. 是否涉及状态流转（若涉及，需先确认状态枚举）

## 3. 执行步骤
1. 在 `docs/api/openapi.yaml` 中登记接口定义（先写契约，后写代码）
2. 按 `templates/` 生成四层代码骨架
3. 逐层填写：
   - `XxxController`：仅做参数校验与路由，**不写业务逻辑**
   - `XxxApplicationService`：用例编排，事务边界标注在此
   - `Xxx`（领域实体/领域服务）：纯业务规则，不依赖框架
   - `XxxRepository`：持久化实现，接口定义在 domain 层，实现在 infrastructure 层
4. 生成 Flyway 迁移脚本，命名 `V{版本}__{描述}.sql`
5. 运行 `python scripts/check-layering.py` 校验依赖方向
6. 生成单元测试（正常流程 + 边界 + 异常，≥3 个用例）
7. 更新 `docs/api/openapi.yaml` 的实际实现说明

## 4. 本项目强制约束（违反即为错误）
- **禁止** Controller 直接调用 Repository，必须经过 ApplicationService
- **禁止** domain 层出现 Spring、MyBatis、Servlet 等框架注解（`import org.springframework.*` 一律不允许）
- **禁止** infrastructure 层反向依赖 application 层
- 所有对外接口统一返回 `Result<T>`，业务异常抛 `BizException` 由 `GlobalExceptionHandler` 转换
- `@Transactional` 只能标注在 `application` 层 public 方法上
- 日志使用 `@Slf4j`，禁止 `System.out.println`，禁止打印密码/手机号
- 金额用 `BigDecimal`，禁止 `double`；时间用 `LocalDateTime`，禁止 `Date`
- 所有查询必须分页，禁止无条件全表查询

## 5. 输出物
- `XxxController.java`、`XxxApplicationService.java`、`Xxx.java`、`XxxRepository.java`（+Impl）
- `XxxRequest.java` / `XxxResponse.java` DTO
- `V{版本}__create_xxx.sql`
- `XxxApplicationServiceTest.java`
- OpenAPI 增量片段

## 6. 自检清单
- [ ] 依赖方向无反向引用（已跑 `check-layering.py` 通过）
- [ ] domain 层无框架 import
- [ ] 每个 public 方法有 Javadoc
- [ ] 无硬编码密钥、无魔法值（常量已提取）
- [ ] `mvn verify` 通过，单测覆盖率未下降

## 7. 常见错误与反例

**错误 1：Controller 里写业务逻辑**
```java
// 错误
@PostMapping("/topic")
public Result<Long> create(@RequestBody TopicReq req) {
    if (req.getTitle().length() > 50) throw new BizException("标题过长");
    Topic t = topicRepository.save(convert(req));   // 直接调 Repository
    return Result.ok(t.getId());
}
```
```java
// 正确
@PostMapping("/topic")
public Result<Long> create(@Valid @RequestBody TopicReq req) {
    return Result.ok(topicApplicationService.create(req));
}
```
业务规则应下沉到领域层或应用层，Controller 只负责参数校验与路由。

**错误 2：事务边界放在 Controller 或 Repository**
事务必须与应用层用例对齐，否则长事务会拖垮连接池。

**错误 3：domain 层引入框架**
领域实体一旦依赖 Spring，就无法脱离容器做单元测试，也破坏了分层意义。
```

---

## 5. 可执行资产：`check-layering.py`

**纯文字描述的 Skill 最高只能得 4 分**。至少要有一个脚本或模板。以下是分层校验脚本示例（Python，无第三方依赖）：

```python
# -*- coding: utf-8 -*-
"""校验四层架构依赖方向：controller -> application -> domain <- infrastructure"""
import os, re, sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else 'backend/src/main/java'
ALLOWED = {
    'controller':     {'application', 'domain', 'common'},
    'application':    {'domain', 'common'},
    'domain':         {'common'},
    'infrastructure': {'domain', 'common'},
}
FORBIDDEN_IMPORT = {
    'domain': ['org.springframework', 'com.baomidou', 'javax.servlet', 'org.apache.ibatis'],
}

errors, checked = [], 0
for base, _, files in os.walk(ROOT):
    layer = os.path.basename(base)
    if layer not in ALLOWED:
        continue
    for f in files:
        if not f.endswith('.java'):
            continue
        checked += 1
        src = open(os.path.join(base, f), encoding='utf-8').read()
        for imp in re.findall(r'import\s+[\w.]+\.(\w+)\.[\w.]+;', src):
            if imp not in ALLOWED[layer]:
                errors.append(f'{f}: 层 {layer} 不允许依赖 {imp}')
        for bad in FORBIDDEN_IMPORT.get(layer, []):
            if bad in src:
                errors.append(f'{f}: 层 {layer} 禁止引入 {bad}')

print(f'已检查 {checked} 个文件')
if errors:
    print('❌ 架构约束校验未通过：')
    for e in errors:
        print('  -', e)
    sys.exit(1)
print('✅ 架构约束校验通过')
```

> 把它挂到 CI 或 Git pre-commit 上，就是一条**可执行的架构约束**——这比在文档里写"请注意分层"有力得多，也是本作业的加分点。

---

## 6. 使用记录模板（`usage-log.md`）

**评分看的是反思，不是流水账。** 每次记录必须包含"AI 生成了什么 / 我改了什么 / 为什么"。

```markdown
## 2026-04-12　新增"话题"模块

- **场景**：需要新增话题模块（Controller + Service + Repository + 迁移脚本）
- **使用的 Skill**：campus-module-generator
- **AI 初次产出**：生成了 TopicController，但把标题长度校验写在了 Controller 里，
  且 Repository 接口直接定义在 infrastructure 层
- **我的修正**：
  1. 把校验规则下沉到 `Topic` 领域实体的构造方法中
  2. Repository 接口上移到 domain 层，infrastructure 只保留实现
- **为什么**：Controller 承担业务规则会导致该规则无法在批量导入场景复用；
  Repository 接口留在基础设施层会让 domain 依赖具体实现
- **对 Skill 的改进**：在"强制约束"中补了第 3 条，并在"常见错误"里加了这个反例
```

| 日期 | 场景 | 使用的 Skill | AI 产出的问题 | 我的修正 | Skill 是否改进 |
|------|------|------------|-------------|---------|--------------|

---

## 7. 评分对照表（8 分）

| 项 | 分值 | 自评 | 检查方式 |
|----|------|------|---------|
| 数量与规范完整性 | 2 | | ≥3 个 Skill；frontmatter 完整；正文七要素齐全 |
| 与架构约束的一致性 | 2 | | Skill 内规范 = 开发架构视图 V2/V7，逐条比对 |
| 可执行资产质量 | 2 | | 脚本可运行 / 模板可直接用；有自检或校验能力 |
| 实效与反思 | 2 | | usage-log ≥5 次；体现"发现问题→改进 Skill"闭环 |

**常见失分**：

| 失分情形 | 后果 |
|---------|------|
| Skill 内容全是通用规范（如"请遵循 SOLID 原则"），与本项目无关 | 一致性项 0 分 |
| 只有 SKILL.md，无任何 scripts/templates | 可执行资产项 0 分 |
| usage-log 只有"用了，很好用" | 实效项 0 分 |
| Skill 里写的目录结构与实际代码结构不一致 | 每处 −1 分 |
| `description` 没写触发场景，AI 根本不会调用 | 规范项 −1 分 |

---

## 8. 一个判断标准

> **如果你的 Skill 拿给同班另一个组的同学，用在他们项目上会生成错误的代码——说明你的 Skill 写对了。**
> 因为它足够具体地描述了"你这个项目"的约束。反之，如果它放之四海皆准，说明你没写出架构约束。
