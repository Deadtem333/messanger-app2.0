package com.messenger.server;

import com.messenger.common.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Основной класс сервера чат-мессенджера.
 * Обрабатывает подключения клиентов, управляет пользователями и лобби.
 *
 * @author Messenger Team
 * @version 1.0.0
 */
public class ChatServer {
    private static final Logger logger = LogManager.getLogger(ChatServer.class);

    private final int port;
    private final Set<ClientHandler> clients;
    private final Map<String, User> registeredUsers;
    private final LobbyManager lobbyManager;
    private ServerSocket serverSocket;
    private boolean running;

    /**
     * Создает новый экземпляр сервера.
     * @param port порт для прослушивания подключений
     */
    public ChatServer(int port) {
        this.port = port;
        this.clients = new CopyOnWriteArraySet<>();
        this.registeredUsers = new ConcurrentHashMap<>();
        this.lobbyManager = new LobbyManager();
        this.running = false;

        initializeAdminUser();
    }

    /**
     * Инициализирует административного пользователя по умолчанию.
     */
    private void initializeAdminUser() {
        User admin = new User("admin", "admin123", true);
        registeredUsers.put(admin.getUsername(), admin);
        logger.info("Admin user created: admin/admin123");
    }

    /**
     * Запускает сервер и начинает прослушивать подключения.
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            logger.info("Chat server started on port {}", port);
            logger.info("Available lobbies: {}", lobbyManager.getAllowedLobbies());

            while (running) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, this, lobbyManager);
                clients.add(clientHandler);
                new Thread(clientHandler).start();

                logger.info("New client connected. Total clients: {}", clients.size());
            }
        } catch (IOException e) {
            if (running) {
                logger.error("Server error: {}", e.getMessage());
            }
        }
    }

    /**
     * Останавливает сервер и освобождает ресурсы.
     */
    public void stop() {
        logger.info("Stopping chat server");
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            logger.info("Chat server stopped successfully");
        } catch (IOException e) {
            logger.error("Error stopping server: {}", e.getMessage());
        }
    }

    /**
     * Регистрирует нового пользователя в системе.
     * @param user объект пользователя для регистрации
     * @return true если регистрация успешна, false если пользователь уже существует
     */
    public boolean registerUser(User user) {
        if (registeredUsers.containsKey(user.getUsername())) {
            logger.warn("Registration failed - user already exists: {}", user.getUsername());
            return false; // Пользователь уже существует
        }
        registeredUsers.put(user.getUsername(), user);
        logger.info("New user registered: {}", user.getUsername());
        return true;
    }

    /**
     * Аутентифицирует пользователя по логину и паролю.
     * @param username имя пользователя
     * @param password пароль
     * @return объект пользователя если аутентификация успешна, null в противном случае
     */
    public User authenticateUser(String username, String password) {
        User user = registeredUsers.get(username);
        if (user != null && user.getPassword().equals(password)) {
            logger.info("User authenticated: {}", username);
            return user;
        }
        logger.warn("Authentication failed for user: {}", username);
        return null;
    }

    /**
     * Рассылает сообщение всем пользователям в указанном лобби.
     * @param message сообщение для рассылки
     * @param lobbyName название лобби
     */
    public void broadcastMessage(Message message, String lobbyName) {
        int sentCount = 0;
        for (ClientHandler client : clients) {
            User user = client.getCurrentUser();
            if (user != null && lobbyName.equals(user.getCurrentLobby())) {
                client.sendMessage(message);
                sentCount++;
            }
        }
        logger.debug("Message broadcast to {} users in lobby: {}", sentCount, lobbyName);
    }

    /**
     * Рассылает обновленный список пользователей всем клиентам в указанном лобби.
     * @param lobbyName название лобби
     */
    public void broadcastUserList(String lobbyName) {
        Set<User> users = lobbyManager.getLobbyUsers(lobbyName);
        StringBuilder userList = new StringBuilder();

        for (User user : users) {
            userList.append(user.getUsername());
            if (user.isAdmin()) {
                userList.append(" (Admin)");
            }
            userList.append(",");
        }

        Message userListMessage = new Message(MessageType.USER_LIST,
                "Server", userList.toString(), lobbyName);

        broadcastMessage(userListMessage, lobbyName);
        logger.debug("User list broadcast for lobby: {}", lobbyName);
    }

    /**
     * Удаляет клиентский обработчик из списка активных клиентов.
     * @param client обработчик клиента для удаления
     */
    public void removeClient(ClientHandler client) {
        clients.remove(client);
        logger.info("Client disconnected. Total clients: {}", clients.size());
    }

    /**
     * Создает новое лобби (только для администраторов).
     * @param lobbyName название лобби
     * @param adminOnly true если лобби только для администраторов
     * @param requester пользователь, запросивший создание
     * @return true если создание успешно, false в противном случае
     */
    public boolean createLobby(String lobbyName, boolean adminOnly, User requester) {
        if (!requester.isAdmin()) {
            logger.warn("Non-admin user attempted to create lobby: {}", requester.getUsername());
            return false; // Только админы могут создавать лобби
        }

        boolean success = lobbyManager.addLobby(lobbyName, adminOnly);
        if (success) {
            logger.info("New lobby created: {} ({})", lobbyName,
                    adminOnly ? "Admin only" : "Public");
            // Автоматически обновляем список лобби для всех клиентов
            broadcastLobbyListToAll();
        } else {
            logger.warn("Failed to create lobby: {} (already exists)", lobbyName);
        }
        return success;
    }

    /**
     * Удаляет лобби (только для администраторов).
     * @param lobbyName название лобби
     * @param requester пользователь, запросивший удаление
     * @return true если удаление успешно, false в противном случае
     */
    public boolean deleteLobby(String lobbyName, User requester) {
        if (!requester.isAdmin()) {
            logger.warn("Non-admin user attempted to delete lobby: {}", requester.getUsername());
            return false; // Только админы могут удалять лобби
        }

        boolean success = lobbyManager.removeLobby(lobbyName);
        if (success) {
            logger.info("Lobby deleted: {}", lobbyName);
            // Автоматически обновляем список лобби для всех клиентов
            broadcastLobbyListToAll();
        } else {
            logger.warn("Failed to delete lobby: {}", lobbyName);
        }
        return success;
    }

    /**
     * Рассылает обновленный список лобби всем подключенным клиентам.
     */
    private void broadcastLobbyListToAll() {
        Set<String> lobbies = lobbyManager.getAllowedLobbies();
        StringBuilder lobbyList = new StringBuilder();

        for (String lobby : lobbies) {
            lobbyList.append(lobby);
            if (lobbyManager.isAdminOnlyLobby(lobby)) {
                lobbyList.append(" (Admin)");
            }
            lobbyList.append(",");
        }

        Message lobbyListMessage = new Message(MessageType.LOBBY_LIST,
                "Server", lobbyList.toString(), null);

        broadcastToAllClients(lobbyListMessage);
        logger.debug("Lobby list updated and broadcast to all clients");
    }

    /**
     * Рассылает сообщение всем подключенным клиентам.
     * @param message сообщение для рассылки
     */
    public void broadcastToAllClients(Message message) {
        int sentCount = 0;
        for (ClientHandler client : clients) {
            if (client.getCurrentUser() != null) { // Отправляем только аутентифицированным клиентам
                client.sendMessage(message);
                sentCount++;
            }
        }
        logger.debug("Message broadcast to {} clients", sentCount);
    }

    /**
     * Точка входа для запуска сервера.
     * @param args аргументы командной строки [port]
     */
    public static void main(String[] args) {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                logger.warn("Invalid port number '{}', using default: 8080", args[0]);
            }
        }

        ChatServer server = new ChatServer(port);
        server.start();
    }
}
