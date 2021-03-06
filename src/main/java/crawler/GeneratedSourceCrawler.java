/*
package crawler;
import java.io.IOException;
import java.util.Map;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;

import java.util.logging.*;
import org.jsoup.Jsoup;

public class GeneratedSourceCrawler extends BrowserCrawler {
	static BrowserVersion ch = BrowserVersion.CHROME;
	static String appName = ch.getApplicationName();
	static String appVersion = ch.getApplicationVersion();
	static float appVersionNumeric = ch.getBrowserVersionNumeric();
	static BrowserVersion versionDesktop = new BrowserVersion(appName, appVersion, HyperScraper.DESKTOP,
			appVersionNumeric);
	static BrowserVersion versionMobile = new BrowserVersion(appName, appVersion, HyperScraper.MOBILE, appVersionNumeric);
	static BrowserVersion versionTablet = new BrowserVersion(appName, appVersion, HyperScraper.TABLET, appVersionNumeric);
	static {
		generatedJsWarningsOff();
	}

	public GeneratedSourceCrawler(String mainUrl, XmlTagSettings xmlTagSettings) {
		this.mainUrl = mainUrl;
		this.xmlTagSettings = xmlTagSettings;
		WebClient webClient = initiateBrowserAndClient(xmlTagSettings.userAgent);
		String key = null;
		String value = null;
		Cookie cookie = null;

		// Finds the directory path
		this.currentDirectory = getCurrentDirectory(mainUrl);
		if (xmlTagSettings.cookies != null) {
			for (Map.Entry<String, String> entry : xmlTagSettings.cookies.entrySet()) {
				key = entry.getKey();
				value = entry.getValue();
				cookie = new Cookie(mainUrl, key, value);
				webClient.getCookieManager().addCookie(cookie);
			}
		}
		try {
			webClient.getOptions().setThrowExceptionOnScriptError(false);
			webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
			webClient.getOptions().setAppletEnabled(true);
			webClient.getOptions().setCssEnabled(true);
			webClient.getOptions().setJavaScriptEnabled(true);
			HyperScraper.writeStatusToLogFile("Processing Generated Source Of:\n" + mainUrl);
			HtmlPage page = webClient.getPage(mainUrl);
			this.document = Jsoup.parse(page.asXml(), getBaseUri(mainUrl));
			webClient.close();
			webClient = null;
			getCssLinkTags();
		} catch (IOException | IllegalStateException | FailingHttpStatusCodeException e) {
			return;
		}
	}

	*/
/**
	 * Initiates a WebClient.
	 * 
	 * @return - The initiated webClient
	 *//*

	protected static WebClient initiateBrowserAndClient(String userAgent) {
		BrowserVersion browserVersion;
		browserVersion = new BrowserVersion(appName, appVersion, userAgent, appVersionNumeric);
		return new WebClient(browserVersion);
	}

	*/
/**
	 * Turns HtmlUnit's generated JS warnings off.
	 *//*

	public static void generatedJsWarningsOff() {
		java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
		java.util.logging.Logger.getLogger("org.apache.http.client.protocol.ResponseProcessCookies")
				.setLevel(Level.OFF);
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
	}
}*/
