package th.co.locus.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class PropertyUtil {

	// Cache keyed by file path so a single JVM can load multiple properties files
	// without colliding. Each path is read from disk once and reused thereafter.
	private static final Map<String, Properties> CACHE = new ConcurrentHashMap<>();

	public static Properties getApplicationProperties(String applicationPropertiesFilePath) throws IOException {
		var cached = CACHE.get(applicationPropertiesFilePath);
		if (cached != null) {
			return cached;
		}

		var props = new Properties();
		try (var resourceStream = Files.newInputStream(Path.of(applicationPropertiesFilePath))) {
			props.load(resourceStream);
		}
		CACHE.put(applicationPropertiesFilePath, props);
		return props;
	}
}
