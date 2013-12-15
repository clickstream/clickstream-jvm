package io.clickstream.api;

import java.io.IOException;
import java.util.concurrent.Callable;

public class Handshake implements Callable<ApiResponse> {
    private HttpApiClient httpApiClient;

    public Handshake(HttpApiClient httpApiClient) {
        this.httpApiClient = httpApiClient;
    }

    public ApiResponse call() throws IOException {
        return httpApiClient.handshake();
    }
}
