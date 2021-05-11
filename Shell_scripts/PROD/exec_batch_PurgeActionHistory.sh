#!/bin/bash

java -jar /app/batch_jar/bay-pcm-job.jar dbo.batch_PurgeActionHistory None /app/batch_jar/config/application_prod.properties /app/batch_jar/logs --log

