package crawler;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Entities.EscapeMode;

public class SourceCrawler extends WebCrawler {
	private StringBuilder tempDOM;
	private byte connectionTries;
	private String cleanLocalDirectory;
	private String localDestination;

	private static HashSet<Character> m_IllegalCharacters = new HashSet<Character>();

	static {
		m_IllegalCharacters.add('?');
		m_IllegalCharacters.add('=');
		m_IllegalCharacters.add('&');
		m_IllegalCharacters.add(',');
		m_IllegalCharacters.add(':');
		m_IllegalCharacters.add('%');
		m_IllegalCharacters.add('!');
		m_IllegalCharacters.add('@');
		m_IllegalCharacters.add('$');
		m_IllegalCharacters.add('^');
		m_IllegalCharacters.add('*');
	}

	public SourceCrawler(String mainUrl, XmlTagSettings xmlTagSettings) {
		this(mainUrl, xmlTagSettings, "");
	}

	public SourceCrawler(String mainUrl, XmlTagSettings xmlTagSettings, String referrer) {
		String imageFail = mainUrl.toLowerCase();
		if (imageFail.endsWith(".png") || imageFail.endsWith(".ico") || imageFail.endsWith(".gif")
				|| imageFail.endsWith(".jpg") || imageFail.endsWith(".svg"))
			return;
		this.mainUrl = mainUrl;
		this.tempDOM = new StringBuilder();
		this.connectionTries = 0;
		this.localDestination = "";

		// Finds the directory path
		this.currentDirectory = getCurrentDirectory(mainUrl);
		this.xmlTagSettings = xmlTagSettings;
		crawlPage(referrer);
	}

	/**
	 * Recursively scrapes CSS files from main domain.
	 *
	 * @param url - the current url(main domain is the main)
	 */
	public void crawlPage(String referrer) {
		try {
			this.document = Jsoup.connect(mainUrl).userAgent(xmlTagSettings.userAgent).cookies(xmlTagSettings.cookies)
					.timeout(30000).maxBodySize(0).referrer(referrer).get();
			document.outputSettings().escapeMode(EscapeMode.xhtml);

			// Finds inner CSS files found in <link> tags
			getCssLinkTags();
			if (mainUrl.toLowerCase().contains("css") || mainUrl.contains("?")) { // For
				// comviq
				localDestination = createFileDestination(mainUrl);
				if (localDestination == null) {
					return;
				}

				// Fetches all of the @import CSS URLs
				getImportsCss();

				// Changes the inner image-urls so they wouldn't be relative
				changeImageUrls();

				this.cleanLocalDirectory = String.format("%s/%d/companies/%s", HyperScraper.dateString,
						HyperScraper.scanForToday, this.currentDirectory.replaceFirst("http:\\/\\/", "")
								.replaceFirst("https:\\/\\/", "").replaceFirst("www\\.", ""));
				// Fetches all of the font URLs and converts them to base 64
				getFonts();

				// Writes the DOM content to a file
				writeCssToFile(localDestination);
			}

			// Connection errors
		} catch (Exception e) {
			try {
				tryReconnecting(e);
			} catch (IOException e1) {
				// Finish this attempt by jumping to the finally block this error was already logged
			}
		} finally {
			this.tempDOM = null;
			this.document = null;
		}
	}

