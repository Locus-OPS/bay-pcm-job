# Deployment Guide — bay-pcm-job

ขั้นตอนการสร้าง runnable JAR จาก Eclipse และนำไปติดตั้งบน Linux server เพื่อรันผ่าน shell script

---

## 1. Prerequisites ฝั่ง Build Machine (Eclipse / Windows)

### 1.1 JDK 11
- ติดตั้ง JDK 11 (แนะนำ Adoptium Temurin 11 หรือ OpenJDK 11)
- Eclipse: **Window → Preferences → Java → Installed JREs**
  - ต้องมี JDK 11 อยู่ในรายการ
  - ตั้งเป็น **default** (ติ๊กที่ checkbox)

### 1.2 Maven Dependencies sync
หลังจากที่ project ประกาศ dependencies ใน `pom.xml` แล้ว Eclipse ต้อง resolve ให้ครบก่อน export:

1. Right-click project `bay-pcm-job` → **Maven → Update Project** (หรือกด `Alt + F5`)
2. ติ๊ก **Force Update of Snapshots/Releases** → OK
3. รอจน Package Explorer แสดง **Maven Dependencies** ที่มี 5 jars ครบ:
   - `commons-io-2.8.0.jar`
   - `jasypt-1.9.3.jar`
   - `mssql-jdbc-8.4.1.jre11.jar`
   - `javax.mail-1.6.2.jar`
   - `activation-1.1.1.jar`
4. ตรวจว่าโปรเจคไม่มี error สีแดงเหลืออยู่

### 1.3 Launch Configuration
ต้องมี launch config ที่ชี้ไปที่ main class ที่ถูกต้อง:

1. คลิกขวาที่ `src/main/java/th/co/locus/pcm_job/ApplicationStart.java` → **Run As → Java Application**
   (จะ exit ทันทีเพราะไม่มี args — ปกติ จุดประสงค์คือให้ Eclipse สร้าง launch config)
2. ไปที่ **Run → Run Configurations** → ภายใต้ Java Application จะมี entry ชื่อ `ApplicationStart`
3. เปลี่ยนชื่อเป็น **`Main - bay-pcm-job`**
4. แท็บ **Main** → ตรวจว่า:
   - Project = `bay-pcm-job`
   - Main class = `th.co.locus.pcm_job.ApplicationStart`
5. Apply → Close

---

## 2. ขั้นตอน Export Runnable JAR

1. **File → Export** → เลือก **Java → Runnable JAR file** → Next
2. **Launch configuration**: เลือก `Main - bay-pcm-job - bay-pcm-job`
3. **Export destination**: ระบุ path ของไฟล์ JAR ที่จะสร้าง เช่น `D:\bay-pcm-job.jar`
4. **Library handling**: เลือก **`Package required libraries into generated JAR`**
   > สำคัญมาก — ห้ามเลือก "Extract" หรือ "Copy" เพราะจะทำให้ JAR รันบน Linux ไม่ได้
5. คลิก **Finish**
   - ถ้า Eclipse เตือนเรื่อง JAR signature ให้ตอบ OK

---

## 3. ตรวจสอบ JAR หลัง Export (ทำที่ build machine)

### 3.1 ตรวจ manifest
```bash
unzip -p bay-pcm-job.jar META-INF/MANIFEST.MF | grep -i main
```
ต้องเห็น:
```
Main-Class: org.eclipse.jdt.internal.jarinjarloader.JarRsrcLoader
Rsrc-Main-Class: th.co.locus.pcm_job.ApplicationStart
```
(Eclipse ใช้ wrapper loader — `Rsrc-Main-Class` คือ main class จริงของเรา)

### 3.2 ตรวจขนาดไฟล์
```bash
ls -lh bay-pcm-job.jar
```
ควรประมาณ **4–6 MB** (ถ้าเล็กกว่า 1 MB แปลว่าไม่ได้ bundle dependencies)

