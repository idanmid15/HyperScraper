package crawler;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebPage {
	private String mainUrl;
	private String userAgent;
	private HashMap<String, String> cookies;
	private String currentDirectory;
	private boolean connectionError;
	private String tempDOM;
	private byte connectionTries;

	public WebPage(String mainUrl, String userAgent, HashMap<String, String> cookies) {
		String imageFail = mainUrl.toLowerCase();
		if (imageFail.endsWith(".png") || imageFail.endsWith(".ico") || imageFail.endsWith(".gif")
				|| imageFail.endsWith(".jpg") || imageFail.endsWith(".svg"))
			return;
		this.mainUrl = mainUrl;
		this.userAgent = userAgent;
		this.cookies = cookies;
		this.connectionError = false;
		this.tempDOM = "";
		this.connectionTries = 0;
		String[] splitsForCurrent = mainUrl.split("/");

		// Finds the directory path
		this.currentDirectory = mainUrl.replace(splitsForCurrent[splitsForCurrent.length - 1], "");
		recursiveCrawl(mainUrl);
	}

	/**
	 * Recursively scrapes CSS files from main domain.
	 * 
	 * @param url
	 *            - the current url(main domain is the main)
	 */
	public void recursiveCrawl(String url) {
		Document doc;
		try {
			doc = Jsoup.connect(url).userAgent(this.userAgent).cookies(this.cookies).timeout(30000).get();

			if (url.toLowerCase().contains("css") || url.contains("?")) { // For
																			// comviq
				String destinaion = createFileDestination(url);
				if (destinaion == null) {
					return;
				}

				// Fetches all of the @import CSS URLs
				getImportsCss(doc);

				// Changes the inner image-urls so they wouldn't be relative
				changeImageUrls(doc);

				// Fetches all of the font URLs and converts them to base 64
				getFonts(doc);

				// Writes the DOM content to a file
				writeCssToFile(destinaion, doc);
			}

			// Finds inner CSS files found in <link> tags
			getLinksCss(doc);

			// Connection errors
		} catch (IOException e)

		{
			this.connectionError = true;
			// System.out.println(e);
			// e.printStackTrace();
			try {
				String error = e.toString();
				BufferedWriter bw = new BufferedWriter(new FileWriter(
						"logs/" + WebCrawler.dateString + "/" + WebCrawler.hourString + "/connectionErrorLogs.txt",
						true));
				bw.write(error + "\n" + "Main url = " + this.mainUrl + "\n\n\n");
				bw.close();

				// Try again
				if (connectionTries < 20 && error.contains("404")) {
					connectionTries++;
					recursiveCrawl(mainUrl);
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * Receives the URL just connected to and returns a valid path for creating
	 * the CSS file, also checks if the file already exists.
	 * 
	 * @param url
	 *            - The URL
	 * @return - The destination of the file. If the file already exists returns
	 *         null.
	 */
	public String createFileDestination(String url) {
		String destination = null;
		int indexQ = url.indexOf('?');

		// For CSS which keep changing
		if (indexQ > -1) {
			if ((url.length() - indexQ) > 70) {
				url = url.substring(0, indexQ + 70);
			}
			url = url.replace('?', '-').replace('=', '-').replace('_', '-').replace('&', '-').replace(',', '-');
			destination = WebCrawler.dateString + "/" + WebCrawler.scanForToday + "/companies/"
					+ url.replaceAll("http:\\/\\/", "").replaceAll("https:\\/\\/", "").replaceAll("www.", "")
							.replace(':', '-').replace('%', '-');
			if (destination.endsWith(".css")) {
				destination = destination.replaceAll("\\.css", "");
			}
			File temp = new File(destination + ".css");

			// Checks file existence in case we don't want to overwrite
			if (temp.exists()) {
				return null;
			}
			new File(destination).mkdirs();
		} else {
			destination = WebCrawler.dateString + "/" + WebCrawler.scanForToday + "/companies/"
					+ url.replaceAll("http:\\/\\/", "").replaceAll("https:\\/\\/", "").replaceAll("\\.css", "")
							.replaceAll("www.", "");

			// Checks file existence in case we don't want to overwrite
			File temp = new File(destination + ".css");
			if (temp.exists()) {
				return null;
			}
			new File(destination).mkdirs();
		}
		return destination + ".css";
	}

	/**
	 * Writes the DOM content to the path of the destination received.
	 * 
	 * @param destination
	 *            - the path of the file to be written.
	 * @param doc
	 *            - the document which the DOM content will be pulled out of
	 */
	public void writeCssToFile(String destination, Document doc) {
		BufferedWriter writer = null;
		try {

			// Checks file existence again in case was already created by
			// multithreading.
			File checkExistence = new File(destination);
			if (checkExistence.exists())
				return;
			writer = new BufferedWriter(new UnicodeWriter(new FileOutputStream(destination), "UTF-8"));

			// In case the current CSS was formatted to byte 64 format or image
			// URLs changed
			if (this.tempDOM.length() > 1) {
				writer.write(this.tempDOM);
			} else {
				writer.write(doc.html());
			}

			// Deletes last folder created(in the suffix of the path)
			File tempFile = new File(destination.substring(0, destination.length() - 4));
			tempFile.delete();
			WebCrawler.numScrapedFiles++;
		}

		// Writing to file errors
		catch (IOException e) {
			imageFontFileErrorHandler("FILE", "", e);
		} finally {
			try {
				if (writer != null)
					writer.close();
			} catch (IOException e) {
				System.out.println(e);
			}
		}
	}

	/**
	 * Generates a url to try out of the current directory and the suffix.
	 * 
	 * @param suffix
	 *            - The suffix to be joined with the main domain.
	 * @return - A new url to try to connect to
	 */
	public String generateMainDomainUrl(String suffix) {
		String[] spt = this.currentDirectory.split("/");
		return "http://" + spt[2] + suffix;
	}

	/**
	 * Receives a partial URL(found in regex) and completes it to a full url
	 * 
	 * @param partUrl
	 *            - The partial url
	 * @return - A complete url
	 */
	public String generateCompleteUrl(String partUrl) {
		String[] spt = partUrl.split("/");
		String prefix = "";
		if (spt[0].length() > 1) {
			prefix = spt[0];
		} else {
			prefix = spt[1];
		}
		if (partUrl.toLowerCase().startsWith("http")) {
			return null;
		} else if (partUrl.startsWith("//")) {
			return "http:" + partUrl;

			// Attempt to join the current directory with the partial url
		} else if (this.currentDirectory.contains(prefix) && !prefix.equals("..")) {
			return this.currentDirectory.substring(0, this.currentDirectory.indexOf(prefix)) + prefix
					+ partUrl.replace(prefix + "/", "");
		} else if (this.currentDirectory.endsWith("/") && partUrl.startsWith("/")) {
			return this.currentDirectory + partUrl.substring(1);
		} else {
			return this.currentDirectory + partUrl;
		}

	}

	/**
	 * Attempts a URLConnection with a known self host
	 * 
	 * @param url
	 *            - The url that the function will try to connect with
	 * @return
	 * @throws IOException
	 *             - Connection exceptions
	 */
	public InputStream knownHostConnection(URL url) throws IOException {
		URLConnection uc = url.openConnection();
		// uc.setRequestProperty("Host", url.getHost());
		// uc.setRequestProperty("Connection", "keep-alive");
		// uc.setRequestProperty("Accept",
		// "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
		// uc.setRequestProperty("Upgrade-Insecure-Requests", "1");
		// uc.setRequestProperty("Accept-Encoding", "gzip, deflate, sdch");
		// uc.setRequestProperty("Accept-Language", "en-US,en;q=0.8");*/
		uc.setRequestProperty("User-Agent", this.userAgent);
		uc.setConnectTimeout(0);
		return uc.getInputStream();
	}

	/**
	 * Changes the inner image-urls inside the CSS DOM in order to match the
	 * original reference to the image
	 * 
	 * @param doc
	 *            - The DOM object
	 */
	public void changeImageUrls(Document doc) {
		if (this.tempDOM.length() <= 1) {
			this.tempDOM = doc.html();
		}

		String urlTester = "";
		// URL url;
		Set<String> imageList = findImageUrls(doc.html());
		for (String imageUrl : imageList) {
			// WebCrawler.imageUrlsTried++;
			urlTester = generateCompleteUrl(imageUrl);
			if (urlTester == null)
				continue;

			this.tempDOM = this.tempDOM.replace(imageUrl,
					urlTester + "<!-- " + generateMainDomainUrl(imageUrl) + " -->");
			WebCrawler.imageUrlsChanged++;
		}
	}

	/**
	 * Receives the current CSS DOM, finds all font types and converts the URLs
	 * to base64 format inside the CSS itself which will later be written to the
	 * file
	 * 
	 * @param doc
	 *            - The current CSS DOM
	 */
	public void getFonts(Document doc) {
		Set<String> fontList = findFontUrls(doc.html());
		String urlTester = "";
		URL url;
		for (String fontUrl : fontList) {
			WebCrawler.fontsTried++;
			try {
				if (fontUrl.toLowerCase().startsWith("http")) {
					urlTester = fontUrl;
				} else {
					urlTester = generateCompleteUrl(fontUrl);
				}
				url = new URL(urlTester);
				encodeAndReplaceFont(url, fontUrl);
				url = null;
			} catch (IOException e) {

				// Connection to font errors
				try {

					// Second attempt
					url = new URL(generateMainDomainUrl(fontUrl));
					encodeAndReplaceFont(url, fontUrl);
					url = null;
				} catch (IOException e1) {
					try {

						// Third and final attempt
						url = new URL(generateMainDomainUrl(fontUrl).replace("http", "https"));
						encodeAndReplaceFont(url, fontUrl);
						url = null;
					} catch (IOException e2) {
						imageFontFileErrorHandler("FONT", urlTester, e2);
					}
				}
			}
		}
	}

	/**
	 * Connects to the URL, encodes the stream to BASE 64 and injects it instead
	 * of the original font URL.
	 * 
	 * @param url
	 *            - The URL object of the font file(to connect to).
	 * @param fontUrl
	 *            - The actual url address of the file.
	 * @throws IOException
	 */
	public void encodeAndReplaceFont(URL url, String fontUrl) throws IOException {
		InputStream fontinputStream = knownHostConnection(url);
		byte[] fontByteArray = getBytesFromInputStream(fontinputStream);
		String base64String = Base64.getEncoder().encodeToString(fontByteArray);
		this.tempDOM = this.tempDOM.replace(fontUrl, base64String);
		WebCrawler.fontsInjected++;
		fontinputStream.close();
	}

	/**
	 * s Receives an input stream and reads it all to a byte array
	 * 
	 * @param is
	 *            - The input stream
	 * @return - The byte array including the stream bytes
	 * @throws IOException
	 */
	public byte[] getBytesFromInputStream(InputStream is) throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		byte[] buffer = new byte[512];
		int count = 0;
		for (int lengthOfReadBytes; (lengthOfReadBytes = is.read(buffer)) != -1; count++) {
			byteArrayOutputStream.write(buffer, 0, lengthOfReadBytes);
		}
		byteArrayOutputStream.flush();
		byte[] b = new byte[count];
		b = byteArrayOutputStream.toByteArray();
		return b;
	}

	/**
	 * Finds inner fonts (woff and ttf)
	 * 
	 * @param input
	 *            - the DOM (CSS)
	 * @return - A list of addresses representing each font partial path
	 */
	public static Set<String> findFontUrls(String input) {
		String pattern = "url\\((['\"]?)([^\\)]*?\\.(woff|ttf)2?[^\\)]*?)\\1";
		Set<String> lt = new HashSet<String>();
		Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(input);
		while (m.find()) {
			if (m.group(2) != null) {
				lt.add(m.group(2));
			}
		}
		return lt;
	}

	/**
	 * Finds inner images (png, jpg, gif)
	 * 
	 * @param input
	 *            - The string to be searched
	 * @return - A list of addresses representing each image partial path
	 */
	public static Set<String> findImageUrls(String input) {
		String pattern = "url\\((['\"])?([^ ]*?\\.(ico|svg|png|jpg|gif)+?)(['\"])?\\)";
		Set<String> lt = new HashSet<String>();
		Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(input);
		while (m.find()) {
			if (m.group(2) != null && m.group(2).length() < 150) {
				lt.add(m.group(2));
			}
		}
		return lt;
	}

	/**
	 * Finds inner CSS addresses found after @import tags (within CSS files).
	 * 
	 * @param input
	 *            - the DOM
	 * @return - a list of addresses representing each inner CSS file
	 */
	public static Set<String> findImportsCss(String input) {
		String pattern = "@import (url\\()?['\"](.*?)['\"]";
		Set<String> lt = new HashSet<String>();
		Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(input);
		while (m.find()) {
			if (m.group(2) != null && m.group(2).length() < 150) {
				lt.add(m.group(2));
			}
		}
		return lt;
	}

	/**
	 * Activates recursiveCrawl on each @import CSS found in the inner CSS.
	 * 
	 * @param doc
	 *            - the current document.
	 */
	public void getImportsCss(Document doc) {
		Set<String> importList = findImportsCss(doc.html());
		String urlTester = "";
		for (String importUrl : importList) {
			if (!importUrl.toLowerCase().startsWith("http")) {
				urlTester = generateCompleteUrl(importUrl);
			} else {
				urlTester = importUrl;
			}
			try {
				tryCssUrl(doc, urlTester, importUrl);
				changeImportedPath(doc, importUrl);
			} catch (IOException e) {
				imageFontFileErrorHandler("connection", importUrl, e);
			}
		}
	}

	/**
	 * @param doc
	 *            - The document.
	 * @param defaultUrl
	 *            - The first and default url to try(which came from
	 *            generateCompleteUrl).
	 * @param partialUrl
	 *            - The partial url which will be used to test other
	 *            combinations with.
	 * @throws IOException
	 */
	protected void tryCssUrl(Document doc, String defaultUrl, String partialUrl) throws IOException {
		String absHref = null;
		try {
			Jsoup.connect(defaultUrl).cookies(this.cookies).get();
			WebPage page = new WebPage(defaultUrl, this.userAgent, this.cookies);
			page = null;
		} catch (IOException e) {
			try {
				absHref = generateMainDomainUrl(partialUrl);
				Jsoup.connect(absHref).cookies(this.cookies).get();
				WebPage page = new WebPage(absHref, this.userAgent, this.cookies);
				page = null;
			} catch (IOException e1) {
				absHref = absHref.replace("http", "https");
				Jsoup.connect(absHref).cookies(this.cookies).get();
				WebPage page = new WebPage(absHref, this.userAgent, this.cookies);
				page = null;
			}
		}
	}

	/**
	 * Changes the url of the import AFTER it has been imported in case it
	 * contains special characters.
	 * 
	 * @param doc
	 *            - The document.
	 * @param importUrl
	 *            - The original import URL.
	 */
	protected void changeImportedPath(Document doc, String importUrl) {
		int indexQ = importUrl.indexOf('?');
		String changedUrl = "";
		// For CSS which keep changing
		if (indexQ > -1) {
			/*
			 * if ((importUrl.length() - indexQ) > 70) { importUrl =
			 * importUrl.substring(0, indexQ + 70); }
			 */
			changedUrl = importUrl.replace('?', '-').replace('=', '-').replace('_', '-').replace('&', '-')
					.replace(',', '-').replace(':', '-').replace('%', '-');
			this.tempDOM = doc.html().replace(importUrl, changedUrl);
		}
	}

	/**
	 * Handles second attempted connection error with fonts, files and
	 * images(writes to errorlogs.txt)
	 * 
	 * @param type
	 *            - The type of error(FONT or IMAGE or FILE)
	 * @param e
	 *            - The error itself
	 */
	public void imageFontFileErrorHandler(String type, String urlAttempt, IOException e) {
		try {
			String error = e.toString();
			BufferedWriter bw = new BufferedWriter(new FileWriter(
					"logs/" + WebCrawler.dateString + "/" + WebCrawler.hourString + "/" + type + "ErrorLogs.txt",
					true));
			bw.write(error + "\n" + "Main url = " + this.mainUrl + "\n" + "Attempted url = " + urlAttempt
					+ "\n\n\n\n\n");
			bw.close();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
	}

	/**
	 * Finds inner CSS files found in <link> tags
	 * 
	 * @param doc
	 *            the current document
	 */
	public void getLinksCss(Document doc) {
		Elements links = doc.select("link"); // [href*=css]
		doc = null;
		for (Element link : links) {
			String absHref = link.attr("abs:href");
			if (absHref.toLowerCase().contains("css") || link.attr("rel").contains("style")) {
				if (absHref.toLowerCase().startsWith("http")) {
					WebPage page = new WebPage(absHref, this.userAgent, this.cookies);
					page = null;
				} else {
					String st = link.attr("href");
					String absHrefComplete = generateCompleteUrl(st);
					try {
						tryCssUrl(doc, absHrefComplete, st);
					} catch (IOException e) {
						imageFontFileErrorHandler("connection", st, e);
					}
				}
			}
		}
	}
}