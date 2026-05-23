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
- **[DEPLOY.md](./DEPLOY.md)** — full Eclipse Export-Runnable-JAR procedure, Linux server requirements (Java 11+, network, file layout, permissions), troubleshooting table, and pre-deploy checklist. **Refer to this before touching build/deploy steps** — the human-facing procedure lives there, not here.

## Build & Run

This is a Maven project on Java 11 (`<java.version>` property, `maven-compiler-plugin` uses `<release>11</release>`, Eclipse `.classpath` and `.settings/org.eclipse.jdt.core.prefs` target `JavaSE-11`). Building requires JDK 11+. The shipped artifact is a fat JAR.

**Two ways to build the JAR — note these disagree on the main class:**

1. **Eclipse "Export → Runnable JAR"** — launch configuration `Main - bay-pcm-job`, which targets `th.co.locus.pcm_job.ApplicationStart`. This is how the JAR is actually produced today. Full step-by-step procedure (including post-export verification) lives in [DEPLOY.md](./DEPLOY.md) — don't re-derive it here.
2. **`maven-shade-plugin`** in `pom.xml`: declares `th.co.locus.ccmjob.Main` as the main class — that package/class **does not exist** in the source tree. It's also nested inside `<pluginManagement>` with no matching `<plugins>` entry, so `./mvnw package` won't actually shade. If you want to switch to a Maven-driven build, fix both: move the plugin out of `<pluginManagement>` and change `mainClass` to `th.co.locus.pcm_job.ApplicationStart`.

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

**Tests:** there are none meaningful — `src/test/java/ccm_job_3/ccm_job_3/AppTest.java` is the Maven archetype placeholder. `./mvnw test` will run it but exercises nothing.

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

`PropertyUtil` caches the first-loaded file statically in a JVM-wide field, so a single JAR invocation is locked to one properties file — fine for the one-procedure-per-run model, but don't try to reuse the loaded JVM across configs.

## Repo Layout Notes

- `Shell_scripts/{DR_SITE,PROD,SIT,UAT}/` — per-environment wrappers ESP invokes. They hardcode `/app/batch_jar/...` paths and the matching properties file. **`external_files/Shell_scripts/...` and `external_files/Property_files/...` are the same files staged for deployment** — keep them in sync when editing.
- `jars/` — legacy folder of third-party JARs (mssql-jdbc-jre8, jasypt, javax.mail, commons-io, activation). The runtime dependencies are now declared in `pom.xml` (commons-io, jasypt, mssql-jdbc-jre11, javax.mail, javax.activation) and resolved via Maven Central, so this folder is no longer needed for compile/build and can be deleted once the Eclipse Export-Runnable-JAR workflow is retired.
- `th.co.locus.test.TestBatchJob` is a dev-only entry point — do not ship.
