package io.clickstream.driver;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

class Inspector implements Runnable {
    public static final String COOKIE_NAME = "clickstream-io";
    public static final int COOKIE_AGE = 60*60;
    private final HttpApiClient httpApiClient;
    private final String hostname;
    private final Pattern filterParams;
    private final ApiResponse handshakeResponse;
    private final String jsFilterParams;
    private Hit hit;
    private ExecutorService executorService = Executors.newCachedThreadPool();

    public Inspector(Config config, ApiResponse handshakeResponse) {
        this.httpApiClient = config.getHttpApiClient();
        this.hostname = config.getHostname();
        this.filterParams = config.getFilterParams();
        this.jsFilterParams = config.getJsFilterParams();
        this.handshakeResponse = handshakeResponse;
    }

    public String investigate(HttpServletRequest request, ResponseWrapper responseWrapper, long start, long end) throws IOException {
        String cookie = setCookie(request, responseWrapper);
        String pid = UUID.randomUUID().toString();
        String body = responseWrapper.toString();
        String contentType = responseWrapper.getContentType();
        this.hit = new Hit(request, responseWrapper, body, start, end, cookie, pid, hostname, filterParams);

        if(handshakeResponse != null && contentType != null && contentType.contains("text/html") && body.length() > 0) {
            body = insertJs(cookie, pid, body);
        }

        executorService.submit(this);
        return body;
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

    public void run() {
        try {
            ApiResponse response = httpApiClient.postData(hit.toJson());
            // TODO: log response
            System.out.println("status:" + response.getStatus() + ", message:" + response.getMessage());
        } catch (IOException e) {
            // TODO: log error
            e.printStackTrace();
        }
    }
}
