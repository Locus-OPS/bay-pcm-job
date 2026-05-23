# Upgrade Notes — Post Java 17 (OpenRewrite) Review

หลังจาก upgrade เป็น Java 17 ด้วย OpenRewrite แล้ว เอกสารนี้รวมรายการ **library ที่ควร upgrade ต่อ**, **code ที่ควร modernize**, และ **bug / security issue** ที่พบระหว่างทบทวน source

> Scope: ครอบคลุม `pom.xml`, `src/main/java/**`, `src/test/java/**`, `src/main/resources/application.properties`

---

## A. Libraries ที่ควร upgrade

| Dependency | ปัจจุบัน | แนะนำ | เหตุผล |
|---|---|---|---|
| `com.microsoft.sqlserver:mssql-jdbc` | `8.4.1.jre11` | `12.8.1.jre11` (หรือ classifier `jre17`) | 8.4.x ตกรุ่นไปนาน, มี security patch + รองรับ TLS 1.3 / `encrypt=true` ของ MSSQL ยุคใหม่ |
| `commons-io` | `2.8.0` | `2.16.1`+ **หรือ ลบทิ้ง** | แก้ CVE-2024-47554 และอื่นๆ. ถ้าเปลี่ยนไปใช้ `java.nio.file.Files.writeString` (Java 11+) จะตัด dependency นี้ออกได้เลย |
| `com.sun.mail:javax.mail` | `1.6.2` | `jakarta.mail:jakarta.mail-api` + `org.eclipse.angus:angus-mail` `2.0.3` | `javax.mail` หยุดพัฒนา; Jakarta Mail คือทายาท (ต้องเปลี่ยน `import javax.mail.*` → `jakarta.mail.*`) |
| `javax.activation:activation` | `1.1.1` | `jakarta.activation:jakarta.activation-api` `2.1.x` | Java 11+ เอา `javax.activation` ออกจาก JDK แล้ว |
| `org.jasypt:jasypt` | `1.9.3` | คงเดิม | เวอร์ชันยัง maintain อยู่ — แต่ดู section C: **อัลกอริทึมอ่อนแอ** ควรเปลี่ยน |
| `junit:junit` | `4.11` | `org.junit.jupiter:junit-jupiter` `5.10.x` | JUnit 4.11 มี CVE-2020-15250 (TemporaryFolder); JUnit 5 รองรับ Java 17 เต็มที่ |
| `maven-surefire-plugin` | `2.22.1` | `3.2.5` | ต้องการ 3.x เพื่อรัน JUnit 5 และทำงานกับ Java 17 module path ได้ดี |
| `maven-jar-plugin` | `3.0.2` | `3.4.x` | bump ตามรอบ |
| `maven-install-plugin` | `2.5.2` | `3.1.x` | 2.x มี warning กับ Maven ใหม่ |
| `maven-deploy-plugin` | `2.8.2` | `3.1.x` | bump ตามรอบ |
| `maven-resources-plugin` | `3.0.2` | `3.3.x` | bump ตามรอบ |
| `maven-clean-plugin` | `3.1.0` | `3.3.x` | bump ตามรอบ |
| `maven-site-plugin` | `3.7.1` | `3.12.x` | bump ตามรอบ |
| Maven Wrapper | `0.5.6` / Maven `3.6.3` | wrapper `3.3.x` / Maven `3.9.x` | Maven 3.6 ไม่ test กับ Java 17 อย่างเป็นทางการ |
| `rewrite-maven-plugin` + `rewrite-migrate-java` | `6.40.0` / `3.35.0` | **ลบทิ้ง** ถ้า migration เสร็จแล้ว | หรือเก็บไว้ bump ตอนทำ Java 21 รอบหน้า |

