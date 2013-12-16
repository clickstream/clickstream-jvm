package io.clickstream.driver;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;

class ResponseWrapper extends HttpServletResponseWrapper {
    private final CharArrayWriter output = new CharArrayWriter();
    private final HttpServletResponse res;

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
}
