package by.lupach.httpserver.app;

import by.lupach.httpserver.core.HttpServer;
import org.apache.commons.cli.*;

import java.util.Map;

public class Main {
    public static void main(String[] args) {
        Options options = new Options();

        options.addOption("p", "port", true, "Port to run the server on (default: 8080)");
        options.addOption("d", "directory", true, "Root directory to serve files from (default: ./www)");
        options.addOption("h", "help", false, "Show help");

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        int port = 8080;
        String rootDirectory = "server/src/main/resources/www";
        Map<String, String> defaultHeaders = Map.of(
                "Access-Control-Allow-Origin", "*",
                "Access-Control-Allow-Methods", "GET, POST, OPTIONS"
        );

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {
                formatter.printHelp("HttpServer", options);
                return;
            }

            if (cmd.hasOption("p")) {
                port = Integer.parseInt(cmd.getOptionValue("p"));
            }

            if (cmd.hasOption("d")) {
                rootDirectory = cmd.getOptionValue("d");
            }

        } catch (ParseException e) {
            System.err.println("Failed to parse command-line arguments: " + e.getMessage());
            formatter.printHelp("HttpServer", options);
            return;
        }

        HttpServer server = new HttpServer(port, rootDirectory, defaultHeaders);
        server.start();
    }

}
