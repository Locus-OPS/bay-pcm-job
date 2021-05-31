#!/bin/bash

java -jar /app/batch_jar/bay-pcm-job.jar dbo.batch_email_daily None /app/batch_jar/config/application_sit.properties /app/batch_jar/shell_scripts/SIT/logs --log

