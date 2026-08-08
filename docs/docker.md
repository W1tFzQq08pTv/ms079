# Docker 运行

[返回文档目录](README.md)

当前 Docker Compose 同时编排 MySQL 5.7 和 Java 8 游戏服务端。MySQL 首次使用空数据目录时会导入仓库 SQL，服务端在数据库健康检查通过后启动。

## 当前容器结构

```text
宿主机
├── ms079-mysql       MySQL 5.7、数据库数据、宿主机端口发布
└── ms079-server      Java 8 游戏服务端
        └── network_mode: service:ms079-mysql
```

`ms079-server` 直接加入 `ms079-mysql` 的网络命名空间。两个容器共享 `127.0.0.1` 和监听端口，因此：

- 服务端配置可使用 `127.0.0.1:3306` 访问 MySQL；
- 登录、频道和商城端口需要在创建 `ms079-mysql` 时发布到宿主机；
- 不能再给 `ms079-server` 单独配置端口映射；
- 服务端通过 Compose 服务 `ms079-mysql` 共享数据库网络命名空间。

## 镜像内容

Dockerfile 使用两阶段构建：

1. Java 8 JDK 阶段下载 Maven 依赖并编译项目；
2. Java 8 JRE 阶段只复制编译产物和运行依赖。

运行容器从宿主机挂载：

| 宿主机 | 容器内 | 权限 |
| --- | --- | --- |
| `./wz` | `/app/wz` | 只读 |
| `./scripts` | `/app/scripts` | 只读 |
| `./config/server.properties` | `/app/config/server.properties` | 只读 |
| `./logs` | `/app/logs` | 可写 |

因此修改 Java 代码后需要重新构建镜像；修改 WZ、脚本或配置后通常只需重启服务端容器，但涉及缓存、删除数据或结构变化时应完整重启并重新验证。

Logback 与 `FileoutputUtil` 均写入 `/app/logs`，由宿主机 `./logs` 统一持久化。登录流程已经移除明文密码日志；迁移前生成的历史日志或备份仍可能含有旧凭据，不能公开分享。

## 准备 MySQL 配置

复制 `.env.example` 为 `.env`，至少设置非空的 `MYSQL_ROOT_PASSWORD`。Compose 使用 MySQL 5.7、健康检查和以下绑定目录：

- `${MYSQL_DATA_DIR}`，默认 `./docker/mysql/data`，持久化现有数据库；
- `${MYSQL_HEALTH_INIT_SQL}`，创建最小权限健康检查账号；
- `${MYSQL_INIT_SQL}`，默认指向 `db/ms079.sql`，仅在数据目录首次初始化时导入。

`config/server.properties` 中的 JDBC 凭据必须与数据库现有账号一致。修改 `.env` 不会自动改写已经初始化的数据目录或服务端配置。

`db/ms079.sql` 包含示例账号、角色和大量历史业务数据，不是空 schema，而且会删除并重建同名表。即使只在容器中运行，也只能导入新建空数据库，并应在对外开放前审查和清理预置数据。不要把 CI 对临时数据库的重建方式套用到持久业务卷。

默认三频道配置需要考虑这些端口：

| 端口 | 用途 |
| --- | --- |
| `3306` | MySQL；只在确实需要宿主机访问时发布 |
| `9595` | 登录服务 |
| `7576`、`7577`、`7578` | 默认三个频道 |
| `8600` | 商城服务 |

频道数量或基础端口改变后，必须同步更新数据库容器的端口发布范围。频道实际端口为 `server.channel.port + 频道编号`；当前运行时代码最多启动 10 个频道，即使配置校验允许填写更大的数值。

项目 CI 使用固定摘要的 MySQL 5.7 镜像、临时容器和临时密码执行真实 schema 导入及启动检查。CI 脚本可作为实现参考，但其中的 CI 密码只适用于隔离临时环境，不应复制为正式凭据。

## 启动服务端

在仓库根目录执行：

```bash
docker compose up -d --build ms079-server
```

查看状态和日志：

```bash
docker compose ps
docker compose logs -f ms079-server
```

首次构建需要下载基础镜像、Maven 和项目依赖。网络受限时应先解决可信镜像源或代理问题，不要绕过镜像摘要、TLS 或依赖校验。

## 验证启动

至少确认：

1. `ms079-server` 保持运行，没有立即退出；
2. 日志出现“服务端启动完毕”；
3. 日志中没有数据库、WZ 或 JavaScript 初始化异常；
4. 登录、频道和商城端口处于监听状态；
5. 数据库容器仍为健康状态。

登录和频道端口绑定失败目前不一定会让 Java 进程退出，因此第 2 项不能替代第 4 项。

查看单个容器日志：

```bash
docker logs ms079-server
docker logs ms079-mysql
```

由于两个容器共享网络命名空间，在数据库容器内部检查游戏端口是符合当前设计的。只验证 `docker ps` 中的“Up”状态不足以证明游戏服务已经完成初始化。

## 重启、停止和删除

重启服务端：

```bash
docker compose restart ms079-server
```

停止并移除服务端 Compose 资源：

```bash
docker compose down
```

该命令会停止并移除本 Compose 项目的服务端和 MySQL 容器，但不会删除 `${MYSQL_DATA_DIR}` 指向的宿主机数据库目录。不要手工删除该目录；重建或恢复数据库前应先备份并确认精确目标。

重新构建 Java 镜像：

```bash
docker compose build --no-cache ms079-server
docker compose up -d ms079-server
```

只有在诊断确认缓存是问题时才需要 `--no-cache`，日常代码修改可以使用普通增量构建。

## Windows 便捷脚本

根目录提供：

```text
start-server-console.bat
start-server.bat
```

两者都调用 Docker Compose：

- 命令行版本启动后持续跟踪日志；
- GUI 命名版本启动后显示 Compose 状态并等待用户关闭窗口；
- 两者都要求 Docker Desktop 已启动，并从 `.env` 读取 Compose 配置。

## WZ 参数说明

Dockerfile 使用源码支持的 `-Dwz.path=wz`，对应容器中的 `/app/wz`。若以后移动 WZ，应同时修改启动参数和挂载路径。

## Apple Silicon 和 ARM64

服务端 Java 镜像是否支持当前主机架构取决于所固定的镜像摘要。MySQL 5.7 更常见的问题是缺少原生 ARM64 镜像；需要时可在创建数据库容器时指定：

```text
--platform linux/amd64
```

这会使用虚拟化或模拟，启动速度和性能可能低于原生架构。不要据此推断所有 ARM64 环境都已获得正式支持。

## 当前设计限制

- 首次初始化前必须在 `.env` 中设置数据库密码，并确认 `config/server.properties` 凭据一致；
- 数据库容器名称和网络模式耦合较强；
- 游戏端口由数据库容器发布，初次接触时不直观；
- 根配置文件直接挂载，不具备独立 secret 管理；
- WZ 和脚本属于大体积只读运行数据；
- 所有应用日志统一持久化到宿主机 `logs/`；
- 没有容器级服务端 healthcheck，需结合日志和端口判断就绪。

这些是当前实现说明，不代表长期推荐架构。若要调整，应另行修改 Compose、配置加载和文档，并通过真实 MySQL 启动测试验证。

继续阅读：[常见问题](troubleshooting.md)。
