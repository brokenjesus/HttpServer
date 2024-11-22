package by.lupach.httpclient.core;

import org.apache.commons.cli.CommandLine;

import java.io.*;
import java.net.Socket;

public class HttpClient {

    public String getRequiredOption(CommandLine cmd, String option, String errorMessage) {
        String value = cmd.getOptionValue(option);
        if (value == null) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value;
    }

    public HttpRequest buildRequest(String url, String method, String headers, String body, String templatePath) throws IOException {
        if (templatePath != null) {
            return HttpRequest.fromTemplate(templatePath);
        } else {
            return HttpRequest.fromParameters(url, method, headers, body);
        }
    }

    public void sendRequest(HttpRequest request) throws IOException {
        try (Socket socket = new Socket(request.getHost(), request.getPort());
             OutputStream outputStream = socket.getOutputStream();
             InputStream inputStream = socket.getInputStream()) {

            // Send request bytes
            System.out.println("Sending HTTP request:\n" + new String(request.getRequestBytes()));
            outputStream.write(request.getRequestBytes());

            // If body is present, send it as bytes
            if (request.getBodyBytes() != null) {
                outputStream.write(request.getBodyBytes());
            }

            // Read response as byte array
            byte[] responseBuffer = new byte[1024];
            int bytesRead;
            System.out.println("Response:");
            while ((bytesRead = inputStream.read(responseBuffer)) != -1) {
                System.out.write(responseBuffer, 0, bytesRead);
            }
        }
    }
}
