---
name: project-scaffold
description: 在拾光笔记项目首次初始化、恢复本地开发环境、生成配置模板或检查工程目录时使用。产出八模块骨架和无密钥环境模板，禁止覆盖既有源码。
---

# 恢复拾光笔记开发骨架

## 1. 适用场景
在拾光笔记项目首次初始化、恢复本地开发环境、生成配置模板或检查工程目录时使用。产出八模块骨架和无密钥环境模板，禁止覆盖既有源码。

## 2. 前置条件
确认仓库根目录、Java21、Docker Desktop已启动；生成目标必须不存在。

## 3. 执行步骤
1. 运行 scripts/scaffold.py --check --root . 检查现有项目。
2. 新工程使用 --output 指定一个不存在的目录；完成后检查八模块及环境模板。
3. 直接运行03-源码/scripts/init-env.py生成随机.env；已有.env时保留原值，不先复制空模板再运行生成器。

## 4. 强制约束
使用identity、content、social、collection、governance、discovery、notification、statistics八模块；Vue3；Java21。MySQL是权限及幂等权威源，Redis不保存唯一业务数据。真实Git日期，Docker不可用时不得伪报集成测试通过。

## 5. 输出物清单
八模块api/application/domain/infrastructure目录、环境模板或检查JSON。

## 6. 自检清单
重复运行生成器必须拒绝覆盖；check模式只读；真实密钥不得出现在模板。

## 7. 常见错误与反例
错误：在已存在源码上直接重建。正确：先--check，新增输出必须为空的新路径。
