package com.messenger.server;

import com.messenger.common.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Тестовый класс для проверки функциональности LobbyManager.
 *
 * @author Messenger Team
 * @version 1.0.0
 */
public class LobbyManagerTest {
    private static final Logger logger = LogManager.getLogger(LobbyManagerTest.class);

    private LobbyManager lobbyManager;
    private User testUser;

    /**
     * Подготавливает тестовое окружение перед каждым тестом.
     */
    @BeforeEach
    void setUp() {
        logger.info("Setting up LobbyManagerTest");
        lobbyManager = new LobbyManager();
        testUser = new User("testuser", "password");
    }

    /**
     * Тестирует успешное присоединение к разрешенному лобби.
     */
    @Test
    void testJoinAllowedLobby() {
        logger.info("Running testJoinAllowedLobby");
        boolean result = lobbyManager.joinLobby(testUser, "Games");
        assertTrue(result);
        assertEquals("Games", testUser.getCurrentLobby());
        logger.info("testJoinAllowedLobby completed successfully");
    }

    /**
     * Тестирует попытку присоединения к несуществующему лобби.
     */
    @Test
    void testJoinNotAllowedLobby() {
        logger.info("Running testJoinNotAllowedLobby");
        boolean result = lobbyManager.joinLobby(testUser, "InvalidLobby");
        assertFalse(result);
        logger.info("testJoinNotAllowedLobby completed successfully");
    }

    /**
     * Тестирует получение списка пользователей в лобби.
     */
    @Test
    void testGetLobbyUsers() {
        logger.info("Running testGetLobbyUsers");
        lobbyManager.joinLobby(testUser, "General");
        assertEquals(1, lobbyManager.getLobbyUsers("General").size());
        logger.info("testGetLobbyUsers completed successfully");
    }

    /**
     * Тестирует получение списка разрешенных лобби.
     */
    @Test
    void testGetAllowedLobbies() {
        logger.info("Running testGetAllowedLobbies");
        assertTrue(lobbyManager.getAllowedLobbies().contains("General"));
        assertTrue(lobbyManager.getAllowedLobbies().contains("Games"));
        logger.info("testGetAllowedLobbies completed successfully");
    }
}
