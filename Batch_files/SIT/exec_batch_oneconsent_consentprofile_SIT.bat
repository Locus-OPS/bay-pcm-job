echo off
C:


if "%1"=="" (set input_date=%date:~10,4%%date:~7,2%%date:~4,2%) else (set input_date=%1)

if "%2"=="" (set mode_run=D) else (set mode_run=%2)

java -jar C:\PCM_JOB\bay-pcm-job.jar dbo.batch_oneconsent_consentprofile DataDate=%input_date%,ModeRun=%mode_run% 192.168.65.238 9677 BAY_CONSENT cm_dev P@ssw0rd --log