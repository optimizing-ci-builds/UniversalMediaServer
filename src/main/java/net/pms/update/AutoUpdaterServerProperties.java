package net.pms.update;

import java.io.IOException;
import net.pms.util.PmsProperties;
import net.pms.util.Version;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.CentralProcessor.ProcessorIdentifier;

/**
 * Data provided by the server for us to update with.  Must be synchronized externally.
 *
 * @author Tim Cox (mail@tcox.org)
 */
public class AutoUpdaterServerProperties {
	private static final String KEY_LATEST_VERSION = "LatestVersion";
	private static final String DEFAULT_LATEST_VERSION = "0";
	private static final String KEY_DOWNLOAD_URL = "DownloadUrl";
	private static final String DEFAULT_DOWNLOAD_URL = "";
	private final PmsProperties properties = new PmsProperties();
	private final OperatingSystem operatingSystem = new OperatingSystem();

	public void loadFrom(byte[] data) throws IOException {
		properties.clear();
		properties.loadFromByteArray(data);
	}

	public boolean isStateValid() {
		return getDownloadUrl().length() > 0 && getLatestVersion().isGreaterThan(new Version("0"));
	}

	public Version getLatestVersion() {
		return new Version(getStringWithDefault(KEY_LATEST_VERSION, DEFAULT_LATEST_VERSION));
	}

	public String getDownloadUrl() {
		return getStringWithDefault(KEY_DOWNLOAD_URL, DEFAULT_DOWNLOAD_URL);
	}

	private String getStringWithDefault(String key, String defaultValue) {
		String platformSpecificKey = getPlatformSpecificKey(key);
		if (properties.containsKey(platformSpecificKey)) {
			return properties.get(platformSpecificKey);
		} else if (properties.containsKey(key)) {
			return properties.get(key);
		} else {
			return defaultValue;
		}
	}

	private String getPlatformSpecificKey(String key) {
		String os = operatingSystem.toString();
		if (os.startsWith("windows")) {
			os = operatingSystem.getPlatformName();
		} else if (os.startsWith("mac")) {
			os = operatingSystem.getPlatformName();

			String osVersionRaw = System.getProperty("os.version");
			Version osVersion = new Version(osVersionRaw);
			boolean isMacOSPreCatalina = osVersion.isLessThan(new Version("10.15"));

			SystemInfo systemInfo = new SystemInfo();
			HardwareAbstractionLayer hardware = systemInfo.getHardware();
			CentralProcessor processor = hardware.getProcessor();
			ProcessorIdentifier processorIdentifier = processor.getProcessorIdentifier();
			String microarchitecture = processorIdentifier.getMicroarchitecture();
			if (isMacOSPreCatalina) {
				os += "-pre10.15";
			} else if (microarchitecture.startsWith("ARM64")) {
				os += "-arm";
			}
		}

		return key + "." + os;
	}
}
