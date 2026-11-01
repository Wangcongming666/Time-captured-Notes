# 数据库脚本

默认使用 Flyway：启动后按 V1、V2 自动创建30张业务表、外键和索引，另有Flyway自身历史表。`01-schema.sql`和`02-roles-and-search.sql`分别与两个迁移文件逐字一致。V1头部保留设计基线注释；已通过真实MySQL8.4集成测试执行，实施状态以测试报告为准。

需要手工初始化时，在**全新空数据库**按顺序执行：

```sql
CREATE DATABASE shiguang CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE shiguang;
SOURCE 01-schema.sql;
SOURCE 02-roles-and-search.sql;
```

手工建表后应关闭Flyway（`SPRING_FLYWAY_ENABLED=false`）或明确建立经过核验的迁移基线；不要在已有表上再次运行V1。生产升级使用新增迁移，不改已经执行的V1/V2。

角色为USER、REVIEWER、ADMIN。演示账号和样例内容由显式启用的SEED_DEMO初始化，密码取自未提交的.env；SQL不包含固定公开密码。MySQL连接会话使用UTC，所有ID经HTTP输出为字符串。
