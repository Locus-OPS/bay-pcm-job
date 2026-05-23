# Upgrade Notes — Post Java 21 (OpenRewrite) Review

หลังจาก upgrade เป็น Java 21 ด้วย OpenRewrite (`org.openrewrite.java.migrate.UpgradeToJava21`) แล้ว เอกสารนี้รวมรายการ **library ที่ควร upgrade ต่อ**, **code ที่ควร modernize**, และ **bug / security issue** ที่ยังเหลือ

> Scope: ครอบคลุม `pom.xml`, `src/main/java/**`, `src/main/resources/application.properties`, เอกสาร (`CLAUDE.md`, `DEPLOY.md`, `README.md`)

---

## สถานะปัจจุบัน (ที่ทำไปแล้วในรอบ Java 17 → 21)

- `pom.xml` → `java.version=21`, `<release>21</release>`
- `.classpath` / `.settings/org.eclipse.jdt.core.prefs` → `JavaSE-21`, compliance/source/target = 21
- Phase 1–4 ของ UPGRADE_NOTES รอบเก่าเสร็จแล้ว: try-with-resources, switch expression, `java.time`, `Files` API, text blocks, `var`, `List.of()`, แก้ SQL injection (`{call proc(?, ?)}` + `setString`), แก้ `EmailUtil` from-name logic, ย้าย `javax.mail` → `jakarta.mail`, ย้าย `javax.activation` → `jakarta.activation`, ลบ `commons-io`, ลบ `jars/`, migrate JUnit 5

---

## A. Libraries ที่ควร bump ต่อ ✅ (เสร็จแล้ว)

| Dependency | ก่อน | หลัง | สถานะ |
|---|---|---|---|
| `com.microsoft.sqlserver:mssql-jdbc` | `12.8.1.jre11` | `12.10.1.jre11` | ✅ bumped |
| `org.junit.jupiter:junit-jupiter` | `5.10.3` | `5.13.4` | ✅ bumped (โปรเจ็กต์ยังไม่มี test แต่พร้อมเขียนแล้ว) |
| `org.eclipse.angus:angus-mail` | `2.0.3` | `2.0.4` | ✅ bumped |
| `maven-shade-plugin` | `3.5.3` | `3.6.0` | ✅ bumped (ยังคงอยู่ใน `<pluginManagement>` — การย้ายออกเป็นเรื่องเปลี่ยน build path จาก Eclipse Export → Maven shade แยกทำเมื่อพร้อม) |
| `org.openrewrite.maven:rewrite-maven-plugin` + `rewrite-migrate-java` | `6.40.0` / `3.35.0` | **ลบทิ้ง** | ✅ removed (migration เสร็จแล้ว) |
| `jakarta.mail:jakarta.mail-api` | `2.1.3` | คงเดิม | — ยังเป็น stable ล่าสุดของสาย Jakarta EE 10 |
| `jakarta.activation:jakarta.activation-api` | `2.1.3` | คงเดิม | — ยังเป็น stable ล่าสุด |
| `org.jasypt:jasypt` | `1.9.3` | คงเดิม | — latest แล้ว (ดู section B4: **อัลกอริทึมที่เลือกอ่อนแอ**) |

---

## B. Bug / Issue ที่ยังเหลือในโค้ด

### 🟠 High ✅ (เสร็จแล้ว B1-B3)

**B1. `PCMJob.java:185-187` — strip prefix `dbo.` ผิด** ✅ FIXED
```java
// before
var pureProcedureName = procedureName.indexOf("dbo.") == -1
        ? procedureName
        : procedureName.substring(4);
// after
var pureProcedureName = procedureName.startsWith("dbo.")
        ? procedureName.substring(4)
        : procedureName;
```
เดิมถ้า procedure ชื่อมี `dbo.` อยู่ตรงกลาง (เช่น `xyzdbo.proc`) → `indexOf("dbo.")=3` → `substring(4)` คืน `bo.proc` → ค้น `batch_Log_ProcessLoad` ไม่เจอ → exit 0 ผิด

