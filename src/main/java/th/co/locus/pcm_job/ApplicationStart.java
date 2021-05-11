package th.co.locus.pcm_job;

public class ApplicationStart {

	/* Java Argument list:
	 * 1 Stored procedure name, for example, "batch_main_import_consent_profile"
	 * 2 batch parameters, for example, "date=20210319,entity_Code=1100", if no batch parameter, set to none.
	 * 3 File configuration path, for example, "/app/batch_jars/config/application.properties"
	 * 3 log path, for example, "/app/batch_jars/logs"
	 * 8 log mode (optional)
	 */
	public static void main(String[] args) {

		try {

			if (args.length == 4 || args.length == 5) {
				// Optional, log mode
				String opt = "";
				if (args.length == 5) {
					opt = args[4];

					System.out.println("call pcm procedure...with option");

				} else {
					System.out.println("call pcm procedure without option");
				}

				PCMJob pcmJob = PCMJob.getInstance();
				pcmJob.run(args[0], args[1], args[2], args[3], opt);
			} else {
				System.out.println("parameter miss match with criteria...SYSTEM EXIT");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}