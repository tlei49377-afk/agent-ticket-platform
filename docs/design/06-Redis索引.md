# Redis 向量索引

> 对应阶段 5.3。索引由 LangChain4j `langchain4j-community-redis-spring-boot-starter` 管理，不要手工再建一个 `kb_idx`。

## 一、实际索引

| 项 | 值 |
|---|---|
| 索引名 | `embedding-index` |
| Redis 端口 | `6380` |
| Redis key 前缀 | `embedding:` |
| Redis key 类型 | `JSON` |
| 文本字段 | `$.text`，别名 `text` |
| 向量字段 | `$.vector`，别名 `vector` |
| 向量维度 | `1024` |
| 距离算法 | COSINE |
| 索引算法 | HNSW |

对应的 `FT.INFO` 结构应类似：

```text
index_name
embedding-index
key_type
JSON
prefixes
embedding:
attributes
  identifier $.text
  attribute text
  type TEXT
  identifier $.vector
  attribute vector
  type VECTOR
```

## 二、应用配置

`application-dev.yml`：

```yaml
langchain4j:
  community:
    redis:
      enabled: true
      host: localhost
      port: 6380
      index-name: embedding-index
      prefix: "embedding:"
      password: 123456
      dimension: 1024
```

`dimension` 必须与 `text-embedding-v3` 实际输出维度一致。

## 三、启动与验证

启动 Redis Stack：

```powershell
docker start redis-vector
& 'E:\Redis\redis-cli.exe' -p 6380 -a 123456 PING
& 'E:\Redis\redis-cli.exe' -p 6380 -a 123456 MODULE LIST
```

启动应用后检查：

```powershell
& 'E:\Redis\redis-cli.exe' -p 6380 -a 123456 FT._LIST
& 'E:\Redis\redis-cli.exe' -p 6380 -a 123456 FT.INFO embedding-index
```

预期：

- `FT._LIST` 包含 `embedding-index`；
- `key_type = JSON`；
- `prefixes = embedding:`；
- 初始 `num_docs = 0` 是正常的，完成文档入库后应大于 0。

## 四、常见问题

| 现象 | 原因 | 处理 |
|---|---|---|
| `FT._LIST` 中出现 `kb_idx`，但没有 `embedding-index` | 使用了旧手册的手工索引 | 删除旧的 `kb_idx`，让应用按当前配置创建 `embedding-index` |
| `num_docs = 0` | 还没有执行入库，或 prefix 不一致 | 检查入库任务和 `embedding:` 前缀 |
| `Vector dimension mismatch` | embedding 维度与 `dimension` 不一致 | 修正配置后重建索引 |
| 应用启动时 401 | `QW-API-KEY` 无效 | 配置真实 Key；starter 会在启动时通过 embedding 模型探测维度 |
| `FT.CREATE` unknown command | Redis 不是 Redis Stack | 使用带 RediSearch 的镜像 |