**B2. `EmailUtil.java:26-28` — mutate system properties ทั่ว JVM** ✅ FIXED
```java
// before
Properties emailSessionProps = System.getProperties();
emailSessionProps.put("mail.smtp.host", smtpHostServer);
// after
Properties emailSessionProps = new Properties();
emailSessionProps.put("mail.smtp.host", smtpHostServer);
```
`System.getProperties()` คืน reference จริง — การ `put` เป็นการแก้ global state ของ JVM

**B3. `EmailUtil.java:77-79` — swallow exception** ✅ FIXED
```java
// before — สวบทุก exception แล้ว printStackTrace
} catch (Exception e) {
    e.printStackTrace();
}
// after — เปลี่ยน method signature เป็น `throws MessagingException, IOException`
// และ caller ใน PCMJob ครอบ try-catch รอบ EmailSender.sendEmail() แล้ว
// log ผ่าน addLogMessage + logException แทน, ไม่ kill batch
```
behavior change: email failure ตอนนี้ปรากฏใน log (ผ่าน `addLogMessage`) แต่ไม่ได้ทำให้ exit code = 1 — exit code ยังคง driven by `Processed_Status` จาก `batch_Log_ProcessLoad` ตามเดิม. การประมวลผล result rows อื่นๆ ก็ยังดำเนินต่อ.

### 🔴 Security

**B4. Jasypt ยังใช้ `PBEWithMD5AndDES`** (`PBEStringEncryptor.java:11`)
- MD5 broken + DES key 56-bit แตกหมดแล้ว
- `secret.key` plaintext วางในไฟล์เดียวกับ ciphertext (`application_<env>.properties`) → ใครอ่าน config ได้ก็ decrypt password ได้
→ เปลี่ยนเป็น `PBEWithHmacSHA512AndAES_256` (Jasypt 1.9.3 รองรับ) + ย้าย `secret.key` ไป env var / secret manager + re-encrypt password ทุก environment

### 🟡 Medium / Known caveat

**B5. `PropertyUtil.appProperties` static cache** ✅ FIXED
- ก่อน: `private static Properties appProperties = null;` — JVM-wide field, lock เข้ากับไฟล์แรกที่โหลด
- หลัง: `private static final Map<String, Properties> CACHE = new ConcurrentHashMap<>();` — cache แยกตาม file path
- พฤติกรรมใหม่: path เดิม → reuse cache (เหมือนเดิม); path ต่างกัน → instance แยก, ไม่ collide
- ข้อจำกัดที่ยังเหลือ: ไฟล์ที่ถูก mutate บน disk ระหว่าง run จะไม่ถูก re-read (cache ไม่มี TTL) — เป็น trade-off ที่ตั้งใจให้เร็ว, CLAUDE.md อัพเดตแล้ว

---

## C. Code modernization ที่ Java 21 เปิดให้ทำ

ส่วนใหญ่ Java 17 idiom ที่ทำไปแล้วครอบคลุมหมด Java 21 มีของใหม่ที่ **ใช้ได้แต่ไม่จำเป็นกับ codebase นี้**:

| Feature | สถานะกับโค้ดนี้ |
|---|---|
| **Pattern matching for switch** (final ใน 21) | switch บน `String` ตรงๆ ใน `PCMJob.run` ไม่มี type hierarchy ที่ได้ประโยชน์ — ข้าม |
| **Record patterns** (final ใน 21) | `SqlParam` ถูก destructure ผ่าน accessor `.name()/.value()` ในที่เดียว ไม่อยู่ใน switch — ข้าม |
| **Sequenced collections** (`getFirst()`/`getLast()`) | โค้ดไม่ได้ใช้ `.get(0)` / `.get(size-1)` ที่ไหน — ข้าม |
| **Virtual threads** | งานเป็น single batch ต่อ JVM, ไม่เกี่ยว — ข้าม |
| **String templates** | ใน 21 ยัง preview และ Java 23 ถอด feature ออก — **ห้ามใช้** |

