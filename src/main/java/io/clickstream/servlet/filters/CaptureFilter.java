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

// TODO: filter params, filter urls, filter crawlers, benchmark, logging?

public class CaptureFilter implements Filter {
    private FilterConfig filterConfig = null;
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private HttpApiClient httpApiClient;
    private Future<ApiResponse> future;
    private ApiResponse handshakeResponse;
    private String hostname;

    public void init(FilterConfig filterConfig) throws ServletException {

        this.filterConfig = filterConfig;
        String apiKey = filterConfig.getInitParameter("api-key");

        if(apiKey == null || apiKey.trim().equals("")) {
            throw new ServletException("API key missing");
        }

        String apiUri = filterConfig.getInitParameter("api-uri");
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

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
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        if(handshakeResponse == null) setHandshakeResponse();

        CharResponseWrapper wrapper = new CharResponseWrapper(response);

        long start = System.currentTimeMillis();
        chain.doFilter(req, wrapper);
        long end = System.currentTimeMillis();

        if(! wrapper.toString().equals("")) {
            String cookie = wrapper.setCookie(getCookie(request, "clickstream"));
            System.out.println(cookie);
            String pid = UUID.randomUUID().toString();
            Inspector inspector = new Inspector(httpApiClient, request, response, wrapper.toString(), start, end, cookie, pid, hostname);
            wrapper.finishResponse(handshakeResponse, cookie, pid);
            executorService.submit(inspector);
        }
        System.out.println(("Done: " + (System.currentTimeMillis() - s - (end - start))));
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

    private void setHandshakeResponse() {
        try {
            handshakeResponse = future.get();
        } catch (Exception e) {
            // TODO: log error
            e.printStackTrace();
        }
    }
}
