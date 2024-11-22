package by.lupach.httpclient.app;

import by.lupach.httpclient.core.HttpClient;
import by.lupach.httpclient.core.HttpRequest;
import org.apache.commons.cli.*;

import java.io.IOException;

public class Main {
    private static CommandLine parseArguments(String[] args) {
        Options options = defineOptions();
        CommandLineParser parser = new DefaultParser();
        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Failed to parse command-line arguments: " + e.getMessage());
            printHelp();
            return null;
        }
    }

    private static void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("HttpClient", defineOptions());
    }

    private static Options defineOptions() {
        Options options = new Options();
        options.addOption("h", "help", false, "Show help");
        options.addOption("u", "url", true, "URL to send the request to (e.g., http://localhost:8080)");
        options.addOption("m", "method", true, "HTTP method (e.g., GET, POST)");
        options.addOption("H", "header", true, "HTTP headers (e.g., 'Key: Value')");
        options.addOption("b", "body", true, "Request body (string)");
        options.addOption("t", "template", true, "Path to a request template file");
        options.addOption("f", "file", true, "Path to image file for upload");
        return options;
    }

    public static void main(String[] args) {
        HttpClient client = new HttpClient();

        CommandLine cmd = parseArguments(args);
        if (cmd == null || cmd.hasOption("h")) {
            printHelp();
            return;
        }

        try {
            String url = null;
            if (cmd.hasOption("f")) {
                // Image upload
                String imagePath = cmd.getOptionValue("f");
                url = client.getRequiredOption(cmd, "url", "URL is required");
                String method = cmd.getOptionValue("method", "POST");
                HttpRequest request = HttpRequest.fileUpload(url, imagePath, method);
                client.sendRequest(request);
            } else {
                url = client.getRequiredOption(cmd, "url", "URL is required");
                String method = cmd.getOptionValue("method", "GET");
                String headers = cmd.getOptionValue("header");
                String body = cmd.getOptionValue("body");
                String templatePath = cmd.getOptionValue("template");

                HttpRequest request = client.buildRequest(url, method, headers, body, templatePath);
                client.sendRequest(request);
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("I/O Error: " + e.getMessage());
        }
    }
}
