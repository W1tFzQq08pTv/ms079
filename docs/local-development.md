# 本地 Java 开发

[返回文档目录](README.md)

本页描述直接使用 JDK 8 和 IDE 开发服务端的流程。只希望通过容器运行时，请改读 [Docker 运行](docker.md)。

## 前置条件

- 已安装并选中 JDK 8；
- 已导入 `db/ms079.sql`；
- 已配置仓库内的 `config/server.properties`；
- 根目录存在完整的 `wz/` 和 `scripts/`；
- MySQL 5.7 可以从本机访问。

## 导入项目

以 Maven 项目方式导入根目录的 `pom.xml`。IDE 配置应满足：

- Project SDK：JDK 8；
- Language level：Java 8；
- Maven Runner：JDK 8；
- Project encoding：UTF-8；
- Working directory：仓库根目录。

不建议绕过 Maven 手工维护依赖 JAR。项目使用 Ebean enhancement 和注解处理器，IDE 构建配置应与 Maven 保持一致。

## 推荐应用入口

当前真实启动类是：

```text
com.github.mrzhqiang.maplestory.MapleStoryApplication
```

入口会创建 Guice Injector，加载服务端和数据库配置，然后调用 `ApplicationStarter` 启动各子系统。旧文档中的 `server.Start` 已不再是有效入口。

需要显式指定 WZ 目录时使用：

```text
-Dfile.encoding=UTF-8 -Dwz.path=wz
```

源码实际读取的系统属性名是 `wz.path`；不传时默认使用工作目录下的 `wz`。Docker 镜像还使用 `-server`。本地开发是否增加堆内存参数，应根据实际数据量和诊断结果决定，不要照抄生产参数。

## 遗留 Swing GUI

仓库还存在另一个带 `main` 方法的入口：

```text
gui.GUIApplication
```

它是实际的 Swing 服务端控制台，提供启动、定时关闭、保存、重载、公告、账号解卡、修改密码和发放物品等管理操作。点击其中的“启动服务端”后，也会创建同样的 Guice 配置并调用 `ApplicationStarter`。

该 GUI 属于遗留管理入口，当前 CI、Dockerfile 和根目录的 `start-server.bat` 都不会启动它。GUI 中包含高权限数据库和玩家操作，部分反馈还会在界面中显示敏感值；只能在隔离开发环境中评估，不应把它当作已经完成权限隔离和生产验证的管理后台。常规开发和自动化仍以 `MapleStoryApplication` 为主入口。

## 工作目录

工作目录必须是仓库根目录，因为运行时使用相对路径读取：

```text
config/server.properties
wz/
scripts/
logs/ 或其他运行日志目录
```

典型错误是 IDE 把工作目录设成模块目录、`target/` 或 IDE 自己的运行目录，导致配置或 WZ 文件找不到。

## 启动过程

服务端大致按以下顺序初始化：

1. 加载配置和数据库连接；
2. 重置账号登录状态；
3. 加载 WZ、NPC、任务和道具数据；
4. 启动计时器和世界服务；
5. 加载技能、排名和商城数据；
6. 启动登录服务；
7. 启动配置数量的频道服务，但当前实现最多启动 10 个频道；
8. 启动商城服务；
9. 注册自动保存、在线统计和关闭钩子。

成功启动时日志中会出现类似：

```text
服务端启动完毕
```

只看到 Java 进程存在或“服务端启动完毕”消息都不代表全部网络服务可用。登录和频道绑定异常当前会被记录但不一定中止整个启动流程，因此还必须检查错误日志以及登录、每个频道和商城端口。

## 日志位置与凭据风险

运行时可能同时生成：

```text
logs/application.log
logs/...
```

前者来自 Logback，后者来自 `FileoutputUtil`。两者均写入 `logs/`；登录处理器已经移除明文密码日志。迁移前生成的历史日志或备份仍可能含有旧凭据，不要分享或提交整个日志目录。

## 端口检查

默认配置下可检查：

```text
9595            登录服务
7576-7578       默认三个频道
8600            商城服务
```

频道数或基础端口改变后，应按 `server.channel.port + 频道编号` 重新计算。

即使配置校验允许频道数达到 100，运行时代码仍把实际启动数量截断为 10。不要只根据配置文件推断端口数量。

macOS/Linux 可以使用：

```bash
lsof -nP -iTCP:9595 -sTCP:LISTEN
lsof -nP -iTCP:7576 -sTCP:LISTEN
lsof -nP -iTCP:8600 -sTCP:LISTEN
```

Windows 可使用 `Get-NetTCPConnection` 或 `netstat`。端口监听只是启动结果的一部分，仍需结合日志确认数据库和数据加载完成。

## 停止服务端

优先通过 IDE 的正常停止操作触发 JVM 关闭钩子。不要在仍有玩家或写入任务运行时直接强制结束进程，否则可能中断保存。

如果服务端无法正常退出，先保留日志和线程状态用于诊断，再决定是否强制终止。不要把旧 PID 或其他开发者机器上的进程信息写入文档或脚本。

## 编译与测试

开发前可先确认基线：

```bash
./mvnw --batch-mode --no-transfer-progress verify
```

开发过程中优先运行相关测试；准备提交前再运行覆盖面更大的验证。完整说明见[构建与测试](build-and-testing.md)。

## 普通 JAR 与分发包

执行：

```bash
./mvnw clean package
```

会生成普通项目 JAR，但当前 `pom.xml` 没有配置可执行 JAR 的 `Main-Class`，也没有将运行依赖打进同一个 JAR，因此不应直接使用：

```text
java -jar target/ms079.jar
```

需要组合 JAR、依赖、WZ、脚本、数据库和启动脚本时，可以使用 release Profile 生成 Maven assembly 分发包；但当前归档缺少 Compose、Dockerfile 和 PowerShell 文件，也没有经过 CI 端到端启动验证。具体限制见[发布](release.md)。

## Windows 启动脚本说明

当前根目录的：

```text
start-server-console.bat
start-server.bat
```

都调用 Docker Compose，并不是本地 JDK 启动脚本；所谓 GUI 脚本只是在启动后展示 Compose 状态，也不会启动 `gui.GUIApplication`。不要用它们判断 IDE、Swing GUI 或本地 Java 配置是否正确。

`start-server.sh` 面向分发目录的 Java classpath 结构，要求根目录和 `lib/` 已经包含相应 JAR。它不等同于在源码仓库中直接运行 Maven 项目。

## 推荐开发循环

1. 用测试或日志明确预期行为；
2. 修改最小范围的 Java、脚本或数据；
3. 运行相关测试；
4. 需要时启动真实服务端验证；
5. 检查日志、端口和数据库写入；
6. 执行 `git diff --check`；
7. 确认没有配置凭据、日志或构建产物进入差异。

继续阅读：[项目结构](project-structure.md)和[构建与测试](build-and-testing.md)。
