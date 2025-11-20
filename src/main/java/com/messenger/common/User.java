package com.messenger.common;

import java.io.Serializable;

/**
 * Класс, представляющий пользователя чат-системы.
 * Содержит информацию об учетных данных и текущем состоянии пользователя.
 *
 * @author Messenger Team
 * @version 1.0.0
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String password;
    private String currentLobby;
    private boolean isAdmin;

    /**
     * Создает нового обычного пользователя.
     * @param username имя пользователя
     * @param password пароль
     */
    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.currentLobby = "General";
        this.isAdmin = false;
    }

    /**
     * Создает нового пользователя с указанными правами администратора.
     * @param username имя пользователя
     * @param password пароль
     * @param isAdmin true если пользователь является администратором
     */
    public User(String username, String password, boolean isAdmin) {
        this.username = username;
        this.password = password;
        this.currentLobby = "General";
        this.isAdmin = isAdmin;
    }

    /**
     * Возвращает имя пользователя.
     * @return имя пользователя
     */
    public String getUsername() { return username; }

    /**
     * Возвращает пароль пользователя.
     * @return пароль
     */
    public String getPassword() { return password; }

    /**
     * Возвращает текущее лобби пользователя.
     * @return название текущего лобби
     */
    public String getCurrentLobby() { return currentLobby; }

    /**
     * Устанавливает текущее лобби пользователя.
     * @param lobby название лобби
     */
    public void setCurrentLobby(String lobby) { this.currentLobby = lobby; }

    /**
     * Проверяет, является ли пользователь администратором.
     * @return true если пользователь администратор
     */
    public boolean isAdmin() { return isAdmin; }

    /**
     * Сравнивает пользователей по имени пользователя.
     * @param obj объект для сравнения
     * @return true если имена пользователей совпадают
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return username.equals(user.username);
    }

    /**
     * Возвращает хэш-код на основе имени пользователя.
     * @return хэш-код
     */
    @Override
    public int hashCode() {
        return username.hashCode();
    }

    /**
     * Возвращает строковое представление пользователя.
     * @return имя пользователя
     */
    @Override
    public String toString() {
        return username + (isAdmin ? " (Admin)" : "");
    }
}
