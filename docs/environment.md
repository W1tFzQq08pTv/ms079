# 环境准备

[返回文档目录](README.md)

ms079 同时支持本地 Java 开发和 Docker 运行。两种方式共享 MySQL、WZ 数据、JavaScript 脚本和服务端配置，但对宿主机 Java 环境的要求不同。

## 环境基线

| 组件 | 当前基线 | 本地 Java 开发 | Docker 运行 |
| --- | --- | --- | --- |
| JDK | Java 8 | 必需 | 宿主机不需要 |
| Maven | Maven Wrapper | 使用仓库脚本 | 镜像构建时使用 |
| MySQL | 5.7 | 必需 | 必须提前准备外部容器 |
| Git | 当前版本即可 | 推荐 | 推荐 |
| Docker Engine / Desktop | 支持 Compose | 可选 | 必需 |
| IDE | 支持 Maven 的 Java IDE | 推荐 | 可选 |

`pom.xml` 的源代码和目标字节码版本均为 Java 8，CI 也使用 Temurin 8 执行 `mvn verify`。不要因为本机已安装更高版本 JDK 就默认项目兼容现代 Java。

## 检查 Java

本地开发前执行：

```bash
java -version
./mvnw -version
```

Windows 使用：

```powershell
java -version
mvnw.cmd -version
```

应确认 Maven 实际使用的是 Java 8。机器上同时安装多个 JDK 时，IDE 的 Project SDK、Maven Runner JDK 和终端中的 `JAVA_HOME` 可能不是同一个版本，需要分别检查。

官方获取入口：

- [Eclipse Temurin 8](https://adoptium.net/temurin/releases/?version=8)
- [IntelliJ IDEA](https://www.jetbrains.com/idea/download/)

原 README 还提供过 JDK 8 历史短链，已保留在[外部资源](external-resources.md)中。该短链不是官方发行渠道，当前也无法自动确认其目标内容。

## 使用 Maven Wrapper

仓库包含：

```text
mvnw
mvnw.cmd
.mvn/wrapper/maven-wrapper.properties
```

因此不要求开发者单独安装固定版本 Maven。首次执行 Wrapper 时可能需要联网下载指定 Maven 发行包和项目依赖。

常见命令：

```bash
./mvnw clean compile
./mvnw test
./mvnw verify
```

Windows 将 `./mvnw` 替换为 `mvnw.cmd`。Maven Wrapper 的行为和下载机制可参考 [Apache Maven Wrapper 官方文档](https://maven.apache.org/tools/wrapper/)。

## 准备 MySQL 5.7

当前运行和 CI 验证基线是 MySQL 5.7。仓库中的 `db/ms079.sql` 文件头显示它最初由 MySQL 5.5.53 导出，之后由 CI 在 MySQL 5.7 中做完整导入和服务端启动验证。MySQL 5.7 已属于旧版本，应从官方归档或可信镜像获取，并只用于受控开发环境。

- [MySQL Community Server 5.7 官方归档](https://downloads.mysql.com/archives/community/?version=5.7)
- [MySQL 5.7 Reference Manual](https://dev.mysql.com/doc/refman/5.7/en/)
- [MySQL Workbench](https://dev.mysql.com/downloads/workbench/)

项目使用 MySQL Connector/J 8.0.25，但这不等于数据库服务端已经验证支持 MySQL 8。初始化 SQL、旧表结构和运行行为仍以 MySQL 5.7 为当前基线。

原 README 中的 MySQL 5.7 网盘链接及提取码也保留在[外部资源](external-resources.md)中，但应优先使用官方归档。

## 准备 Docker

Docker 方式需要 Docker Engine 和 Compose。Docker Desktop 已包含常用的 Engine、CLI 和 Compose 功能：

- [Docker Desktop](https://docs.docker.com/desktop/)
- [Docker Engine 安装文档](https://docs.docker.com/engine/install/)

安装后检查：

```bash
docker version
docker compose version
```

项目运行 Linux 容器。Windows Docker Desktop 应切换到 Linux 容器环境。Apple Silicon 或其他 ARM64 主机运行 MySQL 5.7 时可能需要 `--platform linux/amd64`，因为旧版 MySQL 镜像的架构支持有限；项目 CI 也显式使用了 `linux/amd64`。

## IDE 和文件编码

推荐使用支持 Maven 的 Java IDE，例如 IntelliJ IDEA，但项目不依赖某个特定版本或付费版本的 IDE。

导入时注意：

- 从根目录的 `pom.xml` 导入 Maven 项目；
- Project SDK 和 Maven Runner 均选择 JDK 8；
- Java 源码、Maven 构建和仓库数据校验以 UTF-8 为基线；
- `ServerConfiguration` 当前通过 `FileReader` 使用 JVM 默认字符集读取 `服务端配置.ini`，本地运行应显式使用 `-Dfile.encoding=UTF-8` 或确认系统默认字符集是 UTF-8；
- 不要批量改变 `wz/`、`脚本/`、SQL 或 Java 文件的编码和换行符；
- Windows 上应保留仓库中的中文文件名和目录名。

数据库初始化可以使用 MySQL CLI、MySQL Workbench、IDE 数据库工具或其他合法数据库客户端。工具只负责执行 SQL，不改变数据库版本和字符集要求。

## 已验证与未验证的边界

当前仓库自动化覆盖：

- Java 8 编译、测试和 `verify`；
- WZ XML、JavaScript 脚本和服务端配置校验；
- Docker 镜像构建；
- MySQL 5.7 初始化和服务端启动冒烟测试。

以下组合不应在没有实测时描述为受支持：

- JDK 7；
- JDK 11、17、21 或更高版本；
- MySQL 8 或 MariaDB；
- 不同协议版本的 MapleStory 客户端；
- 将 WZ 或脚本目录移动到任意路径而不调整启动参数和代码。

下一步阅读：[数据库与服务端配置](database-and-configuration.md)。
