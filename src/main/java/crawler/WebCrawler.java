package crawler;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public abstract class WebCrawler
{
    private static Pattern patternForHref = Pattern.compile("href=(['\"])([^\"']*?)\\1", 2);
    protected String mainUrl;
    protected String currentDirectory;
    protected Document document;
    protected XmlTagSettings xmlTagSettings;

    protected abstract void tryCssUrl(String paramString, boolean paramBoolean)
            throws IOException;

    protected static String getCurrentDirectory(String i_Url)
    {
        String[] domains = i_Url.split("/");
        if ((domains.length <= 3) || (domains[3].isEmpty())) {
            return i_Url;
        }
        int indexLastSlash = i_Url.lastIndexOf('/');
        if (indexLastSlash == i_Url.length() - 1)
        {
            i_Url = i_Url.substring(0, i_Url.length() - 1);
            indexLastSlash = i_Url.lastIndexOf('/');
        }
        return String.format("%s/", new Object[] { i_Url.substring(0, indexLastSlash) });
    }

    protected static String findHref(String input)
    {
        Matcher m = patternForHref.matcher(input);
        while (m.find()) {
            if (m.group(2) != null) {
                return m.group(2);
            }
        }
        return "";
    }

    protected void getCssLinkTags()
    {
        String absHref = "";
        String partialUrl = "";
        Elements links = this.document.select("link");
        for (Element link : links)
        {
            absHref = link.attr("abs:href");
            if ((absHref != null) && (!absHref.isEmpty()) && (
                    (absHref.toLowerCase().contains("css")) || (link.attr("rel").contains("style")))) {
                if (absHref.toLowerCase().startsWith("http"))
                {
                    XmlTagSettings tmp99_96 = this.xmlTagSettings;tmp99_96.numOfUserBasedCss = ((short)(tmp99_96.numOfUserBasedCss + 1));
                    new SourceCrawler(absHref, this.xmlTagSettings, this.mainUrl);
                }
                else
                {
                    try
                    {
                        tryCssUrl(link.attr("href"), false);
                    }
                    catch (IOException e)
                    {
                        imageFontFileErrorHandler("connection", partialUrl, e);
                    }
                }
            }
        }
    }

    protected String generateCompleteUrl(String partUrl)
    {
        if ((partUrl == null) || (partUrl.isEmpty())) {
            return null;
        }
        String[] spt = partUrl.split("/");
        String prefix = "";
        String toReturn = "";
        if (spt[0].length() > 1) {
            prefix = spt[0];
        } else {
            prefix = spt[1];
        }
        if (partUrl.toLowerCase().startsWith("http")) {
            return null;
        }
        if (partUrl.startsWith("//")) {
            return decideOnUrlProtocolBasedOnMainUrl(partUrl);
        }
        if ((this.currentDirectory.contains(prefix)) && (!prefix.equals("..")))
        {
            toReturn = String.format("%s%s%s", new Object[] {
                    this.currentDirectory.substring(0, this.currentDirectory.indexOf(prefix)), prefix,
                    partUrl.replace(prefix + "/", "") });
            try
            {
                checkUrlValidity(toReturn);
                return toReturn;
            }
            catch (Exception localException) {}
        }
        if ((this.currentDirectory.endsWith("/")) && (partUrl.startsWith("/")))
        {
            toReturn = String.format("%s%s", new Object[] { this.currentDirectory, partUrl.substring(1) });
            try
            {
                checkUrlValidity(toReturn);
                return toReturn;
            }
            catch (Exception localException1) {}
        }
        return String.format("%s%s", new Object[] { this.currentDirectory, partUrl });
    }

    private String decideOnUrlProtocolBasedOnMainUrl(String url)
    {
        if (this.mainUrl.startsWith("https")) {
            return String.format("https:%s", new Object[] { url });
        }
        return String.format("http:%s", new Object[] { url });
    }

    protected void checkUrlValidity(String url)
            throws MalformedURLException, IOException
    {
        HttpURLConnection uConnection = (HttpURLConnection)new URL(url).openConnection();

        uConnection.getInputStream();
        if ((uConnection.getResponseCode() != 200) && (uConnection.getHeaderField("Location").contains("not"))) {
            throw new MalformedURLException();
        }
    }

    protected String tryUrlPermutation(String permutationUrl)
            throws IOException
    {
        Jsoup.connect(permutationUrl).cookies(this.xmlTagSettings.cookies).userAgent(this.xmlTagSettings.userAgent).get(); XmlTagSettings
            tmp38_35 = this.xmlTagSettings;tmp38_35.numOfUserBasedCss = ((short)(tmp38_35.numOfUserBasedCss + 1));
        return new SourceCrawler(permutationUrl, this.xmlTagSettings, this.mainUrl).getLocalDestination();
    }

    public String generateMainDomainUrlUnsecured(String suffix)
    {
        return String.format("http://%s%s", new Object[] { this.currentDirectory.split("/")[2], suffix });
    }

    protected String getBaseUri(String url)
    {
        String baseUrl = null;
        try
        {
            URL url1 = new URL(url);
            baseUrl = url1.getProtocol() + "://" + url1.getHost();
        }
        catch (MalformedURLException e)
        {
            imageFontFileErrorHandler("connection", url, e);
        }
        return baseUrl;
    }

    public String generateMainDomainUrlSecured(String suffix)
    {
        return String.format("https://%s%s", new Object[] { this.currentDirectory.split("/")[2], suffix });
    }

    protected String turnCookieHashMapToString()
    {
        StringBuilder stringBuilder = new StringBuilder();
        Iterator<Map.Entry<String, String>> it = this.xmlTagSettings.cookies.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<String, String> pair = (Map.Entry)it.next();
            stringBuilder.append((String)pair.getKey() + "=" + (String)pair.getValue() + "; ");
        }
        return stringBuilder.toString();
    }

    public String simplifyPath(String path)
    {
        Stack<String> stack = new Stack();
        while ((path.length() > 0) && (path.charAt(path.length() - 1) == '/')) {
            path = path.substring(0, path.length() - 1);
        }
        pushPathDirectoriesToStack(path, stack);

        LinkedList<String> result = new LinkedList();
        simplifyLinkedList(stack, result);
        if (result.isEmpty()) {
            return "/";
        }
        StringBuilder stringBuilder = new StringBuilder();
        while (!result.isEmpty()) {
            stringBuilder.append((String)result.pop());
        }
        String toReturn = stringBuilder.toString();
        int indexHttp = toReturn.indexOf("/");
        return String.format("%s/%s", new Object[] { toReturn.substring(0, indexHttp), toReturn.substring(indexHttp) });
    }

    private void simplifyLinkedList(Stack<String> stack, LinkedList<String> resultLinkedList)
    {
        int backDirectoryCount = 0;
        while (!stack.isEmpty())
        {
            String topOfStack = (String)stack.pop();
            if ((!topOfStack.equals("/.")) && (!topOfStack.equals("/"))) {
                if (topOfStack.equals("/..")) {
                    backDirectoryCount++;
                } else if (backDirectoryCount > 0) {
                    backDirectoryCount--;
                } else {
                    resultLinkedList.addFirst(topOfStack);
                }
            }
        }
    }

    private void pushPathDirectoriesToStack(String path, Stack<String> stack)
    {
        int previouStart = 0;
        for (int i = 1; i < path.length(); i++) {
            if (path.charAt(i) == '/')
            {
                stack.push(path.substring(previouStart, i));
                previouStart = i;
            }
            else if (i == path.length() - 1)
            {
                stack.push(path.substring(previouStart));
            }
        }
    }

    public void imageFontFileErrorHandler(String type, String urlAttempt, Exception e)
    {
        try
        {
            String error = e.toString();

            BufferedWriter bw = new BufferedWriter(new FileWriter(String.format("logs/%s/%s/ErrorLogs/%sErrorLogs.txt", new Object[] {
                    HyperScraper.dateString, HyperScraper.hourString, type }), true));

            bw.write(String.format("%s\nMain url = %s\nAttempted url = %s\n\n\n\n\n", new Object[] { error, this.mainUrl, urlAttempt }));
            bw.close();
        }
        catch (IOException e2)
        {
            e2.printStackTrace();
        }
    }
}
