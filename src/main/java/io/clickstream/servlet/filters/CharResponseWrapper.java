package io.clickstream.servlet.filters;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

public class CharResponseWrapper extends HttpServletResponseWrapper {
    private CharArrayWriter output;
    private HttpServletResponse res;

    public String toString() {
        return output.toString();
    }

    public CharResponseWrapper(HttpServletResponse response) throws IOException {
        super(response);
        res = response;
        output = new CharArrayWriter();
    }

    public PrintWriter getWriter(){
        return new PrintWriter(output);
    }

    public String setCookie(Cookie cookie) {
        if(cookie == null) {
            String uuid = UUID.randomUUID().toString();
            cookie = new Cookie("clickstream", uuid);
        }
        cookie.setMaxAge(60*60);
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
}
