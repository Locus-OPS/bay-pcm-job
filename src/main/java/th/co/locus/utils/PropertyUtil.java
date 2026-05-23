package th.co.locus.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class PropertyUtil {

	private static Properties appProperties = null;

	public static Properties getApplicationProperties(String applicationPropertiesFilePath) throws IOException {
		if (PropertyUtil.appProperties != null) {
			return PropertyUtil.appProperties;
		}

		// Loads configuration from application.properties.
		var props = new Properties();
		try (var resourceStream = Files.newInputStream(Path.of(applicationPropertiesFilePath))) {
			props.load(resourceStream);
		}
		PropertyUtil.appProperties = props;
		return PropertyUtil.appProperties;
	}
}
