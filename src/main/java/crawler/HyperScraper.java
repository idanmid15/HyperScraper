package crawler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class HyperScraper {
	final static String DESKTOP = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.71 Safari/537.36";
	final static String MOBILE = "Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13B143 Safari/601.1";
	final static String TABLET = "Mozilla/5.0 (iPad; CPU OS 7_0_4 like Mac OS X) AppleWebKit/537.51.1 (KHTML, like Gecko) Version/7.0 Mobile/11B554a Safari/9537.53";
	final static String FIREFOX = "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:48.0) Gecko/20100101 Firefox/48.0";
	static long totalTransferredSizeBytes = 0;

	static String dateString = null;
	static String hourString = null;
	static String currentUserAgent = null;
	static String currentUrl = null;
	static byte scanForToday = 0;
	static short uploadFileCount = 0;	
	static byte numOfFinishedThreads = 0;
	static Object scrapingThreadLockForNumOfThreadsCount = new Object();

	static Object sftpLock = new Object();
	static Object localSearcherWakeLock = new Object();
	static boolean finishedMainFlag = false;
	static boolean debugMode = false;

	protected static ArrayBlockingQueue<Element> urlQueue = new ArrayBlockingQueue<Element>(500);
	protected static ArrayBlockingQueue<File> localDirectoryQueue = new ArrayBlockingQueue<File>(2000);
	protected static ArrayBlockingQueue<File> localFileQueue = new ArrayBlockingQueue<File>(1500);
	protected static ThreadedDownloader[] threadedWebPages;
	protected static ThreadedUploader[] threadedUploaders;
	protected static ThreadedFileSearcher[] threadedFileSearchers;
	protected static Thread[] scrapingThreads;
	protected static Thread[] uploadingThreads;
	protected static Thread[] fileSearchingThreads;
	protected static ConcurrentHashMap<File, Boolean> localFileMap = new ConcurrentHashMap<File, Boolean>();



	public static void main(String[] args) {
		try {
			System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
			System.setProperty("webdriver.chrome.driver", "bin2/chromedriver.exe");
			System.setProperty("http.proxyHost", "localhost");
			System.setProperty("http.proxyPort", "8888");
			Date beginDate = new Date();
			if (!checkInputFileForDebugMode() && !checkIfScrapingTime(beginDate) && lastScrapeSuccess()) {
				return;
			}
			
			createLogsDirectory(beginDate);
			updateStatusFile("Running");
			
			// Creates new log files
			createEmptyLogs();
			new File(dateString + "/" + scanForToday + "/companies/").mkdirs();
			writeStatusToLogFile("Scraper initializing...\n");
			Configuration config = new Configuration();
			if (!config.isValid) return;
			setupThreadNumbers(config);
			startUploadingThreads(config);
			startScrapingThreads();
			startFileSearchingThreads();
			writeStatusToLogFile("Beginning to Scrape!\n\n");
			short numUrls = processInput();
			finishedMainFlag = true;
			terminateThreads(scrapingThreads);
			writeStatusToLogFile("Still uploading local files to SFTP server.......................");
			terminateThreads(fileSearchingThreads);
			terminateThreads(uploadingThreads);
			writeStatusToLogFile("Deleting past scraped folders...\n");
			deletePreviouslyScrapedFromLocal();
			writeStatusToLogFile("Previously scraped folders have been deleted!\n\n");
			Date endDate = new Date();
			runStatistics(beginDate, endDate, numUrls, uploadFileCount);
			updateStatusFile("Success");
			writeStatusToLogFile("\nScraper Terminated Successfully!");
		} catch (Exception e) {
			fileErrorHandler(e);
		} 
	}

	/**
	 * This function sets up the appropriate number of threads in the system, according to what
	 * has been delivered by the config.
	 * @param config
	 */
	private static void setupThreadNumbers(Configuration config) {
		threadedWebPages = new ThreadedDownloader[config.numOfScrapingThreads];
		threadedUploaders = new ThreadedUploader[config.numOfUploadingThreads];
		threadedFileSearchers = new ThreadedFileSearcher[config.numOfLocalFileSearchers];
		scrapingThreads = new Thread[config.numOfScrapingThreads];
		uploadingThreads = new Thread[config.numOfUploadingThreads];
		fileSearchingThreads = new Thread[config.numOfLocalFileSearchers];
		numOfFinishedThreads = (byte) config.numOfScrapingThreads;
	}

	/**
	 * Checks whether or not this is the normal scraping time: 12 am or PM
	 * @param currentDate - The current date.
	 * @return - Whether or not the scraper should scrape
	 */
	private static boolean checkIfScrapingTime(Date currentDate) {
		@SuppressWarnings("deprecation")
		int currentHour = currentDate.getHours();
		return ((currentHour >= 12 && currentHour < 13) || (currentHour >= 0 && currentHour < 1));
	}

	
	/**
	 * Creates all relevant empty log files within the "logs/" directory.
	 * 
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	private static void createEmptyLogs() throws FileNotFoundException, UnsupportedEncodingException {
		createEmptyLogFile("");
		createEmptyLogFile("UPLOAD");
		createEmptyLogFile("DOWNLOAD");
		createEmptyLogFile("ErrorLogs/connectionError");
		createEmptyLogFile("ErrorLogs/IMAGEError");
		createEmptyLogFile("ErrorLogs/FONTError");
		createEmptyLogFile("ErrorLogs/FILEError");
		createEmptyLogFile("ErrorLogs/UPLOADError");
	}

	/**
	 * Creates the current run's logs directory based on the current date and
	 * time.
	 * 
	 * @param beginDate
	 *            - The beginning date object
	 */
	@SuppressWarnings("deprecation")
	private static void createLogsDirectory(Date beginDate) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		SimpleDateFormat hourFormat = new SimpleDateFormat("HH-mm-ss");
		dateString = dateFormat.format(beginDate);
		hourString = hourFormat.format(beginDate);
		scanForToday = (byte) ((beginDate.getHours() >= 12) ? 2 : 1);
		new File("logs/" + dateString + "/" + hourString + "/ErrorLogs").mkdirs();
	}
	
	/**
	 * Checks if the Scraper should run in debug mode or not.
	 * @return - Boolean which represents whether or not Scraper is in debug mode.
	 */
	private static boolean checkInputFileForDebugMode() {
		File input = new File("input/input.xml");
		String firstLine = "";
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(new FileReader(input));
			firstLine = bufferedReader.readLine();
			bufferedReader.close();
		} catch (IOException e) {
			fileErrorHandler(e);
		}
		debugMode = firstLine.contains("<Debug>");
		
		// This is so the user won't forget to delete the debug mode after using it.
		if (debugMode) {
			deleteContainingLinesFromFile("<Debug>", new File("input/input.xml"));
		}
		return debugMode;
	}
	
	/**
	 * Receives a file and a string which will be searched within the file.
	 * Deletes all lines containing the string from the file.
	 * @param i_StringToDelete - The string to be searched and deleted.
	 * @param i_FileToDeleteFrom - The file to delete lines from.
	 */
	private static void deleteContainingLinesFromFile(String i_StringToDelete, File i_FileToDeleteFrom) {
		List<String> lines;
		try {
			lines = FileUtils.readLines(i_FileToDeleteFrom);
			List<String> updatedLines = lines.stream().map(line -> line.replace("<Debug>", "")).collect(Collectors.toList());
			FileUtils.writeLines(i_FileToDeleteFrom, updatedLines, false);
		} catch (IOException e) {
			fileErrorHandler(e);
		}
	}
	
	/**
	 * Checks if the Scraper should run due to the fact that the last run failed.
	 * @return - Boolean which represents whether or not Scraper was successful in the last run.
	 */
	private static boolean lastScrapeSuccess() {
		File input = new File("logs/LastStatus.txt");
		String checkDebugMode = "";
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(new FileReader(input));
			checkDebugMode = bufferedReader.readLine();
			bufferedReader.close();
		} catch (IOException e) {
			fileErrorHandler(e);
		}
		return (checkDebugMode.toLowerCase().startsWith("su"));
	}
	
	/**
	 * Update the status file according to the received status.
	 * @param i_Status - The received status
	 */
	private static void updateStatusFile(String i_Status) {
		File input = new File("logs/LastStatus.txt");
		BufferedWriter bufferedWriter = null;
		try {
			input.createNewFile();
			bufferedWriter = new BufferedWriter(new FileWriter(input, false));
			bufferedWriter.write(i_Status);
			bufferedWriter.close();
		} catch (IOException e) {
			fileErrorHandler(e);
		} 
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
	protected static short processInput() throws IOException {
		File input = new File("input/input.xml");
		Document doc = Jsoup.parse(input, "UTF-8", "");
		Elements links = doc.select("link");
		for (Element link : links) {
			try {
				urlQueue.put(link);
			} catch (InterruptedException e) {
				fileErrorHandler(e);
			}
		}
		return (short) links.size();
	}

	/**
	 * Starts the threads which will each send requests to various URLs.
	 */
	public static void startScrapingThreads() {
		for (int j = 0; j < threadedWebPages.length; j++) {
			threadedWebPages[j] = new ThreadedDownloader();
			scrapingThreads[j] = new Thread(threadedWebPages[j]);
			scrapingThreads[j].start();
		}
	}

	/**
	 * Starts the threads which will each upload various files to the SFTP.
	 */
	public static void startUploadingThreads(Configuration config) {
		for (int j = 0; j < threadedUploaders.length; j++) {
			threadedUploaders[j] = new ThreadedUploader(config);
			uploadingThreads[j] = new Thread(threadedUploaders[j]);
			uploadingThreads[j].start();
		}
	}
	
	/**
	 * Starts the threads which will each upload various files to the SFTP.
	 */
	public static void startFileSearchingThreads() {
		for (int j = 0; j < threadedFileSearchers.length; j++) {
			threadedFileSearchers[j] = new ThreadedFileSearcher();
			fileSearchingThreads[j] = new Thread(threadedFileSearchers[j]);
			fileSearchingThreads[j].start();
		}
	}

	/**
	 * Terminates the threads.
	 */
	public static void terminateThreads(Thread[] threadsToTerminate) {
		for (int i = 0; i < threadsToTerminate.length; i++) {
			try {
				threadsToTerminate[i].join();
			} catch (InterruptedException e) {
				System.out.println("Usage: Thread interrupted exception");
			}
		}
	}

	/**
	 * Creates an empty log file in a folder which matches the starting time of
	 * the program.
	 * 
	 * @param type
	 *            - The type of log file to be created.
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	protected static void createEmptyLogFile(String type) throws FileNotFoundException, UnsupportedEncodingException {
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
	protected static void runStatistics(Date beginDate, Date endDate, short numUrls, short numCssFiles)
			throws IOException {
		BufferedWriter bw;

		// From the loginScraper
		numUrls += 2;
		long secondsDiff = (endDate.getTime() - beginDate.getTime()) / 1000;
		double hoursDiff = ((double) secondsDiff / 3600);
		double minutes = (hoursDiff % 1) * 60;
		double seconds = (minutes % 1) * 60;
		bw = new BufferedWriter(new FileWriter("logs/" + dateString + "/" + hourString + "/logs.txt", true));
		bw.write(
				"--------------------------------------------------------------------------------------------------------------\n\n\n");
		
		bw.write(String.format("Total Scraping and uploading Time = %d Hour %d Minutes %.2f Seconds\n", (int) hoursDiff, (int) minutes, seconds));
		bw.write("Total Number of Urls (from the input) = " + numUrls + " \n");
		bw.write("Total Number of CSS files = " + numCssFiles + " \n");
		
		bw.write(String.format("Average Number of CSS files per Url = %.2f\n\n", ((double) numCssFiles / numUrls)));
		bw.write(String.format("Average Time Per Url = %.2f Seconds\n", ((double) secondsDiff / numUrls)));
		bw.write(String.format("Average Time Per CSS file = %.2f Seconds\n\n", ((double) secondsDiff / numCssFiles)));
		bw.write(String.format("Total Uploaded Size = %.2fMb\n", ((double) totalTransferredSizeBytes / Math.pow(2, 20))));
		bw.write(String.format("Average Uploaded File Size = %.2fMb\n\n", ((double) totalTransferredSizeBytes / Math.pow(2, 20)) / numCssFiles));
		//bw.write(String.format("Average File Increase of Size = %.2f%%\n",((double) totalTransferredSizeBytes / totalOriginalFilesSize  * 100)));
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
					new FileWriter(String.format("logs/%s/%s/logs.txt", dateString, hourString), true));
			bw.write(String.format("%s\n", status));
			bw.close();
		} catch (IOException e) {
			fileErrorHandler(e);
		}
	}
	
	/**
	 * Simply writes the received status as a string to the log file.
	 * 
	 * @param status
	 *            - The received status to write.
	 */
	protected static void writeStatusToSpecificLogFile(String status, String logFile) {
		try {
			BufferedWriter bw = new BufferedWriter(
					new FileWriter(String.format("logs/%s/%s/%slogs.txt", dateString, hourString, logFile), true));
			bw.write(String.format("%s\n", status));
			bw.close();
		} catch (IOException e) {
			fileErrorHandler(e);
		}
	}
	
	/**
	 * Handles File errors (writes them to the fileErrors)
	 * 
	 * @param e
	 *            - The error itself
	 */
	protected static void fileErrorHandler(Exception e) {
		try {
			String error = e.toString();

			BufferedWriter bw = new BufferedWriter(new FileWriter(String.format("logs/%s/%s/ErrorLogs/FILEErrorLogs.txt",
					HyperScraper.dateString, HyperScraper.hourString), true));
			bw.write(String.format("%s\n", error));
			bw.close();
		} catch (IOException e2) {
			e2.printStackTrace();
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
	protected static void deletePreviouslyScrapedFromLocal() {
		File currentDir = new File("./");
		File[] directories = currentDir.listFiles();
		if (directories != null) {
			for (File directory : directories) {
				if ((directory.getName().startsWith(dateString.substring(0, 2)))
						 && getDirectorySize(directory) < 5000) {
					deleteDirectory(directory);
				}
			}
		}
	}
	
	/**
	 * Calculates the given directory's size of CSS files recursively.
	 * 
	 * @param directory
	 *            - The given directory.
	 * @return - The directory's size in bytes.
	 */
	protected static long getDirectorySize(File directory) {
		long numberOfBytes = 0;
		File[] directories = directory.listFiles();
		if (directories != null) {
			for (File innerDirectory : directories) {
				if (innerDirectory.isFile() && innerDirectory.getName().endsWith("css")) {
					if (innerDirectory.getName().endsWith("css")) {
						numberOfBytes += innerDirectory.length();
					}
				} else {
					numberOfBytes += getDirectorySize(innerDirectory);
				}
			}
		}
		return numberOfBytes;
	}
	
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
			cookieValue = "";
			if (cookiesplit.length > 1) {
				cookieValue = "";
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
}


*            - The received status to write.
	 */
	protected static void writeStatusToLogFile(String status) {
		try {
			BufferedWriter bw = new BufferedWriter(
					new FileWriter(String.format("logs/%s/%s/logs.txt", dateString, hourString), true));
			bw.write(String.format("%s\n", status));
			bw.close();
		} catch (IOException e) {
			fileErrorHandler(e);
		}
	}
	
	/**
	 * Simply writes the received status as a string to the log file.
	 * 
	 * @param status
	 *            - The received status to write.
	 */
	protected static void writeStatusToSpecificLogFile(String status, String logFile) {
		try {
			BufferedWriter bw = new BufferedWriter(
					new FileWriter(String.format("logs/%s/%s/%slogs.txt", dateString, hourString, logFile), true));
			bw.write(String.format("%s\n", status));
			bw.close();
		} catch (IOException e) {
			fileErrorHandler(e);
		}
	}
	
	/**
	 * Handles File errors (writes them to the fileErrors)
	 * 
	 * @param e
	 *            - The error itself
	 */
	protected static void fileErrorHandler(Exception e) {
		try {
			String error = e.toString();

			BufferedWriter bw = new BufferedWriter(new FileWriter(String.format("logs/%s/%s/ErrorLogs/FILEErrorLogs.txt",
					HyperScraper.dateString, HyperScraper.hourString), true));
			bw.write(String.format("%s\n", error));
			bw.close();
		} catch (IOException e2) {
			e2.printStackTrace();
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
			