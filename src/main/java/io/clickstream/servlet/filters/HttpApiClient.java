package io.clickstream.servlet.filters;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPOutputStream;

public class HttpApiClient {

    private String apiKey;
    private String apiUri = "http://localhost:15080";

    public HttpApiClient(String apiKey, String apiUri) {
        this.apiKey = apiKey;
        System.out.println("apiUri: " + apiUri);
        if(apiUri != null) this.apiUri = apiUri;
    }

    public ApiResponse handshake() throws IOException {
        HttpURLConnection con = getConnection("/handshake");
        return getResponse(con);
    }

    public ApiResponse postData(String json) throws IOException {
        HttpURLConnection con = getConnection("/capture");
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Encoding", "gzip");
        con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        con.setDoOutput(true);
        byte[] payload = compress(json);
        OutputStream os = con.getOutputStream();
        os.write(payload);
        os.flush();

        return getResponse(con);
    }

    private HttpURLConnection getConnection (String endpoint) throws IOException {
        String url = apiUri + "/" + apiKey + endpoint;
        System.out.println("url: " + url);
        URL objUrl = new URL(url);
        HttpURLConnection con = (HttpURLConnection) objUrl.openConnection();
        con.setRequestProperty("User-Agent", "Clickstream/JVM");
        con.setRequestProperty("Accept", "application/json");
        System.out.println("\nSending request to URL : " + url);
        return con;
    }

    private ApiResponse getResponse(HttpURLConnection con) throws IOException {
        int responseCode = con.getResponseCode();
        System.out.println("Response Code : " + responseCode);

        Gson gson = new Gson();
        ApiResponse apiResponse = gson.fromJson(readInputStream(con.getInputStream()), ApiResponse.class);

        if(responseCode != 200) {
            String errorMessage = "Received non 200 response code: " + Integer.toString(responseCode);
            try {
                errorMessage += " " + apiResponse.getMessage();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            throw new IOException(errorMessage);
        }
        return apiResponse;
    }

    private static String readInputStream(InputStream is) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(is));

        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

    private static byte[] compress(String str) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gzip;
        gzip = new GZIPOutputStream(baos);
        gzip.write(str.getBytes("UTF-8"));
        gzip.close();
        return baos.toByteArray();
    }
}