ที่ทำได้จริง:

| # | จุดที่แก้ | จาก | เป็น | สถานะ |
|---|---|---|---|---|
| C1 | `TextResultConfiguration.index` เป็น `Integer` | `public final Integer index;` | `public final int index;` ตัด autoboxing ทุกครั้งที่อ่าน | ✅ DONE |
| C2 | `CollectionUtils.isExistStringInList` for loop + 2 branch | `for (int i = 0; ...) { if (ignoreCase && ...) ...; if (!ignoreCase && ...) ...; }` | `return list.stream().anyMatch(s -> ignoreCase ? inputString.equalsIgnoreCase(s) : inputString.equals(s));` | ✅ DONE |
| C3 | Logging กระจาย `System.out.println` + `e.printStackTrace()` | print ดิบ + custom `LogMode` (SILENT/DEBUG/INFO) | SLF4J + Logback (ต้องตัดสินใจว่าจะคง args `--log`/`--debug` ไหม) | ⏸ DEFERRED |

---

## D. เอกสาร — sync Java 17 → Java 21 ✅ (เสร็จแล้ว)

| ไฟล์ | แก้แล้ว |
|---|---|
| `CLAUDE.md` | "Java 17" / "JDK 17+" → "Java 21" / "JDK 21+"; ลบ note เก่าเรื่อง `.classpath` ยังอยู่ที่ `JavaSE-11` (sync เป็น `JavaSE-21` แล้ว); bump version list ใน Repo Layout (mssql-jdbc 12.10.1, angus-mail 2.0.4, junit-jupiter 5.13.4); อัพเดต PropertyUtil caveat (per-path cache) |
| `DEPLOY.md` | 1.1 JDK 21 + Adoptium Temurin 21 / OpenJDK 21; jar รายชื่อใน 1.2 bump version; 3.2 ข้อความขนาด JAR (12.10); 4.1 Java 21 / class file version **65.0** / `openjdk-21-jdk`; Troubleshooting row Java 21; Checklist Java 21 |
| `README.md` | Tech Stack Java 21, mssql-jdbc 12.10.1.jre11, angus-mail 2.0.4, junit-jupiter 5.13.4 |

หลัง bump Java 21 → class file version = **65.0** (ไม่ใช่ 61.0) — error message ใน DEPLOY.md อัพเดตให้ตรง

---

## ลำดับ implement ที่แนะนำ

1. **Phase 1 — Bug fix ที่กระทบ runtime (ทำก่อน):** ✅ เสร็จแล้ว
   - B1 (strip `dbo.` prefix) ✅
   - B2 (`System.getProperties()` mutation) ✅
   - B3 (email exception swallowed) ✅
2. **Phase 2 — Library bump (compatible):** ✅ เสร็จแล้ว
   - `mssql-jdbc` → `12.10.1.jre11` ✅
   - `angus-mail` → `2.0.4` ✅
   - `junit-jupiter` → `5.13.4` ✅
   - `maven-shade-plugin` → `3.6.0` ✅ (ยังอยู่ใน `<pluginManagement>` — ย้ายออกเมื่อพร้อมเปลี่ยน build path)
   - ลบ `rewrite-maven-plugin` + `rewrite-migrate-java` ✅
3. **Phase 3 — Code cleanup (optional):**
   - C1 `Integer` → `int`
   - C2 stream `anyMatch`
4. **Phase 4 — Doc sync:**
   - `CLAUDE.md`, `DEPLOY.md`, `README.md` → Java 21
5. **Phase 5 — Security / Long-term:**
   - B4 Jasypt algorithm + secret externalization (กระทบทุก env ต้อง re-encrypt)
   - C3 SLF4J + Logback
   - เริ่มเขียน JUnit 5 test จริง แล้วค่อย bump `junit-jupiter` → 5.13.x
