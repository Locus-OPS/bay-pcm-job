#!/bin/bash

if [ -z "$1" ]
then
	input_date=$(date '+%Y%m%d') 
else
	input_date=$1
fi

if [ -z "$2" ]
then
	entity_code=1100
else
	entity_code=$2
fi

java -jar /app/batch_jar/bay-pcm-job.jar dbo.batch_exportconsentlog DataDate=$input_date/Entitycode=$entity_code /app/batch_jar/config/application_dr_site.properties /app/batch_jar/logs --log

