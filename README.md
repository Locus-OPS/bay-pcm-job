# bay-pcm-job

Java CLI ที่ทำหน้าที่เป็นตัวกลางระหว่าง **ESP scheduler** กับ **SQL Server stored procedures** ของระบบ Consent Management — ESP รัน shell script → shell script เรียก JAR ตัวนี้ → JAR ต่อ JDBC ไปเรียก stored procedure → ส่ง email ตามผลลัพธ์ (ถ้ามี) → คืน exit code ให้ ESP ตาม `batch_Log_ProcessLoad.Processed_Status`

```
ESP job  →  shell script (Shell_scripts/<ENV>/*.sh)  →  java -jar bay-pcm-job.jar  →  EXEC dbo.<procedure>  →  optional email + exit code
```

---

## Documentation

| ไฟล์ | สำหรับ |
|---|---|
| **[DEPLOY.md](./DEPLOY.md)** | ขั้นตอน Export Runnable JAR จาก Eclipse + Requirements ของ Linux server + Troubleshooting + Deployment checklist |
| **[CLAUDE.md](./CLAUDE.md)** | สถาปัตยกรรม, stored procedure contract, configuration, build notes (สำหรับ Claude Code และ developer ใหม่) |

---

## Tech Stack

- **Java 17** (compiler `<release>17</release>`, target JRE 17+)
- **Maven Wrapper** 3.3.2 / Maven 3.9.9 (build via Eclipse "Export → Runnable JAR" หรือ `./mvnw package`)
- **JDBC**: `mssql-jdbc 12.8.1.jre11`
- **Encryption**: Jasypt 1.9.3 (`PBEWithMD5AndDES`) สำหรับ encrypt password ใน `application.properties`
- **Email**: `jakarta.mail-api 2.1.3` + `org.eclipse.angus:angus-mail 2.0.3` + `jakarta.activation-api 2.1.3`
- **File I/O**: `java.nio.file.Files` (JDK) — commons-io ลบทิ้งแล้วหลัง modernization
- **Test**: `junit-jupiter 5.10.3` (JUnit 5)

---

## Quick Run

```bash
java -jar bay-pcm-job.jar <procedureName> <batchParams|None> <propertiesFile> <logDir> [--log|--debug]
```

ตัวอย่าง:
```bash
java -jar bay-pcm-job.jar \
  dbo.batch_email_daily \
  None \
  /app/batch_jar/config/application_prod.properties \
  /app/batch_jar/shell_scripts/PROD/logs \
  --log
```

ดูรายละเอียดเต็มและตัวอย่างต่อ environment ใน **[DEPLOY.md](./DEPLOY.md)**

---

## Repository Layout

```
bay-pcm-job/
├── src/main/java/th/co/locus/
│   ├── pcm_job/           # ApplicationStart (main), PCMJob (core), EmailSender
│   └── utils/             # PropertyUtil, PBEStringEncryptor, EmailUtil, ...
├── src/main/resources/
│   └── application.properties    # template (ตัวจริงอยู่ใน external_files/Property_files/<ENV>/)
├── Shell_scripts/<ENV>/   # shell script ที่ ESP เรียก (SIT, UAT, PROD, DR_SITE)
├── external_files/
│   ├── Property_files/<ENV>/     # properties file สำหรับแต่ละ environment
│   └── Shell_scripts/<ENV>/      # สำเนา shell scripts สำหรับ deploy
├── pom.xml
├── README.md              # ไฟล์นี้
├── DEPLOY.md              # build & deploy guide
└── CLAUDE.md              # architecture & developer notes
```
