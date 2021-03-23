echo off
C:


if "%1"=="" (set input_date=%date:~10,4%%date:~7,2%%date:~4,2%) else (set input_date=%1)

if "%2"=="" (set mode_run=D) else (set mode_run=%2)

java -jar C:\PCM_JOB\bay-pcm-job.jar dbo.TEST_batch DataDate=%input_date%,ModeRun=%mode_run% 192.168.10.182 1433 BAY_CONSENT_DEV sa Locus@123 --log