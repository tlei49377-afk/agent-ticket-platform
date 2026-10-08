# 智能工单 · 客服 Agent 平台

面向中小团队客服场景的智能工单系统，AI Agent 作为处理引擎。

## 当前进度

**阶段 0（需求与设计）已完成**，数据库已建好并灌入初始化数据，多模块骨架已跑通构建。

业务代码尚未开始，阶段 1 会往各模块里填内容。完整操作步骤见 `..\docs\05-项目二操作流程手册.md`。

| 已完成 | 内容 |
|---|---|
| 设计文档 | 5 份，在 `docs/design/` |
| 建库脚本 | 4 个，在 `sql/`，已在本地 MySQL 执行成功 |
| 数据库 | `agent_ticket`，34 张表 |
| 初始化数据 | 4 角色 / 13 权限 / 5 用户 / 7 分类 / 4 SLA 策略 / 15 工具 / 2 Agent / 100 条评测用例 |
| Maven 骨架 | 父工程 + 5 个子模块，`mvn clean install` 通过 |

## 模块结构

```
agent-ticket-platform        父工程（packaging=pom，只做依赖与版本管理）
├── atp-common               公共基础设施：统一响应、异常、常量、工具类
├── atp-pojo                 实体与 DTO / VO
├── atp-agent                Agent 内核：工具、记忆、RAG、状态图
├── atp-eval                 评测执行器与指标计算
└── atp-server               Web 层与启动模块（唯一有 @SpringBootApplication 的模块）
```

- 父工程 `com.atp:agent-ticket-platform:1.0-SNAPSHOT`，基于 Spring Boot `3.5.11` + Java 21。
- 包名统一 `com.atp.*`，例如 `com.atp.common`、`com.atp.agent`。
- 除 `atp-server` 外，其余模块都是**类库**，不要加 `@SpringBootApplication`。
- 模块互相依赖时，**必须先在根目录 `mvn clean install`**，否则 IDEA 报 `Cannot resolve symbol`。

## 目录说明

```
docs/design/    设计文档（PRD、工具清单、工单状态机、Agent 状态图、评测指标）
sql/            建库脚本与初始化数据
```

## 快速开始

### 1. 前置

- JDK 21
- MySQL 8（默认 `root` / `123456`，按自己的改动）
- Maven 3.9+

### 2. 构建

```bash
mvn clean install -DskipTests "-Dmaven.repo.local=E:\Java\maven-LocalRepository"
```

`-Dmaven.repo.local` 这一项如果本机用的是默认仓库路径，可以省略。

### 3. 建库

按顺序执行，**不要跳步，也不要并行跑**：

```bash
mysql -uroot -p123456 --default-character-set=utf8mb4 -e "source sql/00-drop-all.sql"
mysql -uroot -p123456 --default-character-set=utf8mb4 -e "source sql/01-schema.sql"
mysql -uroot -p123456 --default-character-set=utf8mb4 -e "source sql/02-init-data.sql"
mysql -uroot -p123456 --default-character-set=utf8mb4 -e "source sql/03-eval-data.sql"
```

说明：

- `00-drop-all.sql` 会**连库一起删掉**，只在需要重建时执行。
- `01-schema.sql` 开头自带 `DROP DATABASE IF EXISTS`，单独跑它也能重置。
- `source` 后面的路径用正斜杠，且不能带空格。

也可以不装 MySQL 命令行 —— 用 IDEA 的 Database 工具窗逐个右键 `Run`，步骤见手册第 0.5.3 节。

### 4. 验证

```sql
select '表数量' as item, count(*) as cnt from information_schema.tables where table_schema='agent_ticket'
union all select '角色', count(*) from sys_role
union all select '权限点', count(*) from sys_permission
union all select '用户', count(*) from sys_user
union all select '工单分类', count(*) from ticket_category
union all select 'SLA策略', count(*) from sla_policy
union all select '工具', count(*) from agent_tool
union all select 'Agent', count(*) from agent
union all select '评测用例', count(*) from eval_case;
```

预期：`34 / 4 / 13 / 5 / 7 / 4 / 15 / 2 / 100`。

## 默认账号

密码统一 `123456`，库中存的是 BCrypt 哈希，不是明文。

| 用户名 | 密码 | 角色 | 说明 |
|---|---|---|---|
| admin | 123456 | 管理员 | 配置 Agent、知识库、用户与权限 |
| zhangsan | 123456 | 主管 | 指派、审批 AI 写操作、看统计 |
| lisi | 123456 | 客服 | 处理工单、回复用户 |
| wangwu | 123456 | 客服 | 处理工单、回复用户 |
| zhaoliu | 123456 | 提交人 | 提交工单并跟踪进度 |

## 配置文件

真实的 `application-dev.yml` **不入库**（`.gitignore` 已挡住），仓库里只有 `.example` 模板。
阶段 1 生成模板后，复制一份改名并填入真实值：

```bash
cp atp-server/src/main/resources/application-dev.yml.example \
   atp-server/src/main/resources/application-dev.yml
```

模型 API Key 建议走环境变量 `QW-API-KEY`，不要写进配置文件。

## IDEA 环境准备

从零建项目、配 Maven 与 JDK、连数据库、跑 SQL、配运行配置与 Git，全部写在
手册 **第 0.5 章 IDEA 环境准备与建项目**。其中"方式 C"专门讲用 Spring Initializr
建完之后要怎么改回多模块结构。

## 技术栈（规划）

Java 21 · Spring Boot 3.5 · MyBatis · MySQL 8 · Redis · Redis Stack（RediSearch）
LangChain4j 1.0.1-beta6 · LangGraph4j 1.9.2 · Vue 3 + Vite + Element Plus