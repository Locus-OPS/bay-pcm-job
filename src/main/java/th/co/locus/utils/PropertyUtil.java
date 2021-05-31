package th.co.locus.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyUtil {
	
	private static Properties appProperties = null;
	
	public static Properties getApplicationProperties(String applicationPropertiesFilePath) throws IOException {
		if (PropertyUtil.appProperties != null) {
			return PropertyUtil.appProperties;
		}
		
		// Loads configuration from application.properties.
		InputStream resourceStream = new FileInputStream(applicationPropertiesFilePath);
		PropertyUtil.appProperties = new Properties();
		PropertyUtil.appProperties.load(resourceStream);
		return PropertyUtil.appProperties;
	}
}
