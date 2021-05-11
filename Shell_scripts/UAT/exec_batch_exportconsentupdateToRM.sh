#!/bin/bash

if [ -z "$1" ]
then
        input_date=$(date '+%Y%m%d')
else
        input_date=$1
fi

if [ -z "$2" ]
then
        consent_type=103
else
        consent_type=$2
fi

java -jar /app/batch_jar/bay-pcm-job.jar dbo.batch_exportconsentupdateToRM DataDate=$input_date/ConsentType=$consent_type /app/batch_jar/config/application_uat.properties /app/batch_jar/shell_scripts/UAT/logs --log

