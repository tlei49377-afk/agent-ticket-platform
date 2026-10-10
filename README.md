# 智能工单 · 客服 Agent 平台

面向中小团队客服场景的智能工单系统，AI Agent 作为处理引擎。

## 当前进度

**阶段 0（需求与设计）、阶段 1（后端骨架）已完成**：库建好并灌入了初始化数据，5 个模块能构建，Web 层已经能登录、鉴权、发统一响应、带 traceId，另有 Actuator 与 OpenAPI 两个口子。业务接口本身从阶段 2 开始写。

完整操作步骤见 `..\docs\05-项目二操作流程手册.md`。

| 已完成 | 内容 |
|---|---|
| 设计文档 | 5 份，在 `docs/design/` |
| 建库脚本 | 4 个，在 `sql/`，已在本地 MySQL 执行成功 |
| 数据库 | `agent_ticket`，34 张表 |
| 初始化数据 | 4 角色 / 13 权限 / 5 用户 / 7 分类 / 4 SLA 策略 / 15 工具 / 2 Agent / 100 条评测用例 |
| Maven 骨架 | 父工程 + 5 个子模块，`mvn clean install` 通过 |
| Web 基础设施 | 统一响应 `Result`、全局异常处理、JWT 登录与拦截器、`@RequirePermission` 权限切面（权限码缓存在 Redis） |
| 可观测与调试 | traceId 贯穿日志与 `X-Trace-Id` 响应头；Actuator（`/actuator/health`、`/actuator/prometheus`）；OpenAPI（`/v3/api-docs`，导入 Apifox 用） |

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

## 接口约定

所有接口都返回同一个形状，前端只写一套解析逻辑：

| 场景 | HTTP 状态 | 响应体 |
|---|---|---|
| 成功 | 200 | `{"code":1,"msg":"success","data":...}` |
| 业务失败（参数不对、状态不允许） | 200 | `{"code":0,"msg":"失败原因"}` |
| 未登录 / token 失效 | 401 | 空（拦截器在进 Controller 之前就返回了） |
| 登录了但缺权限 | 403 | `{"code":0,"msg":"你没有执行该操作的权限"}` |

- 登录成功后的 token 放在请求头 `token` 里（不是 `Authorization`），除 `/login`、`/actuator/**`、`/v3/api-docs/**` 之外的接口都要带。
- 每个请求的 traceId 会回写到响应头 `X-Trace-Id`，和日志里 `%X{traceId}` 打出来的是同一个值。排查问题时拿它去搜日志即可。

## 快速开始

### 1. 前置

- JDK 21
- MySQL 8（默认 `root` / `123456`，按自己的改动）
- Redis 7+（权限码缓存用，默认 `localhost:6379`，库号 `10`）
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

### 5. 启动

先起 Redis（权限缓存在它上面），再起应用：

```bash
mvn -pl atp-server spring-boot:run "-Dmaven.repo.local=E:\Java\maven-LocalRepository"
```

IDEA 里直接跑 `AtpApplication` 也一样。看到 `Tomcat started on port 18096` 就是起来了。

**冒烟**（阶段 1 只有 `/login` 和 `/logout` 两个接口，所以先验登录、统一响应和健康检查）：

```powershell
$base = "http://127.0.0.1:18096"

# 1) 登录拿 token
$r = Invoke-RestMethod -Uri "$base/login" -Method Post -ContentType "application/json" `
     -Body '{"username":"admin","password":"123456"}'
$r | ConvertTo-Json -Depth 5
#    预期 {"code":1,"msg":"success","data":{"id":1,"username":"admin","name":"系统管理员","token":"eyJ..."}}

# 2) 调一个还不存在的接口 → 404，响应体是统一 Result 而不是 HTML 错误页
& curl.exe -s -i "$base/ticket/page"
#    预期 {"code":0,"msg":"接口不存在"}，响应头里有 X-Trace-Id

# 3) 健康检查
& curl.exe -s "$base/actuator/health"
#    预期 {"status":"UP"}
```

⚠️ **401 / 403 用 `/ticket/page` 这种不存在的路径是验不出来的**：`JwtTokenInterceptor` 只对真正映射到 Controller 方法的请求校验 token，路径不存在时直接放行，最后掉进 404，看起来像"鉴权没生效"。阶段 1 想验这两条，得先临时加一个带 `@RequirePermission` 的接口 —— 操作手册 **1.8.6** 有现成的代码和四条预期结果。

- `/actuator/prometheus` 能看到 JVM 指标。
- `/v3/api-docs` 返回 OpenAPI 文档，Apifox 里用「通过 URL 导入」填这个地址。

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
复制一份改名，再照着注释填真实值：

```bash
cp atp-server/src/main/resources/application-dev.yml.example \
   atp-server/src/main/resources/application-dev.yml
```

模型 API Key 走环境变量 `QW-API-KEY`，不要写进配置文件。**这个变量必须设**：配置里写的是 `${QW-API-KEY}` 占位符，没有默认值，不设的话应用起不来。

## IDEA 环境准备

从零建项目、配 Maven 与 JDK、连数据库、跑 SQL、配运行配置与 Git，全部写在
手册 **第 0.5 章 IDEA 环境准备与建项目**。其中"方式 C"专门讲用 Spring Initializr
建完之后要怎么改回多模块结构。

## 技术栈（规划）

Java 21 · Spring Boot 3.5 · MyBatis · MySQL 8 · Redis · Redis Stack（RediSearch）
LangChain4j 1.0.1-beta6 · LangGraph4j 1.9.2 · Vue 3 + Vite + Element Plus
