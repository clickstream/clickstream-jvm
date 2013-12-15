package io.clickstream.driver;

import io.clickstream.api.ApiResponse;
import io.clickstream.api.Handshake;
import io.clickstream.api.HttpApiClient;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

// TODO: STATUS_WITH_NO_ENTITY_BODY, benchmark, logging

public class CaptureFilter implements Filter {
    public static final String COOKIE_NAME = "clickstream-io";
    public static final int COOKIE_AGE = 60*60;
    public static final String CRAWLERS = "(Baidu|Gigabot|Googlebot|libwww-perl|lwp-trivial|msnbot|SiteUptime|Slurp|WordPress|ZIBB|ZyBorg|bot|crawler|spider|robot|crawling|facebook|w3c|coccoc|Daumoa|panopta)";
    private boolean capture = false;
    private boolean benchmark = false;
    private FilterConfig filterConfig = null;
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private HttpApiClient httpApiClient;
    private Future<ApiResponse> future;
    private ApiResponse handshakeResponse;
    private String hostname;
    private Pattern filterParams;
    private String jsFilterParams;
    private Pattern filterUri;
    private Pattern crawlers;
    private boolean captureCrawlers = false;
    private Inspector inspector;

    public void init(FilterConfig filterConfig) throws ServletException {

        this.filterConfig = filterConfig;

        if(isConfigParameterTrue("capture"))
            capture = true;
        else
            return;

        if(isConfigParameterTrue("bench"))
            benchmark = true;

        String apiKey = filterConfig.getInitParameter("api-key");
        if(apiKey == null || apiKey.trim().equals("")) {
            throw new ServletException("API key missing");
        }

        String apiUri = filterConfig.getInitParameter("api-uri");
        httpApiClient = new HttpApiClient(apiKey, apiUri);

        filterParams = getFilterRegex("filter-params");
        jsFilterParams = getJsFilter("filter-params");
        filterUri = getFilterRegex("filter-uri");
        crawlers = getFilterRegex("filter-crawlers");
        if(crawlers == null) crawlers = Pattern.compile(CRAWLERS);
        if(filterConfig.getInitParameter("capture-crawlers") != null)
            captureCrawlers = true;

        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        doHandshake();
    }

    private boolean isConfigParameterTrue(String name) {
        String param = filterConfig.getInitParameter(name);
        return param != null && param.equals("true");
    }

    private Pattern getFilterRegex(String param) {
        String sFilter = filterConfig.getInitParameter(param);
        return sFilter != null && !sFilter.trim().equals("") ?
                Pattern.compile("(" + sFilter + ")") :
                null;
    }

    private String getJsFilter(String param) {
        String sFilter = filterConfig.getInitParameter(param);
        return sFilter != null ?
                "[" + join(sFilter.split("/"), ",") + "]" :
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

    private void doHandshake() {
        Handshake handshake = new Handshake(httpApiClient);
        ExecutorService handshakeService = Executors.newSingleThreadExecutor();
        future = handshakeService.submit(handshake);
    }

    public void destroy() {
        this.filterConfig = null;
    }

    public void doFilter(ServletRequest req, ServletResponse res,
                         FilterChain chain) throws IOException, ServletException {

        long s = System.currentTimeMillis();
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        if(handshakeResponse == null) setHandshakeResponse();

        if(capture && ! isFilteredUri(request) && ! isBot(request)) {
            ResponseWrapper responseWrapper = new ResponseWrapper(response);

            long start = System.currentTimeMillis();
            chain.doFilter(req, responseWrapper);
            long end = System.currentTimeMillis();

            if(responseWrapper.isCapturable()) {
                doCapture(request, responseWrapper, start, end);
                System.out.println(("Done: " + (System.currentTimeMillis() - s - (end - start))));
            } else {
                responseWrapper.finishResponse();
            }


        }  else {
            chain.doFilter(req, res);
        }
    }

    private void setHandshakeResponse() {
        try {
            handshakeResponse = future.get();
        } catch (Exception e) {
            // TODO: log error
            e.printStackTrace();
        }
    }

    private boolean isFilteredUri(HttpServletRequest request) {
        return filterUri.matcher(request.getRequestURI()).find();
    }

    private boolean isBot(HttpServletRequest request) {
        return ! captureCrawlers && crawlers.matcher(request.getHeader("User-Agent")).find();
    }

    private void doCapture(HttpServletRequest request, ResponseWrapper responseWrapper, long start, long end) throws IOException {
        String cookie = setCookie(request, responseWrapper);
        String pid = UUID.randomUUID().toString();
        String body = responseWrapper.toString();
        String contentType = responseWrapper.getContentType();
        inspector = new Inspector(httpApiClient, hostname, filterParams);
        inspector.investigate(request, responseWrapper, body, start, end, cookie, pid);

        if(handshakeResponse != null && contentType != null && contentType.contains("text/html") && body.length() > 0) {
            body = insertJs(cookie, pid, body);
        }

        responseWrapper.finishResponse(body);
        executorService.submit(inspector);
    }

    private String setCookie(HttpServletRequest request, ResponseWrapper responseWrapper) {
        Cookie cookie = getCookie(request, COOKIE_NAME);
        if(cookie == null) {
            String uuid = UUID.randomUUID().toString();
            cookie = new Cookie(COOKIE_NAME, uuid);
        }
        responseWrapper.setCookie(cookie, COOKIE_AGE);
        return cookie.getValue();
    }

    private Cookie getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if(cookies != null) {
            for(Cookie cookie : cookies) {
                if(cookie.getName().equals(name))
                    return cookie;
            }
        }
        return null;
    }

    private String insertJs(String cookie, String pid, String body) throws IOException {
        CharArrayWriter caw = new CharArrayWriter();
        String script = "<script>(function(){var uri='" + handshakeResponse.getWs() + "', cid='" + handshakeResponse.getClientId() +
                "', sid='" + cookie + "', pid='" + pid + "', paramsFilter = " + jsFilterParams + ";" +
                handshakeResponse.getJs() +"})();</script>";
        caw.write(body + "\n" + script);
        return caw.toString();
    }
}
