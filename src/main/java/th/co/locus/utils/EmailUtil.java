package th.co.locus.utils;

import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;

public class EmailUtil {
	/**
	 * Utility method to send simple HTML email
	 * 
	 */
	public static void sendEmail(String fromAddress, String fromName, List<String> toAddresses,
			List<String> ccAddresses, List<String> bccAddress, String subject, String body, boolean isHtml, String fileConfigPath) {
		try {
			// Read email configuration from application.properties.
			Properties appProperties = PropertyUtil.getApplicationProperties(fileConfigPath);
			String smtpHostServer = appProperties.getProperty("email.host");

			// Prepares email session properties.
			Properties emailSessionProps = System.getProperties();

			emailSessionProps.put("mail.smtp.host", smtpHostServer);

			Session emailSession = Session.getInstance(emailSessionProps, null);

			MimeMessage msg = new MimeMessage(emailSession);
			// set message headers
			msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
			msg.addHeader("format", "flowed");
			msg.addHeader("Content-Transfer-Encoding", "8bit");

			// Set from address.
			if (fromName != null) {
				msg.setFrom(new InternetAddress(fromAddress));
			} else {
				msg.setFrom(new InternetAddress(fromAddress, fromName));
			}

			msg.setSubject(subject, "UTF-8");

			if (isHtml) {
				msg.setText(body, "utf-8", "html");
			} else {
				msg.setText(body);
			}

			msg.setSentDate(new Date());

			// Set to email.
			for (String to : toAddresses) {
				msg.addRecipient(RecipientType.TO, new InternetAddress(to));
			}

			// Set CC list.
			if (!CollectionUtils.isEmpty(ccAddresses)) {
				for (String cc : ccAddresses) {
					msg.addRecipient(RecipientType.CC, new InternetAddress(cc));
				}
			}

			// Set BCC list.
			if (!CollectionUtils.isEmpty(bccAddress)) {
				for (String bcc : bccAddress) {
					msg.addRecipient(RecipientType.BCC, new InternetAddress(bcc));
				}
			}
			System.out.println("Message is ready");
			Transport.send(msg);

			System.out.println("EMail Sent Successfully!!");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