	/**
	 * Receives the URL just connected to and returns a valid path for creating
	 * the CSS file, also checks if the file already exists.
	 *
	 * @param url - The URL
	 * @return - The destination of the file. If the file already exists returns
	 * null.
	 */
	public String createFileDestination(String url) {
		String destination = null;
		int indexQ = url.indexOf('?');

		// For CSS which keep changing
		if (indexQ > -1) {
			if ((url.length() - indexQ) > 70) {
				url = url.substring(0, indexQ + 70);
			}
			if (xmlTagSettings.userBasedCss) {
				url = url.substring(0, indexQ);
				destination = String.format("%s/%d/companies/%s-%d", HyperScraper.dateString, HyperScraper.scanForToday,
						url.replaceFirst("http:\\/\\/", "").replaceFirst("https:\\/\\/", "").replaceFirst("www\\.", ""),
						xmlTagSettings.numOfUserBasedCss);
			} else {
				destination = String.format("%s/%d/companies/%s", HyperScraper.dateString, HyperScraper.scanForToday,
						url.replaceFirst("http:\\/\\/", "").replaceFirst("https:\\/\\/", "").replaceFirst("www\\.",
								""));
				for (char illegalChar : m_IllegalCharacters) {
					destination = destination.replace(illegalChar, '-');
				}
			}
			if (destination.endsWith(".css")) {
				destination = destination.replaceAll("\\.css", "");
			}
		} else if (xmlTagSettings.userBasedCss) {
			destination = String.format("%s/%d/companies/%s-%d", HyperScraper.dateString,
					HyperScraper.scanForToday, this.currentDirectory.replaceFirst("http:\\/\\/", "")
							.replaceFirst("https:\\/\\/", "").replaceFirst("www\\.", ""),
					xmlTagSettings.numOfUserBasedCss);
		} else {
			destination = String.format("%s/%d/companies/%s", HyperScraper.dateString, HyperScraper.scanForToday,
					url.replaceFirst("http:\\/\\/", "").replaceFirst("https:\\/\\/", "").replaceAll("\\.css", "")
							.replaceFirst("www\\.", ""));
		}

		// Checks file existence in case we don't want to overwrite
		File temp = new File(destination + ".css");
		if (temp.exists()) {
			return null;
		}
		if (xmlTagSettings.lowerCaseDirectories) {
			destination = destination.toLowerCase();
		}
		new File(destination).mkdirs();
		return String.format("%s.css", destination);
	}

	/**
	 * Tries to reconnect to the main URL until it meets the allowed limit.
	 *
	 * @param e
	 * @throws IOException
	 */
	private void tryReconnecting(Exception e) throws IOException {
		String error = e.toString();

		// Try again
		if (connectionTries < 5 && error.contains("404")) {
			connectionTries++;
			crawlPage(this.mainUrl);
		} else {
			BufferedWriter bw = new BufferedWriter(new FileWriter(String.format(
					"logs/%s/%s/ErrorLogs/connectionErrorLogs.txt", HyperScraper.dateString, HyperScraper.hourString), true));

			bw.write(String.format("%s\nMain url = %s\n\n\n", error, mainUrl));
			bw.close();
		}
	}


	/**
	 * Writes the DOM content to the path of the destination received.
	 *
	 * @param destination - the path of the file to be written.
	 */
	public void writeCssToFile(String destination) {
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
				writer.write(String.format("/* %s */\n%s", generateRegexForSftp(destination), this.tempDOM.toString()));
			} else {
				writer.write(
						String.format("/* %s */\n%s", generateRegexForSftp(destination), this.document.body().text()));
			}

			// Deletes last folder created(in the suffix of the path)
			File tempFolder = new File(destination.substring(0, destination.length() - 4));
			tempFolder.delete();
			HyperScraper.writeStatusToSpecificLogFile(String.format("%s: Downloaded File: %s",
					new Date().toString().substring(11, 19), this.mainUrl), "DOWNLOAD");
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
	 * Generates a regEx for replacing the main URL with the SFTP url
	 *
	 * @param i_LocalDestination - The local path of this CSS file - in order to parse the main
	 *                           domain.
	 * @return - The regEx.
	 */
	private String generateRegexForSftp(String i_LocalDestination) {
		String mainDomain = i_LocalDestination.split("/")[3];
		String regEx = String.format("href=['\"][^\"']*?(%s)?", mainDomain.replace(".", "\\\\."));
		int indexQ = mainUrl.indexOf('?');

		// For CSS which keep changing
		if (indexQ > -1) {

			regEx = String.format("%s([^\"']*?css)([^\"']*?)\\\\?([^\"']*?)", regEx);
			StringBuilder stringBuilder = new StringBuilder();
			for (int i = indexQ + 1; i < mainUrl.length(); i++) {
				char currentChar = mainUrl.charAt(i);
				if (m_IllegalCharacters.contains(currentChar)) {
					stringBuilder.append(currentChar);
					stringBuilder.append("([^\"']*?)");
				}
			}
			regEx = String.format("%s%s", regEx, stringBuilder.toString());
		} else {
			regEx = String.format("%s([^\"']*?\\\\.css)", regEx);
		}
		return String.format("%s['\"]", regEx);
	}

