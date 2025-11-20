package com.messenger.common;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Класс, представляющий сообщение в чат-системе.
 * Содержит информацию о типе, отправителе, содержимом и временной метке сообщения.
 *
 * @author Messenger Team
 * @version 1.0.0
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private MessageType type;
    private String sender;
    private String content;
    private String lobby;
    private LocalDateTime timestamp;

    /**
     * Создает новое сообщение.
     * @param type тип сообщения
     * @param sender отправитель сообщения
     * @param content содержимое сообщения
     * @param lobby лобби, для которого предназначено сообщение
     */
    public Message(MessageType type, String sender, String content, String lobby) {
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.lobby = lobby;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Возвращает тип сообщения.
     * @return тип сообщения
     */
    public MessageType getType() { return type; }

    /**
     * Возвращает отправителя сообщения.
     * @return имя отправителя
     */
    public String getSender() { return sender; }

    /**
     * Возвращает содержимое сообщения.
     * @return текст сообщения
     */
    public String getContent() { return content; }

    /**
     * Возвращает лобби сообщения.
     * @return название лобби
     */
    public String getLobby() { return lobby; }

    /**
     * Возвращает временную метку сообщения.
     * @return время создания сообщения
     */
    public LocalDateTime getTimestamp() { return timestamp; }

    /**
     * Возвращает форматированное строковое представление сообщения.
     * @return отформатированная строка сообщения
     */
    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String time = timestamp.format(formatter);

        return String.format("[%s] [%s] %s: %s",
                time,
                lobby != null ? lobby : "General",
                sender != null ? sender : "System",
                content);
    }
}
