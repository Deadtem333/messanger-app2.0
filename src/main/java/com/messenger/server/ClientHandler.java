package com.messenger.server;

import com.messenger.common.*;
import java.io.*;
import java.net.Socket;
import java.util.Set;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final ChatServer server;
    private final LobbyManager lobbyManager;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private User currentUser;
    private boolean running;

    public ClientHandler(Socket socket, ChatServer server, LobbyManager lobbyManager) {
        this.clientSocket = socket;
        this.server = server;
        this.lobbyManager = lobbyManager;
        this.running = true;
    }

    @Override
    public void run() {
        try {
            initializeStreams();
            handleClientCommunication();
        } catch (IOException e) {
            System.err.println("Client connection error: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void initializeStreams() throws IOException {
        outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
        inputStream = new ObjectInputStream(clientSocket.getInputStream());
    }

    private void handleClientCommunication() {
        while (running && !clientSocket.isClosed()) {
            try {
                Message message = (Message) inputStream.readObject();
                processMessage(message);
            } catch (IOException | ClassNotFoundException e) {
                running = false;
            }
        }
    }

    private void processMessage(Message message) {
        switch (message.getType()) {
            case REGISTER:
                handleRegistration(message);
                break;
            case LOGIN:
                handleLogin(message);
                break;
            case MESSAGE:
                handleChatMessage(message);
                break;
            case JOIN_LOBBY:
                handleJoinLobby(message);
                break;
            case LEAVE_LOBBY:
                handleLeaveLobby(message);
                break;
            case USER_LIST:
                sendUserList();
                break;
            case LOBBY_LIST:
                sendLobbyList();
                break;
            case CREATE_LOBBY:
                handleCreateLobby(message);
                break;
            case DELETE_LOBBY:
                handleDeleteLobby(message);
                break;
        }
    }

    private void handleRegistration(Message message) {
        try {
            String[] credentials = message.getContent().split(":");
            if (credentials.length == 2) {
                String username = credentials[0];
                String password = credentials[1];

                User newUser = new User(username, password);
                boolean success = server.registerUser(newUser);

                Message response;
                if (success) {

                    currentUser = newUser;
                    lobbyManager.joinLobby(newUser, "General");

                    response = new Message(MessageType.SUCCESS, "Server",
                            "Registration and login successful", null);


                    broadcastUserListToAllLobbies();


                    Message welcomeMsg = new Message(MessageType.MESSAGE,
                            "Server",
                            "User " + currentUser.getUsername() + " joined the chat",
                            "General");
                    server.broadcastMessage(welcomeMsg, "General");
                } else {
                    response = new Message(MessageType.ERROR, "Server",
                            "Username already exists", null);
                }

                sendMessage(response);
            }
        } catch (Exception e) {
            sendErrorMessage("Invalid registration format. Use: username:password");
        }
    }

    private void handleLogin(Message message) {
        try {
            String[] credentials = message.getContent().split(":");
            if (credentials.length == 2) {
                String username = credentials[0];
                String password = credentials[1];

                User user = server.authenticateUser(username, password);

                if (user != null) {
                    currentUser = user;
                    lobbyManager.joinLobby(user, "General");

                    Message response = new Message(MessageType.SUCCESS,
                            "Server",
                            "Login successful" + (user.isAdmin() ? " (Admin)" : ""),
                            null);
                    sendMessage(response);

                    broadcastUserListToAllLobbies();

                    Message welcomeMsg = new Message(MessageType.MESSAGE,
                            "Server",
                            "User " + currentUser.getUsername() + " joined the chat",
                            "General");
                    server.broadcastMessage(welcomeMsg, "General");
                } else {
                    sendErrorMessage("Invalid credentials");
                }
            }
        } catch (Exception e) {
            sendErrorMessage("Invalid login format. Use: username:password");
        }
    }

    private void handleChatMessage(Message message) {
        if (currentUser != null && message.getLobby() != null) {

            if (message.getLobby().equals(currentUser.getCurrentLobby())) {

                Message broadcastMessage = new Message(
                        MessageType.MESSAGE,
                        currentUser.getUsername(),
                        message.getContent(),
                        message.getLobby()
                );
                server.broadcastMessage(broadcastMessage, message.getLobby());
            } else {
                sendErrorMessage("You are not in this lobby");
            }
        }
    }

    private void handleJoinLobby(Message message) {
        if (currentUser != null) {
            String previousLobby = currentUser.getCurrentLobby();
            String newLobbyName = message.getContent();
            boolean success = lobbyManager.joinLobby(currentUser, newLobbyName);

            Message response = success ?
                    new Message(MessageType.SUCCESS, "Server",
                            "Joined lobby: " + newLobbyName, null) :
                    new Message(MessageType.ERROR, "Server",
                            getJoinLobbyErrorMessage(newLobbyName), null);

            sendMessage(response);

            if (success) {

                broadcastUserList(previousLobby);
                broadcastUserList(newLobbyName);


                Message welcomeMsg = new Message(MessageType.MESSAGE,
                        "Server",
                        currentUser.getUsername() + " joined the lobby",
                        newLobbyName);
                server.broadcastMessage(welcomeMsg, newLobbyName);


                if (!"General".equals(previousLobby)) {
                    Message leaveMsg = new Message(MessageType.MESSAGE,
                            "Server",
                            currentUser.getUsername() + " left the lobby",
                            previousLobby);
                    server.broadcastMessage(leaveMsg, previousLobby);
                }
            }
        }
    }

    private String getJoinLobbyErrorMessage(String lobbyName) {
        if (!lobbyManager.isLobbyAllowed(lobbyName)) {
            return "Lobby does not exist: " + lobbyName;
        } else if (lobbyManager.isAdminOnlyLobby(lobbyName)) {
            return "Admin access required for lobby: " + lobbyName;
        } else {
            return "Cannot join lobby: " + lobbyName;
        }
    }

    private void handleLeaveLobby(Message message) {
        if (currentUser != null && message.getContent() != null) {
            String lobbyName = message.getContent();
            String previousLobby = currentUser.getCurrentLobby();

            lobbyManager.leaveLobby(currentUser, lobbyName);

            if (lobbyName.equals(currentUser.getCurrentLobby())) {
                lobbyManager.joinLobby(currentUser, "General");

                broadcastUserList(previousLobby);
                broadcastUserList("General");


                Message generalMsg = new Message(MessageType.MESSAGE,
                        "Server",
                        currentUser.getUsername() + " moved to General lobby",
                        "General");
                server.broadcastMessage(generalMsg, "General");
            }
        }
    }

    private void handleCreateLobby(Message message) {
        if (currentUser != null && currentUser.isAdmin()) {
            String[] parts = message.getContent().split(":");
            String lobbyName = parts[0];
            boolean adminOnly = parts.length > 1 && "admin".equals(parts[1]);

            boolean success = server.createLobby(lobbyName, adminOnly, currentUser);

            Message response = success ?
                    new Message(MessageType.SUCCESS, "Server",
                            "Lobby created: " + lobbyName + (adminOnly ? " (Admin only)" : ""), null) :
                    new Message(MessageType.ERROR, "Server",
                            "Failed to create lobby: " + lobbyName, null);

            sendMessage(response);

            if (success) {

                broadcastLobbyListToAll();
            }
        } else {
            sendErrorMessage("Admin privileges required to create lobbies");
        }
    }

    private void handleDeleteLobby(Message message) {
        if (currentUser != null && currentUser.isAdmin()) {
            String lobbyName = message.getContent();
            boolean success = server.deleteLobby(lobbyName, currentUser);

            Message response = success ?
                    new Message(MessageType.SUCCESS, "Server",
                            "Lobby deleted: " + lobbyName, null) :
                    new Message(MessageType.ERROR, "Server",
                            "Failed to delete lobby: " + lobbyName, null);

            sendMessage(response);

            if (success) {

                broadcastLobbyListToAll();
            }
        } else {
            sendErrorMessage("Admin privileges required to delete lobbies");
        }
    }

    private void sendUserList() {
        if (currentUser != null) {
            Set<User> users = lobbyManager.getLobbyUsers(currentUser.getCurrentLobby());
            StringBuilder userList = new StringBuilder();

            for (User user : users) {
                userList.append(user.getUsername());
                if (user.isAdmin()) {
                    userList.append(" (Admin)");
                }
                userList.append(",");
            }

            Message response = new Message(MessageType.USER_LIST,
                    "Server", userList.toString(), currentUser.getCurrentLobby());
            sendMessage(response);
        }
    }

    private void sendLobbyList() {
        Set<String> lobbies = lobbyManager.getAllowedLobbies();
        StringBuilder lobbyList = new StringBuilder();

        for (String lobby : lobbies) {
            lobbyList.append(lobby);
            if (lobbyManager.isAdminOnlyLobby(lobby)) {
                lobbyList.append(" (Admin)");
            }
            lobbyList.append(",");
        }

        Message response = new Message(MessageType.LOBBY_LIST,
                "Server", lobbyList.toString(), null);
        sendMessage(response);
    }


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


        server.broadcastToAllClients(lobbyListMessage);
    }


    private void broadcastUserListToAllLobbies() {
        Set<String> allLobbies = lobbyManager.getAllowedLobbies();
        for (String lobby : allLobbies) {
            broadcastUserList(lobby);
        }
    }


    private void broadcastUserList(String lobbyName) {
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


        server.broadcastMessage(userListMessage, lobbyName);
    }

    public void sendMessage(Message message) {
        try {
            outputStream.writeObject(message);
            outputStream.flush();
        } catch (IOException e) {
            System.err.println("Error sending message to client: " + e.getMessage());
        }
    }

    private void sendErrorMessage(String error) {
        Message errorMessage = new Message(MessageType.ERROR, "Server", error, null);
        sendMessage(errorMessage);
    }

    private void broadcastUserList() {
        if (currentUser != null) {
            broadcastUserList(currentUser.getCurrentLobby());
        }
    }

    private void cleanup() {
        if (currentUser != null) {
            String currentLobby = currentUser.getCurrentLobby();
            lobbyManager.leaveLobby(currentUser, currentLobby);
            server.removeClient(this);

            broadcastUserListToAllLobbies();


            Message leaveMsg = new Message(MessageType.MESSAGE,
                    "Server",
                    currentUser.getUsername() + " left the chat",
                    currentLobby);
            server.broadcastMessage(leaveMsg, currentLobby);
        }

        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            System.err.println("Error cleaning up client handler: " + e.getMessage());
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }
}