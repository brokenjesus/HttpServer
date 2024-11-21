package by.lupach.core;

import java.io.IOException;
import java.util.logging.*;

public class LoggerSetup {
    public static Logger configureLogger(String loggerName) {
        Logger logger = Logger.getLogger(loggerName);

        // Удаляем дефолтные обработчики
        logger.setUseParentHandlers(false);

        // Форматтер для логов
        SimpleFormatter formatter = new SimpleFormatter();

        // Консольный обработчик
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(formatter);
        logger.addHandler(consoleHandler);

        // Файловый обработчик
        try {
            FileHandler fileHandler = new FileHandler("server.log", true);
            fileHandler.setFormatter(formatter);
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            logger.severe("Failed to initialize file handler: " + e.getMessage());
        }

        // Устанавливаем уровень логирования
        logger.setLevel(Level.INFO);

        return logger;
    }
}
