package io.clickstream.driver;

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

    public void finishResponse() throws IOException {
        finishResponse(output.toString());
    }

    public void finishResponse(String output) throws IOException {
        PrintWriter out = res.getWriter();
        res.setContentLength(output.length());
        out.write(output);
        out.close();
    }

    public boolean isCapturable() {
        return  res.getContentType() != null && SUPPORTED_CONTENT_TYPES.matcher(res.getContentType()).find();
    }
}
