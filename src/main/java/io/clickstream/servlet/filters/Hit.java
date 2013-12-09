package io.clickstream.servlet.filters;

import com.google.gson.Gson;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Hit {
    private final String sid;
    private final String pid;
    private final Request request;
    private final Response response;
    private final String hostname;

    public Hit(HttpServletRequest req, HttpServletResponse res, String body, long start, long end, String cookie, String pid, String hostname) {
        this.sid = cookie;
        this.pid = pid;
        this.request = new Request(req);
        this.response = new Response(res, body, start, end);
        this.hostname = hostname;
    }

    public String toJson () {
        request.prepare();
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
