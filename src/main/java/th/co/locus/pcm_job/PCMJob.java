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

	public void run(String procedureName, String server, String port, String database, String user, String password,
			String option) {

		switch (option) {
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
		callStoredProcedure(procedureName, server, port, database, user, password);
		writeLog();

	}

	public void callStoredProcedure(String procedureName, String server, String port, String dbname, String user,
			String password) {

		if (server != null) {
			addLogMessage("Connecting to db server : " + server);
		} else {
			addLogMessage("Error: Not found server on configuration !!");
		}

		if (port != null) {
			addLogMessage("Port :" + port);
		} else {
			addLogMessage("Error: Not found port on configuration !!");
		}

		if (user != null && password != null) {
			String u = "";
			for (int i = 0; i < user.length(); i++) {
				u = u + "*";
			}
			String p = "";
			for (int i = 0; i < password.length(); i++) {
				p = p + "*";
			}

			addLogMessage("Check database user : " + u + " , password : " + p + " ");
		} else {
			addLogMessage("Error: Not found DB login user or password !!");
		}

		if (dbname != null) {
			addLogMessage("Database : " + dbname);
		} else {
			addLogMessage("Error: Not found database on configuration !!");
		}

		if (procedureName != null) {
			addLogMessage("Call procuedure name : " + procedureName);
		} else {
			addLogMessage("Error: Not found procedure_name on configuration !!");
		}

		long startTime = System.currentTimeMillis();

		String connectionString = "jdbc:sqlserver://" + server + ":" + port + ";databaseName=" + dbname + ";user="
				+ user + ";password=" + password;

		Connection con = null;
		ResultSet rs = null;
		CallableStatement cstmt = null;
		try {

			con = DriverManager.getConnection(connectionString);
		} catch (Exception e) {
			addLogMessage("Error: Could not connect database server. " + "Please verify the connection properties. "
					+ "Make sure that TCP connections to the port are not blocked by a firewall.");
		}

		try {
			addLogMessage("Start call procedure ..." + procedureName);

			String sql = "{call " + procedureName + "}";
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
	private void writeLog() {
		if (logMode == LogMode.INFO) {
			try {
				Date current = new Date();
				String dateTimePattern = PCMJob.DATE_TIME_LOG_FILE_PATTERN;
				SimpleDateFormat dateFormat = new SimpleDateFormat(dateTimePattern);
				String dateTimeOutput = dateFormat.format(current);
				String fileName = PCMJob.PREFIX_LOG_FILE + dateTimeOutput + PCMJob.LOG_FILE_EXTENSION;
				FileUtils.writeStringToFile(new File(fileName), logMessage.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
