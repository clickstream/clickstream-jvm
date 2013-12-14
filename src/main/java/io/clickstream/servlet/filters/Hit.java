package io.clickstream.servlet.filters;

import com.google.gson.Gson;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.regex.Pattern;

public class Hit {
    private final String sid;
    private final String pid;
    private final Request request;
    private final Response response;
    private final String hostname;
    private final Pattern filterParams;

    public Hit(HttpServletRequest req, HttpServletResponse res, String body, long start, long end,
               String cookie, String pid, String hostname, Pattern filterParams) {
        this.sid = cookie;
        this.pid = pid;
        this.request = new Request(req);
        this.response = new Response(res, body, start, end);
        this.hostname = hostname;
        this.filterParams = filterParams;
    }

    public String toJson () {
        request.prepare(filterParams);
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
