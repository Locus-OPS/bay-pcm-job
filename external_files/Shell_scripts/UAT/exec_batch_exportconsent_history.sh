#!/bin/bash

if [ -z "$1" ]
then
	input_date=$(date '+%Y%m%d') 
else
	input_date=$1
fi

java -jar /app/batch_jar/bay-pcm-job.jar dbo.batch_exportconsent_history DataDate=$input_date /app/batch_jar/config/application_uat.properties /app/batch_jar/shell_scripts/UAT/logs --log

