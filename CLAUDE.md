# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Purpose

`bay-pcm-job` is a thin Java CLI that sits between an **ESP (scheduler) process** and **SQL Server stored procedures**. The runtime chain is:

```
ESP job  →  shell script (Shell_scripts/<ENV>/*.sh)  →  java -jar bay-pcm-job.jar  →  EXEC dbo.<procedure>  →  optional email + exit code
```

The JAR is intentionally minimal: it opens a JDBC connection, executes a single stored procedure, optionally emails any `RESULT`/`RESULT2` rows the procedure returns, then derives an exit code from `batch_Log_ProcessLoad.Processed_Status`. All real work lives in the stored procedures — this project is the dispatch glue.

## Companion Docs

- **[README.md](./README.md)** — public-facing overview, tech stack, quick run example.
- **[DEPLOY.md](./DEPLOY.md)** — full Eclipse Export-Runnable-JAR procedure, Linux server requirements (Java 21+, network, file layout, permissions), troubleshooting table, and pre-deploy checklist. **Refer to this before touching build/deploy steps** — the human-facing procedure lives there, not here.

## Build & Run

This is a Maven project on Java 21 (`<java.version>` property, `maven-compiler-plugin` uses `<release>21</release>`). Building requires JDK 21+. The shipped artifact is a fat JAR.

> Eclipse `.classpath` and `.settings/org.eclipse.jdt.core.prefs` target `JavaSE-21` (in sync with `pom.xml`). If you see a stale JRE container, run **Maven → Update Project** in Eclipse.

**Two ways to build the JAR — note these disagree on the main class:**

1. **Eclipse "Export → Runnable JAR"** — launch configuration `Main - bay-pcm-job`, which targets `th.co.locus.pcm_job.ApplicationStart`. This is how the JAR is actually produced today. Full step-by-step procedure (including post-export verification) lives in [DEPLOY.md](./DEPLOY.md) — don't re-derive it here.
2. **`maven-shade-plugin`** in `pom.xml`: now sets `mainClass=th.co.locus.pcm_job.ApplicationStart`, but it is still nested inside `<pluginManagement>` with no matching `<plugins>` entry, so `./mvnw package` won't actually shade. If you want to switch to a Maven-driven build, move the plugin out of `<pluginManagement>` into a real `<plugins>` block under `<build>`.

**Run the JAR (5 positional args):**

```
java -jar bay-pcm-job.jar <procedure> <batchParams|None> <propertiesFile> <logDir> [--log|--debug]
```

Example (from `Shell_scripts/SIT/TEST_batch.sh`):

```
java -jar /app/batch_jar/bay-pcm-job.jar \
  dbo.TEST_batch \
  DataDate=20210319/ModeRun=D \
  /app/batch_jar/config/application_sit.properties \
  /app/batch_jar/shell_scripts/SIT/logs \
  --log
```

- `<batchParams>`: `name=value` pairs joined by `batch.split.character` (default `/`). Use literal `None` or `No` for no parameters. They become `@name='value'` in `EXEC <proc> @p1='v1', @p2='v2'`.
- Log modes: `--log` writes `ccm_job_log_<yyyyMMddHHmmss>.txt` to `<logDir>` **and** prints to stdout; `--debug` prints only; omitted → SILENT.
- The `ApplicationStart` arg-count check accepts only 4 or 5 args. Older docstring/`TestBatchJob` comments mention 8 args (host/port/db/user/pass separately) — that signature is gone; ignore it.

**Quick smoke run from the IDE:** `th.co.locus.test.TestBatchJob#main` invokes `ApplicationStart` with hardcoded SIT-ish args.

**Tests:** there are none. The Maven archetype `AppTest` placeholder has been removed; `src/test/java/` is empty. `./mvnw test` is a no-op until real tests are added.

## Stored Procedure Contract

Procedures invoked by this JAR must follow these conventions or behavior degrades silently:

- **Email dispatch** is driven by result-set column names. If a result row has a column named `RESULT` (notify) or `RESULT2` (reject), its string value is parsed as the email payload. Empty strings are skipped.
- **Email payload format**: a single string split by `text.result.split.string` (default `\^\^`, i.e. `^^`) into exactly 8 fields, ordered per `TextResultConfiguration`:
  `FROM ^^ DISPLAY_NAME ^^ TO ^^ CC ^^ BCC ^^ SUBJECT ^^ BODY ^^ BODY_FORMAT`
  `BODY_FORMAT` equal to `html.flag` (default `H`) is sent as HTML; anything else is plain text. `TO`/`CC`/`BCC` are `;`-separated.
- **Exit code is derived from `batch_Log_ProcessLoad`**, not from the procedure's return value: after the proc runs, the JAR runs
  `SELECT TOP 1 Processed_Status FROM batch_Log_ProcessLoad WHERE BatchName = '<proc without "dbo." prefix>' ORDER BY CREATED_DATE DESC;`
  If `Processed_Status = 'FAIL'` the JAR exits 1, otherwise 0. The procedure name passed in is stripped of a leading `dbo.` before matching `BatchName`.

## Configuration & Secrets

Each environment has its own `application_<env>.properties` (template: `src/main/resources/application.properties`):

- `datasource.url` — full JDBC URL including `;databaseName=...`
- `datasource.username`
- `datasource.password.encrypted` — Jasypt `StandardPBEStringEncryptor` ciphertext, algorithm `PBEWithMD5AndDES`
- `secret.key` — Jasypt password used to decrypt the above. Note the secret sits next to the ciphertext in the same file; rotating credentials requires re-encrypting with `PBEStringEncryptor#encrypt`.
- `email.host`, `html.flag`, `text.result.split.string`, `batch.split.character`

`PropertyUtil` caches loaded `Properties` per file path in a static `ConcurrentHashMap` — each path is read from disk once, then reused for the rest of the JVM lifetime. Different paths get independent cached instances. The file is **not** re-read if it changes on disk during the run.

## Repo Layout Notes

- `Shell_scripts/{DR_SITE,PROD,SIT,UAT}/` — per-environment wrappers ESP invokes. They hardcode `/app/batch_jar/...` paths and the matching properties file. **`external_files/Shell_scripts/...` and `external_files/Property_files/...` are the same files staged for deployment** — keep them in sync when editing.
- ~~`jars/`~~ — removed. Runtime dependencies are declared in `pom.xml` (jasypt 1.9.3, mssql-jdbc 12.10.1.jre11, jakarta.mail-api 2.1.3 + angus-mail 2.0.4, jakarta.activation-api 2.1.3, junit-jupiter 5.13.4) and resolved via Maven Central. commons-io was dropped after migrating to `java.nio.file.Files`. If you see stale references in Eclipse `.classpath`, run **Maven → Update Project**.
- `th.co.locus.test.TestBatchJob` is a dev-only entry point — do not ship.
