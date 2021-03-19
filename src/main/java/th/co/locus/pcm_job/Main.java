package th.co.locus.pcm_job;

public class Main {

	/* parameter */
	/*
	 * 1 procedure_name 2 server 3 port 4 database 5 user 6 password
	 */
	public static void main(String[] args) {

		try {

			if (args.length == 6 || args.length == 7) {
				// Optional, log mode
				String opt = "";
				if (args.length == 7) {
					opt = args[6];

					// System.out.println("call ccm procedure...with option");

				} else {
					// System.out.println("call ccm procedure...");
				}

				PCMJob ccmJob = PCMJob.getInstance();
				ccmJob.run(args[0], args[1], args[2], args[3], args[4], args[5], opt);

			} else {
				System.out.println("parameter miss match with criteria...SYSTEM EXIT");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}