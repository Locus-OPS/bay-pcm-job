echo off
C:


if "%1"=="" (set input_date=%date:~10,4%%date:~7,2%%date:~4,2%) else (set input_date=%1)

if "%2"=="" (set entityCode=1100) else (set entityCode=%2)

java -jar C:\PCM_JOB\bay-pcm-job.jar dbo.batch_exportconsentlog DataDate=%input_date%,Entity_Code=%entityCode% 192.168.10.182 1433 BAY_CONSENT_DEV sa Locus@123 --log