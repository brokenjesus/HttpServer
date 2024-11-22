package by.lupach.httpclient.core;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class HttpRequest {
    private final String host;
    private final int port;
    private final byte[] requestBytes;
    private final byte[] bodyBytes;

    private HttpRequest(String host, int port, byte[] requestBytes, byte[] bodyBytes) {
        this.host = host;
        this.port = port;
        this.requestBytes = requestBytes;
        this.bodyBytes = bodyBytes;
    }

    public static HttpRequest fromParameters(String url, String method, String headers, String body, String filePath) throws IOException {
        if (Objects.equals(method, "POST") && filePath !=null) {
            return fileUpload(url, filePath, method);
        }

        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("URL is required");
        }

        String host;
        int port;
        String path = "/";

        // Parse URL
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
            port = 80; // Default HTTP port
        }

        if (slashIndex != -1) {
            path = url.substring(slashIndex);
        }

        // Forming the request headers as byte array
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write((method + " " + path + " HTTP/1.1\r\n").getBytes());
        byteArrayOutputStream.write(("Host: " + host + "\r\n").getBytes());
        if (headers != null) {
            byteArrayOutputStream.write((headers + "\r\n").getBytes());
        }
        byteArrayOutputStream.write("Connection: close\r\n".getBytes());
        if (body != null) {
            byteArrayOutputStream.write(("Content-Length: " + body.length() + "\r\n").getBytes());
            byteArrayOutputStream.write("\r\n".getBytes());
            byteArrayOutputStream.write(body.getBytes());
        } else {
            byteArrayOutputStream.write("\r\n".getBytes());
        }

        byte[] requestBytes = byteArrayOutputStream.toByteArray();
        byte[] bodyBytes = body != null ? body.getBytes() : null;

        return new HttpRequest(host, port, requestBytes, bodyBytes);
    }

    public static HttpRequest fileUpload(String url, String filePath, String method) throws IOException {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("URL is required");
        }

        String host;
        int port;
        String path = "/";

        // Parse URL
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

        // Read file content as bytes
        File file = new File(filePath);
        byte[] fileContent = Files.readAllBytes(file.toPath());

        // Build multipart/form-data body
        String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(("--" + boundary + "\r\n").getBytes());
        byteArrayOutputStream.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n").getBytes());
        byteArrayOutputStream.write(("Content-Type: " + Files.probeContentType(file.toPath()) + "\r\n\r\n").getBytes());

        // Write file content
        byteArrayOutputStream.write(fileContent);
        byteArrayOutputStream.write(("\r\n--" + boundary + "--\r\n").getBytes());

        byte[] finalBody = byteArrayOutputStream.toByteArray();

        // Prepare request headers
        ByteArrayOutputStream requestStream = new ByteArrayOutputStream();
        requestStream.write((method + " " + path + " HTTP/1.1\r\n").getBytes());
        requestStream.write(("Host: " + host + "\r\n").getBytes());
        requestStream.write(("Content-Type: multipart/form-data; boundary=" + boundary + "\r\n").getBytes());
        requestStream.write(("Content-Length: " + finalBody.length + "\r\n").getBytes());
        requestStream.write("Connection: close\r\n".getBytes());
        requestStream.write("\r\n".getBytes());

        byte[] requestBytes = requestStream.toByteArray();

        return new HttpRequest(host, port, requestBytes, finalBody);
    }

    public static HttpRequest fromTemplate(String templatePath) throws IOException {
        byte[] requestBytes;

        // Read the file content as bytes
        requestBytes = Files.readAllBytes(Paths.get(templatePath));

        // Now we can extract the host and port as before, but we'll work with the byte array
        String host = "localhost";
        int port = 80;

        // We will convert the byte array to a String to parse the headers
        String requestStr = new String(requestBytes);
        BufferedReader br = new BufferedReader(new StringReader(requestStr));

        String filePath = null;
        String directory = null;

        String line;
        while ((line = br.readLine()) != null) {
            // Extract host and port from the 'Host:' header
            if (line.toLowerCase().startsWith("filepath")) {
                String[] parts = line.split("=");
                filePath = parts[1].trim();
            }
            if (line.toLowerCase().startsWith("post")) {
                String[] parts = line.split(" ");
                directory = parts[1].trim();
            }
            if (line.toLowerCase().startsWith("host:")) {
                String[] parts = line.split(":");
                host = parts[1].trim();
                if (parts.length > 2) {
                    port = Integer.parseInt(parts[2].trim());
                }
            }
        }

        // Construct URL from host and port
        String url = "http://" + host + ":" + port + "/" + directory;

        if (filePath != null) {
            return fileUpload(url, filePath, "POST");
        }

        return new HttpRequest(host, port, requestBytes, null);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public byte[] getRequestBytes() {
        return requestBytes;
    }

    public byte[] getBodyBytes() {
        return bodyBytes;
    }
}