### หมายเหตุเพิ่มเติม
- **Folder `jars/`** (activation-1.1, commons-io-2.8.0, jasypt-1.9.0, javax.mail-1.6.1, mssql-jdbc-8.4.1.jre8) — `CLAUDE.md` ระบุไว้แล้วว่า **ลบได้** เพราะ resolve ผ่าน Maven Central หมดแล้ว
- **`maven-shade-plugin`** ตอนนี้อยู่ใน `<pluginManagement>` และ `mainClass=th.co.locus.ccmjob.Main` (ไม่มีอยู่จริง) → ถ้าจะเปลี่ยนมา build ด้วย Maven จริง ให้ย้ายออกจาก `<pluginManagement>` และแก้ `mainClass` เป็น `th.co.locus.pcm_job.ApplicationStart`

---

## B. Code modernization (Java 11 / 17 idioms)

| # | จุดที่แก้ | จาก | เป็น |
|---|---|---|---|
| B1 | `PCMJob.java:20`, `EmailSender.java:10`, `CollectionUtils.java:5` ใช้ `com.microsoft.sqlserver.jdbc.StringUtils` (**internal class**) | `StringUtils.isEmpty(x)` | `x == null \|\| x.isBlank()` (Java 11+) — internal class อาจหายตอน bump driver |
| B2 | `PCMJob.callStoredProcedure` — manual try/finally 4 resource | `try { ... } finally { rs.close(); ... }` | **try-with-resources** ครอบ Connection / CallableStatement / ResultSet |
| B3 | `PCMJob.run`, `PCMJob.addLogMessage` — classic switch + break | `switch (x) { case "a": ...; break; }` | **switch expression** (Java 14+) `switch (x) { case "a" -> ...; }` |
| B4 | `PCMJob.java:290-293` — `SimpleDateFormat` + `new Date()` | `new SimpleDateFormat(pattern).format(new Date())` | `LocalDateTime.now().format(DateTimeFormatter.ofPattern(pattern))` (thread-safe) |
| B5 | `PCMJob.java:294-297` — `new File(path).mkdir()` (ไม่ check return) | `if (!folder.exists()) folder.mkdir();` | `Files.createDirectories(Path.of(logPath))` |
| B6 | `PropertyUtil.java:18` — `FileInputStream` | `new FileInputStream(path)` | `Files.newInputStream(Path.of(path))` + try-with-resources |
| B7 | `PCMJob.java:302,305` — commons-io `FileUtils.writeStringToFile` | `FileUtils.writeStringToFile(file, msg, UTF_8)` | `Files.writeString(path, msg, StandardCharsets.UTF_8)` — ทำให้ตัด `commons-io` ทิ้งได้ |
| B8 | `PCMJob.java:137-143, 162-168` — String concat ใน loop | `columnNames += ", " + name;` | `StringBuilder` หรือ `String.join(", ", list)` |
| B9 | `EmailSender.getReceiverList` คืน `null` | `return null;` | `return List.of();` / `Collections.emptyList()` (กัน NPE) |
| B10 | `PCMJob.java:121, 190` — SQL string ยาว | `"exec " + proc + params + ";"` | **Text blocks** (Java 15+) `"""..."""` |
| B11 | local variables ทั่วๆ ไป | `Properties appProperties = ...` | ใช้ `var` (Java 10+) ในจุดที่ type ชัดเจน |
| B12 | logging — กระจาย `System.out.println` + `e.printStackTrace()` | print ดิบ | ใช้ `java.util.logging` หรือเพิ่ม SLF4J + Logback |

---

## C. Bugs / Security ที่พบ (ควรแก้ตอนนี้)

### 🔴 Critical

**C1. SQL Injection ใน `PCMJob.callStoredProcedure`** (`PCMJob.java:121`)
```java
String sql = "exec " + procedureName + batchParams + ";";
cstmt = con.prepareCall(sql);
```
ESP เป็น trusted source ก็จริง แต่ `batchParameters` มาจาก argv → ถ้าใครรัน shell มือก็ใส่ payload ได้
→ เปลี่ยนเป็น `con.prepareCall("{call dbo.proc(?, ?)}")` + `cstmt.setString(i, value)`

**C2. Logic bug ใน `EmailUtil.java:39-43`** (เงื่อนไขกลับด้าน)
```java
if (fromName != null) {
    msg.setFrom(new InternetAddress(fromAddress));        // ทิ้ง name
} else {
    msg.setFrom(new InternetAddress(fromAddress, fromName)); // ส่ง null name
}
```
→ สลับเงื่อนไข `if (fromName == null) ... else ...`

