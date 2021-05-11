package th.co.locus.test;

import th.co.locus.pcm_job.ApplicationStart;

public class TestBatchJob {
	public static void main(String[] args) {
		String[] params = { "dbo.TEST_batch", "DataDate=20210420/ModeRun=D", "192.168.10.182", "1433",
				"BAY_CONSENT_DEV", "sa", "Locus@123", "--log" };
//		String[] params = { "dbo.TEST_batch", "DataDate=20210420/ModeRun=D", "192.168.65.238", "9677",
//				"BAY_CONSENT", "cm_dev", "P@ssw0rd", "--log" };
		ApplicationStart.main(params);
	}
}
