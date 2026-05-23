package th.co.locus.pcm_job;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.mail.MessagingException;

import th.co.locus.utils.PropertyUtil;
import th.co.locus.utils.TextResultConfiguration;
import th.co.locus.utils.EmailUtil;

public class EmailSender {

	public static void sendEmail(String text, String fileConfigPath) throws MessagingException, IOException {
		if (text == null || text.isBlank()) {
			return;
		}
		var appProperties = PropertyUtil.getApplicationProperties(fileConfigPath);
		var htmlFlag = appProperties.getProperty("html.flag");
		var textSplitString = appProperties.getProperty("text.result.split.string");

		var fieldValues = text.split(textSplitString);

		var from = fieldValues[TextResultConfiguration.FROM.index];
		var fromDisplayName = fieldValues[TextResultConfiguration.DISPLAY_NAME.index];
		var tos = fieldValues[TextResultConfiguration.TO.index];
		var toList = getReceiverList(tos);
		var ccs = fieldValues[TextResultConfiguration.CC.index];
		var ccList = getReceiverList(ccs);
		var bccs = fieldValues[TextResultConfiguration.BCC.index];
		var bccList = getReceiverList(bccs);
		var subject = fieldValues[TextResultConfiguration.SUBJECT.index];
		var content = fieldValues[TextResultConfiguration.BODY.index];
		var bodyFormat = fieldValues[TextResultConfiguration.BODY_FORMAT.index];
		boolean isHtml = htmlFlag.equalsIgnoreCase(bodyFormat);

		EmailUtil.sendEmail(from, fromDisplayName, toList, ccList, bccList, subject, content, isHtml, fileConfigPath);
	}

	private static List<String> getReceiverList(String receivers) {
		if (receivers == null || receivers.isBlank()) {
			return List.of();
		}
		var receiverArray = receivers.split(";");
		var receiverList = new ArrayList<String>();
		for (String receiver : receiverArray) {
			if (receiver != null && !receiver.isBlank()) {
				receiverList.add(receiver.trim());
			}
		}
		return receiverList;
	}
}
