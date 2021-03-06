package crawler;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Date;
import org.jsoup.nodes.Element;

public class ThreadedDownloader implements Runnable {

	//private static LoginCrawler loginCrawler = new LoginCrawler();
	/*static {
		loginCrawler.nikeShoes();
		loginCrawler.alamoSelectCar(new Date());
	}*/
	public void run() {
		try {
			processAndDownload();
		} catch (FileNotFoundException e) {
			HyperScraper.fileErrorHandler(e);
		}
	}

	private void processAndDownload() throws FileNotFoundException {
		Element xmlElement = null;
		boolean finishedWork = false;
		do {
			try {
				if (HyperScraper.numOfFinishedThreads == HyperScraper.scrapingThreads.length && HyperScraper.urlQueue.size() == 0) {
					Thread.sleep(1000); // For all scenarios where the scraper terminates too early(extremely rare)
				}
				xmlElement = HyperScraper.urlQueue.take();
			} catch (InterruptedException e) {
				HyperScraper.fileErrorHandler(e);
			}
			if (!xmlElement.baseUri().contains("endthis.com")) {
				synchronized (HyperScraper.scrapingThreadLockForNumOfThreadsCount) {
					HyperScraper.numOfFinishedThreads--;
				}
				try {
					processElement(xmlElement);
				} catch (Exception e1) {
					e1.printStackTrace((new PrintWriter(String.format("logs/%s/%s/ErrorLogs/FILEErrorLogs.txt", HyperScraper.dateString, HyperScraper.hourString))));
				}
				synchronized (HyperScraper.scrapingThreadLockForNumOfThreadsCount) {
					HyperScraper.numOfFinishedThreads++;
				}
			} else {
				synchronized (HyperScraper.scrapingThreadLockForNumOfThreadsCount) {
					HyperScraper.numOfFinishedThreads++;
				}
				finishedWork = true;
			}
		} while (!finishedWork);
		HyperScraper.writeStatusToLogFile(Thread.currentThread().getName() + " terminated\n");
	}

	
	private static void processElement(Element i_ElementToProcess) {
		String url = i_ElementToProcess.attr("url");
		boolean imitateBrowser = Boolean.valueOf(i_ElementToProcess.attr("imitateBrowser"));
		boolean generatedSource = Boolean.valueOf(i_ElementToProcess.attr("generatedSource"));
		XmlTagSettings xmlTagSettings = new XmlTagSettings(i_ElementToProcess);
		if (!imitateBrowser) {
			if (generatedSource) {
				//new GeneratedSourceCrawler(url, xmlTagSettings);
			} else {
				new SourceCrawler(url, xmlTagSettings);
			}
		} else {
			//new LoginCrawler().imitateBrowser(url, xmlTagSettings);
		}
		HyperScraper.writeStatusToLogFile(
				String.format("%s: %s:\n---------------------------------------------------------------------------------------------------------------------------------------\n"
						+ "Finished Scraping: Url = %s\n%s\n---------------------------------------------------------------------------------------------------------------------------------------\n",
				new Date().toString().substring(11, 19), Thread.currentThread().getName(), url, xmlTagSettings));
	}
}