package th.co.locus.pcm_job;

import java.io.File;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

import com.microsoft.sqlserver.jdbc.StringUtils;

import th.co.locus.utils.CollectionUtils;
import th.co.locus.utils.LogMode;
import th.co.locus.utils.PBEStringEncryptor;
import th.co.locus.utils.PropertyUtil;

public class PCMJob {

	private PCMJob() {
	}

	public static PCMJob getInstance() {
		return new PCMJob();
	}

	private LogMode logMode;
	private static StringBuilder logMessage = new StringBuilder();
	private static final String PREFIX_LOG_FILE = "ccm_job_log_";
	private static final String DATE_TIME_LOG_FILE_PATTERN = "yyyyMMddHHmmss";
	private static final String LOG_FILE_EXTENSION = ".txt";
	private static final String TEXT_EMIAL_RESULT_COLUMN = "RESULT";
	private static final String REJECT_EMIAL_RESULT_COLUMN = "RESULT2";

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
		case "--debug":
			logMode = LogMode.DEBUG;
			System.out.println("Run on debug mode");
			break;
		case "--log":
			logMode = LogMode.INFO;
			System.out.println("Run with log mode");
			break;
		default:
			logMode = LogMode.SILENT;
			System.out.println("Run with silent mode.");
		
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

		if (procedureName != null) {
			addLogMessage("Call procuedure name : " + procedureName);
		} else {
			addLogMessage("Error: Not found procedure_name on configuration !!");
		}

		long startTime = System.currentTimeMillis();

		Connection con = null;
		ResultSet rs = null;
		CallableStatement cstmt = null;

		ResultSet rs2 = null;
		CallableStatement cstmt2 = null;
		try {
			Properties appProperties = PropertyUtil.getApplicationProperties(fileConfigPath);
			String datasourceUrl = appProperties.getProperty("datasource.url");
			String username = appProperties.getProperty("datasource.username");
			String encryptedPassword = appProperties.getProperty("datasource.password.encrypted");
			String secretKey = appProperties.getProperty("secret.key");
			PBEStringEncryptor encryptor = new PBEStringEncryptor(secretKey);
			String decryptedPassword = encryptor.decrypt(encryptedPassword);
			
			String connectionString = datasourceUrl
					+ ";user=" + username + ";password=" + decryptedPassword;

			con = DriverManager.getConnection(connectionString);
		} catch (Exception e) {
			e.printStackTrace();
			addLogMessage("Error: Cannot connect database server. Please verify the connection properties. "
					+ "Make sure that TCP connections to the port are not blocked by a firewall.");
			return 1;
		}

