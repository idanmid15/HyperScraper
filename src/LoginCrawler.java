
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;

public class LoginCrawler extends BrowserCrawler {

	@SuppressWarnings("deprecation")
	public void alamoSelectCar(Date currentDate) {
		try {
			WebDriver webDriver = new ChromeDriver();
			webDriver.get("https://www.alamo.com/en_US/car-rental/reservation/startReservation.html");
			webSleep(1500);
			WebElement pickUpLocationElement = webDriver.findElement(By.id(
					"_content_alamo_en_US_car_rental_reservation_startReservation_jcr_content_cq_colctrl_lt30_c1_start_pickUpLocation_searchCriteria"));
			pickUpLocationElement.sendKeys("Miam");

			webSleep(1500);
			pickUpLocationElement.sendKeys(Keys.ARROW_DOWN);
			pickUpLocationElement.sendKeys(Keys.ENTER);

			WebElement selectPickUpAddressElement = webDriver.findElement(By.name("pickUpDateTime.date"));
			selectPickUpAddressElement.click();

			List<WebElement> xPathCalendar1 = webDriver.findElements(By.cssSelector("a.ui-state-default"));
			Iterator<WebElement> iter = xPathCalendar1.iterator();

			int currentDay = currentDate.getDay() + 1;
			WebElement xPathCalender1Element = null;
			webSleep(500);
			do {
				if ((xPathCalender1Element = iter.next()).getText().equals(currentDay)) {
					break;
				}
			} while (iter.hasNext());
			xPathCalender1Element.click();

			WebElement selectReturnAddressElement = webDriver.findElement(By.name("dropOffDateTime.date"));
			selectReturnAddressElement.click();
			xPathCalendar1 = null;
			xPathCalendar1 = webDriver.findElements(By.cssSelector("a.ui-state-default"));
			iter = xPathCalendar1.iterator();
			webSleep(500);

			int middleIndicator = 0;
			int middleIndex = xPathCalendar1.size() / 2;
			xPathCalender1Element = null;

			// Stop at the middle of the calendar.
			do {
				xPathCalender1Element = iter.next();
				if (middleIndicator >= middleIndex)
					break;
			} while (iter.hasNext());
			xPathCalender1Element.click();

			webDriver
					.findElement(
							By.id("_content_alamo_en_US_car_rental_reservation_startReservation_jcr_content_cq_colctrl_lt30_c1_start_submit"))
					.click();
			webSleep(1500);
			HyperScraper.writeStatusToLogFile("Processing Generated Source of:\n"
					+ "https://www.alamo.com/en_US/car-rental/reservation/selectCar.html");
			webSleep(2500);
			this.document = Jsoup.parse(webDriver.getPageSource());
			java.util.Set<org.openqa.selenium.Cookie> cookies = webDriver.manage().getCookies();
			webDriver.close();
			webDriver = null;
			this.mainUrl = "https://www.alamo.com/en_US/car-rental/reservation/selectCar.html";
			this.currentDirectory = getCurrentDirectory(
					"https://www.alamo.com/en_US/car-rental/reservation/selectCar.html");
			this.xmlTagSettings = new XmlTagSettings(HyperScraper.DESKTOP, mapCookies(cookies), false, false, false);
			getCssLinkTags();
		} catch (Exception e) {
			imageFontFileErrorHandler("connection", "https://www.alamo.com/en_US/car-rental/reservation/selectCar.html",
					e);
		}
	}

	public void imitateBrowser(String url, XmlTagSettings xmlTagSettings) {
		try {
			HyperScraper.writeStatusToLogFile("Processing Browser Immitated Generated Source of:\n" + url);
			WebDriver webDriver = new ChromeDriver();
			webDriver.get(url);
			webSleep(xmlTagSettings.waitTimeForCss * 1000);
			this.document = Jsoup.parse(webDriver.getPageSource(), getBaseUri(url));
			this.mainUrl = url;
			this.currentDirectory = getCurrentDirectory(url);
			java.util.Set<org.openqa.selenium.Cookie> cookies = webDriver.manage().getCookies();
			webDriver.close();
			webDriver.quit();
			webDriver = null;
			xmlTagSettings.setCookies(mapCookies(cookies));
			this.xmlTagSettings = xmlTagSettings;
			getCssLinkTags();
		} catch (Exception e) {
			imageFontFileErrorHandler("connection", url, e);
		}
	}

