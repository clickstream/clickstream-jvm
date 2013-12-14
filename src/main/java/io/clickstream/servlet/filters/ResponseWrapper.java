package io.clickstream.servlet.filters;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;

public class ResponseWrapper extends HttpServletResponseWrapper {
    public static final Pattern SUPPORTED_CONTENT_TYPES = Pattern.compile("(text/html|application/json|application/xml|text/plain)");
    private final CharArrayWriter output = new CharArrayWriter();
    private final HttpServletResponse res;
    private String body;

    public String toString() {
        return output.toString();
    }

    public ResponseWrapper(HttpServletResponse response) throws IOException {
        super(response);
        res = response;
    }

    public PrintWriter getWriter(){
        return new PrintWriter(output);
    }

    public String setCookie(Cookie cookie, int age) {
        cookie.setMaxAge(age);
        res.addCookie(cookie);
        return cookie.getValue();
    }

    public void finishResponse(ApiResponse client, String cookie, String pid) throws IOException {
        PrintWriter out = res.getWriter();
        String contentType = res.getContentType();

        if(client != null && contentType != null && contentType.contains("text/html")) {
            CharArrayWriter caw = new CharArrayWriter();
            String body = output.toString();

            String script = "<script>(function(){var uri='" + client.getWs() + "', cid='" + client.getClientId() +
                    "', sid='" + cookie + "', pid='" + pid + "';" + client.getJs() +"})();</script>";

            caw.write(body + "\n" + script);
            res.setContentLength(caw.toString().length());
            out.write(caw.toString());
        } else
            out.write(output.toString());

        out.close();
    }

    public boolean isCapturable() {
        return  output.size() > 0 && SUPPORTED_CONTENT_TYPES.matcher(res.getContentType()).find();
    }
}
