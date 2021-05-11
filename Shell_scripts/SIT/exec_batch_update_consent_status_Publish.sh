#!/bin/bash

java -jar /app/batch_jar/bay-pcm-job.jar dbo.batch_update_consent_status_Publish None /app/batch_jar/config/application_sit.properties /app/batch_jar/shell_scripts/SIT/logs --log