	/**
	 * Changes the inner image-urls inside the CSS DOM in order to match the
	 * original reference to the image
	 */
	public void changeImageUrls() {
		if (this.tempDOM.length() <= 1) {
			this.tempDOM.append(this.document.body().text());
		}

		String tempUrl = "";
		Set<String> imageList = RegexFinders.findImageUrls(this.document.html());
		for (String imageUrl : imageList) {
			tempUrl = generateCompleteUrl(imageUrl);

			// In case the url already is a valid url ("http...")
			if (tempUrl == null)
				continue;

			if (this.mainUrl.contains("redhat.com")) {
				tempUrl = generateMainDomainUrlUnsecured(imageUrl);
			}
			replaceAllOccurrencesWithinTempDOM(imageUrl, tempUrl);
		}
	}

	/**
	 * This method is made to replace all occurrences of a string within the
	 * StringBuilder object.
	 *
	 * @param original    - The original string to replace
	 * @param replacement - The replacement string.
	 */
	private void replaceAllOccurrencesWithinTempDOM(String original, String replacement) {
		int currentIndexOfOriginal = this.tempDOM.indexOf(original);
		int lookAHead = 0;
		char lookAHeadChar = 0;
		if (currentIndexOfOriginal != -1) {
			do {
				lookAHead = currentIndexOfOriginal + original.length();
				lookAHeadChar = this.tempDOM.charAt(lookAHead);
				if (lookAHeadChar == '?' || (lookAHeadChar == '2' && original.endsWith("woff"))) { // For
					// cases
					// where
					// woff
					// replaces
					// woff2
					// mistakenly
					currentIndexOfOriginal = this.tempDOM.indexOf(original, currentIndexOfOriginal + 1);
					continue;
				}
				this.tempDOM.delete(currentIndexOfOriginal, currentIndexOfOriginal + original.length());
				this.tempDOM.insert(currentIndexOfOriginal, replacement);
				currentIndexOfOriginal = this.tempDOM.indexOf(original, currentIndexOfOriginal + replacement.length());
			} while (currentIndexOfOriginal != -1);
		}
	}