### 3.3 Smoke test
```bash
java -jar bay-pcm-job.jar dbo.NotExist None /tmp/dummy.properties /tmp --log
```
- ถ้าเจอ error `Cannot connect database` หรือ `Cannot find application.properties` → ปกติ (โหลด class ครบแล้ว)
- ถ้าเจอ `NoClassDefFoundError` หรือ `ClassNotFoundException` → export ผิด ต้องทำใหม่และเลือก "Package required libraries"

---

## 4. Requirements ฝั่ง Linux Server

### 4.1 Java Runtime — ต้องเป็น Java 11 ขึ้นไป

ตรวจ version ที่ server:
```bash
java -version
```
ต้องขึ้น `11.x.x` หรือสูงกว่า

ถ้ายังเป็น Java 8 จะรันไม่ได้ — error:
```
java.lang.UnsupportedClassVersionError:
  th/co/locus/pcm_job/ApplicationStart has been compiled by a more recent version
  of the Java Runtime (class file version 55.0), this version of the Java Runtime
  only recognizes class file versions up to 52.0
```

ติดตั้ง JDK 11:
```bash
# RHEL / CentOS / Oracle Linux
sudo yum install java-11-openjdk

# Ubuntu / Debian
sudo apt update && sudo apt install openjdk-11-jdk

# ตรวจหลังติดตั้ง
java -version
```

ถ้าเครื่องมีหลาย Java ติดตั้งอยู่ ตั้ง default:
```bash
sudo alternatives --config java     # RHEL family
sudo update-alternatives --config java   # Debian family
```

### 4.2 Network access
Server ต้องเข้าถึงปลายทางได้:
- **SQL Server**: TCP port ตามที่ระบุใน `datasource.url` (ปกติ 1433 หรือ port ที่กำหนด)
- **SMTP host**: TCP 25 (หรือ port ที่ตั้งใน `email.host`) สำหรับส่ง email ผลลัพธ์ batch

ตรวจ connectivity:
```bash
nc -zv 192.168.65.238 9677    # SQL Server
nc -zv 192.168.1.21 25        # SMTP
```

### 4.3 File system layout

shell scripts ที่ใช้อยู่ hardcode path เป็น `/app/batch_jar/...` — ให้สร้างโครงสร้างนี้:

```
/app/batch_jar/
├── bay-pcm-job.jar
├── config/
│   ├── application_sit.properties
│   ├── application_uat.properties
│   ├── application_prod.properties
│   └── application_dr.properties
└── shell_scripts/
    ├── SIT/
    │   ├── logs/                          (writable)
    │   ├── exec_batch_PurgeActionHistory.sh
    │   ├── exec_batch_email_daily.sh
    │   └── ... (อื่นๆ)
    ├── UAT/
    ├── PROD/
    └── DR_SITE/
```

ถ้าจะใช้ path อื่น ต้องแก้ shell scripts ทุกไฟล์

### 4.4 Permissions
```bash
# JAR ไม่จำเป็นต้อง executable
chmod 644 /app/batch_jar/bay-pcm-job.jar

# Shell scripts ต้อง executable
chmod 755 /app/batch_jar/shell_scripts/*/*.sh

# Properties files ควรอ่านได้เฉพาะ user ที่รัน batch (มี password)
chmod 600 /app/batch_jar/config/*.properties

# logs folder ต้องเขียนได้
chmod 755 /app/batch_jar/shell_scripts/*/logs
```

### 4.5 Line endings ของ shell scripts
ถ้า edit script บน Windows ระวัง CRLF — บน Linux จะเจอ error `bad interpreter: No such file or directory`:
```bash
# ตรวจ
file /app/batch_jar/shell_scripts/SIT/exec_batch_email_daily.sh

# แก้ถ้าเป็น CRLF
dos2unix /app/batch_jar/shell_scripts/**/*.sh
```

---

## 5. Run JAR — รูปแบบ command

```
java -jar bay-pcm-job.jar <procedureName> <batchParams|None> <propertiesFile> <logDir> [--log|--debug]
```

