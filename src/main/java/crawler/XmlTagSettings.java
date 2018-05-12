package crawler;

import java.util.HashMap;
import org.jsoup.nodes.Element;


public class XmlTagSettings {
	protected String userAgent;
	protected HashMap<String, String> cookies;
	protected boolean userBasedCss;
	protected boolean lowerCaseDirectories;
	protected boolean securedFonts;
	protected short numOfUserBasedCss;
	protected byte waitTimeForCss;
	
	protected XmlTagSettings(String userAgent, HashMap<String, String> cookies, boolean userBasedCss,
			boolean lowerCaseDirectories, boolean securedFonts) {
		this.userAgent = userAgent;
		this.cookies = cookies;
		this.userBasedCss = userBasedCss;
		this.lowerCaseDirectories = lowerCaseDirectories;
		this.numOfUserBasedCss = 0;
		this.securedFonts = securedFonts;
		this.waitTimeForCss = 1;
	}
	
	protected XmlTagSettings(Element i_ElementToProcess) {
		this.userBasedCss = Boolean.valueOf(i_ElementToProcess.attr("userBasedCss"));
		this.lowerCaseDirectories = Boolean.valueOf(i_ElementToProcess.attr("lowerCaseDirectories"));
		this.securedFonts = Boolean.valueOf(i_ElementToProcess.attr("securedFonts"));
		this.cookies = HyperScraper.mapCookies(i_ElementToProcess.attr("cookies"));
		this.userAgent = processUserAgent(i_ElementToProcess.attr("ua").charAt(0));
		parseWaitTime(i_ElementToProcess);
	}

	private void parseWaitTime(Element i_ElementToProcess) {
		String waitTimeString = i_ElementToProcess.attr("waitTimeForCss"); 
		if (waitTimeString != null && waitTimeString.length() > 0) {
			try {
				this.waitTimeForCss = Byte.valueOf(waitTimeString);
			} catch (Exception e) {
				HyperScraper.fileErrorHandler(e);
			}
		}
	}
	
	private static String processUserAgent(char i_UserAgentChar) {
		String toReturn = "";
		if (Character.toUpperCase(i_UserAgentChar) == 'D') {
			toReturn = HyperScraper.DESKTOP;
		} else if (Character.toUpperCase(i_UserAgentChar) == 'M') {
			toReturn = HyperScraper.MOBILE;
		} else {
			toReturn = HyperScraper.TABLET;
		}
		return toReturn;
	}
	
	protected char getUserAgentChar() {
		char toReturn = ' ';
		if (this.userAgent.equals(HyperScraper.DESKTOP)) {
			toReturn = 'D';
		} else if (this.userAgent.equals(HyperScraper.MOBILE)) {
			toReturn = 'M';
		} else {
			toReturn = 'T';
		}
		return toReturn;
	}
	
	@Override
	public String toString() {
		return String.format("UA = %c\nuserBasedCss = %b\nlowerCaseDirectories = %b\nsecuredFonts = %b", getUserAgentChar(), userBasedCss, lowerCaseDirectories, securedFonts);
	}
	
	protected void setCookies(HashMap<String, String> cookies) {
		this.cookies = cookies;
	}
}