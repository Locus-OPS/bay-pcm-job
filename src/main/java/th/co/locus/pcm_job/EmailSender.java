package th.co.locus.pcm_job;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.MessagingException;

import com.microsoft.sqlserver.jdbc.StringUtils;

import th.co.locus.utils.PropertyUtil;
import th.co.locus.utils.TextResultConfiguration;
import th.co.locus.utils.EmailUtil;

public class EmailSender {

	public static void sendEmail(String text, String fileConfigPath) throws MessagingException, IOException {
		Properties appProperties = PropertyUtil.getApplicationProperties(fileConfigPath);
		String htmlFlag = appProperties.getProperty("html.flag");
		String textSplitString = appProperties.getProperty("text.result.split.string");
		
		String[] fieldValues = text.split(textSplitString);
		
		String from = fieldValues[TextResultConfiguration.FROM.index];
		String fromDisplayName = fieldValues[TextResultConfiguration.DISPLAY_NAME.index];
		String tos = fieldValues[TextResultConfiguration.TO.index];
		List<String> toList = getReceiverList(tos);
		String ccs = fieldValues[TextResultConfiguration.CC.index];
		List<String> ccList = getReceiverList(ccs);
		String bccs = fieldValues[TextResultConfiguration.BCC.index];
		List<String> bccList = getReceiverList(bccs);
		String subject = fieldValues[TextResultConfiguration.SUBJECT.index];
		String content = fieldValues[TextResultConfiguration.BODY.index];
		String bodyFormat = fieldValues[TextResultConfiguration.BODY_FORMAT.index];
		boolean isHtml = htmlFlag.equalsIgnoreCase(bodyFormat);

		//EmailSender.sendEmail(from, fromDisplayName, toList, ccList, bccList, subject, content, htmlFlag);
		
		EmailUtil.sendEmail(from, fromDisplayName, toList, ccList, bccList,
				subject, content, isHtml, fileConfigPath);
		
	}
    
    private static List<String> getReceiverList(String receivers) {
    	if (StringUtils.isEmpty(receivers)) {
    		return null;
    	}
    	String[] receiverArray = receivers.split(";");
    	if (receiverArray != null) {
    		List<String> receiverList = new ArrayList<>();
    		for (String receiver : receiverArray) {
    			if (!StringUtils.isEmpty(receiver)) {
    				receiver = receiver.trim();
    				receiverList.add(receiver);
    			}
    		}
    		return receiverList;
    	}
        return null;
    }
}