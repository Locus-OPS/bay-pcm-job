package th.co.locus.test;

import th.co.locus.pcm_job.ApplicationStart;

public class TestBatchJob {
	public static void main(String[] args) {
		String[] params = {
				"dbo.TEST_batch",
				"DataDate=20210420/ModeRun=D",
				"external_files/Property_files/SIT/application_sit.properties",
				"logs",
				"--log"
		};
		ApplicationStart.main(params);
	}
}
