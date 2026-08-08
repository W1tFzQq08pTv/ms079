# 项目结构

[返回文档目录](README.md)

ms079 由 Java 服务端、WZ XML 数据、JavaScript 游戏脚本和 MySQL 历史数据转储组成。只阅读 Java 源码不足以理解完整运行行为。

## 根目录

| 路径 | 作用 |
| --- | --- |
| `pom.xml` | Maven 依赖、Java 8 编译、Ebean enhancement 和 release Profile |
| `mvnw`、`mvnw.cmd` | Linux/macOS 和 Windows Maven Wrapper |
| `config/server.properties` | 数据库、网络、倍率、职业、事件和日志配置 |
| `db/ms079.sql` | 源自 MySQL 5.5.53、由 CI 在 MySQL 5.7 验证的完整历史数据转储，包含示例账号和角色 |
| `wz/` | 服务端运行时读取的 WZ XML 数据 |
| `scripts/` | NPC、事件、任务、传送点和反应堆 JavaScript |
| `Dockerfile` | Java 8 多阶段服务端镜像 |
| `docker-compose.yml` | 编排 MySQL 与服务端，并在数据库健康后启动服务端 |
| `.github/` | CI、依赖审查、安全门禁和源码发布工作流 |
| `SECURITY.md` | 安全范围、威胁边界和私密报告流程 |
| `old-files/` | 历史归档；正常构建和运行不应依赖其中内容 |

`target/`、日志和本地压缩包属于生成内容，不是源码入口，也不应作为修改基线。

## Java 源码

Java 源码位于 `src/main/java`。主要区域如下。

### 应用、配置和 WZ

```text
com.github.mrzhqiang.maplestory
```

包含当前应用入口、Guice 配置、数据库配置、领域对象和新的 WZ 管理实现。关键入口包括：

- `MapleStoryApplication`：创建依赖注入容器并启动服务端；
- `config.ServerConfiguration`：读取 `config/server.properties`；
- `config.DatabaseConfiguration`：创建 Ebean 数据库配置；
- `config.ServerProperties`：解析服务端参数；
- `wz.WzManage`、`wz.WzData`：定位和加载 WZ XML。

### Swing 管理控制台

```text
gui.GUIApplication
```

这是一个真实的遗留 Swing 应用入口，不只是图标资源。它可以启动同一个 `ApplicationStarter`，并提供保存、重载、公告、账号和道具等高权限管理操作。当前 Dockerfile、CI 和 `start-server.bat` 均不使用该入口；常规服务端入口仍是 `MapleStoryApplication`。

### 服务端业务

```text
server
```

包含服务启动、关闭、道具、商城、交易、仓库、任务、地图和怪物等核心逻辑。常见子目录：

- `server.maps`：地图、传送点、掉落物、召唤物和地图对象；
- `server.life`：怪物、NPC 和生成点；
- `server.quest`：任务；
- `server.shops`：商店；
- `server.movement`：移动数据。

`server.ApplicationStarter` 负责按顺序加载运行数据并启动各网络服务。

### 网络服务和协议处理

```text
handling
```

主要区域：

- `handling.login`：登录服务和角色登录；
- `handling.channel`：频道服务及大量游戏数据包处理器；
- `handling.cashshop`：商城服务；
- `handling.world`：跨频道世界状态、公会、队伍和家族；
- `handling.mina`：Apache MINA 编解码和连接处理；
- `SendPacketOpcode`、`RecvPacketOpcode`：协议 Opcode 映射。

处理客户端数据包时，应把客户端传入的角色、道具、数量、状态和权限视为不可信输入，不能只检查数据包能否正常解析。

### 客户端会话和角色模型

```text
client
```

包含账号会话、角色、技能、背包、宠物、状态、反作弊和 GM 命令。虽然目录名是 `client`，这里大部分仍是服务端对客户端状态的建模，不是游戏客户端源码。

### JavaScript 运行层

```text
scripting
```

负责加载和执行 `scripts/` 下的 JavaScript，包括 NPC、事件、任务、传送点和反应堆。项目依赖 Java 8 自带的 JavaScript 引擎及其 `Compilable` 能力，这也是数据校验固定使用 Java 8 的原因之一。

### 数据库访问

```text
database
com.github.mrzhqiang.maplestory.domain
```

仓库同时存在旧 JDBC 辅助代码和较新的 Ebean 模型。修改数据库逻辑前应确认目标代码实际使用哪一条访问路径，不要假定所有 SQL 都由同一个 ORM 管理。

### 工具和协议数据

```text
tools
tools.data
tools.packet
tools.wztosql
```

包含字节序读写、加密、数据包构造、日期和字符串辅助，以及从 WZ 生成或修正数据库数据的历史工具。工具类可能带有独立 `main` 方法，但不代表它们是服务端入口。

### 常量和历史代码

```text
constants
KinMS
```

`constants` 包含协议版本、游戏规则和服务端常量；`KinMS` 是历史功能代码。修改全局常量可能影响多个处理器和数据包，应通过搜索引用评估影响范围。

## Java 资源

`src/main/resources` 包含：

| 文件或目录 | 作用 |
| --- | --- |
| `sendops.properties` | 服务端发送 Opcode 映射 |
| `recvops.properties` | 服务端接收 Opcode 映射 |
| `logback.xml` | 运行日志配置 |
| `gui/` | `GUIApplication` 使用的 Swing 图标资源 |

修改 Opcode 时应同步检查对应枚举、数据包构造和处理器，不能只改变 properties 中的数值。

## 日志实现

项目并存两套日志机制：

- Logback 输出到 `logs/application.log` 并保留按日期滚动的压缩文件；
- `tools.FileoutputUtil` 的分类日志也统一输出到 `logs/` 下。

后者被多个账号、反作弊、脚本和数据包路径直接调用。成功登录流程已经移除明文密码日志，但迁移前生成的历史日志或备份仍可能含有旧凭据，不能作为普通调试附件公开上传。

## 测试结构

测试位于 `src/test/java`，覆盖角色、背包、宠物、商城、地图、WZ、配置、脚本和数据包等区域。

`RepositoryDataValidationTest` 是仓库级数据门禁，负责：

- 使用 Java 8 JavaScript 引擎编译全部 `scripts/**/*.js`；
- 以禁用外部实体的安全配置解析全部 `wz/**/*.xml`；
- 检查 `config/server.properties` 的必需键、重复键、端口和倍率范围。

它不连接真实数据库，也不证明完整游戏流程正确。普通 Ebean 测试使用内存 H2；真实 MySQL 5.7 初始化和启动由 CI 的独立 Docker 冒烟脚本覆盖。

## 启动流程

```text
MapleStoryApplication
    │
    ├── ServerConfiguration ──> config/server.properties
    ├── DatabaseConfiguration ─> MySQL / Ebean
    │
    └── ApplicationStarter
          ├── 重置账号登录状态
          ├── 加载 WZ、任务、道具和脚本相关数据
          ├── 启动世界与定时器
          ├── 启动登录服务
          ├── 启动频道服务
          ├── 启动商城服务
          └── 注册保存任务和关闭钩子
```

开发启动问题时，应先确定失败发生在哪个阶段，再检查对应输入和日志，而不是只围绕最后一条异常信息补丁。

继续阅读：[游戏数据与脚本](game-data-and-scripts.md)。
