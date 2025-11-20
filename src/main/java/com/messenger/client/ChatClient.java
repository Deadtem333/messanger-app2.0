package com.messenger.client;

import com.messenger.common.*;
import java.io.*;
import java.net.Socket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Клиент для подключения к чат-серверу.
 * Обеспечивает отправку и получение сообщений, управление лобби и пользователями.
 *
 * @author Messenger Team
 * @version 1.0.0
 */
public class ChatClient {
    private static final Logger logger = LogManager.getLogger(ChatClient.class);

    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private MessageListener messageListener;
    private boolean connected;
    private String currentUser;

    /**
     * Интерфейс для обработки входящих сообщений и изменений статуса соединения.
     */
    public interface MessageListener {
        /**
         * Вызывается при получении нового сообщения от сервера.
         * @param message полученное сообщение
         */
        void onMessageReceived(Message message);

        /**
         * Вызывается при изменении статуса соединения с сервером.
         * @param connected true если соединение установлено, false если разорвано
         */
        void onConnectionStatusChanged(boolean connected);
    }

    /**
     * Подключается к чат-серверу.
     * @param host хост сервера
     * @param port порт сервера
     * @throws IOException если произошла ошибка при подключении
     */
    public void connect(String host, int port) throws IOException {
        logger.info("Attempting to connect to server {}:{}", host, port);
        socket = new Socket(host, port);
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        inputStream = new ObjectInputStream(socket.getInputStream());
        connected = true;
        logger.info("Successfully connected to server");

        if (messageListener != null) {
            messageListener.onConnectionStatusChanged(true);
        }

        startMessageListener();
    }

    /**
     * Отключается от сервера и освобождает ресурсы.
     */
    public void disconnect() {
        logger.info("Disconnecting from server");
        connected = false;
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null) socket.close();
            logger.info("Successfully disconnected from server");
        } catch (IOException e) {
            logger.error("Error disconnecting: {}", e.getMessage());
        }

        if (messageListener != null) {
            messageListener.onConnectionStatusChanged(false);
        }
    }

    /**
     * Запускает поток для прослушивания входящих сообщений от сервера.
     */
    private void startMessageListener() {
        logger.debug("Starting message listener thread");
        Thread listenerThread = new Thread(() -> {
            while (connected && !socket.isClosed()) {
                try {
                    Message message = (Message) inputStream.readObject();
                    logger.debug("Received message from server: {}", message.getType());
                    if (messageListener != null) {
                        messageListener.onMessageReceived(message);
                    }
                } catch (IOException | ClassNotFoundException e) {
                    if (connected) {
                        logger.error("Connection lost: {}", e.getMessage());
                        disconnect();
                    }
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /**
     * Отправляет сообщение на сервер.
     * @param message сообщение для отправки
     */
    public void sendMessage(Message message) {
        if (connected && outputStream != null) {
            try {
                outputStream.writeObject(message);
                outputStream.flush();
                logger.debug("Sent message to server: {}", message.getType());
            } catch (IOException e) {
                logger.error("Error sending message: {}", e.getMessage());
                disconnect();
            }
        } else {
            logger.warn("Attempted to send message while disconnected");
        }
    }

    /**
     * Регистрирует нового пользователя на сервере.
     * @param username имя пользователя
     * @param password пароль
     */
    public void register(String username, String password) {
        logger.info("Registering new user: {}", username);
        this.currentUser = username;
        Message registerMessage = new Message(MessageType.REGISTER,
                username, username + ":" + password, null);
        sendMessage(registerMessage);
    }

    /**
     * Выполняет вход пользователя в систему.
     * @param username имя пользователя
     * @param password пароль
     */
    public void login(String username, String password) {
        logger.info("Logging in user: {}", username);
        this.currentUser = username;
        Message loginMessage = new Message(MessageType.LOGIN,
                username, username + ":" + password, null);
        sendMessage(loginMessage);
    }

    /**
     * Отправляет текстовое сообщение в чат.
     * @param content содержимое сообщения
     * @param lobby название лобби
     * @param sender отправитель
     */
    public void sendChatMessage(String content, String lobby, String sender) {
        logger.debug("Sending chat message to lobby '{}'", lobby);
        Message chatMessage = new Message(MessageType.MESSAGE, sender, content, lobby);
        sendMessage(chatMessage);
    }

    /**
     * Присоединяется к указанному лобби.
     * @param lobbyName название лобби
     */
    public void joinLobby(String lobbyName) {
        logger.info("Joining lobby: {}", lobbyName);
        Message joinMessage = new Message(MessageType.JOIN_LOBBY,
                currentUser, lobbyName, null);
        sendMessage(joinMessage);
    }

    /**
     * Покидает указанное лобби.
     * @param lobbyName название лобби
     */
    public void leaveLobby(String lobbyName) {
        logger.info("Leaving lobby: {}", lobbyName);
        Message leaveMessage = new Message(MessageType.LEAVE_LOBBY,
                currentUser, lobbyName, null);
        sendMessage(leaveMessage);
    }

    /**
     * Запрашивает список пользователей у сервера.
     */
    public void requestUserList() {
        logger.debug("Requesting user list");
        Message userListRequest = new Message(MessageType.USER_LIST, currentUser, "", null);
        sendMessage(userListRequest);
    }

    /**
     * Запрашивает список лобби у сервера.
     */
    public void requestLobbyList() {
        logger.debug("Requesting lobby list");
        Message lobbyListRequest = new Message(MessageType.LOBBY_LIST, currentUser, "", null);
        sendMessage(lobbyListRequest);
    }

    /**
     * Создает новое лобби (только для администраторов).
     * @param lobbyName название лобби
     * @param adminOnly true если лобби только для администраторов
     */
    public void createLobby(String lobbyName, boolean adminOnly) {
        logger.info("Creating new lobby: {} (adminOnly: {})", lobbyName, adminOnly);
        String content = adminOnly ? lobbyName + ":admin" : lobbyName;
        Message createMessage = new Message(MessageType.CREATE_LOBBY, currentUser, content, null);
        sendMessage(createMessage);
    }

    /**
     * Удаляет лобби (только для администраторов).
     * @param lobbyName название лобби
     */
    public void deleteLobby(String lobbyName) {
        logger.info("Deleting lobby: {}", lobbyName);
        Message deleteMessage = new Message(MessageType.DELETE_LOBBY, currentUser, lobbyName, null);
        sendMessage(deleteMessage);
    }

    /**
     * Устанавливает обработчик входящих сообщений.
     * @param listener обработчик сообщений
     */
    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
        logger.debug("Message listener set");
    }

    /**
     * Проверяет статус соединения с сервером.
     * @return true если соединение установлено
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Возвращает текущего пользователя.
     * @return имя текущего пользователя
     */
    public String getCurrentUser() {
        return currentUser;
    }
}
