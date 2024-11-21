package by.lupach.httpclient.core;

import by.lupach.httpclient.core.HttpRequest;
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
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Отправка запроса
            System.out.println("Sending HTTP request:\n" + request.getRequestString());
            writer.write(request.getRequestString());
            writer.flush();

            // Чтение ответа
            System.out.println("Response:");
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }
    }
}