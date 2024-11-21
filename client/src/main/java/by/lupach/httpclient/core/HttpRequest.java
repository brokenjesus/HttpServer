package by.lupach.httpclient.core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class HttpRequest {
    private final String host;
    private final int port;
    private final String requestString;

    private HttpRequest(String host, int port, String requestString) {
        this.host = host;
        this.port = port;
        this.requestString = requestString;
    }

    public static HttpRequest fromParameters(String url, String method, String headers, String body) {
        // Парсинг URL
        String host;
        int port;
        String path = "/";
        if (url.startsWith("http://")) {
            url = url.substring(7);
        }
        int colonIndex = url.indexOf(":");
        int slashIndex = url.indexOf("/");
        if (colonIndex != -1) {
            host = url.substring(0, colonIndex);
            port = Integer.parseInt(url.substring(colonIndex + 1, slashIndex != -1 ? slashIndex : url.length()));
        } else {
            host = url.substring(0, slashIndex != -1 ? slashIndex : url.length());
            port = 80;
        }
        if (slashIndex != -1) {
            path = url.substring(slashIndex);
        }

        // Формирование запроса
        StringBuilder request = new StringBuilder();
        request.append(method).append(" ").append(path).append(" HTTP/1.1\r\n");
        request.append("Host: ").append(host).append("\r\n");
        if (headers != null) {
            request.append(headers).append("\r\n");
        }
        request.append("Connection: close\r\n");
        if (body != null) {
            request.append("Content-Length: ").append(body.length()).append("\r\n");
            request.append("\r\n").append(body);
        } else {
            request.append("\r\n");
        }

        return new HttpRequest(host, port, request.toString());
    }
    public static HttpRequest fromTemplate(String templatePath) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(templatePath))) {
            StringBuilder request = new StringBuilder();
            String line;
            String host = "localhost";
            int port = 80;

            while ((line = br.readLine()) != null) {
                request.append(line).append("\r\n");

                // Extract host and port from the 'Host:' header
                if (line.toLowerCase().startsWith("host:")) {
                    String[] parts = line.split(":");
                    host = parts[1].trim();
                    if (parts.length > 2) {
                        port = Integer.parseInt(parts[2].trim());
                    }
                }
            }

            // Construct URL from host and port
            String url = "http://" + host + ":" + port;

            return new HttpRequest(host, port, request.toString());
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getRequestString() {
        return requestString;
    }
}