	/**
	 * Receives the current CSS DOM, finds all font types and converts the URLs
	 * to base64 format inside the CSS itself which will later be written to the
	 * file
	 */
	public void getFonts() {
		Set<String> fontList = RegexFinders.findFontUrls(this.document.html());
		String urlTester = "";
		URL url;
		for (String fontUrl : fontList) {
			// WebCrawler.fontsTried++;
			try {
				if (fontUrl.toLowerCase().startsWith("http")) {
					urlTester = fontUrl;
				} else {
					urlTester = generateCompleteUrl(fontUrl);
				}
				url = new URL(simplifyPath(urlTester));
				encodeAndReplaceFont(url, fontUrl);
				url = null;
			} catch (IOException e) {

				// Connection to font errors
				try {

					// Second attempt
					url = new URL(simplifyPath(generateMainDomainUrlUnsecured(fontUrl)));
					encodeAndReplaceFont(url, fontUrl);
					url = null;
				} catch (IOException e1) {
					try {

						// Third and final attempt
						url = new URL(simplifyPath(generateMainDomainUrlUnsecured(fontUrl).replace("http", "https")));
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
	 * @param url     - The URL object of the font file(to connect to).
	 * @param fontUrl - The actual url address of the file.
	 * @throws IOException
	 */
	public void encodeAndReplaceFont(URL url, String fontUrl) throws IOException {
		String fontName = url.getFile().substring(url.getFile().lastIndexOf('/') + 1);
		int indexQ = fontName.indexOf("?");
		if (indexQ > -1) {
			fontName = fontName.substring(0, indexQ);
		}
		File tempFont = new File(String.format("%s/%s", this.cleanLocalDirectory, fontName));
		if (xmlTagSettings.securedFonts) {
			URLConnection uc = url.openConnection();
			uc.setRequestProperty("Upgrade-Insecure-Requests", "1");
			uc.setRequestProperty("User-Agent", xmlTagSettings.userAgent);
			uc.setRequestProperty("Host", url.getHost());
			uc = followRedirects(uc);
			copyURLToFile(uc, tempFont);
		} else {
			FileUtils.copyURLToFile(url, tempFont, 10000, 10000);
		}
		byte[] fontByteArray = FileUtils.readFileToByteArray(tempFont);
		String base64String = Base64.getEncoder().encodeToString(fontByteArray);
		tempFont.delete();
		if (fontUrl.contains("woff2")) {
			replaceAllOccurrencesWithinTempDOM(fontUrl, String.format("data:font/woff2;base64,%s", base64String));
		} else if (fontUrl.contains("ttf")) {
			replaceAllOccurrencesWithinTempDOM(fontUrl, String.format("data:font/ttf;base64,%s", base64String));
		} else if (fontUrl.contains("eot")) {
			replaceAllOccurrencesWithinTempDOM(fontUrl, String.format("data:font/eot;base64,%s", base64String));
		} else if (fontUrl.contains("svg")) {
			replaceAllOccurrencesWithinTempDOM(fontUrl, String.format("data:image/svg+xml;base64,%s", base64String));
		} else {
			replaceAllOccurrencesWithinTempDOM(fontUrl, String.format("data:font/woff;base64,%s", base64String));
		}
		base64String = null;
	}

	/**
	 * Follows redirects until reaching an actual url
	 *
	 * @param uc - The initial url connection
	 * @return - The url connection to the actual url
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	private URLConnection followRedirects(URLConnection uc) throws MalformedURLException, IOException {
		HttpURLConnection conn = (HttpURLConnection) uc;
		String newUrl = null;
		String cookies = null;
		int status = conn.getResponseCode();
		boolean redirect = isRedirect(status);
		while (redirect) {

			// get redirect url from "location" header field
			newUrl = conn.getHeaderField("Location");
			if (newUrl.startsWith("https")) {
				return followSecuredRedirects(new URL(newUrl));
			}

			// get the cookie if need, for login
			cookies = conn.getHeaderField("Set-Cookie");

			// open the new connnection again
			conn = (HttpURLConnection) new URL(newUrl).openConnection();
			conn.setRequestProperty("Cookie", cookies);
			conn.setRequestProperty("Upgrade-Insecure-Requests", "1");
			conn.setRequestProperty("User-Agent", xmlTagSettings.userAgent);
			isRedirect(conn.getResponseCode());
		}
		return conn;
	}


	/**
	 * Follows redirects until reaching an actual url - SECURED
	 *
	 * @param url - The first secured url
	 * @return - The url connection to the actual url
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	private URLConnection followSecuredRedirects(URL url) throws IOException {
		HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
		String newUrl = null;
		String cookies = null;
		int status = conn.getResponseCode();
		boolean redirect = isRedirect(status);
		while (redirect) {

			// get redirect url from "location" header field
			newUrl = conn.getHeaderField("Location");

			// get the cookie if need, for login
			cookies = conn.getHeaderField("Set-Cookie");

			// open the new connnection again
			conn = (HttpsURLConnection) new URL(newUrl).openConnection();
			conn.setRequestProperty("Cookie", cookies);
			conn.setRequestProperty("Upgrade-Insecure-Requests", "1");
			conn.setRequestProperty("User-Agent", xmlTagSettings.userAgent);
			isRedirect(conn.getResponseCode());
		}
		return conn;
	}

	/**
	 * Checks if a given status code is a redirection
	 *
	 * @param status - The status code
	 * @return - Is a redirect
	 */
	private boolean isRedirect(int status) {
		boolean redirect = false;
		if (status != HttpURLConnection.HTTP_OK) {
			if (status == HttpURLConnection.HTTP_MOVED_TEMP
					|| status == HttpURLConnection.HTTP_MOVED_PERM
					|| status == HttpURLConnection.HTTP_SEE_OTHER)
				redirect = true;
		}
		return redirect;
	}

	/*
	 * Copies bytes from the URL source to a file destination. The directories
	 * up to destination will be created if they don't already exist.
	 * destination will be overwritten if it already exists. Warning: this
	 * method does not set a connection or read timeout and thus might block
	 * forever. Use copyURLToFile(URL, File, int, int) with reasonable timeouts
	 * to prevent this.
	 *
	 * Parameters: source - the URLConnection to copy bytes from, must not be
	 * null destination - the non-directory File to write bytes to (possibly
	 * overwriting), must not be null
	 */
	public void copyURLToFile(URLConnection urlConnection, File destination) throws IOException {
		InputStream input = urlConnection.getInputStream();
		try {
			FileOutputStream output = FileUtils.openOutputStream(destination);
			try {
				IOUtils.copy(input, output);
			} finally {
				IOUtils.closeQuietly(output);
			}
		} finally {
			IOUtils.closeQuietly(input);
		}
	}


	/**
	 * Activates recursiveCrawl on each @import CSS found in the inner CSS.
	 */
	public void getImportsCss() {
		Set<String> importList = RegexFinders.findImportsCss(this.document.html());
		for (String importUrl : importList) {
			try {
				tryCssUrl(importUrl, true);

				// This only happens after the import CSS files have been crawled
				changeImportedPathQueryString(importUrl);
			} catch (IOException e) {
				imageFontFileErrorHandler("connection", importUrl, e);
			}
		}
	}

	@Override
	protected void tryCssUrl(String partialOriginalUrl, boolean changeImportUrls) throws IOException {
		String absHref = null;
		String completeUrl = partialOriginalUrl;
		if (!partialOriginalUrl.toLowerCase().startsWith("http")) {
			completeUrl = generateCompleteUrl(partialOriginalUrl);
		}
		try {
			completeUrl = tryUrlPermutation(completeUrl);
			changeImportUrls(partialOriginalUrl, completeUrl, changeImportUrls);
		} catch (IOException e) {
			try {
				absHref = generateMainDomainUrlUnsecured(partialOriginalUrl);
				absHref = tryUrlPermutation(absHref);
				changeImportUrls(partialOriginalUrl, absHref, changeImportUrls);
			} catch (IOException e1) {
				absHref = absHref.replace("http", "https");
				absHref = tryUrlPermutation(absHref);
				changeImportUrls(partialOriginalUrl, absHref, changeImportUrls);
			}
		}
	}

	/**
	 * Checks if it is needed to change the import urls (if imported from within a css file)
	 *
	 * @param originalPartialUrl
	 * @param permutation
	 * @param changeImportUrls
	 */
	private void changeImportUrls(String originalPartialUrl, String permutation, boolean changeImportUrls) {
		if (changeImportUrls) {
			if (this.tempDOM.length() <= 100) {
				this.tempDOM.append(this.document.body().text());
			}
			replaceAllOccurrencesWithinTempDOM(originalPartialUrl, String.format("http://dummytest.clicktale-samples.com/HyperScraper/%s", permutation));
		}
	}

	/**
	 * Changes the url of the import AFTER it has been imported in case it
	 * contains special characters.
	 *
	 * @param importUrl - The original import URL.
	 */
	protected void changeImportedPathQueryString(String importUrl) {
		int indexQ = importUrl.indexOf('?');
		String changedUrl = "";

		// For CSS which keep changing
		if (indexQ > -1) {
			/*
			 * if ((importUrl.length() - indexQ) > 70) { importUrl =
			 * importUrl.substring(0, indexQ + 70); }
			 */
			if (importUrl.startsWith("https://fonts")) {
				return;
			}
			changedUrl = importUrl.replace('?', '-').replace('=', '-').replace('&', '-').replace(',', '-')
					.replace(':', '-').replace('%', '-');
			if (!changedUrl.toLowerCase().endsWith(".css")) {
				changedUrl += ".css";
			}
			if (this.tempDOM.length() <= 1) {
				this.tempDOM.append(this.document.body().text());
			}
			replaceAllOccurrencesWithinTempDOM(importUrl, changedUrl);
		}
	}

	protected String getLocalDestination() {
		return this.localDestination;
	}

}
