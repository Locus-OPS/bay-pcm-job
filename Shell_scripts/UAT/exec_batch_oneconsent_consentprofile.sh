#!/bin/bash

if [ -z "$1" ]
then
	input_date=$(date '+%Y%m%d') 
else
	input_date=$1
fi

if [ -z "$2" ]
then
	mode_run=D
else
	mode_run=$2
fi

java -jar /app/batch_jar/bay-pcm-job.jar dbo.batch_oneconsent_consentprofile DataDate=$input_date/ModeRun=$mode_run /app/batch_jar/config/application_uat.properties /app/batch_jar/shell_scripts/UAT/logs --log
