package com.messenger;

import com.messenger.client.ClientGUI;
import com.messenger.server.ChatServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Главный класс приложения Messenger.
 * Обеспечивает запуск как серверной, так и клиентской части приложения.
 *
 * @author Messenger Team
 * @version 1.0.0
 */
public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);

    /**
     * Точка входа в приложение.
     * Определяет режим запуска (сервер или клиент) на основе аргументов командной строки.
     *
     * @param args аргументы командной строки:
     *             - без аргументов: запуск клиента
     *             - "server [port]": запуск сервера на указанном порту (по умолчанию 8080)
     */
    public static void main(String[] args) {
        logger.info("Messenger application starting with args: {}", (Object) args);

        if (args.length > 0 && "server".equals(args[0])) {
            // Запуск сервера
            int port = 8080;
            if (args.length > 1) {
                try {
                    port = Integer.parseInt(args[1]);
                    logger.info("Starting server on custom port: {}", port);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid port number '{}', using default: 8080", args[1]);
                }
            } else {
                logger.info("Starting server on default port: {}", port);
            }

            ChatServer server = new ChatServer(port);
            server.start();
        } else {
            // Запуск GUI клиента
            if (args.length > 0) {
                logger.info("Starting client with arguments: {}", (Object) args);
            } else {
                logger.info("Starting client without arguments");
            }
            ClientGUI.main(args);
        }

        logger.info("Messenger application shutdown");
    }
}
