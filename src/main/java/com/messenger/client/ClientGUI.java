package com.messenger.client;

import com.messenger.common.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Графический интерфейс клиента чат-мессенджера.
 * Обеспечивает взаимодействие пользователя с чат-системой через GUI.
 *
 * @author Messenger Team
 * @version 1.0.0
 */
public class ClientGUI extends Application {
    private static final Logger logger = LogManager.getLogger(ClientGUI.class);

    private ChatClient client;
    private String currentUser;
    private String currentLobby = "General";
    private boolean isAdmin = false;

    // UI components
    private TextArea chatArea;
    private TextField messageField;
    private ListView<String> userListView;
    private ListView<String> lobbyListView;
    private Label statusLabel;
    private Button adminButton;
    private Stage primaryStage;

    /**
     * Основной метод запуска JavaFX приложения.
     * @param primaryStage главное окно приложения
     */
    @Override
    public void start(Stage primaryStage) {
        logger.info("Starting ClientGUI application");
        this.primaryStage = primaryStage;
        client = new ChatClient();
        setupConnectionDialog();
    }

    /**
     * Настраивает диалоговое окно для подключения к серверу.
     */
    private void setupConnectionDialog() {
        logger.debug("Setting up connection dialog");
        Dialog<String[]> connectionDialog = new Dialog<>();
        connectionDialog.setTitle("Connect to Chat Server");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField hostField = new TextField("localhost");
        TextField portField = new TextField("8080");
        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField();
        CheckBox registerCheckbox = new CheckBox("Register new user");

        grid.add(new Label("Host:"), 0, 0);
        grid.add(hostField, 1, 0);
        grid.add(new Label("Port:"), 0, 1);
        grid.add(portField, 1, 1);
        grid.add(new Label("Username:"), 0, 2);
        grid.add(usernameField, 1, 2);
        grid.add(new Label("Password:"), 0, 3);
        grid.add(passwordField, 1, 3);
        grid.add(registerCheckbox, 1, 4);

        ButtonType connectButtonType = new ButtonType("Connect", ButtonBar.ButtonData.OK_DONE);
        connectionDialog.getDialogPane().getButtonTypes().addAll(connectButtonType, ButtonType.CANCEL);
        connectionDialog.getDialogPane().setContent(grid);

        Platform.runLater(usernameField::requestFocus);

        connectionDialog.setResultConverter(dialogButton -> {
            if (dialogButton == connectButtonType) {
                return new String[]{
                        hostField.getText(),
                        portField.getText(),
                        usernameField.getText(),
                        passwordField.getText(),
                        String.valueOf(registerCheckbox.isSelected())
                };
            }
            return null;
        });

        connectionDialog.showAndWait().ifPresent(credentials -> {
            connectToServer(credentials);
        });
    }

    /**
     * Подключается к серверу с указанными учетными данными.
     * @param credentials массив с учетными данными [host, port, username, password, register]
     */
    private void connectToServer(String[] credentials) {
        try {
            String host = credentials[0];
            int port = Integer.parseInt(credentials[1]);
            String username = credentials[2];
            String password = credentials[3];
            boolean register = Boolean.parseBoolean(credentials[4]);

            logger.info("Connecting to server {}:{} as user {}", host, port, username);

            client.setMessageListener(new ChatClient.MessageListener() {
                @Override
                public void onMessageReceived(Message message) {
                    Platform.runLater(() -> handleIncomingMessage(message));
                }

                @Override
                public void onConnectionStatusChanged(boolean connected) {
                    Platform.runLater(() -> updateConnectionStatus(connected));
                }
            });

            client.connect(host, port);

            if (register) {
                client.register(username, password);
            } else {
                client.login(username, password);
            }

            currentUser = username;
            setupMainInterface();
            logger.info("Successfully connected and set up main interface");

        } catch (Exception e) {
            logger.error("Connection error: {}", e.getMessage());
            showError("Connection Error", "Failed to connect to server: " + e.getMessage());
        }
    }

    /**
     * Настраивает основной интерфейс приложения после успешного подключения.
     */
    private void setupMainInterface() {
        logger.debug("Setting up main interface for user: {}", currentUser);
        primaryStage.setTitle("Chat Messenger - " + currentUser);

        // Main layout
        BorderPane mainLayout = new BorderPane();

        // Chat area
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);

