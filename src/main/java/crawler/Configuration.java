package crawler;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class Configuration {

	protected String sftpUsername = null;
	protected String sftpPassword = null;
	protected String sftpIp = null;
	protected int numOfUploadingThreads = 0;
	protected int numOfScrapingThreads = 0;
	protected int numOfLocalFileSearchers = 0;
	protected boolean isValid = false;

	public Configuration() {

		try (InputStream inputStream = new FileInputStream("./config/config.properties")) {
			Properties prop = new Properties();
			prop.load(inputStream);
			sftpUsername = prop.getProperty("sftpUsername");
			sftpPassword = prop.getProperty("sftpPassword");
			sftpIp = prop.getProperty("sftpIp");
			if (sftpIp == null || sftpPassword == null || sftpUsername == null) {
				HyperScraper.writeStatusToLogFile("One of the configuration input strings given were null");
				return;
			}
			try {
				numOfUploadingThreads = Integer.parseInt(prop.getProperty("numOfUploadingThreads"));
				numOfScrapingThreads = Integer.parseInt(prop.getProperty("numOfScrapingThreads"));
				numOfLocalFileSearchers = Integer.parseInt(prop.getProperty("numOfLocalFileSearchers"));
			} catch (Exception e) {
				HyperScraper
						.writeStatusToLogFile("One of the configuration input ints given were not a correct integer");
				return;
			}
			
			isValid = true;
			HyperScraper.writeStatusToLogFile("Configuration file loaded successfully!\nNum of scraping threads = " + numOfScrapingThreads + "\nNum of uploading threads = " +
					numOfUploadingThreads + "\nNum of local file searchers = " + numOfLocalFileSearchers);
		} catch (Exception e) {
			HyperScraper.writeStatusToLogFile("Exception in configuration file: " + e);
		}
	}
}