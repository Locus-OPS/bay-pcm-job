package th.co.locus.pcm_job;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import th.co.locus.utils.CollectionUtils;
import th.co.locus.utils.LogMode;
import th.co.locus.utils.PBEStringEncryptor;
import th.co.locus.utils.PropertyUtil;

public class PCMJob {

	private PCMJob() {
	}

	public static PCMJob newInstance() {
		return new PCMJob();
	}

	private LogMode logMode;
	private final StringBuilder logMessage = new StringBuilder();
	private static final String PREFIX_LOG_FILE = "ccm_job_log_";
	private static final String DATE_TIME_LOG_FILE_PATTERN = "yyyyMMddHHmmss";
	private static final String LOG_FILE_EXTENSION = ".txt";
	private static final String TEXT_EMIAL_RESULT_COLUMN = "RESULT";
	private static final String REJECT_EMIAL_RESULT_COLUMN = "RESULT2";

	// Only letters, digits, underscore, and dot — enough for `dbo.MyProc` style.
	// Identifiers are interpolated into SQL (procedure name, @param names) and
	// cannot be bound via setString, so they must be strictly validated.
	private static final Pattern SAFE_SQL_IDENTIFIER = Pattern.compile("[A-Za-z0-9_.]+");

	private record SqlParam(String name, String value) {}

	/**
	 * Execute a store procedure with a parameter list and return with exit code.
	 * @param procedureName
	 * @param batchParameters
	 * @param fileConfigPath
	 * @param logPath
	 * @param logOption
	 * @return
	 * @throws IOException
	 */
	public int run(String procedureName, String batchParameters, String fileConfigPath, String logPath, String logOption) throws IOException {

		switch (logOption) {
			case "--debug" -> {
				logMode = LogMode.DEBUG;
				System.out.println("Run on debug mode");
			}
			case "--log" -> {
				logMode = LogMode.INFO;
				System.out.println("Run with log mode");
			}
			default -> {
				logMode = LogMode.SILENT;
				System.out.println("Run with silent mode.");
			}
		}
		int exitCode = callStoredProcedure(procedureName, batchParameters, fileConfigPath);
		writeLog(logPath);
		return exitCode;
	}

	/**
	 * Call a stored procedure with parameter list and return with exit code.
	 * @param procedureName
	 * @param batchParameters
	 * @param fileConfigPath
	 * @return the exit code
	 */
	public int callStoredProcedure(String procedureName, String batchParameters, String fileConfigPath) {

		if (procedureName == null) {
			addLogMessage("Error: Not found procedure_name on configuration !!");
			return 1;
		}
		if (!SAFE_SQL_IDENTIFIER.matcher(procedureName).matches()) {
			addLogMessage("Error: Invalid procedure name (must match " + SAFE_SQL_IDENTIFIER + "): " + procedureName);
			return 1;
		}
		addLogMessage("Call procuedure name : " + procedureName);

		long startTime = System.currentTimeMillis();

		Connection con;
		try {
			var appProperties = PropertyUtil.getApplicationProperties(fileConfigPath);
			var datasourceUrl = appProperties.getProperty("datasource.url");
			var username = appProperties.getProperty("datasource.username");
			var encryptedPassword = appProperties.getProperty("datasource.password.encrypted");
			var secretKey = appProperties.getProperty("secret.key");
			var encryptor = new PBEStringEncryptor(secretKey);
			var decryptedPassword = encryptor.decrypt(encryptedPassword);

			var connectionString = datasourceUrl
					+ ";user=" + username + ";password=" + decryptedPassword;

			con = DriverManager.getConnection(connectionString);
		} catch (Exception e) {
			logException(e);
			addLogMessage("Error: Cannot connect database server. Please verify the connection properties. "
					+ "Make sure that TCP connections to the port are not blocked by a firewall.");
			return 1;
		}

		try (con) {
			var batchParams = getStoredProcParams(batchParameters, fileConfigPath);

			var placeholders = batchParams.stream()
					.map(p -> "@" + p.name() + "=?")
					.collect(Collectors.joining(", "));
			var sql = batchParams.isEmpty()
					? "{call " + procedureName + "}"
					: "{call " + procedureName + "(" + placeholders + ")}";
			addLogMessage("Start call procedure ..." + sql);

			try (CallableStatement cstmt = con.prepareCall(sql)) {
				for (int i = 0; i < batchParams.size(); i++) {
					cstmt.setString(i + 1, batchParams.get(i).value());
				}
				boolean results = cstmt.execute();
				if (results) {
					try (ResultSet rs = cstmt.getResultSet()) {
						addLogMessage("Execute result ...");
						if (rs != null) {
							var metadata = rs.getMetaData();
							int totalColumns = metadata.getColumnCount();
							var columnNameList = new ArrayList<String>();
							for (int c = 1; c <= totalColumns; c++) {
								columnNameList.add(metadata.getColumnName(c));
							}
							addLogMessage(String.join(", ", columnNameList));

							while (rs.next()) {
								if (CollectionUtils.isExistStringInList(columnNameList, TEXT_EMIAL_RESULT_COLUMN, false)) {
									var text = rs.getString(TEXT_EMIAL_RESULT_COLUMN);
									if (text != null && !text.isBlank()) {
										EmailSender.sendEmail(text, fileConfigPath);
									}
								}

								if (CollectionUtils.isExistStringInList(columnNameList, REJECT_EMIAL_RESULT_COLUMN, false)) {
									var rejectMessage = rs.getString(REJECT_EMIAL_RESULT_COLUMN);
									if (rejectMessage != null && !rejectMessage.isBlank()) {
										EmailSender.sendEmail(rejectMessage, fileConfigPath);
									}
								}

								var rowValues = new ArrayList<String>();
								for (int c = 1; c <= totalColumns; c++) {
									rowValues.add(rs.getString(c));
								}
								addLogMessage(String.join(", ", rowValues));
							}
						} else {
							addLogMessage("No result to send email.");
						}
					}
				} else {
					addLogMessage("No result to send email.");
				}
			}

			long endTime = System.currentTimeMillis();
			long process_time = endTime - startTime;
			addLogMessage("Call procedure finished in..." + process_time + " ms.");

			var pureProcedureName = procedureName.indexOf("dbo.") == -1
					? procedureName
					: procedureName.substring(4);

			var sqlForQuery = """
					SELECT TOP 1 Processed_Status
					FROM batch_Log_ProcessLoad
					WHERE BatchName = ?
					ORDER BY CREATED_DATE DESC""";
			addLogMessage("Start call query ..." + sqlForQuery + " [BatchName=" + pureProcedureName + "]");

			try (PreparedStatement pstmt2 = con.prepareStatement(sqlForQuery)) {
				pstmt2.setString(1, pureProcedureName);
				boolean results2 = pstmt2.execute();
				if (results2) {
					try (ResultSet rs2 = pstmt2.getResultSet()) {
						addLogMessage("Execute result ...");
						if (rs2 != null) {
							while (rs2.next()) {
								var processedStatus = rs2.getString("Processed_Status");
								addLogMessage("Result ... " + processedStatus);
								if ("FAIL".equals(processedStatus)) {
									return 1;
								}
							}
						}
					}
				}
			}
			return 0;
		} catch (Exception e) {
			logException(e);
			addLogMessage(e.getMessage());
			return 1;
		}
	}

