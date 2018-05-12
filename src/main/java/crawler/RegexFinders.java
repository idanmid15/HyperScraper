package crawler;

import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexFinders {
	
	private static Pattern patternForFontUrls = Pattern.compile("url\\((['\"]?)([^\\)]*?\\.((woff|ttf|svg)[^\\)]*))\\1",
			Pattern.CASE_INSENSITIVE);
	private static Pattern patternForImageUrls = Pattern
			.compile("url\\((['\"])?([^ ]*?\\.(ico|png|jpg|gif)+?)(['\"])?\\)", Pattern.CASE_INSENSITIVE);
	private static Pattern patternForImportUrls = Pattern.compile("@import[\\s](url\\()?(['\"]?)(.*?)\\2\\)",
			Pattern.CASE_INSENSITIVE);

	/**
	 * Finds inner fonts (woff and ttf)
	 * 
	 * @param input
	 *            - the DOM (CSS)
	 * @return - A list of addresses representing each font partial path
	 */
	public static LinkedHashSet<String> findFontUrls(String input) {
		LinkedHashSet<String> lt = new LinkedHashSet<String>();
		Matcher m = patternForFontUrls.matcher(input);
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
	public static LinkedHashSet<String> findImageUrls(String input) {
		LinkedHashSet<String> lt = new LinkedHashSet<String>();
		Matcher m = patternForImageUrls.matcher(input);
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
	public static LinkedHashSet<String> findImportsCss(String input) {
		LinkedHashSet<String> lt = new LinkedHashSet<String>();
		Matcher m = patternForImportUrls.matcher(input);
		while (m.find()) {
			if (m.group(3) != null && m.group(3).length() < 150) {
				lt.add(m.group(3));
			}
		}
		return lt;
	}

}