# 数据库与服务端配置

[返回文档目录](README.md)

服务端启动前必须准备数据库、导入仓库中的初始化 SQL，并保证运行环境能够读取根目录下的 `config/server.properties`。

## 初始化数据库

数据库运行基线是 MySQL 5.7，默认 schema 名称为 `ms079`。初始化文件位于：

```text
db/ms079.sql
```

该文件不是只有 `CREATE TABLE` 的空 schema：它是一个历史数据转储，文件头记录的源数据库版本为 MySQL 5.5.53，并包含示例账号、角色、背包、任务、掉落、商城和 WZ 派生表等大量初始记录。CI 会将它完整导入 MySQL 5.7，但这不代表文件中的示例账号或旧业务数据适合直接暴露到共享或公网环境。

导入后应使用隔离环境和测试凭据，并在对外运行前审查、替换或删除示例账号、角色、MAC、地址及其他不应继承的数据。不要把转储中的预置管理员账号视为安全的默认管理员。

该 SQL 会关闭外键检查、执行大量 `DROP TABLE IF EXISTS`、重建表、写入记录并重建视图，最后再恢复外键检查。它不是增量迁移，也不是对已有业务库安全可重跑的初始化脚本。只能导入新建的空 schema；已有数据的数据库必须先做可恢复备份并单独设计迁移，不能直接执行本文件。

使用 MySQL CLI 的一种做法是：

```bash
mysql -u root -p -e "CREATE DATABASE ms079 CHARACTER SET utf8 COLLATE utf8_general_ci;"
mysql --binary-mode=1 -u root -p ms079 < db/ms079.sql
```

这里故意不使用 `IF NOT EXISTS`：如果 `ms079` 已经存在，创建操作应失败并要求开发者确认目标，而不是继续对可能含有数据的 schema 执行破坏性转储。不要把密码直接写进命令历史或文档。远程数据库、Docker 数据库或非默认端口应使用对应客户端参数安全连接。

SQL 文件使用 `SET NAMES utf8mb4`，但历史表显式混用了 `utf8`、`gbk` 和 `latin1` 字符集，以及 InnoDB、MyISAM 和 MEMORY 引擎。创建 schema 时使用 UTF-8 只是默认值，不能覆盖表级定义。不要在没有完整数据回归的情况下批量统一字符集、排序规则或存储引擎。

也可以使用 MySQL Workbench、IDE 数据库工具、Navicat 或其他 SQL 客户端：

1. 连接 MySQL 5.7；
2. 确认目标不是已有业务库，并创建全新的空 `ms079` schema；
3. 将 `ms079` 设为目标 schema；
4. 执行 `db/ms079.sql`；
5. 检查执行过程没有中途终止。

原 README 中的 Navicat 历史下载链接仍保存在[外部资源](external-resources.md)。Navicat 是商业软件，应优先从其官方渠道获取合法版本；MySQL Workbench Community Edition 可以作为官方免费替代。

## 验证导入结果

至少确认 schema 中已经存在大量业务表，而不是只创建了空数据库：

```sql
SELECT COUNT(*)
FROM information_schema.tables
WHERE table_schema = 'ms079';
```

项目 CI 还会读取 `accounts` 和 `characters` 等核心表，并要求初始化后的表数量达到合理下限。如果导入中途失败，应先修复 SQL 错误再启动服务端，不要依靠运行时自动补表。表数量通过也只证明结构和少量核心查询存在，不代表历史数据、账号安全和所有业务关系已经完成审查。

## 配置文件加载方式

应用默认从当前工作目录读取：

```text
config/server.properties
```

可通过 JVM 参数 `-Dms079.config=<路径>` 或环境变量 `MS079_CONFIG_FILE=<路径>` 覆盖默认位置，JVM 参数优先。未覆盖时，本地 Java 运行的工作目录应为仓库根目录；Docker 将文件挂载到 `/app/config/server.properties`。

