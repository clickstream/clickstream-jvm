package io.clickstream.servlet.filters;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Inspector implements Runnable {

    private HttpApiClient httpApiClient;
    private Hit hit;

    public Inspector(HttpApiClient httpApiClient, HttpServletRequest req, HttpServletResponse res, String body, long start, long end, String cookie, String pid, String hostname) {
        this.httpApiClient = httpApiClient;
        this.hit = new Hit(req, res, body, start, end, cookie, pid, hostname);
    }

    public void run() {
        System.out.println(hit.toJson());
        try {
            ApiResponse response = httpApiClient.postData(hit.toJson());
            // TODO: log response
        } catch (IOException e) {
            // TODO: log error
            e.printStackTrace();
        }
    }
}