	private List<SqlParam> getStoredProcParams(String batchParameters, String fileConfigPath) throws IOException {

		if ("NONE".equalsIgnoreCase(batchParameters) || "NO".equalsIgnoreCase(batchParameters)) {
			return List.of();
		}

		var appProperties = PropertyUtil.getApplicationProperties(fileConfigPath);
		var batchSplitCharacter = appProperties.getProperty("batch.split.character");

		var params = new ArrayList<SqlParam>();
		var batchParameterArray = batchParameters.split(batchSplitCharacter);
		for (String batchParameter : batchParameterArray) {
			var paramAndValue = batchParameter.split("=", 2);
			var paramName = paramAndValue[0];
			var value = paramAndValue[1];
			if (!SAFE_SQL_IDENTIFIER.matcher(paramName).matches()) {
				throw new IllegalArgumentException("Invalid parameter name: " + paramName);
			}
			params.add(new SqlParam(paramName, value));
		}
		return params;
	}

	private void addLogMessage(String newLogMessage) {
		switch (logMode) {
			case DEBUG -> System.out.println(newLogMessage);
			case INFO -> {
				System.out.println(newLogMessage);
				if (logMessage.length() > 0) {
					logMessage.append("\n\r");
				}
				logMessage.append(newLogMessage);
			}
			default -> {
				// SILENT — no output
			}
		}
	}

	private void logException(Throwable t) {
		var sw = new StringWriter();
		t.printStackTrace(new PrintWriter(sw));
		addLogMessage(sw.toString());
	}

	private void writeLog(String logPath) throws IOException {
		if (logMode == LogMode.INFO) {
			var dateTimeOutput = LocalDateTime.now()
					.format(DateTimeFormatter.ofPattern(DATE_TIME_LOG_FILE_PATTERN));
			Files.createDirectories(Path.of(logPath));

			var filePath = Path.of(logPath, PREFIX_LOG_FILE + dateTimeOutput + LOG_FILE_EXTENSION);
			try {
				Files.writeString(filePath, logMessage.toString(), StandardCharsets.UTF_8);
			} catch (IOException e) {
				logException(e);
				Files.writeString(filePath, e.getMessage(), StandardCharsets.UTF_8);
			}
		}
	}
}
