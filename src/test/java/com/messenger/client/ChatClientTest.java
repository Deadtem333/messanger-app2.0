package com.messenger.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Тестовый класс для проверки функциональности ChatClient.
 *
 * @author Messenger Team
 * @version 1.0.0
 */
public class ChatClientTest {
    private static final Logger logger = LogManager.getLogger(ChatClientTest.class);

    /**
     * Тестирует начальное состояние клиента.
     * Проверяет, что клиент изначально не подключен к серверу.
     */
    @Test
    void testClientInitialState() {
        logger.info("Running testClientInitialState");
        ChatClient client = new ChatClient();
        assertFalse(client.isConnected());
        logger.info("testClientInitialState completed successfully");
    }
}
