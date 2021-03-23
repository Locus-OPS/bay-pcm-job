package th.co.locus.pcm_job;

public class TestBatchJiob {
	public static void main(String[] args) {
		String[] params = { "dbo.TEST_batch", "DataDate=20210319,ModeRun=D", "192.168.10.182", "1433",
				"BAY_CONSENT_DEV", "sa", "Locus@123", "--log" };
		ApplicationStart.main(params);
	}
}
