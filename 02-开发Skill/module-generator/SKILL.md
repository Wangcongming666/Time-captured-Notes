---
name: module-generator
description: 在拾光笔记新增业务模块骨架、接口或检查模块分层时使用。产出模块模板和分层检查结果，约束跨模块只通过api、跨模块事务由workflows协调。
---

# 生成并检查拾光模块

## 1. 适用场景
在拾光笔记新增业务模块骨架、接口或检查模块分层时使用。产出模块模板和分层检查结果，约束跨模块只通过api、跨模块事务由workflows协调。

## 2. 前置条件
读取01-架构设计文档/开发实施计划.md和目标模块；八模块边界已审批，新增边界先更新ADR。

## 3. 执行步骤
1. 用 scripts/module.py --module content --output <新目录> 生成参考骨架。
2. 实现前先增加失败业务测试。
3. 用 scripts/module.py --check --root . 检查真实源码。
4. 运行mvn verify验证ArchUnit及MySQL真实集成测试。

## 4. 强制约束
modules/*/domain不依赖Spring、web、platform；application依赖自己的domain和shared；infrastructure实现domain仓储；跨模块只能引用api，编排在workflows。web不得访问Database。收藏和实践归属本人，公开DTO必须投影，不能直接序列化实体；默认PRIVATE。

## 5. 输出物清单
可直接使用的Repository.java模板、四层目录与检查JSON；实现后同步OpenAPI及追溯表。

## 6. 自检清单
检查domain框架依赖、跨模块实现依赖、web直连Database；生成器不覆盖输出。

## 7. 常见错误与反例
错误：content.application调用governance.infrastructure。正确：workflows通过ContentApi与GovernanceApi在一个事务中编排。
