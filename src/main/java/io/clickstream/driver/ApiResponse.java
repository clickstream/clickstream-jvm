package io.clickstream.driver;

class ApiResponse {
    private String clientId;
    private String ws;
    private String js;
    private String secret;
    private String message;
    private String status;

    public ApiResponse() {

    }

    public String getClientId() {
        return clientId;
    }

    public String getWs() {
        return ws;
    }

    public String getJs() {
        return js;
    }

    public String getSecret() {
        return secret;
    }

    public String getMessage() {
        return message;
    }

    public String getStatus() {
        return status;
    }
}
