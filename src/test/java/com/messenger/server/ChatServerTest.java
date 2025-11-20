package com.messenger.server;

import com.messenger.common.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Тестовый класс для проверки функциональности ChatServer.
 *
 * @author Messenger Team
 * @version 1.0.0
 */
public class ChatServerTest {
    private static final Logger logger = LogManager.getLogger(ChatServerTest.class);

    private ChatServer chatServer;

    /**
     * Подготавливает тестовое окружение перед каждым тестом.
     */
    @BeforeEach
    void setUp() {
        logger.info("Setting up test environment");
        chatServer = new ChatServer(8080);
    }

    /**
     * Тестирует регистрацию нового пользователя.
     * Проверяет успешную регистрацию и предотвращение дублирования.
     */
    @Test
    void testRegisterUser() {
        logger.info("Running testRegisterUser");
        User user = new User("newuser", "password");
        assertTrue(chatServer.registerUser(user));
        logger.info("testRegisterUser completed successfully");
    }

    /**
     * Тестирует попытку регистрации дублирующегося пользователя.
     * Проверяет, что система предотвращает создание пользователей с одинаковыми именами.
     */
    @Test
    void testRegisterDuplicateUser() {
        logger.info("Running testRegisterDuplicateUser");
        User user1 = new User("user", "pass1");
        User user2 = new User("user", "pass2");

        assertTrue(chatServer.registerUser(user1));
        assertFalse(chatServer.registerUser(user2));
        logger.info("testRegisterDuplicateUser completed successfully");
    }

    /**
     * Тестирует аутентификацию пользователя.
     * Проверяет успешный вход с правильными учетными данными.
     */
    @Test
    void testAuthenticateUser() {
        logger.info("Running testAuthenticateUser");
        User user = new User("authuser", "authpass");
        chatServer.registerUser(user);

        User authenticated = chatServer.authenticateUser("authuser", "authpass");
        assertNotNull(authenticated);
        assertEquals("authuser", authenticated.getUsername());
        logger.info("testAuthenticateUser completed successfully");
    }

    /**
     * Тестирует аутентификацию с неверными учетными данными.
     * Проверяет, что система отклоняет несуществующих пользователей.
     */
    @Test
    void testAuthenticateInvalidUser() {
        logger.info("Running testAuthenticateInvalidUser");
        assertNull(chatServer.authenticateUser("nonexistent", "pass"));
        logger.info("testAuthenticateInvalidUser completed successfully");
    }
}