        // Message input
        HBox messageBox = new HBox(10);
        messageField = new TextField();
        messageField.setPromptText("Type your message...");
        Button sendButton = new Button("Send");

        messageField.setOnAction(e -> sendMessage());
        sendButton.setOnAction(e -> sendMessage());

        messageBox.getChildren().addAll(messageField, sendButton);
        HBox.setHgrow(messageField, Priority.ALWAYS);

        // User list
        VBox userBox = new VBox(10);
        userBox.setPadding(new Insets(10));
        userBox.setPrefWidth(200);

        Label userLabel = new Label("Online Users");
        userListView = new ListView<>();

        userBox.getChildren().addAll(userLabel, userListView);

        // Lobby list
        VBox lobbyBox = new VBox(10);
        lobbyBox.setPadding(new Insets(10));
        lobbyBox.setPrefWidth(200);

        Label lobbyLabel = new Label("Lobbies");
        lobbyListView = new ListView<>();

        HBox lobbyButtonBox = new HBox(5);
        Button refreshLobbyButton = new Button("Refresh");
        adminButton = new Button("Admin Tools");
        adminButton.setVisible(false);

        refreshLobbyButton.setOnAction(e -> {
            logger.debug("Refreshing lobby list");
            client.requestLobbyList();
        });
        adminButton.setOnAction(e -> showAdminTools());

        lobbyButtonBox.getChildren().addAll(refreshLobbyButton, adminButton);

        lobbyBox.getChildren().addAll(lobbyLabel, lobbyListView, lobbyButtonBox);

        // Status bar
        statusLabel = new Label("Connected");
        HBox statusBar = new HBox(statusLabel);
        statusBar.setPadding(new Insets(5));
        statusBar.setStyle("-fx-background-color: #e0e0e0;");

        // Combine side panels
        SplitPane sidePanels = new SplitPane();
        sidePanels.getItems().addAll(userBox, lobbyBox);
        sidePanels.setOrientation(javafx.geometry.Orientation.VERTICAL);

        mainLayout.setCenter(chatArea);
        mainLayout.setBottom(messageBox);
        mainLayout.setRight(sidePanels);
        mainLayout.setTop(statusBar);

        Scene scene = new Scene(mainLayout, 1000, 700);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Request initial data
        client.requestLobbyList();
        client.requestUserList();
        logger.debug("Initial data requests sent");