`config/server.properties` 当前是 Git 跟踪文件。修改数据库账号、密码和地址后，提交前务必检查：

```bash
git diff -- config/server.properties
git status --short
```

不要提交真实生产密码、云数据库地址、令牌或其他秘密。

## 数据库配置

数据库相关键以 `datasource.` 开头：

| 配置 | 用途 |
| --- | --- |
| `datasource.driver` | JDBC 驱动类 |
| `datasource.url` | JDBC 地址、schema、字符集和时区参数 |
| `datasource.username` | 数据库用户 |
| `datasource.password` | 数据库密码 |
| `datasource.maxConnections` | 最大连接数 |
| `datasource.leakTimeMinutes` | 连接泄漏检查时间 |
| `datasource.waitTimeout` | 获取连接的等待时间 |

示例只应使用占位值：

```properties
datasource.url=jdbc:mysql://127.0.0.1:3306/ms079?characterEncoding=UTF-8&useUnicode=true&useSSL=false&serverTimezone=Asia/Shanghai
datasource.username=<数据库用户>
datasource.password=<数据库密码>
```

如果使用当前 Docker Compose，服务端与 `ms079-mysql` 共享网络命名空间，因此数据库地址继续使用 `127.0.0.1:3306`。如果以后改成普通 Compose service 网络，则数据库主机名和网络设计也需要同步调整。

## 服务端基础配置

| 配置 | 说明 |
| --- | --- |
| `server.name` | 世界或服务端显示名称 |
| `server.flag` | 服务端标识 |
| `server.limit.online` | 在线人数上限 |
| `server.limit.characters` | 单账号角色数量限制 |
| `server.address` | 返回给客户端的可访问地址 |
| `server.register.auto` | 是否自动注册账号 |
| `server.rand.drop` | 是否启用随机掉落相关功能 |

`server.address` 必须是客户端实际可以访问的地址。本机联调通常使用 `127.0.0.1`；局域网联调应使用局域网地址。不要在没有网络和安全评估的情况下直接改为公网地址。

`server.register.auto=false` 时，当前服务端日志会提示网页注册和程序注册尚未实现，需要通过受控的 GM 命令或数据库流程创建账号。遗留 Swing GUI 可以解卡账号和修改已有账号密码，但没有创建账号入口。

当前自动注册实现还存在一个重要限制：登录处理器先检查 `server.register.auto`，随后又检查独立的静态字段 `AutoRegister.autoRegister`；后者在类初始化时取自另一个默认值为 `false` 的全局开关，配置加载和 GM 切换命令都没有同步更新它。因此仅把 `server.register.auto` 改成 `true`，日志可能显示“自动注册”，但实际创建账号的分支仍可能不会执行。修复代码并验证前，不应把自动注册描述为可靠可用功能。

账号密码实现也保留了多代兼容逻辑。当前自动注册使用无盐 SHA-1；登录流程兼容旧哈希和盐化 SHA-512，但部分兼容成功路径会把数据库密码更新为无盐 SHA-1。明文登录日志已经移除，但这套认证实现仍不适合承载真实或复用密码。面向真实用户前需要单独完成密码存储迁移和兼容策略，不能只修改配置开关。

## 登录、频道和商城

| 配置 | 说明 |
| --- | --- |
| `server.login.port` | 登录服务监听端口，默认 `9595` |
| `server.login.admin` | 是否只允许管理员登录 |
| `server.login.message` | 登录消息 |
| `server.login.message.event` | 活动消息 |
| `server.channel.port` | 频道基础端口，默认 `7575` |
| `server.channel.count` | 频道数量 |
| `server.mall.port` | 商城服务端口，默认 `8600` |

频道实际端口由“基础端口 + 频道编号”计算。默认基础端口 `7575`、频道数 `3` 时，频道监听在 `7576`、`7577` 和 `7578`，而不是三个频道都监听 `7575`。

