# ms079 开发文档

本目录面向需要阅读、运行或修改 ms079 服务端的开发者。根目录现有 `README.md` 暂时保留；新的详细说明以本目录为入口，待文档稳定后再决定是否替换根 README。

## 推荐阅读顺序

首次搭建开发环境时，建议按以下顺序阅读：

1. [环境准备](environment.md)
2. [数据库与服务端配置](database-and-configuration.md)
3. 选择 [本地 Java 开发](local-development.md) 或 [Docker 运行](docker.md)
4. [项目结构](project-structure.md)
5. [游戏数据与脚本](game-data-and-scripts.md)
6. [构建与测试](build-and-testing.md)

## 文档目录

| 文档 | 适用场景 |
| --- | --- |
| [环境准备](environment.md) | 安装或检查 JDK、MySQL、Docker、IDE 及 Maven Wrapper |
| [数据库与服务端配置](database-and-configuration.md) | 初始化 `ms079` 数据库，理解 `服务端配置.ini` |
| [本地 Java 开发](local-development.md) | 在 IDE 中导入、启动、调试和停止服务端 |
| [Docker 运行](docker.md) | 使用当前 Dockerfile 和外部 MySQL 容器运行服务端 |
| [项目结构](project-structure.md) | 了解 Java 包、资源、测试和启动流程 |
| [游戏数据与脚本](game-data-and-scripts.md) | 修改 WZ XML、NPC、事件、地图及其他脚本 |
| [构建与测试](build-and-testing.md) | 执行 Maven、数据校验、Docker 校验和 CI 对齐检查 |
| [客户端与兼容性](client-and-compatibility.md) | 使用 v079 客户端、登录器和 Windows 守护脚本联调 |
| [发布](release.md) | 生成分发包、创建版本标签和理解源码发布工作流 |
| [常见问题](troubleshooting.md) | 按现象排查 Java、数据库、WZ、脚本、端口及容器问题 |
| [外部资源](external-resources.md) | 查找官方依赖、第三方镜像、历史下载链接和提取码 |
| [安全策略](../SECURITY.md) | 了解安全范围以及私密漏洞报告流程 |

## 按修改类型选择文档

### Java 服务端代码

先阅读 [项目结构](project-structure.md)，修改后按 [构建与测试](build-and-testing.md) 运行相关单元测试或 `verify`。

### WZ XML 或 JavaScript 脚本

先阅读 [游戏数据与脚本](game-data-and-scripts.md)，至少运行仓库数据校验。不要只依赖服务端能够编译，因为数据和脚本不一定在 Java 编译阶段被读取。

### 数据库或配置

阅读 [数据库与服务端配置](database-and-configuration.md)。数据库结构、Java 查询和配置文件之间存在运行时依赖，修改其中一项时应检查另外两项。

### Docker 和启动脚本

阅读 [Docker 运行](docker.md)。当前 Compose 只管理游戏服务端，数据库容器是外部前置条件，不是同一个 Compose 项目中的服务。

### 客户端联调

阅读 [客户端与兼容性](client-and-compatibility.md)。客户端和兼容资源来自独立发行渠道，不属于 Maven 构建依赖。

## 基本约定

- 当前编译和 CI 基线是 Java 8。
- 当前数据库运行基线是 MySQL 5.7。
- Java 应用入口是 `com.github.mrzhqiang.maplestory.MapleStoryApplication`。
- 本地运行时的工作目录应为仓库根目录。
- `wz/`、`脚本/` 和 `服务端配置.ini` 都是运行时输入，不只是发布附件。
- `db/ms079.sql` 是带示例账号、角色及大量游戏数据的历史数据转储，不是空 schema。
- 当前遗留登录代码会把成功登录时的账号、明文密码、MAC 和地址写入 `日志/logs/ACPW.txt`；只能使用隔离测试凭据，不能直接承载真实账号。
- 不要提交数据库密码、生产地址、日志、`target/` 或临时压缩包。
- 安全问题不要先创建公开 Issue，请按 [`SECURITY.md`](../SECURITY.md) 私密报告。
