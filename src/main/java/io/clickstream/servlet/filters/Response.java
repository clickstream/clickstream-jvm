package io.clickstream.servlet.filters;

import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Response {
    private final int status;
    private final Map<String,String> headers;
    private final long size;
    private final String body;
    private final long start;
    private final long end;
    private final long time;

    public Response(HttpServletResponse res, String body, long start, long end) {
        this.status = res.getStatus();
        this.headers = getHeaders(res);
        this.size = body.length();
        this.body = body;
        this.start = start;
        this.end = end;
        this.time = end - start;
    }

    private Map<String,String> getHeaders(HttpServletResponse res) {
        Map<String,String> map = new HashMap<String, String>();
        Collection<String> headerNames = res.getHeaderNames();
        for(String header : headerNames) {
            map.put(header, res.getHeader(header));
        }
        if(map.get("Content-Type") == null) map.put("Content-Type", res.getContentType());
        return map;
    }
}
