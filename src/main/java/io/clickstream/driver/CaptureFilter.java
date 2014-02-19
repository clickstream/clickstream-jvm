package io.clickstream.driver;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

// TODO: STATUS_WITH_NO_ENTITY_BODY, benchmark, logging

public class CaptureFilter implements Filter {
    public static final Pattern ACCEPTED_CONTENT_TYPES = Pattern.compile("(text/html|application/json|application/xml|text/plain)");
    private Config config;
    private ApiResponse handshakeResponse;
    private Future<ApiResponse> future;

    public void init(FilterConfig filterConfig) throws ServletException {
        config = new Config(filterConfig);
        if (config.isCapture()) doHandshake();
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

        if(config.isCapture() && ! isFilteredUri(request) && ! isBot(request)) {
            if(handshakeResponse == null) setHandshakeResponse();
            ResponseWrapper responseWrapper = new ResponseWrapper(response);

            long start = System.currentTimeMillis();
            chain.doFilter(req, responseWrapper);
            long end = System.currentTimeMillis();

            if(isContentTypeAccepted(responseWrapper.getContentType())) {
                Inspector inspector = new Inspector(config, handshakeResponse);
                String body = inspector.investigate(request, responseWrapper, start, end);
                responseWrapper.finishResponse(body);
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
        String userAgent = request.getHeader("User-Agent");
        return ! config.isCaptureCrawlers() && userAgent != null && config.getCrawlers().matcher(userAgent).find();
    }

    private boolean isContentTypeAccepted(String contentType) {
        return contentType != null && ACCEPTED_CONTENT_TYPES.matcher(contentType).find();
    }


}