配置校验允许 `server.channel.count` 取 `1` 到 `100`，但 `ChannelServer.startChannel_Main` 在运行时会把实际启动数量上限固定为 `10`。因此配置大于 `10` 时，不会按配置数量创建全部频道；端口发布、客户端守护和容量规划都应以实际最多 10 个频道为准。这个校验范围与运行上限目前并不一致。

## 世界倍率和刷新参数

| 配置前缀或名称 | 说明 |
| --- | --- |
| `server.world.rate.exp` | 经验倍率 |
| `server.world.rate.gold` | 金币倍率 |
| `server.world.rate.drop` | 普通掉落倍率 |
| `server.world.rate.drop.boss` | Boss 掉落倍率 |
| `server.world.rate.cash` | 点券倍率 |
| `server.world.mob-respawn-interval` | 普通怪物刷新间隔，单位毫秒 |
| `server.world.mob-density-multiplier` | 怪物密度倍率 |
| `server.world.flags` | 世界标志位 |

源码会限制部分参数范围。例如怪物刷新间隔不会低于安全下限，密度倍率也会被限定在代码允许的区间。配置值异常时应先检查源码中的解析和约束，不要只看配置文件文本。

## 职业、事件和商城开关

| 配置 | 说明 |
| --- | --- |
| `server.job.adventurer` | 冒险家职业开关 |
| `server.job.knights` | 骑士团职业开关 |
| `server.job.war-god` | 战神职业开关 |
| `server.events` | 启用的事件列表 |
| `server.mall.disabled` | 商城禁用项列表 |
| `server.cashjy` | 项目历史业务配置 |
| `server.gysj` | 项目历史业务配置 |

这些字段通过简单的 `split(",")` 解析，不会自动去除空格或统一去重。事件管理器最终用脚本名作为 Map 键，重复事件名会覆盖先前条目；其他历史列表则可能继续保留重复值。当前跟踪配置中已经存在重复事件名和重复商城屏蔽 ID，而仓库数据测试只检查重复配置键，不检查列表内部重复项。调整列表时应先规范化目标项，并检查 `scripts/` 中是否存在对应实现。

配置文件还包含以 `;` 开头的历史说明行。仓库数据测试把 `;` 当作注释，但 Java `Properties.load` 正式识别的注释前缀是 `#` 和 `!`，所以运行时会把部分 `;` 行解析为未使用的额外属性。新增说明应使用 `#` 或 `!`，在修复解析方式前不要依赖 `;` 注释具有标准 Java Properties 语义。

## 调试和日志

| 配置 | 说明 |
| --- | --- |
| `server.debug.enabled` | 调试模式 |
| `server.logger.packet` | 数据包日志 |
| `server.logger.packet.debug` | 更详细的数据包调试日志 |

项目同时存在两套日志路径：

- Logback 写入 `logs/application.log`，并按日期滚动压缩；
- `FileoutputUtil` 的分类日志也写入 `logs/` 下。

登录成功流程已经移除账号和明文密码日志。迁移前生成的历史日志或备份仍可能包含旧凭据；应限制访问、禁止提交或公开分享，并轮换其中出现过的真实或复用密码。

数据包和调试日志还可能快速增长并包含账号、角色、会话或完整数据包信息。只在隔离开发环境中按需启用，问题处理完成后及时关闭。

## 启动前检查

- MySQL 5.7 正在运行；
- `ms079` schema 已完整导入；
- schema 在导入前为空，未对已有业务数据直接执行历史转储；
- JDBC 地址和凭据有效；
- `config/server.properties` 位于运行工作目录；
- `server.address` 对目标客户端可达；
- 登录、频道和商城端口未被占用；
- `wz/` 和 `scripts/` 存在且可读；
- 只使用可丢弃的测试账号，并审查历史日志中可能残留的旧凭据；
- 已审查 SQL 转储中的示例账号、角色和其他初始数据；
- Git 差异中没有真实凭据。

下一步可选择：[本地 Java 开发](local-development.md)或[Docker 运行](docker.md)。
