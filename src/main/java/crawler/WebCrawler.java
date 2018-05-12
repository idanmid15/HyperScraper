package crawler;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WebCrawler {

	static final String DESKTOP = "User-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.116 Safari/537.36";
	static final String MOBILE = "Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13B143 Safari/601.1";
	static final String TABLET = "Mozilla/5.0 (iPad; CPU OS 7_0_4 like Mac OS X) AppleWebKit/537.51.1 (KHTML, like Gecko) Version/7.0 Mobile/11B554a Safari/9537.53";

	static short fontsTried = 0;
	static short fontsInjected = 0;
	static short imageUrlsChanged = 0;
	static short numScrapedFiles = 0;
	static String dateString = null;
	static String hourString = null;
	static String currentUserAgent = null;
	static String currentUrl = null;
	static byte scanForToday = 0;
	static boolean finishedScraping = false;
	static HashMap<String, String> currentMappedCookies = null;

	/**
	 * Receives cookies as a single String and returns a HashMap representing
	 * all cookies.
	 * 
	 * @param cookies
	 *            - The cookie string.
	 * @return - Hashmap (pairs) of cookie(key, value).
	 */
	public static HashMap<String, String> mapCookies(String cookies) {
		HashMap<String, String> cookieMap = new HashMap<>();
		if (cookies.length() <= 1)
			return cookieMap;
		String cookieKey = "";
		String cookieValue = "";
		String[] cooksArray = cookies.split(";");
		for (String cookie : cooksArray) {
			String[] cookiesplit = cookie.split("=");
			cookieKey = cookiesplit[0];
			if (cookiesplit.length <= 1) {
				cookieValue = "";
			} else {
				for (int i = 1; i < cookiesplit.length; i++) {
					cookieValue += cookiesplit[i];
					if (i < cookiesplit.length - 1) {
						cookieValue += "=";
					}
				}
			}
			if (!cookieMap.containsKey(cookieKey)) { 
				cookieMap.put(cookieKey, cookieValue);
			}
		}
		return cookieMap;
	}

	@SuppressWarnings("deprecation")
	public static void main(String[] args) {
		try {
			System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
			
			Date beginDate = new Date();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
			SimpleDateFormat hourFormat = new SimpleDateFormat("HH-mm-ss");
			dateString = dateFormat.format(beginDate);
			hourString = hourFormat.format(beginDate);
			if (beginDate.getHours() >= 12) {
				scanForToday = 2;
			} else {
				scanForToday = 1;
			}
			new File("logs/" + dateString + "/" + hourString).mkdirs();

			// Creates new log files
			createEmptyLogs("");
			createEmptyLogs("connectionError");
			createEmptyLogs("IMAGEError");
			createEmptyLogs("FONTError");
			createEmptyLogs("FILEError");
			createEmptyLogs("UPLOAD");
			createEmptyLogs("UPLOADError");

			new File(dateString + "/" + scanForToday + "/companies/").mkdirs();
			writeStatusToLogFile("Scraper initializing...\n");

			File input = new File("input/input.xml");
			Document doc = Jsoup.parse(input, "UTF-8", "");
			writeStatusToLogFile("Beginning to Scrape!\n\n");
			//short numUrls = processInput(doc);
			Date endDate = new Date();
			//runStatistics(beginDate, endDate, numUrls);
			GeneratedSource.generatedJsWarningsOff();

			HashMap<String, String> fidelityCookies = GeneratedSource.logInFidelity();
			GeneratedSource.browse("https://www.fidelity.com/customer-service/overview", DESKTOP, fidelityCookies);
			finishedScraping = true;
			writeStatusToLogFile("Scraper finished downloading locally!\n\n");
			writeStatusToLogFile("Deleting past scraped folders(1 day and above)...\n");
			deletePreviouslyScraped();
			writeStatusToLogFile("Previously scraped folders have been deleted!\n\n");
			writeStatusToLogFile("Still uploading local files to SFTP server.......................");
			//GeneratedSource.logInSears("http://www.sears.com/craftsman-10-pc-air-tool-set/p-00916852000P?prdNo=1&blockNo=1&blockType=G1", DESKTOP);
			} catch (IOException e) {
			e.printStackTrace();
		} 
	}

	/**
	 * Runs statistics for the upload to SFTP process and writes to the log
	 * file.
	 * 
	 * @param beginDate
	 *            - The start date.
	 * @param numUrls
	 *            - The number of urls scraped.
	 * @param endDate
	 *            - The end time of the upload
	 * @throws IOException
	 */
	protected static void runStatisticsUpload(Date beginDate, Date endDate) throws IOException {
		long secondsDiff = (endDate.getTime() - beginDate.getTime()) / 1000;
		double hoursDiff = ((double) secondsDiff / 3600);
		double minutes = (hoursDiff % 1) * 60;
		double seconds = (minutes % 1) * 60;
		BufferedWriter bw = new BufferedWriter(
				new FileWriter("logs/" + dateString + "/" + hourString + "/logs.txt", true));
		bw.write(
				"-----------------------------------------------------------------------------------------------------------------------\n\n\n");
		bw.write("Total Upload Time = " + (int) hoursDiff + " Hour " + (int) minutes + " Minutes " + seconds
				+ " Seconds\n");
		bw.write("Average Time Per Url = " + ((double) secondsDiff / numScrapedFiles) + " Seconds\n");
		bw.close();
	}

	/**
	 * Receives the document read from the "input.xml" file and scrapes each
	 * url. according to the xml preferences.
	 * 
	 * @param doc
	 *            - The input xml.
	 * @return - The number of urls scraped.
	 * @throws IOException
	 */
	protected static short processInput(Document doc) throws IOException {
		Thread uploadThread = new Thread(new UploadFoldersToFtp());
		uploadThread.start();
		String url, stringCookies;
		boolean generatedSource = false;
		char userAgentChar;
		short countUrls = 1;
		Elements links = doc.select("link");
		GeneratedSource.generatedJsWarningsOff();
		for (Element link : links) {
			url = link.attr("url");
			currentUrl = link.attr("url");
			userAgentChar = link.attr("ua").charAt(0);
			stringCookies = link.attr("cookies");
			generatedSource = Boolean.valueOf(link.attr("generatedSource"));
			currentMappedCookies = mapCookies(stringCookies);
			if (Character.toUpperCase(userAgentChar) == 'D') {
				currentUserAgent = DESKTOP;
			} else if (Character.toUpperCase(userAgentChar) == 'M') {
				currentUserAgent = MOBILE;
			} else {
				currentUserAgent = TABLET;
			}
			if (generatedSource) {
				GeneratedSource.browse(url, currentUserAgent, currentMappedCookies);
			} else {
				if (countUrls % 4 != 0) {
					Thread webThread = new Thread(new ThreadedWebPage());
					webThread.start();	
					try {
						
						// Required wait time before we iterate to the next url.
						webThread.sleep(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} else {
					WebPage page = new WebPage(currentUrl, currentUserAgent, currentMappedCookies);
					page = null;
				}
				
			}
			writeStatusToLogFile(new Date() + " " + url + " " + userAgentChar + "\n");
			countUrls++;
		}
		return --countUrls;
	}

	/**
	 * Creates empty log files in a folder which matches the starting time of
	 * the program.
	 * 
	 * @param type
	 *            - The type of log file to be created.
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	protected static void createEmptyLogs(String type) throws FileNotFoundException, UnsupportedEncodingException {
		PrintWriter writer = new PrintWriter("logs/" + dateString + "/" + hourString + "/" + type + "Logs.txt",
				"UTF-8");
		writer.close();
	}

	/**
	 * Runs statistics of the entire scrape.
	 * 
	 * @param beginDate
	 *            - The time the run has started.
	 * @param endDate
	 *            - The time the run has ended.
	 * @param numUrls
	 *            - The number of urls read from the input.xml file.
	 * @throws IOException
	 */
	protected static void runStatistics(Date beginDate, Date endDate, short numUrls) throws IOException {
		BufferedWriter bw;
		long secondsDiff = (endDate.getTime() - beginDate.getTime()) / 1000;
		double hoursDiff = ((double) secondsDiff / 3600);
		double minutes = (hoursDiff % 1) * 60;
		double seconds = (minutes % 1) * 60;
		bw = new BufferedWriter(new FileWriter("logs/" + dateString + "/" + hourString + "/logs.txt", true));
		bw.write(
				"-----------------------------------------------------------------------------------------------------------------------\n\n\n");
		bw.write("Total Scraping Time = " + (int) hoursDiff + " Hour " + (int) minutes + " Minutes " + seconds
				+ " Seconds\n");
		bw.write("Total Number of urls (from the input) = " + numUrls + " \n");
		bw.write("Average Time Per Url = " + ((double) secondsDiff / numUrls) + " Seconds\n");
		//bw.write("Average Function Time writeCssToFile() = " + ((double) (writeSeconds / 1000) / numUrls)
		//		+ " Seconds\n");
		bw.write("Font injection success = " + String.format("%.2f", ((double) fontsInjected / fontsTried) * 100)
				+ "%\n");
		bw.write("Total number of font files = " + fontsTried + "\n");
		bw.write("Number of font files succesfully injected = " + fontsInjected + "\n");
		bw.write("Number of image urls succesfully changed = " + imageUrlsChanged + "\n");
		bw.write("Number of css files written = " + numScrapedFiles + "\n");
		bw.close();
	}

	/**
	 * Simply writes the received status as a string to the log file.
	 * 
	 * @param status
	 *            - The received status to write.
	 */
	protected static void writeStatusToLogFile(String status) {
		try {
			BufferedWriter bw = new BufferedWriter(
					new FileWriter("logs/" + dateString + "/" + hourString + "/logs.txt", true));
			bw.write(status);
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Deletes a given directory and all of its inner files.
	 * 
	 * @param directory
	 *            - The given directory
	 */
	protected static void deleteDirectory(File directory) {
		File[] contents = directory.listFiles();
		if (contents != null) {
			for (File inner : contents) {
				deleteDirectory(inner);
			}
		}
		directory.delete();
	}

	/**
	 * Deletes scraped folders which were most likely already uploaded to the
	 * SFTP server. The general assumption is to keep only one day's scraping
	 * locally.
	 */
	protected static void deletePreviouslyScraped() {
		File currentDir = new File("./");
		File[] directories = currentDir.listFiles();
		if (directories != null) {
			for (File directory : directories) {
				if ((directory.getName().startsWith(dateString.substring(0, 4)))
						&& !directory.getName().equals(dateString)) {
					deleteDirectory(directory);
				}
			}
		}
	}
}
t time before we iterate to the next url.
						webThread.sleep(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} else {
					WebPage page = new WebPage(currentUrl, currentUserAgent, currentMappedCookies);
					page = null;
				}
				
			}
			writeStatusToLogFile(new Date() + " " + url + " " + userAgentChar + "\n");
			countUrls++;
		}
		return --countUrls;
	}

	/**
	 * Creates empty log files in a folder which matches the starting time of
	 * the program.
	 * 
	 * @param type
	 *            - The type of log file to be created.
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	protected static void createEmptyLogs(String type) throws FileNotFoundException, UnsupportedEncodingException {
		PrintWriter writer = new PrintWriter("logs/" + dateString + "/" + hourString + "/" + type + "Logs.txt",
				"UTF-8");
		writer.close();
	}

	/**
	 * Runs statistics of the entire scrape.
	 * 
	 * @param beginDate
	 *            - The time the run has started.
	 * @param endDate
	 *            - The time the run has ended.
	 * @param numU