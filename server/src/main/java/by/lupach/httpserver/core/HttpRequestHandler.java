package by.lupach.httpserver.core;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpRequestHandler implements Runnable {
    private final Socket clientSocket;
    private final String rootDirectory;
    private final Map<String, String> defaultHeaders;
    private final Logger logger;

    public HttpRequestHandler(Socket clientSocket, String rootDirectory, Map<String, String> defaultHeaders, Logger logger) {
        this.clientSocket = clientSocket;
        this.rootDirectory = rootDirectory;
        this.defaultHeaders = defaultHeaders;
        this.logger = logger;
    }

    @Override
    public void run() {
        try (InputStream inputStream = clientSocket.getInputStream();
             OutputStream outputStream = clientSocket.getOutputStream()) {

            String requestLine = readLine(inputStream);
            if (requestLine == null || requestLine.isEmpty()) {
                return;
            }

            logger.info("Received request: " + requestLine);
            String[] parts = requestLine.split(" ");
            if (parts.length != 3) {
                sendResponse(outputStream, 400, "Bad Request", "Invalid request format");
                return;
            }

            String method = parts[0];
            String path = parts[1];
            String httpVersion = parts[2];

            if (!httpVersion.equals("HTTP/1.1")) {
                sendResponse(outputStream, 505, "HTTP Version Not Supported", "Unsupported HTTP version");
                return;
            }

            switch (method) {
                case "GET":
                    handleGet(outputStream, path);
                    break;
                case "POST":
                    handlePost(inputStream, outputStream, path);
                    break;
                case "OPTIONS":
                    handleOptions(outputStream);
                    break;
                default:
                    sendResponse(outputStream, 405, "Method Not Allowed", "Unsupported method: " + method);
            }
        } catch (IOException e) {
            logger.severe("Error handling request: " + e.getMessage());
        }
    }

    private void handleGet(OutputStream outputStream, String path) throws IOException {
        File file = new File(rootDirectory, path);
        if (!file.exists() || file.isDirectory()) {
            sendResponse(outputStream, 404, "Not Found", "File not found: " + path);
            return;
        }

        String contentType = Files.probeContentType(file.toPath());
        if (contentType == null) contentType = "application/octet-stream";

        try (InputStream fileInputStream = new FileInputStream(file)) {
            sendHeaders(outputStream, 200, "OK", contentType, file.length());
            byte[] buffer = new byte[8192]; // Буфер для передачи
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        }
    }

    private void sendHeaders(OutputStream outputStream, int statusCode, String statusMessage, String contentType, long contentLength) throws IOException {
        String headers = "HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Content-Length: " + contentLength + "\r\n\r\n";
        outputStream.write(headers.getBytes());
    }

//    private void sendHeaders(OutputStream outputStream, int statusCode, String statusMessage, String contentType, long contentLength) throws IOException {
//        String headers = "HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n" +
//                "Content-Type: " + contentType + "\r\n" +
//                "Content-Length: " + contentLength + "\r\n" +
//                "Cache-Control: no-cache, no-store, must-revalidate\r\n" +
//                "Pragma: no-cache\r\n" +
//                "Connection: close\r\n\r\n";
//        outputStream.write(headers.getBytes());
//    }


    private void handlePost(InputStream inputStream, OutputStream outputStream, String path) throws IOException {
        String contentType = null;
        int contentLength = -1;

        // Read headers
        while (true) {
            String line = readLine(inputStream);
            if (line == null || line.isEmpty()) {
                break;
            }
            if (line.startsWith("Content-Type:")) {
                contentType = line.split(":")[1].trim();
            }
            if (line.startsWith("Content-Length:")) {
                contentLength = Integer.parseInt(line.split(":")[1].trim());
            }
        }

        if (contentType == null || !contentType.startsWith("multipart/form-data")) {
            sendResponse(outputStream, 415, "Unsupported Media Type", "Content type must be multipart/form-data");
            return;
        }

        // Extract boundary
        String boundary = extractBoundary(contentType);
        if (boundary == null) {
            sendResponse(outputStream, 400, "Bad Request", "Boundary missing in multipart/form-data");
            return;
        }

        // Read the body
        byte[] body = inputStream.readNBytes(contentLength);

        // Extract file details
        String bodyString = new String(body);
        String fileName = extractFileName(body, boundary);
        byte[] fileData = extractFileData(body, boundary, contentLength);

        if (fileData != null && fileName != null) {
            File uploadDir = new File(rootDirectory, "uploads");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            File file = new File(uploadDir, fileName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(fileData);
            }

            sendResponse(outputStream, 200, "OK", "File uploaded successfully.");
        } else {
            sendResponse(outputStream, 400, "Bad Request", "No file data found in the request.");
        }
    }

    private void handleOptions(OutputStream outputStream) throws IOException {
        sendResponse(outputStream, 200, "OK", "Allowed methods: GET, POST, OPTIONS");
    }

    private void sendResponse(OutputStream outputStream, int statusCode, String statusMessage, String body) throws IOException {
        byte[] bodyBytes = body.getBytes();
        sendResponse(outputStream, statusCode, statusMessage, bodyBytes, "multipart/form-data");
    }

    private void sendResponse(OutputStream outputStream, int statusCode, String statusMessage, byte[] body, String contentType) throws IOException {
        String headers = "HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Content-Length: " + body.length + "\r\n\r\n";
        outputStream.write(headers.getBytes());
        outputStream.write(body);
        outputStream.flush(); // Убедитесь, что данные полностью отправлены
    }


    private String readLine(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int b;
        while ((b = inputStream.read()) != -1) {
            if (b == '\r') {
                int next = inputStream.read();
                if (next == '\n') {
                    break;
                }
            }
            buffer.write(b);
        }
        return buffer.toString();
    }

    private String extractBoundary(String contentType) {
        String[] parts = contentType.split(";");
        for (String part : parts) {
            part = part.trim();
            if (part.startsWith("boundary=")) {
                return part.substring("boundary=".length());
            }
        }
        return null;
    }

    private String extractFileName(byte[] body, String boundary) {
        String bodyString = new String(body);
        Pattern pattern = Pattern.compile("Content-Disposition: form-data; name=\"file\"; filename=\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(bodyString);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private byte[] extractFileData(byte[] body, String boundary, int contentLength) {
        String boundaryMarker = "--" + boundary;
        String bodyString = new String(body);
        int startIdx = bodyString.indexOf(boundaryMarker) + boundaryMarker.length();
        int contentStartIdx = bodyString.indexOf("\r\n\r\n", startIdx) + 4;
        int endIdx = bodyString.indexOf("--" + boundary + "--", contentStartIdx);
        if (startIdx != -1 && endIdx != -1) {
            return Arrays.copyOfRange(body, contentStartIdx, contentLength);
        }
        return null;
    }
}