| Argument | คำอธิบาย |
|---|---|
| `procedureName` | ชื่อ stored procedure เต็มรวม schema เช่น `dbo.batch_email_daily` |
| `batchParams` | parameters คั่นด้วย `/` เช่น `DataDate=20210319/ModeRun=D` หรือใส่ `None` ถ้าไม่มี params |
| `propertiesFile` | path เต็มของไฟล์ application properties |
| `logDir` | folder สำหรับเขียน log file |
| `--log` (optional) | เขียน log ไฟล์ + พิมพ์ stdout |
| `--debug` (optional) | พิมพ์ stdout เท่านั้น ไม่เขียนไฟล์ |
| (omit) | silent mode |

### ตัวอย่าง (จาก `Shell_scripts/SIT/exec_batch_PurgeActionHistory.sh`)
```bash
#!/bin/bash
java -jar /app/batch_jar/bay-pcm-job.jar \
  dbo.batch_PurgeActionHistory \
  None \
  /app/batch_jar/config/application_sit.properties \
  /app/batch_jar/shell_scripts/SIT/logs \
  --log
```

### Exit codes
- `0` = สำเร็จ (`batch_Log_ProcessLoad.Processed_Status` ของ batch นั้นไม่ใช่ `FAIL`)
- `1` = ล้มเหลว (เกิดจากกรณีใดกรณีหนึ่ง: argument count ไม่ตรง, connect DB ไม่ได้, stored procedure throw exception, หรือ `Processed_Status = 'FAIL'`) — ESP จะเห็น exit code นี้และจัดการตาม policy

---

## 6. Troubleshooting

| อาการ | สาเหตุที่พบบ่อย | วิธีตรวจ/แก้ |
|---|---|---|
| `UnsupportedClassVersionError` (class file version 55.0) | Server ยังเป็น Java 8 | `java -version` → ติดตั้ง JDK 11 |
| `NoClassDefFoundError` / `ClassNotFoundException` | Export ไม่ได้เลือก "Package required libraries" | Export JAR ใหม่ |
| `bad interpreter: No such file or directory` ตอนรัน .sh | Shell script เป็น CRLF (Windows line endings) | `dos2unix script.sh` |
| `Cannot connect database server` | Network / firewall / wrong credentials / encrypted password ใช้ secret.key ผิด | ตรวจ `application_*.properties` และ `nc -zv <host> <port>` |
| `parameter miss match with criteria` | จำนวน argument ที่ส่งไม่ใช่ 4 หรือ 5 | ตรวจ shell script |
| ไม่มี log file แม้ใส่ `--log` | logDir ไม่มีสิทธิ์เขียน หรือ path ผิด | `ls -ld <logDir>` และตรวจ permission |
| Log file ภาษาไทยเป็น `???` | ใช้ JAR build เก่าก่อน fix UTF-8 | rebuild JAR ใหม่จาก code ล่าสุด |

---

## 7. Checklist สรุปก่อน Deploy

- [ ] Eclipse: Maven → Update Project แล้ว ไม่มี error
- [ ] Eclipse: ใช้ JRE 11 เป็น default
- [ ] Launch configuration `Main - bay-pcm-job` ชี้ที่ `th.co.locus.pcm_job.ApplicationStart`
- [ ] Export Runnable JAR ด้วย option "Package required libraries into generated JAR"
- [ ] ขนาด JAR อยู่ระหว่าง 4–6 MB
- [ ] Smoke test JAR ที่ build machine ผ่าน (ไม่เจอ `NoClassDefFoundError`)
- [ ] Linux server มี Java 11+ (`java -version`)
- [ ] Server connect ไปยัง SQL Server และ SMTP host ได้
- [ ] Folder `/app/batch_jar/config/`, `/app/batch_jar/shell_scripts/<ENV>/logs/` มีอยู่และ permission ถูกต้อง
- [ ] Properties file ของ environment นั้นๆ มี encrypted password ที่ถูก encrypt ด้วย `secret.key` ใน file เดียวกัน
- [ ] Shell scripts เป็น LF (Unix line endings) และ executable