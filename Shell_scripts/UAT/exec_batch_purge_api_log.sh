#!/bin/bash

java -jar /app/batch_jar/bay-pcm-job.jar dbo.purge_api_log None /app/batch_jar/config/application_uat.properties /app/batch_jar/shell_scripts/UAT/logs --log
