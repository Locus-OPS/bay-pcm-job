Step to create jar file from this project.
1. Select Export... => Runable JAR File => Click Next
2. Select "Main - bay-pcm-job" in Launch Configuration,
   choose your Export Destination,
   select "Package required libraries into generated JAR" in Library handling,
   click Finish.

Example command to run JAR file:
java -jar bay-pcm-job.jar dbo.TEST_batch DataDate=20210319,ModeRun=D 192.168.10.182 1433 BAY_CONSENT_DEV sa Locus@123 --log