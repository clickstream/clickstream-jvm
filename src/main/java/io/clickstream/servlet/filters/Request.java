package io.clickstream.servlet.filters;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Enumeration;
import java.util.Map;
import java.util.HashMap;

public class Request {
    private final String ip;
    private final String uri;
    private final String user_agent;
    private final String referer;
    private final String method;
    private final String script_name;
    private final String path_info;
    private final String querystring;
    private final String scheme;
    private final String host;
    private final int port;
    private final String url;
    private final String content_type;
    private final String content_charset;
    private final String protocol;
    private boolean xhr;
    private Map<String,String> params;
    private final Map<String,String> headers;
    private final Map<String,String> cookies;
    private final Map<String,String> session;

    public Request(HttpServletRequest req) {
        // TODO: session_options, path_parameters?
        this.ip = getClientIp(req);
        this.uri = req.getRequestURI();
        this.user_agent = req.getHeader("User-Agent");
        this.referer = req.getHeader("Referer");
        this.method = req.getMethod();
        this.script_name = req.getServletPath();
        this.path_info = req.getPathInfo();
        this.querystring = req.getQueryString();
        this.scheme = req.getScheme();
        this.host = req.getServerName();
        this.port = req.getServerPort();
        this.url = req.getRequestURL().toString();
        this.content_type = req.getContentType();
        this.content_charset = req.getCharacterEncoding();
        this.protocol = req.getProtocol();
        this.xhr = false; //req.getHeader("HTTP_X_REQUESTED_WITH").equals("XMLHttpRequest");
        this.headers = getHeaders(req);
        this.cookies = getCookies(req);
        this.session = getSession(req);
    }

    public void prepare () {
        this.params = getParams();
    }

    private Map<String,String> getHeaders(HttpServletRequest req) {
        Map<String,String> map = new HashMap<String, String>();
        Enumeration<String> headerNames = req.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String header = headerNames.nextElement();
            if(! header.equals("cookie")) {
                map.put(header, req.getHeader(header));
            }
        }
        return map;
    }

    private Map<String, String> getCookies(HttpServletRequest req) {
        Map<String,String> map = new HashMap<String, String>();
        Cookie[] cookies = req.getCookies();
        if(cookies != null) {
            for(Cookie cookie : cookies) {
                map.put(cookie.getName(), cookie.getValue());
            }
        }
        return map;
    }

    private Map<String, String> getSession(HttpServletRequest req) {
        Map<String,String> map = new HashMap<String, String>();
        HttpSession session = req.getSession();
        Enumeration<String> sessionAttributeNames = session.getAttributeNames();
        while (sessionAttributeNames.hasMoreElements()) {
            String sessionAttributeName = sessionAttributeNames.nextElement();
            map.put(sessionAttributeName, String.valueOf(session.getAttribute(sessionAttributeName)));
        }
        return map;
    }

    private Map<String,String> getParams() {
        Map<String,String> map = new HashMap<String, String>();
        if(querystring != null) {
            String[] nameValuePairs = querystring.split("&");
            for(String nameValuePair: nameValuePairs) {
                String[] params = nameValuePair.split("=");
                map.put(params[0], params[1]);
            }
        }
        return map;
    }

    private String getClientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = req.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = req.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = req.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = req.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = req.getRemoteAddr();
        }
        return ip;
    }

//        return Json.createObjectBuilder()
//                .add("path", script_name + path_info)
//                .add("fullpath", querystring == null || querystring.equals("") ? uri : uri + "?" + querystring)
//                .add("url", url)
//                .add("base_url", scheme + "://" + host + (port != 80 && port != 443 ? ":" + port : ""))
//                .add("content_type", content_type)
//                .add("content_charset", char_set)
//                .add("protocol", protocol)
////                .add("xhr", xhr)
//                .build();
//    }
}
