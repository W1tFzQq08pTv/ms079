# 外部资源

[返回文档目录](README.md)

本页集中保存构建、运行和客户端联调涉及的外部链接。官方来源应优先使用；原 README 中的历史短链、网盘地址和提取码按要求保留，但无法自动确认其当前内容，不代表仓库对第三方文件作出安全、授权或可用性保证。

链接状态检查日期：2026-08-06。

## 状态说明

| 状态 | 含义 |
| --- | --- |
| 官方 | 链接指向项目或厂商官方站点 |
| 已访问 | 本次整理时可以打开页面，不代表其中所有历史版本始终可下载 |
| 未验证 | 自动检查无法打开、确认跳转目标或验证文件内容 |
| 历史第三方 | 来自旧 README 的非官方链接，保留用于追溯 |

外部链接随时可能变化。下载二进制后应使用官方签名或发布方提供的哈希校验；没有校验值时应明确记录来源和风险。

## Java 8

| 资源 | 链接 | 来源与状态 | 说明 |
| --- | --- | --- | --- |
| Eclipse Temurin 8 | [官方下载页面](https://adoptium.net/temurin/releases/?version=8) | 官方，已访问 | 本地开发推荐来源，选择 JDK 8 |
| 原 README JDK 8 短链 | [https://alywp.net/5whNJG](https://alywp.net/5whNJG) | 历史第三方，未验证 | 原链接没有提取码；无法确认当前跳转目标 |

本地 Java 开发需要 JDK 8；只使用 Docker 运行服务端时，宿主机不需要安装 JDK。

## MySQL 5.7

| 资源 | 链接 | 来源与状态 | 说明 |
| --- | --- | --- | --- |
| MySQL Community Server 5.7 | [官方归档](https://downloads.mysql.com/archives/community/?version=5.7) | 官方，已访问 | 5.7 已是旧版本，应从归档选择适用平台 |
| MySQL 5.7 手册 | [官方文档](https://dev.mysql.com/doc/refman/5.7/en/) | 官方 | 数据库行为和 SQL 参考 |
| 原 README MySQL 5.7.30+ | [百度网盘](https://pan.baidu.com/s/1v-2jXg9xqNmo5ww5YjUhQQ) | 历史第三方，未验证 | 提取码：`6ifn` |

当前项目以 MySQL 5.7 为运行基线。不要因为 JDBC 驱动版本较新就默认切换到 MySQL 8。

## IDE、Maven 和数据库客户端

| 资源 | 链接 | 来源与状态 | 说明 |
| --- | --- | --- | --- |
| IntelliJ IDEA | [官方下载](https://www.jetbrains.com/idea/download/) | 官方，已访问 | 可选；其他支持 Maven 的 Java IDE 也可 |
| Apache Maven Wrapper | [官方文档](https://maven.apache.org/tools/wrapper/) | 官方，已访问 | 仓库已提供 `mvnw`，无需另装固定 Maven |
| Apache Maven | [官方下载](https://maven.apache.org/download.cgi) | 官方 | 仅了解 Maven 本身时使用 |
| MySQL Workbench | [官方下载](https://dev.mysql.com/downloads/workbench/) | 官方，已访问 | 可用于导入 `db/ms079.sql` |
| Navicat | [官方网站](https://www.navicat.com/) | 官方 | 商业软件，应使用合法授权版本 |
| 原 README Navicat Premium 15 | [百度网盘](https://pan.baidu.com/s/1kZwb2ZdOjf5ZG_HPkWtwWQ) | 历史第三方，未验证 | 提取码：`j6lt`；无法确认版本、授权和文件完整性 |

数据库工具只是 SQL 客户端。无论使用哪种工具，都应创建 `ms079` schema 并完整导入仓库中的 `db/ms079.sql`。

## Docker

| 资源 | 链接 | 来源与状态 | 说明 |
| --- | --- | --- | --- |
| Docker Desktop | [官方文档与下载入口](https://docs.docker.com/desktop/) | 官方，已访问 | Windows/macOS 常用安装方式，包含 Compose |
| Docker Engine | [官方安装文档](https://docs.docker.com/engine/install/) | 官方，已访问 | Linux 可按发行版安装 |

Docker 方式仍然需要准备外部 MySQL 5.7 容器。详见 [Docker 运行](docker.md)。

## v079 客户端与兼容资源

以下内容来自原 README，用于保留历史联调入口。它们不是项目 Maven 依赖，也不在 CI 中下载或验证。

| 资源 | 链接 | 来源与状态 | 说明 |
| --- | --- | --- | --- |
| 冒险岛 v079 客户端 | [https://alywp.net/2bBtbJ](https://alywp.net/2bBtbJ) | 历史第三方，未验证 | 原 README 未提供提取码；无法确认当前跳转目标和客户端哈希 |
| “079 私服过 HS 文件” | [百度网盘](https://pan.baidu.com/s/1gAOhxhwxd1T4bqX8HSoFNQ) | 历史第三方，未验证 | 提取码：`7i0u`；用于原 README 所述 HShield 替换流程 |

使用第三方客户端或兼容文件前建议：

1. 在隔离环境中下载和扫描；
2. 记录最终下载 URL、获取日期和 SHA-256；
3. 不使用日常账号或含敏感资料的系统直接运行未知程序；
4. 确认文件与 v079 协议及当前登录器匹配；
5. 不把客户端和第三方二进制提交到本仓库；
6. 不把“文档保留链接”理解为维护者对文件来源和授权的背书。

客户端启动、守护和端口要求见[客户端与兼容性](client-and-compatibility.md)。

## 原 README 链接保留清单

为便于核对，原 README 的 7 个引用链接均已迁移：

| 原编号 | 资源 | 新文档状态 |
| --- | --- | --- |
| `[1]` | JDK 8 短链 | 已保留，未验证 |
| `[2]` | IntelliJ IDEA | 已替换为同一官方入口并保留 |
| `[3]` | MySQL 5.7 网盘 | 已保留，含提取码 |
| `[4]` | Apache Maven | 已保留官方入口，并补充 Wrapper 文档 |
| `[5]` | Navicat Premium 15 网盘 | 已保留，含提取码和授权风险说明 |
| `[6]` | v079 客户端短链 | 已保留，未验证 |
| `[7]` | HShield 兼容文件网盘 | 已保留，含提取码和安全风险说明 |

## 后续维护规则

- 新增依赖时优先记录官方主页、版本和许可证；
- 第三方镜像必须同时保留原始项目来源；
- 不把数据库密码、网盘账号、Cookie 或私有下载令牌写入文档；
- 链接失效时移到历史区并注明日期，不要无记录地替换成来源不明的新文件；
- 有官方校验值时记录 SHA-256 或签名验证方式；
- 客户端和 HShield 等第三方资源的哈希应按实际下载文件记录，不能根据文件名猜测。
