package by.lupach.httpserver.core;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class HttpServer {
    private static final Logger LOGGER = LoggerSetup.configureLogger(HttpServer.class.getName());
    private final int port;
    private final String rootDirectory;
    private final Map<String, String> defaultHeaders;


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
