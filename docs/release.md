# 发布

[返回文档目录](README.md)

项目存在两类不同产物：Maven assembly 生成的分发归档，以及由 Git 标签触发的源码归档。两者不要混淆。当前 CI 只验证 Docker/MySQL 启动，不对 Maven assembly 归档做端到端启动测试，因此不能直接把它称为已经验证的“一键运行包”。

## 普通 Maven JAR

执行：

```bash
./mvnw clean package
```

生成：

```text
target/ms079.jar
```

该 JAR 不是自包含可执行 JAR：

- 没有在 manifest 中配置当前应用入口；
- 运行依赖没有合并进同一个文件；
- WZ、脚本、数据库和配置不在 JAR 内。

因此普通 JAR 更适合作为分发包的一部分，不能仅使用 `java -jar target/ms079.jar` 运行完整服务端。

## Maven assembly 分发包

执行：

```bash
./mvnw -Prelease clean package
```

Windows：

```powershell
mvnw.cmd -Prelease clean package
```

当前项目版本为 `1.0-SNAPSHOT` 时，release Profile 生成：

```text
target/ms079-1.0-SNAPSHOT-dist.zip
target/ms079-1.0-SNAPSHOT-dist.tar.gz
```

分发包由 `src/main/assembly/assembly.xml` 定义，包含：

- 根目录 README、LICENSE/NOTICE（如果仓库中存在）；
- BAT、Shell 和 INI 文件；
- `target` 中的 JAR；
- `lib/` 下的运行依赖；
- `wz/`；
- `脚本/`；
- `db/*.sql`。

当前仓库没有顶层 `LICENSE` 文件，所以 assembly 中的 `LICENSE*` 模式不会凭空生成许可证。不要把原 README 的用途声明当成正式许可证。

当前 descriptor **不包含**：

- `Dockerfile`；
- `compose.yaml`；
- `docs/`；
- `*.ps1`，包括客户端守护和紧急停止使用的 PowerShell 实现；
- `.github/` 下的 CI 启动脚本；
- `SECURITY.md`，因为它不匹配 `README*`、`LICENSE*` 或 `NOTICE*`。

这还会产生一个实际限制：归档虽然包含 `启动服务端-命令行.bat` 和 `启动服务端-GUI.bat`，但这两个 BAT 当前都调用 Docker Compose，而归档没有包含 `compose.yaml` 和 `Dockerfile`，所以它们不能在一个独立解压目录中直接工作。`启动服务端-命令行.sh` 使用 `ms079.jar` 与 `lib/` classpath，更接近该 assembly 的目录结构，但它也没有被 CI 在解压后的归档中验证。

由于 `docs/` 也不在 descriptor 的 include 列表中，Maven assembly 当前只带根 README，不会附带本目录的新开发文档。标签源码包则会包含提交中正常跟踪的 `docs/`。如果未来希望运行分发包自带新文档，需要单独调整 assembly 并验证归档内容。

另外，assembly 会原样打入根目录的 `服务端配置.ini` 和完整 `db/ms079.sql`。前者可能包含本地数据库凭据，后者包含示例账号、角色和大量历史业务数据。生成归档成功不代表这些内容适合公开发布。

## 验证分发包

生成后至少检查：

```bash
unzip -l target/ms079-1.0-SNAPSHOT-dist.zip
tar -tzf target/ms079-1.0-SNAPSHOT-dist.tar.gz
```

重点确认：

- `ms079.jar` 存在；
- `lib/` 中有运行依赖；
- `wz/` 和 `脚本/` 不是空目录；
- `db/ms079.sql` 存在；
- `服务端配置.ini` 存在；
- Shell 和 BAT 启动脚本存在，并已确认其依赖文件是否被一同打包；
- 没有打入 `.git`、IDE 配置、日志或本地秘密。

真正发布前还应：

1. 在全新目录解压；
2. 审查或替换 `服务端配置.ini`；
3. 审查 SQL 转储中的示例账号和数据；
4. 根据目标启动方式补齐 Compose/Dockerfile 或使用并修正 Shell 启动路径；
5. 使用隔离数据库做真实启动验证；
6. 确认 PowerShell 客户端辅助脚本是否需要另行分发。

这样可以避免构建机上残留的 `target/`、Compose 文件或本地依赖掩盖归档缺失内容。

## 源码标签发布

`.github/workflows/source-release.yml` 在推送 `v*` 标签时运行，并进一步校验标签格式。支持示例：

```text
v1.0.0
v1.0.0-rc.1
v1.0.0+build.1
```

标签指向的提交必须已经包含在远端 `main` 中。工作流会拒绝尚未进入 `main` 的标签提交，避免从临时分支意外发布源码。

## 源码归档产物

以 `v1.0.0` 为例，工作流通过 `git archive` 生成：

```text
ms079-v1.0.0-source.tar.gz
ms079-v1.0.0-source.zip
SHA256SUMS
```

源码包只包含标签提交中的 Git 跟踪文件：

- 不包含 `.git` 目录；
- 不包含未跟踪文件；
- 不包含开发者本地工作区中尚未提交的修改；
- 会包含标签提交中正常跟踪的 WZ、脚本、SQL 和其他文件。

工作流会测试 TAR.GZ 和 ZIP 是否可读取，并为两个归档生成 SHA-256 清单。

因为 `服务端配置.ini` 当前是 Git 跟踪文件，源码归档也会包含标签提交中的该文件；如果提交中存在真实凭据，它们会直接进入公开资产。标签发布前不能只检查未跟踪文件，还必须审查已提交配置及其历史。

## GitHub Release 行为

- 如果标签对应的 GitHub Release 不存在，工作流创建 Release；
- 带 `-` 后缀的版本标签会标记为 prerelease；
- 如果 Release 已存在，工作流会覆盖上传同名资产；
- Release notes 由 GitHub 自动生成；
- 工作流使用标签提交作为归档来源。

## 发布前检查清单

1. 版本和标签名称已确认；
2. 目标提交已经进入远端 `main`；
3. Java 8 `verify` 通过；
4. WZ、脚本和配置校验通过；
5. Docker/MySQL 启动冒烟通过；
6. assembly 分发包在干净目录中可以解压，并针对选定启动方式补齐依赖后完成启动；
7. 已审查会被打包的 `服务端配置.ini` 和 `db/ms079.sql`；
8. 没有数据库密码、令牌、日志或本地配置进入提交和归档；
9. `SECURITY.md` 和外部资源说明仍然准确；
10. 已确认仓库级许可证和第三方资源授权边界；
11. 标签创建和推送已经获得明确授权。

创建标签、推送和发布都是 Git 写操作或远端操作，不应因为文档已经准备好就自动执行。

## 分发包与源码包的区别

| 项目 | Maven assembly 分发包 | 标签源码包 |
| --- | --- | --- |
| 生成入口 | Maven `release` Profile | GitHub Actions 标签工作流 |
| 主要用途 | 组合 JAR、依赖和运行数据；仍需补齐并验证启动方式 | 保存标签对应源码快照 |
| 编译产物 | 包含 JAR 和 `lib/` | 不包含未跟踪构建产物 |
| WZ/脚本/SQL | 包含 | 标签提交跟踪时包含 |
| 校验 | 当前 CI 未做归档端到端启动验证 | 归档可读性和 `SHA256SUMS` |

继续阅读：[构建与测试](build-and-testing.md)。
