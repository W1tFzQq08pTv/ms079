# Repository Guidelines

## Project Structure & Module Organization

This is a Java 8 MapleStory v079 server built with Maven. Production code lives in `src/main/java`: `com.github.mrzhqiang.maplestory` contains application, configuration, domain, and WZ layers; `server`, `handling`, and `client` contain gameplay, protocol, and session logic; `scripting` loads game scripts. Resources are in `src/main/resources`; tests and fixtures are under `src/test`.

Repository data is part of the application: `wz/` stores WZ XML, `脚本/` stores JavaScript game logic, `db/ms079.sql` initializes MySQL, and `服务端配置.ini` controls runtime settings. `old-files/` is archival. See `docs/` for architecture and operations notes.

## Build, Test, and Development Commands

- `./mvnw clean compile` — compile with the required Java 8 toolchain.
- `./mvnw test` — run the JUnit 4 suite.
- `./mvnw --batch-mode --no-transfer-progress verify` — reproduce the main Maven CI check.
- `./mvnw -Dtest=MapleKeyLayoutTest test` — run one test class.
- `./mvnw -Dtest=RepositoryDataValidationTest test` — validate all scripts, WZ XML, and server configuration.
- `./mvnw -Prelease clean package` — create the full assembly distribution.
- `docker compose up -d --build ms079-server` — build and start the server against the existing `ms079-mysql` container.

## Coding Style & Naming Conventions

Use UTF-8, four-space indentation, and Java 8-compatible syntax. Follow existing conventions: PascalCase classes, camelCase methods and fields, `UPPER_SNAKE_CASE` constants, and packages matching directory paths. There is no repository-wide autoformatter, so avoid unrelated reformatting. Name tests `<Subject>Test` and methods after observable behavior, for example `persistsRemovingTheLastKey`.

## Testing Guidelines

Add focused JUnit 4 tests beside the affected package. Unit tests use H2 where possible; database, Docker, network, WZ, or script changes may require MySQL 5.7 and startup smoke validation. At minimum, run the closest test plus `git diff --check`. Do not claim `verify` proves a complete game flow.

## Commit & Pull Request Guidelines

History favors concise subjects such as `ci：增加标签源码发布工作流`, `docs：...`, or `fix: ...`. Use an imperative summary with a clear type or scope; keep commits cohesive. PRs should explain behavior, risk, and validation; link issues, call out database/configuration changes, and include screenshots for Swing GUI changes. Never commit credentials, logs, `target/`, archives, or IDE files.

## Security & Configuration

Use isolated development credentials. `服务端配置.ini`, generated logs, and legacy login logging may expose sensitive values; redact them from issues and PRs. Follow `SECURITY.md` for private vulnerability reporting.