		try {
			String batchParams = getStoredProcParams(batchParameters, fileConfigPath);

			String sql = "exec " + procedureName + batchParams + ";";
			addLogMessage("Start call procedure ..." + sql);
			cstmt = con.prepareCall(sql);

			boolean results = cstmt.execute();
			if (results) {
				rs = cstmt.getResultSet();
				addLogMessage("Execute result ...");

				if (rs != null) {
					ResultSetMetaData metadata = rs.getMetaData();
					int totalColumns = metadata.getColumnCount();
					String columnNames = "";
					List<String> columnNameList = new ArrayList<>();

					for (int c = 1; c <= totalColumns; c++) {
						if (c > 1) {
							columnNames += ", ";
						}
						String columnName = metadata.getColumnName(c);;
						columnNames += metadata.getColumnName(c);
						columnNameList.add(columnName);
					}
					addLogMessage(columnNames);

					while (rs.next()) {

						if (CollectionUtils.isExistStringInList(columnNameList, TEXT_EMIAL_RESULT_COLUMN, false)) {
							String text = rs.getString(TEXT_EMIAL_RESULT_COLUMN);
							if (!StringUtils.isEmpty(text)) {
								EmailSender.sendEmail(text, fileConfigPath);
							}
						}

						if (CollectionUtils.isExistStringInList(columnNameList, REJECT_EMIAL_RESULT_COLUMN, false)) {
							String rejectMessage = rs.getString(REJECT_EMIAL_RESULT_COLUMN);
							if (!StringUtils.isEmpty(rejectMessage)) {
								EmailSender.sendEmail(rejectMessage, fileConfigPath);
							}
						}

						String rowData = "";
						for (int c = 1; c <= totalColumns; c++) {
							if (c > 1) {
								rowData += ", ";
							}
							rowData += rs.getString(c);
						}
						addLogMessage(rowData);
					}
				} else {
					addLogMessage("No result to send email.");
				}
			} else {
				addLogMessage("No result to send email.");
			}

			long endTime = System.currentTimeMillis();
			long process_time = endTime - startTime;
			addLogMessage("Call procedure finished in..." + process_time + " ms.");
			
			String sqlForQuery = "SELECT TOP 1 Processed_Status FROM batch_Log_ProcessLoad WHERE BatchName = '" + procedureName.substring(4) + "' ORDER BY CREATED_DATE DESC;";
			addLogMessage("Start call query ..." + sqlForQuery);
			cstmt2 = con.prepareCall(sqlForQuery);			
			boolean results2 = cstmt2.execute();
			if (results2) {
				rs2 = cstmt2.getResultSet();
				addLogMessage("Execute result ...");				
				if (rs2 != null) {
					while(rs2.next()) {
						addLogMessage("Result ... " + rs2.getString("Processed_Status"));
						if (!rs2.getString("Processed_Status").isEmpty() && !"".equals(rs2.getString("Processed_Status"))) {
							if ("FAIL".equals(rs2.getString("Processed_Status"))) {
								return 1;
							}
						}
					}
				}
			}
			return 0;
		} catch (Exception e) {
			e.printStackTrace();
			addLogMessage(e.getMessage());
			return 1;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException ex) {
					addLogMessage(ex.getMessage());

				}
			}
			if (cstmt != null) {
				try {
					cstmt.close();
				} catch (SQLException ex) {
					addLogMessage(ex.getMessage());
				}
			}
		}

	}
	
	private String getStoredProcParams(String batchParameters, String fileConfigPath) throws IOException {
		
		if ("NONE".equalsIgnoreCase(batchParameters) || "NO".equalsIgnoreCase(batchParameters)) {
			return "";
		}
		
		Properties appProperties = PropertyUtil.getApplicationProperties(fileConfigPath);
		String batchSplitCharacter = appProperties.getProperty("batch.split.character");
		
		String storedProcParams = "";
		String[] batchParameterArray = batchParameters.split(batchSplitCharacter);
		for (String batchParameter : batchParameterArray) {
			String[] paramAndValue = batchParameter.split("=");
			String paramName = paramAndValue[0];
			String value = paramAndValue[1];
			if (storedProcParams != null && !"".equals(storedProcParams)) {
				storedProcParams += ",";
			}
			storedProcParams += " @" + paramName + " = '" + value + "'";
		}
		return storedProcParams;
	}

	private void addLogMessage(String newLogMessage) {
		switch (logMode) {
		case DEBUG:
			System.out.println(newLogMessage);
			break;
		case INFO:
			System.out.println(newLogMessage);
			if (logMessage.toString() != null && !logMessage.toString().equals("")) {
				logMessage.append("\n\r");
			}
			logMessage.append(newLogMessage);
			break;
		default:
			break;
		}
	}

	@SuppressWarnings("deprecation")
	private void writeLog(String logPath) throws IOException {
		if (logMode == LogMode.INFO) {
			Date current = new Date();
			String dateTimePattern = PCMJob.DATE_TIME_LOG_FILE_PATTERN;
			SimpleDateFormat dateFormat = new SimpleDateFormat(dateTimePattern);
			String dateTimeOutput = dateFormat.format(current);
			File logsFolder = new File(logPath);
			if (!logsFolder.exists()) {
				logsFolder.mkdir();
			}
			
			String fileName = logPath + File.separator + PCMJob.PREFIX_LOG_FILE + dateTimeOutput + PCMJob.LOG_FILE_EXTENSION;
			File file = new File(fileName);
			try {
				FileUtils.writeStringToFile(file, logMessage.toString());
			} catch (IOException e) {
				e.printStackTrace();
				FileUtils.writeStringToFile(file, e.getMessage());
			}
		}
	}
}
