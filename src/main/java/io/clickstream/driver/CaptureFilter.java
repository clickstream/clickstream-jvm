package io.clickstream.driver;

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
    public static final Pattern ACCEPTED_CONTENT_TYPES = Pattern.compile("(text/html|application/json|application/xml|text/plain)");
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private Future<ApiResponse> future;
    private ApiResponse handshakeResponse;
    private String hostname;
    private Config config;

    public void init(FilterConfig filterConfig) throws ServletException {
        config = new Config(filterConfig);

        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        doHandshake();
    }

    private void doHandshake() {
        Handshake handshake = new Handshake(config.getHttpApiClient());
        ExecutorService handshakeService = Executors.newSingleThreadExecutor();
        future = handshakeService.submit(handshake);
    }

    public void destroy() {
        config.setFilterConfig(null);
    }

    public void doFilter(ServletRequest req, ServletResponse res,
                         FilterChain chain) throws IOException, ServletException {

        long s = System.currentTimeMillis();
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        if(handshakeResponse == null) setHandshakeResponse();

        if(config.isCapture() && ! isFilteredUri(request) && ! isBot(request)) {
            ResponseWrapper responseWrapper = new ResponseWrapper(response);

            long start = System.currentTimeMillis();
            chain.doFilter(req, responseWrapper);
            long end = System.currentTimeMillis();

            if(isContentTypeAccepted(responseWrapper.getContentType())) {
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
        return config.getFilterUri().matcher(request.getRequestURI()).find();
    }

    private boolean isBot(HttpServletRequest request) {
        return ! config.isCaptureCrawlers() && config.getCrawlers().matcher(request.getHeader("User-Agent")).find();
    }

    private boolean isContentTypeAccepted(String contentType) {
        return contentType != null && ACCEPTED_CONTENT_TYPES.matcher(contentType).find();
    }

    private void doCapture(HttpServletRequest request, ResponseWrapper responseWrapper, long start, long end) throws IOException {
        String cookie = setCookie(request, responseWrapper);
        String pid = UUID.randomUUID().toString();
        String body = responseWrapper.toString();
        String contentType = responseWrapper.getContentType();
        Inspector inspector = new Inspector(config.getHttpApiClient(), hostname, config.getFilterParams());
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
                "', sid='" + cookie + "', pid='" + pid + "', paramsFilter = " + config.getJsFilterParams() + ";" +
                handshakeResponse.getJs() +"})();</script>";
        caw.write(body + "\n" + script);
        return caw.toString();
    }
}
