# ms079

> 那一年，我们从彩虹村出发，在射手村相遇，为一件装备、一次组队任务和深夜响起的升级音效而欣喜。v079 留住的不只是一版游戏，也是一段属于许多冒险家的青春。愿这个仓库让熟悉的世界再次被看见，也让后来者有机会了解、学习并延续这份记忆。

重温大巨变前的经典冒险岛 v079。本仓库整理并维护相关服务端源码与配套资源，仅供学习、研究与交流。

项目包含 Java 服务端、WZ XML 数据、JavaScript 游戏脚本、MySQL 历史数据转储、Docker 运行配置、自动化测试及开发文档。开始运行或修改项目前，请先阅读对应专题，避免遗漏数据库、配置和运行数据之间的依赖。

## 推荐阅读顺序

首次搭建开发环境时，建议按以下顺序阅读：

1. [环境准备](docs/environment.md)
2. [数据库与服务端配置](docs/database-and-configuration.md)
3. 选择 [本地 Java 开发](docs/local-development.md) 或 [Docker 运行](docs/docker.md)
4. [项目结构](docs/project-structure.md)
5. [游戏数据与脚本](docs/game-data-and-scripts.md)
6. [构建与测试](docs/build-and-testing.md)

## 文档目录

| 文档 | 适用场景 |
| --- | --- |
| [环境准备](docs/environment.md) | 安装或检查 JDK、MySQL、Docker、IDE 及 Maven Wrapper |
| [数据库与服务端配置](docs/database-and-configuration.md) | 初始化 `ms079` 数据库，理解 `config/server.properties` |
| [本地 Java 开发](docs/local-development.md) | 在 IDE 中导入、启动、调试和停止服务端 |
| [Docker 运行](docs/docker.md) | 使用 Docker Compose 编排 MySQL 5.7 与游戏服务端 |
| [项目结构](docs/project-structure.md) | 了解 Java 包、资源、测试和启动流程 |
| [游戏数据与脚本](docs/game-data-and-scripts.md) | 修改 WZ XML、NPC、事件、地图及其他脚本 |
| [构建与测试](docs/build-and-testing.md) | 执行 Maven、数据校验、Docker 校验和 CI 对齐检查 |
| [客户端与兼容性](docs/client-and-compatibility.md) | 使用 v079 客户端、登录器和 Windows 守护脚本联调 |
| [发布](docs/release.md) | 生成分发包、创建版本标签和理解源码发布工作流 |
| [常见问题](docs/troubleshooting.md) | 按现象排查 Java、数据库、WZ、脚本、端口及容器问题 |
| [外部资源](docs/external-resources.md) | 查找官方依赖、第三方镜像、历史下载链接和提取码 |
| [安全策略](SECURITY.md) | 了解安全范围以及私密漏洞报告流程 |

## 按修改类型选择文档

### Java 服务端代码

先阅读 [项目结构](docs/project-structure.md)，修改后按 [构建与测试](docs/build-and-testing.md) 运行相关单元测试或 `verify`。

### WZ XML 或 JavaScript 脚本

先阅读 [游戏数据与脚本](docs/game-data-and-scripts.md)，至少运行仓库数据校验。不要只依赖服务端能够编译，因为数据和脚本不一定在 Java 编译阶段被读取。

### 数据库或配置

阅读 [数据库与服务端配置](docs/database-and-configuration.md)。数据库结构、Java 查询和配置文件之间存在运行时依赖，修改其中一项时应检查另外两项。

### Docker 和启动脚本

阅读 [Docker 运行](docs/docker.md)。当前 Compose 同时管理 MySQL 5.7 和游戏服务端，并在数据库健康检查通过后启动服务端。

### 客户端联调

阅读 [客户端与兼容性](docs/client-and-compatibility.md)。客户端和兼容资源来自独立发行渠道，不属于 Maven 构建依赖。

## 基本约定

- 当前编译和 CI 基线是 Java 8。
- 当前数据库运行基线是 MySQL 5.7。
- Java 应用入口是 `com.github.mrzhqiang.maplestory.MapleStoryApplication`。
- 本地运行时的工作目录应为仓库根目录。
- `wz/`、`scripts/` 和 `config/server.properties` 都是运行时输入，不只是发布附件。
- `db/ms079.sql` 是带示例账号、角色及大量游戏数据的历史数据转储，不是空 schema。
- 登录流程不再写入明文密码；历史日志或备份仍可能包含旧凭据，公开或共享前必须审查并脱敏。
- 不要提交数据库密码、生产地址、日志、`target/` 或临时压缩包。
- 安全问题不要先创建公开 Issue，请按 [`SECURITY.md`](SECURITY.md) 私密报告。

## 使用声明

本项目仅供个人学习、研究与技术交流，不得用于商业用途。客户端及其他外部资源不属于本仓库，使用前请确认其来源和授权条件。
