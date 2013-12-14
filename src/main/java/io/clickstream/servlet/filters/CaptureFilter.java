package io.clickstream.servlet.filters;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
    public static final String COOKIE_NAME = "clickstream.io";
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
    private Pattern filterUri;
    private Pattern crawlers;
    private boolean captureCrawlers = false;

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
        filterParams = setFilterRegex("filter-params");
        filterUri = setFilterRegex("filter-uri");
        crawlers = setFilterRegex("filter-crawlers");
        if(crawlers == null) crawlers = Pattern.compile(CRAWLERS);
        if(filterConfig.getInitParameter("capture-crawlers") != null)
            captureCrawlers = true;

        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        doHandshake(apiKey, apiUri);
    }

    private boolean isConfigParameterTrue(String name) {
        String param = filterConfig.getInitParameter(name);
        return param != null && param.equals("true");
    }

    private Pattern setFilterRegex(String param) {
        String sFilter = filterConfig.getInitParameter(param);
        return sFilter != null && !sFilter.trim().equals("") ?
                Pattern.compile('(' + sFilter + ')') :
                null;
    }

    private void doHandshake(String apiKey, String apiUri) {
        httpApiClient = new HttpApiClient(apiKey, apiUri);
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
        if(capture) {
            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) res;

            if(! isFilteredUri(request) && ! isBot(request)) {
                if(handshakeResponse == null) setHandshakeResponse();

                CharResponseWrapper wrapper = new CharResponseWrapper(response);

                long start = System.currentTimeMillis();
                chain.doFilter(req, wrapper);
                long end = System.currentTimeMillis();

                if(wrapper.isCapturable()) {
                    String cookie = wrapper.setCookie(getCookie(request, COOKIE_NAME), COOKIE_NAME);
                    String pid = UUID.randomUUID().toString();
                    Inspector inspector = new Inspector(httpApiClient, hostname, filterParams);
                    inspector.investigate(request, response, wrapper.toString(), start, end, cookie, pid);
                    wrapper.finishResponse(handshakeResponse, cookie, pid);
                    executorService.submit(inspector);
                }
                System.out.println(("Done: " + (System.currentTimeMillis() - s - (end - start))));
            } else {
                long start = System.currentTimeMillis();
                chain.doFilter(req, res);
                long end = System.currentTimeMillis();
                System.out.println(("Done: " + (System.currentTimeMillis() - s - (end - start))));
            }
        }  else {
            long start = System.currentTimeMillis();
            chain.doFilter(req, res);
            long end = System.currentTimeMillis();
            System.out.println(("Done: " + (System.currentTimeMillis() - s - (end - start))));
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

    private boolean isBot(HttpServletRequest request) {
        return ! captureCrawlers && crawlers.matcher(request.getHeader("User-Agent")).find();
    }

    private boolean isFilteredUri(HttpServletRequest request) {
        System.out.println("uri:" + request.getRequestURI());
        System.out.println("filterUri:" + filterUri.toString());
        return filterUri.matcher(request.getRequestURI()).find();
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
}
