/*
package crawler;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class LoginCrawler extends BrowserCrawler
{
	public LoginCrawler() {}

	public void alamoSelectCar(Date currentDate)
	{
		try
		{
			WebDriver webDriver = new org.openqa.selenium.chrome.ChromeDriver();
			webDriver.get("https://www.alamo.com/en_US/car-rental/reservation/startReservation.html");
			webSleep(1500L);
			WebElement pickUpLocationElement = webDriver.findElement(By.id(
					"_content_alamo_en_US_car_rental_reservation_startReservation_jcr_content_cq_colctrl_lt30_c1_start_pickUpLocation_searchCriteria"));
			pickUpLocationElement.sendKeys(new CharSequence[] { "Miam" });

			webSleep(1500L);
			pickUpLocationElement.sendKeys(new CharSequence[] { org.openqa.selenium.Keys.ARROW_DOWN });
			pickUpLocationElement.sendKeys(new CharSequence[] { org.openqa.selenium.Keys.ENTER });

			WebElement selectPickUpAddressElement = webDriver.findElement(By.name("pickUpDateTime.date"));
			selectPickUpAddressElement.click();

			List<WebElement> xPathCalendar1 = webDriver.findElements(By.cssSelector("a.ui-state-default"));
			Iterator<WebElement> iter = xPathCalendar1.iterator();

			int currentDay = currentDate.getDay() + 1;
			WebElement xPathCalender1Element = null;
			webSleep(500L);

			while ((!(xPathCalender1Element = (WebElement)iter.next()).getText().equals(Integer.valueOf(currentDay))) &&


					(iter.hasNext())) {}
			xPathCalender1Element.click();

			WebElement selectReturnAddressElement = webDriver.findElement(By.name("dropOffDateTime.date"));
			selectReturnAddressElement.click();
			xPathCalendar1 = null;
			xPathCalendar1 = webDriver.findElements(By.cssSelector("a.ui-state-default"));
			iter = xPathCalendar1.iterator();
			webSleep(500L);

			int middleIndicator = 0;
			int middleIndex = xPathCalendar1.size() / 2;
			xPathCalender1Element = null;

			do
			{
				xPathCalender1Element = (WebElement)iter.next();
			} while ((middleIndicator < middleIndex) &&

					(iter.hasNext()));
			xPathCalender1Element.click();

			webDriver
					.findElement(
							By.id("_content_alamo_en_US_car_rental_reservation_startReservation_jcr_content_cq_colctrl_lt30_c1_start_submit"))
					.click();
			webSleep(1500L);
			HyperScraper.writeStatusToLogFile("Processing Generated Source of:\nhttps://www.alamo.com/en_US/car-rental/reservation/selectCar.html");

			webSleep(2500L);
			document = org.jsoup.Jsoup.parse(webDriver.getPageSource());
			Set<Cookie> cookies = webDriver.manage().getCookies();
			webDriver.close();
			webDriver = null;
			mainUrl = "https://www.alamo.com/en_US/car-rental/reservation/selectCar.html";
			currentDirectory = getCurrentDirectory(
					"https://www.alamo.com/en_US/car-rental/reservation/selectCar.html");
			xmlTagSettings = new XmlTagSettings("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.71 Safari/537.36", mapCookies(cookies), false, false, false);
			getCssLinkTags();
		} catch (Exception e) {
			imageFontFileErrorHandler("connection", "https://www.alamo.com/en_US/car-rental/reservation/selectCar.html",
					e);
		}
	}

	public void imitateBrowser(String url, XmlTagSettings xmlTagSettings) {
		try {
			HyperScraper.writeStatusToLogFile("Processing Browser Immitated Generated Source of:\n" + url);
			WebDriver webDriver = new org.openqa.selenium.chrome.ChromeDriver();
			webDriver.get(url);
			webSleep(waitTimeForCss * 1000);
			document = org.jsoup.Jsoup.parse(webDriver.getPageSource(), getBaseUri(url));
			mainUrl = url;
			currentDirectory = getCurrentDirectory(url);
			Set<Cookie> cookies = webDriver.manage().getCookies();
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
			Map<String, String> mobileEmulation = new HashMap();
			mobileEmulation.put("deviceName", "Google Nexus 5");
			Map<String, Object> chromeOptions = new HashMap();
			chromeOptions.put("mobileEmulation", mobileEmulation);
			org.openqa.selenium.remote.DesiredCapabilities capabilities = org.openqa.selenium.remote.DesiredCapabilities.chrome();
			capabilities.setCapability("chromeOptions", chromeOptions);
			WebDriver webDriver = new org.openqa.selenium.chrome.ChromeDriver(capabilities);
			webDriver.get("http://m.nike.com/us/en_us/product/sb-stefan-janoski-max-id/?pbid=1064242502");
			webSleep(1500L);
			HyperScraper.writeStatusToLogFile("Processing Generated Source of:\nhttp://m.nike.com/us/en_us/product/sb-stefan-janoski-max-id/?pbid=1064242502");

			document = org.jsoup.Jsoup.parse(webDriver.getPageSource());
			Set<Cookie> cookies = webDriver.manage().getCookies();
			webDriver.close();
			webDriver = null;
			mainUrl = "http://m.nike.com/us/en_us/product/sb-stefan-janoski-max-id/?pbid=1064242502";
			xmlTagSettings = new XmlTagSettings("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.71 Safari/537.36", mapCookies(cookies), false, false, false);
			currentDirectory = getCurrentDirectory(
					"http://m.nike.com/us/en_us/product/sb-stefan-janoski-max-id/?pbid=1064242502");
			getCssLinkTags();
		} catch (Exception e) {
			imageFontFileErrorHandler("connection",
					"http://m.nike.com/us/en_us/product/sb-stefan-janoski-max-id/?pbid=1064242502", e);
		}
	}





	private void webSleep(long timeToSleep)
	{
		try
		{
			Thread.sleep(timeToSleep);
		}
		catch (InterruptedException localInterruptedException) {}
	}








	private static HashMap<String, String> mapCookies(Set<Cookie> cookies)
	{
		HashMap<String, String> cookieMap = new HashMap();
		if (cookies.size() <= 1)
			return cookieMap;
		String cookieKey = "";
		String cookieValue = "";
		for (Cookie cookie : cookies) {
			cookieKey = cookie.getName();
			cookieValue = cookie.getValue();
			if (!cookieMap.containsKey(cookieKey)) {
				cookieMap.put(cookieKey, cookieValue);
			}
		}
		return cookieMap;
	}
}*/