### 🟠 High

**C3. อัลกอริทึม encryption อ่อนแอ** (`PBEStringEncryptor.java:11`)
- `PBEWithMD5AndDES` = MD5 (broken) + DES (56-bit, broken)
- `secret.key` เก็บ plaintext **ในไฟล์เดียวกับ ciphertext** (`application_<env>.properties`)
→ พิจารณา `PBEWithHmacSHA512AndAES_256` (Jasypt 1.9.3 รองรับ) + ย้าย `secret.key` ไป env var / vault แล้ว rotate ciphertext ทั้งหมด

**C4. `logMessage` เป็น `static StringBuilder`** (`PCMJob.java:37`)
- ค้าง state ระหว่าง run ถ้ามีการเรียกใน same JVM, ไม่ thread-safe
→ เปลี่ยนเป็น instance field

### 🟡 Medium / Low

**C5. `getInstance()` ที่ return `new PCMJob()` ทุกครั้ง** (`PCMJob.java:32`)
- ชื่อชวนเข้าใจผิดว่าเป็น singleton
→ เปลี่ยนชื่อเป็น `create()` / `newInstance()` หรือทำให้เป็น singleton จริง

**C6. Redundant check** (`PCMJob.java:200`)
```java
if (!rs2.getString("Processed_Status").isEmpty()
    && !"".equals(rs2.getString("Processed_Status"))) {
```
→ เงื่อนไขเดียวกันสองครั้ง — ตัดอันที่สองทิ้ง และ cache ค่าใน local variable

**C7. `TestBatchJob.java` ใช้ signature 8 args เก่า**
```java
String[] params = { "dbo.TEST_batch", "DataDate=...", "192.168.10.182", "1433",
                    "BAY_CONSENT_DEV", "sa", "Locus@123", "--log" };
```
→ `ApplicationStart` รับแค่ 4–5 args แล้ว — รันไม่ได้ ควร sync หรือลบทิ้ง (CLAUDE.md บอกว่า dev-only)

**C8. `AppTest` เป็น placeholder ของ Maven archetype**
→ ลบ หรือเขียน test จริงด้วย JUnit 5

**C9. `metadata.getColumnName(c);;`** (`PCMJob.java:140`)
→ double semicolon — cosmetic

---

## ลำดับ implement ที่แนะนำ

1. **Phase 1 — Quick wins (ไม่ break runtime):**
   - ลบ folder `jars/`
   - แก้ bug C1 (SQL injection), C2 (email logic), C6 (redundant check), C9 (semicolon)
   - แก้ `TestBatchJob` (C7) หรือลบทิ้ง
2. **Phase 2 — Library bump (compatible):**
   - bump mssql-jdbc → 12.8.x
   - bump commons-io → 2.16+ (ถ้ายังเก็บไว้)
   - bump junit → 4.13.2 หรือ migrate JUnit 5
   - bump maven plugins ทั้งหมด
   - bump Maven Wrapper
3. **Phase 3 — Code modernization:**
   - แทน `MSSQL StringUtils` ด้วย `String.isBlank()` (B1)
   - try-with-resources (B2)
   - switch expression (B3)
   - `java.time` (B4)
   - `Files` API (B5, B6, B7) → **ตัด commons-io ทิ้งได้**
   - StringBuilder / `String.join` (B8)
   - return `List.of()` (B9)
4. **Phase 4 — Breaking changes:**
   - Migrate `javax.mail` → `jakarta.mail` (เปลี่ยน import ทั้งหมด, bump JDK ก็ยังต้องทำ)
   - Migrate `javax.activation` → `jakarta.activation`
   - Migrate Jasypt algorithm (C3) — ต้อง re-encrypt password ทุก environment
5. **Phase 5 — Cleanup:**
   - ลบ `rewrite-maven-plugin` (ถ้า migration เสร็จ)
   - ลบ `AppTest` placeholder (C8)
   - พิจารณาเพิ่ม SLF4J + Logback แทน `System.out`