        primaryStage.setOnCloseRequest(e -> {
            logger.info("Application closing, disconnecting from server");
            client.disconnect();
        });
    }

    /**
     * Обрабатывает входящие сообщения от сервера.
     * @param message полученное сообщение
     */
    private void handleIncomingMessage(Message message) {
        logger.debug("Handling incoming message type: {}", message.getType());
        switch (message.getType()) {
            case MESSAGE:
                chatArea.appendText(message.toString() + "\n");
                break;
            case USER_LIST:
                updateUserList(message.getContent());
                break;
            case LOBBY_LIST:
                updateLobbyList(message.getContent());
                break;
            case SUCCESS:
                chatArea.appendText("SUCCESS: " + message.getContent() + "\n");
                // Проверяем, не сообщение ли это о успешном входе админа
                if (message.getContent().contains("(Admin)")) {
                    isAdmin = true;
                    adminButton.setVisible(true);
                    logger.info("Admin privileges activated for user: {}", currentUser);
                }
                break;
            case ERROR:
                chatArea.appendText("ERROR: " + message.getContent() + "\n");
                logger.warn("Error message received: {}", message.getContent());
                break;
            default:
                logger.warn("Unknown message type received: {}", message.getType());
                break;
        }
    }

    /**
     * Обновляет список пользователей в интерфейсе.
     * @param userListString строка со списком пользователей
     */
    private void updateUserList(String userListString) {
        logger.debug("Updating user list");
        userListView.getItems().clear();
        String[] users = userListString.split(",");
        for (String user : users) {
            if (!user.isEmpty()) {
                userListView.getItems().add(user);
            }
        }
    }

    /**
     * Обновляет список лобби в интерфейсе.
     * @param lobbyListString строка со списком лобби
     */
    private void updateLobbyList(String lobbyListString) {
        logger.debug("Updating lobby list");
        lobbyListView.getItems().clear();
        String[] lobbies = lobbyListString.split(",");
        for (String lobby : lobbies) {
            if (!lobby.isEmpty()) {
                lobbyListView.getItems().add(lobby);
            }
        }

        lobbyListView.setOnMouseClicked(e -> {
            String selectedLobby = lobbyListView.getSelectionModel().getSelectedItem();
            if (selectedLobby != null && !selectedLobby.equals(currentLobby)) {
                // Убираем пометку "(Admin)" для чистого имени лобби
                String cleanLobbyName = selectedLobby.replace(" (Admin)", "").trim();
                client.joinLobby(cleanLobbyName);
                currentLobby = cleanLobbyName;
                chatArea.appendText("Joining lobby: " + cleanLobbyName + "\n");
                client.requestUserList();
                logger.info("User {} joined lobby: {}", currentUser, cleanLobbyName);
            }
        });
    }

    /**
     * Отправляет текстовое сообщение в текущее лобби.
     */
    private void sendMessage() {
        String messageText = messageField.getText().trim();
        if (!messageText.isEmpty() && client.isConnected()) {
            client.sendChatMessage(messageText, currentLobby, currentUser);
            messageField.clear();
            logger.debug("Message sent to lobby: {}", currentLobby);
        } else if (!client.isConnected()) {
            logger.warn("Attempted to send message while disconnected");
            showError("Send Error", "Not connected to server");
        }
    }

    /**
     * Обновляет статус соединения в интерфейсе.
     * @param connected true если соединение установлено, false если разорвано
     */
    private void updateConnectionStatus(boolean connected) {
        if (connected) {
            statusLabel.setText("Connected");
            statusLabel.setStyle("-fx-text-fill: green;");
            logger.info("Connection status: Connected");
        } else {
            statusLabel.setText("Disconnected");
            statusLabel.setStyle("-fx-text-fill: red;");
            logger.warn("Connection status: Disconnected");
        }
    }

    /**
     * Показывает диалоговое окно административных инструментов.
     */
    private void showAdminTools() {
        logger.debug("Showing admin tools dialog");
        Dialog<Void> adminDialog = new Dialog<>();
        adminDialog.setTitle("Admin Tools");
        adminDialog.setHeaderText("Lobby Management");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField lobbyNameField = new TextField();
        CheckBox adminOnlyCheckbox = new CheckBox("Admin Only");
        Button createButton = new Button("Create Lobby");
        Button deleteButton = new Button("Delete Selected Lobby");

        createButton.setOnAction(e -> {
            String lobbyName = lobbyNameField.getText().trim();
            if (!lobbyName.isEmpty()) {
                client.createLobby(lobbyName, adminOnlyCheckbox.isSelected());
                adminDialog.close();
                logger.info("Admin created lobby: {} (adminOnly: {})",
                        lobbyName, adminOnlyCheckbox.isSelected());
            }
        });

        deleteButton.setOnAction(e -> {
            String selectedLobby = lobbyListView.getSelectionModel().getSelectedItem();
            if (selectedLobby != null) {
                String cleanLobbyName = selectedLobby.replace(" (Admin)", "").trim();
                if (!"General".equals(cleanLobbyName)) {
                    client.deleteLobby(cleanLobbyName);
                    adminDialog.close();
                    logger.info("Admin deleted lobby: {}", cleanLobbyName);
                } else {
                    logger.warn("Attempt to delete General lobby blocked");
                    showError("Delete Error", "Cannot delete General lobby");
                }
            }
        });

        grid.add(new Label("Lobby Name:"), 0, 0);
        grid.add(lobbyNameField, 1, 0);
        grid.add(adminOnlyCheckbox, 1, 1);
        grid.add(createButton, 0, 2);
        grid.add(deleteButton, 1, 2);

        adminDialog.getDialogPane().setContent(grid);
        adminDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        adminDialog.showAndWait();
    }

    /**
     * Показывает диалоговое окно с сообщением об ошибке.
     * @param title заголовок окна
     * @param message текст сообщения об ошибке
     */
    private void showError(String title, String message) {
        logger.warn("Showing error dialog: {} - {}", title, message);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Точка входа для запуска клиентского приложения.
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        logger.info("Launching ClientGUI application");
        launch(args);
    }
}
