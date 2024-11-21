package by.lupach.httpserver.core;

import org.apache.commons.cli.*;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.*;

public class HttpServer {
    private final int port;
    private final String rootDirectory;
    private final Map<String, String> defaultHeaders;
    private static final Logger LOGGER = LoggerSetup.configureLogger(HttpServer.class.getName());


    public HttpServer(int port, String rootDirectory, Map<String, String> defaultHeaders) {
        this.port = port;
        this.rootDirectory = rootDirectory;
        this.defaultHeaders = defaultHeaders;

        try {
            FileHandler fileHandler = new FileHandler("server.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
            LOGGER.setUseParentHandlers(false);
        } catch (IOException e) {
            System.err.println("Failed to initialize logger: " + e.getMessage());
        }
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("HTTP Server started on port " + port);
            LOGGER.info("Server started on port " + port + ", serving directory: " + rootDirectory);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new HttpRequestHandler(clientSocket, rootDirectory, defaultHeaders, LOGGER)).start();
            }
        } catch (IOException e) {
            LOGGER.severe("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
