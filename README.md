# Tech Exercise Secretlab

[中文版本](README.zh-CN.md)

This is a version-controlled key-value store built with Spring Boot. It accepts arbitrary JSON values, creates an incrementing version for every write to the same key, keeps append-only history, and supports reading the latest value, reading a historical value by write timestamp, and listing the latest value for every key.

## Features

- Store one key-value pair: `POST /object`. The request body must contain exactly one key. The value can be any non-null JSON value, including a string, number, array, or object.
- Read the latest value: `GET /object/{key}` returns the latest JSON value for the key directly.
- Read a historical value: `GET /object/{key}?timestamp={epochSecond}` returns the value written at the given Unix epoch second.
- List all latest records: `GET /object/get_all_records` returns the current latest value for every key.
- Validate requests consistently: invalid input returns `400` with a structured error response; unexpected failures return `500`.
- Protect concurrent writes: concurrent writes to the same key are serialized so version numbers remain sequential.

## Tech Stack

- Java 21
- Spring Boot 3.4.5: Spring Web, Spring Data JPA, Spring Validation, Spring AOP, Thymeleaf
- H2 Database: in-memory database for local development and tests
- Lombok and MapStruct: reduce boilerplate and handle object mapping
- Gradle Kotlin DSL: project build and task management
- JUnit 5, Spring Boot Test, JaCoCo: unit/integration testing and coverage reporting
- Spotless, SpotBugs, SonarQube: formatting, static analysis, and quality checks
- Docker, GitHub Actions, AWS ECR, EC2: containerized build and automated deployment

## API Examples

Store a value:

```sh
curl -X POST http://localhost:8080/object \
  -H 'Content-Type: application/json' \
  -d '{"hero": {"name": "batman", "level": 7}}'
```

Example response:

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

Read the latest value:

```sh
curl http://localhost:8080/object/hero
```

Read the value written at a specific timestamp:

```sh
curl 'http://localhost:8080/object/hero?timestamp=1710000000'
```

List the latest value for every key:

```sh
curl http://localhost:8080/object/get_all_records
```

## Design

The project follows a conventional Spring Boot layered structure:

- `controller` handles HTTP requests, validation, and response assembly.
- `service` contains the business logic for versioned writes and reads.
- `repository` uses Spring Data JPA for database access.
- `model` contains JPA entities, DTOs, and VOs.
- `common` contains stateless helpers for JSON conversion and validation.
- `config/aspect` contains cross-cutting concerns such as global exception handling and request logging.

The storage model uses two tables:

- `key_value`: one row per key. It stores the current version, latest value, and latest update timestamp. It also acts as the pessimistic-lock anchor for concurrent writes to the same key, and makes latest-value reads fast.
- `key_value_history`: an append-only history table. Every write creates one row with the key, version, JSON value, and creation timestamp. History rows are never updated or deleted.

Write flow:

1. The controller validates that the request body contains exactly one key-value pair, the key is not blank, and the value is not null.
2. The service opens a transaction and queries `key_value` with a `PESSIMISTIC_WRITE` lock for that key.
3. If the key exists, the service increments the version and updates the latest value. If the key does not exist, it creates version 1.
4. The service appends a new row to `key_value_history`.
5. The API returns the key, value, version, and timestamp for the write.

Concurrency is handled with a database row-level pessimistic lock. Writes to the same key queue on the `key_value` row, and each transaction reads the latest committed version before incrementing it.

## Running Locally

The project uses Java 21 and the Gradle Wrapper.

Start the service:

```sh
./gradlew bootRun
```

The default server port is `8080`. The H2 in-memory database console is available at:

```text
http://localhost:8080/h2-console
```

The database schema is managed by `src/main/resources/schema.sql` and is initialized automatically on startup.

## Checks And Quality Gates

Run the full verification suite:

```sh
./gradlew check
```

`check` covers tests, formatting checks, SpotBugs, and JaCoCo coverage verification. The current coverage threshold is 90%.

Apply formatting automatically:

```sh
./gradlew spotlessApply
```

Generate the JaCoCo report:

```sh
./gradlew jacocoTestReport
```

## Automated Deployment

The repository includes a GitHub Actions CD workflow:

```text
.github/workflows/deploy-aws.yml
```

When code is pushed to the `main` branch, the workflow runs automatically and:

1. Builds the application with Java 21 and `./gradlew clean build`.
2. Builds a Docker image and pushes it to AWS ECR with the `latest` tag.
3. Connects to EC2 over SSH, pulls the latest image, stops and removes the old container, and starts a new container.

The workflow depends on the `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `EC2_SSH_KEY`, and `EC2_HOST` GitHub Secrets. Local pushes still go through the `pre-push` quality gate first; the remote deployment starts only after a successful push to `main`.

## Git Hook

The repository includes a versioned `pre-push` hook:

```text
config/git-hooks/pre-push
```

Install it locally:

```sh
./gradlew installGitHooks
```

After installation, every `git push` runs:

```sh
./gradlew check
```

If `check` fails, Git stops the push. Git does not install hooks automatically when a repository is cloned, so every developer needs to run the install command once locally. For remote enforcement, pair this local hook with CI and branch protection so merges also require the same checks to pass.
