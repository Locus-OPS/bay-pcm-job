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
import java.util.Date;

import org.apache.commons.io.FileUtils;

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

	public void run(String procedureName, String batchParameters, String databaseIpAddesss, String databasePort,
			String databaseName, String databaseUser, String password, String logOption) throws IOException {
		
		if ("--debug".equalsIgnoreCase(logOption)) {
			logMode = LogMode.DEBUG;
			System.out.println("Run on debug mode");
		} else if ("--log".equalsIgnoreCase(logOption)) {
			logMode = LogMode.INFO;
			System.out.println("Run with log mode");
		} else {
			logMode = LogMode.SILENT;
			System.out.println("Run with silent mode.");
		}

		callStoredProcedure(procedureName, batchParameters, databaseIpAddesss, databasePort, databaseName, databaseUser, password);
		writeLog();

	}

	public void callStoredProcedure(String procedureName, String batchParameters, String databaseIpAddesss, String databasePort,
			String databaseName, String databaseUser, String password) {

		if (databaseIpAddesss != null) {
			addLogMessage("Connecting to db server : " + databaseIpAddesss);
		} else {
			addLogMessage("Error: Not found server on configuration !!");
		}

		if (databasePort != null) {
			addLogMessage("Port: " + databasePort);
		} else {
			addLogMessage("Error: Not found port on configuration !!");
		}

		if (databaseUser != null && password != null) {
			String u = "";
			for (int i = 0; i < databaseUser.length(); i++) {
				u = u + "*";
			}
			String p = "";
			for (int i = 0; i < password.length(); i++) {
				p = p + "*";
			}

			addLogMessage("Check database user : " + u + " , password : " + p + " ");
		} else {
			addLogMessage("Error: Not found Database login user or password !!");
		}

		if (databaseName != null) {
			addLogMessage("Database name : " + databaseName);
		} else {
			addLogMessage("Error: Not found database on configuration !!");
		}

		if (procedureName != null) {
			addLogMessage("Call procuedure name : " + procedureName);
		} else {
			addLogMessage("Error: Not found procedure_name on configuration !!");
		}

		long startTime = System.currentTimeMillis();

		String connectionString = "jdbc:sqlserver://" + databaseIpAddesss + ":" + databasePort + ";databaseName=" + databaseName + ";user="
				+ databaseUser + ";password=" + password;

		Connection con = null;
		ResultSet rs = null;
		CallableStatement cstmt = null;
		try {
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
			con = DriverManager.getConnection(connectionString);
		} catch (Exception e) {
			e.printStackTrace();
			addLogMessage("Error: Cannot connect database server. Please verify the connection properties. "
					+ "Make sure that TCP connections to the port are not blocked by a firewall.");
		}

		try {
			String batchParams = getStoredProcParams(batchParameters);

			String sql = "exec " + procedureName + batchParams + ";";
			addLogMessage("Start call procedure ..." + sql);
			cstmt = con.prepareCall(sql);

			boolean results = cstmt.execute();
			if (results) {
				rs = cstmt.getResultSet();
				addLogMessage("execute result ...");

				ResultSetMetaData metadata = rs.getMetaData();
				int totalColumns = metadata.getColumnCount();
				String columnNames = "";

				for (int c = 1; c <= totalColumns; c++) {
					if (c > 1) {
						columnNames += ", ";
					}
					columnNames += metadata.getColumnName(c);
				}
				addLogMessage(columnNames);

				while (rs.next()) {
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
				addLogMessage("No result.");
			}

			long endTime = System.currentTimeMillis();
			long process_time = endTime - startTime;
			addLogMessage("Call procedure finished in..." + process_time + " ms.");

		} catch (Exception e) {
			addLogMessage(e.getMessage());
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
	
	private String getStoredProcParams(String batchParameters) {
		if ("NONE".equalsIgnoreCase(batchParameters) || "NO".equalsIgnoreCase(batchParameters)) {
			return "";
		}
		String storedProcParams = "";
		String[] batchParameterArray = batchParameters.split(",");
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

	private void writeLog() throws IOException {
		if (logMode == LogMode.INFO) {
			Date current = new Date();
			String dateTimePattern = PCMJob.DATE_TIME_LOG_FILE_PATTERN;
			SimpleDateFormat dateFormat = new SimpleDateFormat(dateTimePattern);
			String dateTimeOutput = dateFormat.format(current);
			File logsFolder = new File("logs");
			if (!logsFolder.exists()) {
				logsFolder.mkdir();
			}
			
			String fileName = "logs/" + PCMJob.PREFIX_LOG_FILE + dateTimeOutput + PCMJob.LOG_FILE_EXTENSION;
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
