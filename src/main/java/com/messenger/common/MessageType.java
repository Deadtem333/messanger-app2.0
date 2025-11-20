package com.messenger.common;

public enum MessageType {
    REGISTER,       // Регистрация нового пользователя
    LOGIN,          // Вход в систему
    MESSAGE,        // Текстовое сообщение в чат
    JOIN_LOBBY,     // Присоединение к лобби
    LEAVE_LOBBY,    // Выход из лобби
    USER_LIST,      // Запрос списка пользователей
    LOBBY_LIST,     // Запрос списка лобби
    CREATE_LOBBY,   // Создание нового лобби (админ)
    DELETE_LOBBY,   // Удаление лобби (админ)
    ERROR,          // Сообщение об ошибке
    SUCCESS         // Успешное выполнение
}