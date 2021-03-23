package th.co.locus.pcm_job;

public class ApplicationStart {

	/* Java Argument list:
	 * 1 Stored procedure name, for example, "batch_main_import_consent_profile"
	 * 2 batch parameters, for example, "date=20210319,entity_Code=1100", if no batch parameter, set to none.
	 * 3 Database IP address, for example, "192.168.65.238"
	 * 4 Database port, for example, "9677"
	 * 5 Database name, for example, "BAY_CONSENT"
	 * 6 Database user, for example, "pcm_user"
	 * 7 password, for example, "locus123"
	 * 8 log mode (optional)
	 */
	public static void main(String[] args) {

		try {

			if (args.length == 7 || args.length == 8) {
				// Optional, log mode
				String opt = "";
				if (args.length == 8) {
					opt = args[7];

					System.out.println("call ccm procedure...with option");

				} else {
					System.out.println("call pcm procedure without option");
				}

				PCMJob ccmJob = PCMJob.getInstance();
				ccmJob.run(args[0], args[1], args[2], args[3], args[4], args[5], args[6], opt);
			} else {
				System.out.println("parameter miss match with criteria...SYSTEM EXIT");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}