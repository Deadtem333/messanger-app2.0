package com.messenger.server;

import com.messenger.common.User;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Менеджер лобби для управления комнатами чата.
 * Обеспечивает создание, удаление лобби и управление пользователями в них.
 *
 * @author Messenger Team
 * @version 1.0.0
 */
public class LobbyManager {
    private static final Logger logger = LogManager.getLogger(LobbyManager.class);

    private final Map<String, Set<User>> lobbies;
    private final Set<String> allowedLobbies;
    private final Set<String> adminOnlyLobbies;

    /**
     * Создает новый менеджер лобби с настройками по умолчанию.
     */
    public LobbyManager() {
        this.lobbies = new ConcurrentHashMap<>();
        this.allowedLobbies = ConcurrentHashMap.newKeySet();
        this.adminOnlyLobbies = ConcurrentHashMap.newKeySet();
        initializeDefaultLobbies();
        logger.info("LobbyManager initialized with default lobbies");
    }

    /**
     * Инициализирует лобби по умолчанию.
     */
    private void initializeDefaultLobbies() {
        // Общедоступные лобби
        addLobby("General", false);
        addLobby("Games", false);
        addLobby("Movies", false);
        addLobby("Music", false);

        // Админские лобби
        addLobby("Admin", true);
        addLobby("Moderation", true);

        logger.debug("Default lobbies initialized");
    }

    /**
     * Добавляет новое лобби в систему.
     * @param lobbyName название лобби
     * @param adminOnly true если лобби только для администраторов
     * @return true если лобби успешно добавлено, false если уже существует
     */
    public boolean addLobby(String lobbyName, boolean adminOnly) {
        if (allowedLobbies.contains(lobbyName)) {
            logger.warn("Attempt to add existing lobby: {}", lobbyName);
            return false; // Лобби уже существует
        }

        allowedLobbies.add(lobbyName);
        lobbies.put(lobbyName, ConcurrentHashMap.newKeySet());

        if (adminOnly) {
            adminOnlyLobbies.add(lobbyName);
        }

        logger.info("Lobby added: {} ({})", lobbyName,
                adminOnly ? "Admin only" : "Public");
        return true;
    }

    /**
     * Удаляет лобби из системы.
     * @param lobbyName название лобби для удаления
     * @return true если лобби успешно удалено, false если удаление невозможно
     */
    public boolean removeLobby(String lobbyName) {
        if ("General".equals(lobbyName)) {
            logger.warn("Attempt to delete General lobby blocked");
            return false; // Нельзя удалить General
        }

        // Перемещаем всех пользователей в General
        Set<User> users = lobbies.get(lobbyName);
        if (users != null) {
            for (User user : users) {
                user.setCurrentLobby("General");
                lobbies.get("General").add(user);
            }
            logger.info("Moved {} users from {} to General", users.size(), lobbyName);
        }

        allowedLobbies.remove(lobbyName);
        adminOnlyLobbies.remove(lobbyName);
        lobbies.remove(lobbyName);

        logger.info("Lobby removed: {}", lobbyName);
        return true;
    }

    /**
     * Добавляет пользователя в указанное лобби.
     * @param user пользователь для добавления
     * @param lobbyName название лобби
     * @return true если пользователь успешно добавлен, false в противном случае
     */
    public boolean joinLobby(User user, String lobbyName) {
        if (!isLobbyAllowed(lobbyName)) {
            logger.warn("User {} attempted to join non-existent lobby: {}",
                    user.getUsername(), lobbyName);
            return false; // Лобби не существует
        }

        if (isAdminOnlyLobby(lobbyName) && !user.isAdmin()) {
            logger.warn("Non-admin user {} attempted to join admin-only lobby: {}",
                    user.getUsername(), lobbyName);
            return false; // Пользователь не админ
        }

        // Выходим из текущего лобби
        String currentLobby = user.getCurrentLobby();
        if (currentLobby != null) {
            leaveLobby(user, currentLobby);
        }

        // Входим в новое лобби
        user.setCurrentLobby(lobbyName);
        lobbies.computeIfAbsent(lobbyName, k -> ConcurrentHashMap.newKeySet()).add(user);

        logger.debug("User {} joined lobby: {}", user.getUsername(), lobbyName);
        return true;
    }

    /**
     * Удаляет пользователя из указанного лобби.
     * @param user пользователь для удаления
     * @param lobbyName название лобби
     */
    public void leaveLobby(User user, String lobbyName) {
        Set<User> lobbyUsers = lobbies.get(lobbyName);
        if (lobbyUsers != null) {
            lobbyUsers.remove(user);
            logger.debug("User {} left lobby: {}", user.getUsername(), lobbyName);
        }
    }

    /**
     * Возвращает множество пользователей в указанном лобби.
     * @param lobbyName название лобби
     * @return неизменяемое множество пользователей
     */
    public Set<User> getLobbyUsers(String lobbyName) {
        Set<User> users = lobbies.getOrDefault(lobbyName, Collections.emptySet());
        logger.trace("Retrieved {} users from lobby: {}", users.size(), lobbyName);
        return Collections.unmodifiableSet(users);
    }

    /**
     * Возвращает множество всех разрешенных лобби.
     * @return неизменяемое множество названий лобби
     */
    public Set<String> getAllowedLobbies() {
        return Collections.unmodifiableSet(allowedLobbies);
    }

    /**
     * Возвращает множество общедоступных лобби.
     * @return неизменяемое множество названий публичных лобби
     */
    public Set<String> getPublicLobbies() {
        Set<String> publicLobbies = new HashSet<>(allowedLobbies);
        publicLobbies.removeAll(adminOnlyLobbies);
        logger.trace("Retrieved {} public lobbies", publicLobbies.size());
        return Collections.unmodifiableSet(publicLobbies);
    }

    /**
     * Проверяет, существует ли указанное лобби.
     * @param lobbyName название лобби
     * @return true если лобби существует
     */
    public boolean isLobbyAllowed(String lobbyName) {
        return allowedLobbies.contains(lobbyName);
    }

    /**
     * Проверяет, является ли лобби доступным только для администраторов.
     * @param lobbyName название лобби
     * @return true если лобби только для администраторов
     */
    public boolean isAdminOnlyLobby(String lobbyName) {
        return adminOnlyLobbies.contains(lobbyName);
    }

    /**
     * Возвращает количество лобби в системе.
     * @return количество лобби
     */
    public int getLobbyCount() {
        return allowedLobbies.size();
    }
}