	public void nikeShoes() {
		try {
			Map<String, String> mobileEmulation = new HashMap<String, String>();
			mobileEmulation.put("deviceName", "Google Nexus 5");
			Map<String, Object> chromeOptions = new HashMap<String, Object>();
			chromeOptions.put("mobileEmulation", mobileEmulation);
			DesiredCapabilities capabilities = DesiredCapabilities.chrome();
			capabilities.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
			WebDriver webDriver = new ChromeDriver(capabilities);
			webDriver.get("http://m.nike.com/us/en_us/product/sb-stefan-janoski-max-id/?pbid=1064242502");
			webSleep(1500);
			HyperScraper.writeStatusToLogFile("Processing Generated Source of:\n"
					+ "http://m.nike.com/us/en_us/product/sb-stefan-janoski-max-id/?pbid=1064242502");
			this.document = Jsoup.parse(webDriver.getPageSource());
			java.util.Set<org.openqa.selenium.Cookie> cookies = webDriver.manage().getCookies();
			webDriver.close();
			webDriver = null;
			this.mainUrl = "http://m.nike.com/us/en_us/product/sb-stefan-janoski-max-id/?pbid=1064242502";
			this.xmlTagSettings = new XmlTagSettings(HyperScraper.DESKTOP, mapCookies(cookies), false, false, false);
			this.currentDirectory = getCurrentDirectory(
					"http://m.nike.com/us/en_us/product/sb-stefan-janoski-max-id/?pbid=1064242502");
			getCssLinkTags();
		} catch (Exception e) {
			imageFontFileErrorHandler("connection",
					"http://m.nike.com/us/en_us/product/sb-stefan-janoski-max-id/?pbid=1064242502", e);
		}
	}

	/**
	 * Sets the browser to sleep for a specified amount of ms.
	 * 
	 * @param timeToSleep
	 *            - ms to sleep.
	 */
	private void webSleep(long timeToSleep) {
		try {
			Thread.sleep(timeToSleep);
		} catch (InterruptedException e) {
		}
	}

	/**
	 * Receives cookies as a single String and returns a HashMap representing
	 * all cookies.
	 * 
	 * @param cookies
	 *            - The cookie string.
	 * @return - Hashmap (pairs) of cookie(key, value).
	 */
	private static HashMap<String, String> mapCookies(java.util.Set<org.openqa.selenium.Cookie> cookies) {
		HashMap<String, String> cookieMap = new HashMap<>();
		if (cookies.size() <= 1)
			return cookieMap;
		String cookieKey = "";
		String cookieValue = "";
		for (org.openqa.selenium.Cookie cookie : cookies) {
			cookieKey = cookie.getName();
			cookieValue = cookie.getValue();
			if (!cookieMap.containsKey(cookieKey)) {
				cookieMap.put(cookieKey, cookieValue);
			}
		}
		return cookieMap;
	}
}
/reservation/selectCar.html");
			webSleep(2500);
			this.document = Jsoup.parse(webDriver.getPageSource());
			java.util.Set<org.openqa.selenium.Cookie> cookies = webDriver.manage().getCookies();
			webDriver.close();
			webDriver = null;
			this.mainUrl = "https://www.alamo.com/en_US/car-rental/reservation/selectCar.html";
			this.currentDirectory = getCurrentDirectory(
					"https://www.alamo.com/en_US/car-rental/reservation/selectCar.html");
			this.xmlTagSettings = new XmlTagSettings(HyperScraper.DESKTOP, mapCookies(cookies), false, false, false);
			getCssLinkTags();
		} catch (Exception e) {
			imageFontFileErrorHandler("connection", "https://www.alamo.com/en_US/car-rental/reservation/selectCar.html",
					e);
		}
	}

	public void imitateBrowser(String url, XmlTagSettings xmlTagSettings) {
		try {
			HyperScraper.writeStatusToLogFile("Processing Browser Immitated Generated Source of:\n" + url);
			WebDriver webDriver = new ChromeDriver();
			webDriver.get(url);
			webSleep(xmlTagSettings.waitTimeForCss * 1000);
			this.document = Jsoup.parse(webDriver.getPageSource(), getBaseUri(url));
			this.mainUrl = url;
			this.currentDirectory = getCurrentDirectory(url);
			java.util.Set<org.openqa.selenium.Cookie> cookies = webDriver.manage().getCookies();
			webDriver.close();
			webDriver.quit();
			webDriver = null;
			xmlTagSettings.setCookies(map