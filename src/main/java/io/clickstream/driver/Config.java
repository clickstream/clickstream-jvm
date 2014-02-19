package io.clickstream.driver;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

public class Config {
    public static final String CRAWLERS = "(Baidu|Gigabot|Googlebot|libwww-perl|lwp-trivial|msnbot|SiteUptime|Slurp|WordPress|ZIBB|ZyBorg|bot|crawler|spider|robot|crawling|facebook|w3c|coccoc|Daumoa|panopta)";
    private String hostname;
    private boolean capture = false;
    private FilterConfig filterConfig;
    private boolean benchmark = false;
    private HttpApiClient httpApiClient;
    private Pattern filterParams;
    private String jsFilterParams;
    private Pattern filterUri;
    private Pattern crawlers;
    private boolean captureCrawlers = false;

    public Config(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;

        if(isConfigParameterTrue("capture"))
            capture = true;
        else
            return;

        benchmark = isConfigParameterTrue("bench");

        String apiKey =  getInitParameter("api-key");
        if(apiKey == null || apiKey.trim().equals("")) {
            throw new ServletException("API key missing");
        }

        String apiUri = getInitParameter("api-uri");
        httpApiClient = new HttpApiClient(apiKey, apiUri);

        filterParams = getFilterRegex("filter-params");
        jsFilterParams = getJsFilter("filter-params");
        filterUri = getFilterRegex("filter-uri");
        crawlers = getFilterRegex("filter-crawlers");
        if(crawlers == null) crawlers = Pattern.compile(CRAWLERS);
        if(getInitParameter("capture-crawlers") != null)
            captureCrawlers = true;

        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private String getInitParameter(String name) {
        String value = filterConfig.getInitParameter(name);
        if (value != null) return value;
        name = "io.clickstream." + name;
        value = filterConfig.getServletContext().getInitParameter(name);
        if (value != null) return value;
        return System.getenv(name);
    }

    public void setFilterConfig(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }


    private boolean isConfigParameterTrue(String name) {
        String param = getInitParameter(name);
        return param != null && param.equals("true");
    }

    private Pattern getFilterRegex(String param) {
        String sFilter = getInitParameter(param);
        return sFilter != null && !sFilter.trim().equals("") ?
                Pattern.compile("(" + sFilter + ")") :
                null;
    }

    private String getJsFilter(String param) {
        String sFilter = getInitParameter(param);
        return sFilter != null ?
                "[" + join(sFilter.split("\\|"), ",") + "]" :
                null;
    }

    private String join(String[] strings, String glue) {
        int length = strings.length;
        if (length == 0) return "";
        StringBuilder out = new StringBuilder();
        appendQuotedString(out, strings[0]);
        for (int i = 1; i < length; i++) appendQuotedString(out.append(glue), strings[i]);
        return out.toString();
    }

    private void appendQuotedString(StringBuilder out, String string) {
        out.append("'").append(string).append("'");
    }

    public HttpApiClient getHttpApiClient() {
        return httpApiClient;
    }

    public boolean isCapture() {
        return capture;
    }

    public boolean isBenchmark() {
        return benchmark;
    }

    public Pattern getFilterUri() {
        return filterUri;
    }

    public Pattern getCrawlers() {
        return crawlers;
    }

    public boolean isCaptureCrawlers() {
        return captureCrawlers;
    }

    public Pattern getFilterParams() {
        return filterParams;
    }

    public String getJsFilterParams() {
        return jsFilterParams;
    }

    public String getHostname() {
        return hostname;
    }
}
