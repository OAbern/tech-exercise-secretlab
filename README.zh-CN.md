# Tech Exercise Secretlab

这是一个基于 Spring Boot 的版本化 Key-Value 存储服务。服务允许客户端写入任意 JSON 值，并为同一个 key 的每次写入生成递增版本，同时保留历史记录，支持读取最新值、按写入时间戳读取历史值，以及列出所有 key 的最新值。

## 主要功能

- 写入单个 key-value：`POST /object`，请求体必须只包含一个 key，value 可以是字符串、数字、数组或对象等任意非 null JSON 值。
- 读取最新值：`GET /object/{key}`，直接返回该 key 最新保存的 JSON 值。
- 读取历史值：`GET /object/{key}?timestamp={epochSecond}`，按 Unix epoch 秒查询对应时间戳写入的历史值。
- 获取所有最新记录：`GET /object/get_all_records`，返回每个 key 当前最新值的列表。
- 参数校验和异常处理：无效请求返回 `400`，响应体包含统一错误信息；未预期异常返回 `500`。
- 并发写入保护：同一个 key 的并发写入会被串行化，版本号保持连续递增。

## 技术栈

- Java 21
- Spring Boot 3.4.5：Spring Web、Spring Data JPA、Spring Validation、Spring AOP、Thymeleaf
- H2 Database：本地和测试环境使用的内存数据库
- Lombok 和 MapStruct：减少样板代码并处理对象转换
- Gradle Kotlin DSL：项目构建和任务管理
- JUnit 5、Spring Boot Test、JaCoCo：单元/集成测试与覆盖率统计
- Spotless、SpotBugs、SonarQube：代码格式、静态分析和质量检查
- Docker、GitHub Actions、AWS ECR、EC2：容器化构建与自动部署

## API 示例

写入：

```sh
curl -X POST http://localhost:8080/object \
  -H 'Content-Type: application/json' \
  -d '{"hero": {"name": "batman", "level": 7}}'
```

响应示例：

```json
{
  "key": "hero",
  "value": {
    "name": "batman",
    "level": 7
  },
  "version": 1,
  "timestamp": 1710000000
}
```

读取最新值：

```sh
curl http://localhost:8080/object/hero
```

读取指定写入时间戳的历史值：

```sh
curl 'http://localhost:8080/object/hero?timestamp=1710000000'
```

列出所有 key 的最新值：

```sh
curl http://localhost:8080/object/get_all_records
```

## 设计思路

项目采用典型 Spring Boot 分层结构：

- `controller` 负责 HTTP 请求、参数校验和响应组装。
- `service` 承载写入版本、读取最新值、读取历史值等业务逻辑。
- `repository` 使用 Spring Data JPA 访问数据库。
- `model` 定义 JPA Entity、DTO 和 VO。
- `common` 放置 JSON 转换和校验等无状态工具。
- `config/aspect` 处理全局异常和日志切面。

数据模型拆成两张表：

- `key_value`：每个 key 一行，保存当前版本号、最新值和更新时间。它也是同 key 并发写入时的悲观锁锚点，并让读取最新值不需要扫描历史表。
- `key_value_history`：追加式历史表，每次写入都会新增一行，保存 key、版本号、JSON 值和创建时间戳。历史记录不会被更新或删除。

写入流程：

1. Controller 校验请求体必须只有一个 key-value，key 不能为空，value 不能为 null。
2. Service 在事务中按 key 查询 `key_value` 并加 `PESSIMISTIC_WRITE` 锁。
3. 如果 key 已存在，递增版本号并更新最新值；如果 key 不存在，创建版本 1。
4. 写入一条新的 `key_value_history` 历史记录。
5. 返回本次写入的 key、value、version 和 timestamp。

并发控制依赖数据库行级悲观锁。同一个 key 的并发写入会在 `key_value` 行上排队，后进入的事务会读取已提交的新版本号，因此版本号可以保持顺序递增。

## 本地运行

项目使用 Java 21 和 Gradle Wrapper。

启动服务：

```sh
./gradlew bootRun
```

默认端口是 `8080`，H2 内存数据库控制台地址是：

```text
http://localhost:8080/h2-console
```

数据库 schema 由 `src/main/resources/schema.sql` 管理，应用启动时会自动初始化。

## 校验与质量门禁

运行完整校验：

```sh
./gradlew check
```

`check` 会覆盖测试、格式检查、SpotBugs 和 JaCoCo 覆盖率校验。当前覆盖率门槛配置为 90%。

自动格式化：

```sh
./gradlew spotlessApply
```

生成 JaCoCo 报告：

```sh
./gradlew jacocoTestReport
```

## 自动部署

仓库通过 GitHub Actions 提供自动 CD 流程，配置文件位于：

```text
.github/workflows/deploy-aws.yml
```

当代码 `git push` 到 `main` 分支后，工作流会自动执行以下步骤：

1. 使用 Java 21 运行 `./gradlew clean build` 构建应用。
2. 构建 Docker 镜像并以 `latest` 标签推送到 AWS ECR 仓库。
3. 通过 SSH 登录 EC2，拉取最新镜像，停止并删除旧容器，然后启动新容器。

该流程依赖 GitHub Secrets 中配置的 `AWS_ACCESS_KEY_ID`、`AWS_SECRET_ACCESS_KEY`、`EC2_SSH_KEY` 和 `EC2_HOST`。本地推送前仍会先经过 `pre-push` hook 的质量检查；只有成功推送到 `main` 后才会触发远程自动部署。

## Git Hook

仓库提供了版本化的 `pre-push` hook：

```text
config/git-hooks/pre-push
```

安装 hook：

```sh
./gradlew installGitHooks
```

安装后，每次 `git push` 前都会执行：

```sh
./gradlew check
```

如果 `check` 失败，`git push` 会被 Git 中止。注意 Git 不会在 clone 时自动安装 hooks，所以每个开发者需要在本地运行一次安装命令。远程仓库侧仍建议配合 CI 和分支保护，确保合并前也必须通过同样的校验。
