# 构建与测试

[返回文档目录](README.md)

项目使用 Maven Wrapper 固定构建入口。除非维护 Wrapper 本身，否则不需要先安装某个全局 Maven 版本。

## Maven Wrapper

Linux/macOS：

```bash
./mvnw -version
```

Windows：

```powershell
mvnw.cmd -version
```

当前 Wrapper 配置使用 Maven 3.9.16，并为下载的 Maven 发行包配置 SHA-256。首次运行可能联网下载 Maven 和项目依赖，之后会复用本地缓存。

## 常用命令

| 目的 | Linux/macOS | Windows |
| --- | --- | --- |
| 编译 | `./mvnw clean compile` | `mvnw.cmd clean compile` |
| 运行测试 | `./mvnw test` | `mvnw.cmd test` |
| 完整 Maven 验证 | `./mvnw verify` | `mvnw.cmd verify` |
| 普通打包 | `./mvnw clean package` | `mvnw.cmd clean package` |
| Maven assembly 分发包 | `./mvnw -Prelease clean package` | `mvnw.cmd -Prelease clean package` |

CI 使用非交互参数：

```bash
./mvnw --batch-mode --no-transfer-progress verify
```

本地需要复现 CI Maven 行为时也建议使用相同参数。

## 命令区别

- `compile`：编译主源码，不运行全部测试；
- `test`：编译并运行测试；
- `verify`：执行 Maven 生命周期中的测试和后续验证，是提交前推荐命令；
- `package`：生成普通项目 JAR；
- `-Prelease package`：额外生成包含运行依赖和仓库运行数据的分发包。

当前普通 JAR 没有配置为自包含可执行 JAR，不能仅凭 `package` 成功就推断 `java -jar target/ms079.jar` 可以启动。

## 运行单个测试

测试类：

```bash
./mvnw -Dtest=MapleKeyLayoutTest test
```

单个测试方法：

```bash
./mvnw -Dtest=MapleKeyLayoutTest#方法名 test
```

测试类和方法名应以当前源码为准。测试可能依赖 `src/test/resources`、仓库根目录或特定 Java 8 行为。

## 测试数据库边界

普通 Maven 测试中的 Ebean 配置使用内存 H2，并关闭 Ebean Test 的 Docker 数据库；部分测试还显式使用 `jdbc:h2:mem:` 的 MySQL 兼容模式。因此 `./mvnw verify` 通过不能证明 SQL 在真实 MySQL 5.7 上完全兼容。

真实 MySQL 覆盖来自独立的 Docker 启动冒烟流程：它导入 `db/ms079.sql`、读取核心表并启动服务端，但不会遍历每一项游戏业务。数据库改动应同时考虑 H2 单元测试和 MySQL 5.7 实证。

## 仓库数据校验

```bash
./mvnw --batch-mode --no-transfer-progress \
  -Dtest=RepositoryDataValidationTest test
```

覆盖：

- 全部 JavaScript 脚本语法编译；
- 全部 WZ XML 安全解析；
- 服务端配置必需键；
- 重复配置键；
- 端口、频道数、倍率和刷新参数范围。

当前配置测试允许频道数最高为 100，而运行时最多启动 10 个频道；它也不会检查逗号列表内部的重复事件名或 ID。测试通过不代表这两类配置具有预期运行效果。

修改 `scripts/`、`wz/` 或 `config/server.properties` 时，这项检查是最低要求。

## Docker 静态校验

检查 Dockerfile：

```bash
docker build --check \
  --build-arg 'BUILDKIT_DOCKERFILE_CHECK=error=true' \
  .
```

检查 Compose 展开配置：

```bash
docker compose config --quiet
```

构建运行镜像：

```bash
docker build --target runtime --tag ms079-server:local .
```

这些命令不会替代真实数据库启动。Dockerfile 能构建、Compose 能解析，也不代表服务端能够加载 SQL、WZ 和脚本。

## 真实启动冒烟测试

CI 的 `.github/scripts/mysql-startup-smoke.sh` 会：

1. 创建临时 MySQL 5.7 容器；
2. 等待数据库健康；
3. 导入 `db/ms079.sql`；
4. 检查表数量和核心表；
5. 创建临时服务端容器；
6. 挂载 WZ、脚本和 CI 配置；
7. 等待“服务端启动完毕”日志；
8. 验证登录、默认频道和商城端口；
9. 自动删除临时容器。

该脚本会创建和删除 Docker 容器，日常文档改动不需要执行。修改 Docker、数据库或启动流程时，可在明确理解其临时资源命名和清理行为后运行。

## CI 检查

### PR Checks

- Java 8 / Maven `verify`；
- 游戏脚本、WZ 和配置校验；
- Dockerfile 和 Compose 检查；
- 运行镜像构建；
- MySQL 5.7 schema 导入和启动冒烟测试。

### Dependency Review

Pull Request 修改依赖时检查新增依赖风险，当前门槛会阻止达到配置严重程度的依赖问题。

### Repository Policy

- `git diff --check`；
- 禁止新增构建产物、日志和归档文件；
- 限制超大新文件；
- 对 PR 提交运行秘密扫描。

### Source Release

符合语义化格式的版本标签会触发源码归档和 GitHub Release。详见[发布](release.md)。

当前 PR Checks 不执行 `-Prelease clean package`，也不解压或运行 Maven assembly 分发包。CI 中通过的是 Maven `verify`、数据校验和 Docker/MySQL 启动流程，不能据此宣称 ZIP/TAR.GZ 分发包已经完成端到端运行验证。

## 按修改范围选择最小验证

| 修改范围 | 最低建议 |
| --- | --- |
| 仅 Markdown 文档 | 检查链接、`git diff --check` |
| 单个 Java 工具或纯函数 | 相关单元测试、编译 |
| 网络、账号、角色、道具或数据库逻辑 | 相关测试、`verify`、必要的真实流程 |
| `scripts/` | `RepositoryDataValidationTest`、目标脚本真实流程 |
| `wz/` | `RepositoryDataValidationTest`、完整重启和目标数据验证 |
| `config/server.properties` | `RepositoryDataValidationTest`、启动检查、秘密检查 |
| `db/ms079.sql` | SQL 导入、核心表检查、服务端启动 |
| Dockerfile/Compose | Docker 静态检查、镜像构建、MySQL 启动冒烟 |

## 提交前检查

```bash
git status --short
git diff --check
git diff --stat
```

特别确认没有加入：

- `target/`、`out/`、`build/`；
- 日志；
- 临时 ZIP/TAR.GZ；
- IDE 私有配置；
- 数据库密码或公网地址；
- 与任务无关的大规模 WZ 或脚本格式化。

继续阅读：[发布](release.md)。
