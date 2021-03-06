package crawler;
import java.io.IOException;


public abstract class BrowserCrawler extends WebCrawler {
	
	@Override
	protected void tryCssUrl(String partialOriginalUrl, boolean changeImportUrls) throws IOException {
		String absHref = null;
		String completeUrl = partialOriginalUrl;
		if (!partialOriginalUrl.toLowerCase().startsWith("http")) {
			completeUrl = generateCompleteUrl(partialOriginalUrl);
		}
		try {
			if (completeUrl.contains("alamo.com/en_US/car-rental/reservation/")) {
				absHref = generateMainDomainUrlUnsecured(partialOriginalUrl);
				tryUrlPermutation(absHref);
			} else {
				tryUrlPermutation(completeUrl);
			}
		} catch (IOException e) {
			try {
				if (completeUrl.contains("alamo.com/en_US/car-rental/reservation/")) {
					tryUrlPermutation(completeUrl);
				} else {
					absHref = generateMainDomainUrlSecured(partialOriginalUrl);
					tryUrlPermutation(absHref);
				}
			} catch (IOException e1) {
				try {
					absHref = generateMainDomainUrlUnsecured(partialOriginalUrl);
					tryUrlPermutation(absHref);
				} catch (Exception e2) {
					absHref = absHref.replace("http", "https");
					tryUrlPermutation(absHref);
				}
			}
		}
	}
}