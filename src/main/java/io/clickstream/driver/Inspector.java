package io.clickstream.driver;

import io.clickstream.api.ApiResponse;
import io.clickstream.api.HttpApiClient;
import io.clickstream.hit.Hit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

public class Inspector implements Runnable {

    private final HttpApiClient httpApiClient;
    private final String hostname;
    private final Pattern filterParams;
    private Hit hit;

    public Inspector(HttpApiClient httpApiClient, String hostname, Pattern filterParams) {

        this.httpApiClient = httpApiClient;
        this.hostname = hostname;
        this.filterParams = filterParams;
    }

    public void investigate(HttpServletRequest req, HttpServletResponse res, String body,
                            long start, long end, String cookie, String pid) {

        this.hit = new Hit(req, res, body, start, end, cookie, pid, hostname, filterParams);
    }

    public void run() {
        try {
            ApiResponse response = httpApiClient.postData(hit.toJson());
            // TODO: log response
            System.out.println("status:" + response.getStatus() + ", message:" + response.getMessage());
        } catch (IOException e) {
            // TODO: log error
            e.printStackTrace();
        }
    }
}
