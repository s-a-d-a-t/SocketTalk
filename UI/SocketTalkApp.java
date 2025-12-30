package UI;

import Clients.ChatClient;
import UI.Controllers.LoginController;
import UI.Controllers.MainController;
import UI.Controllers.RegisterController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SocketTalkApp extends Application {
    
    private Stage primaryStage;
    private ChatClient client;
    
    // Hold reference to active controller if needed for callbacks
    private LoginController loginController;
    private RegisterController registerController;
    private MainController mainController;
    private UI.Controllers.AdminController adminController;

    private static final String CSS_PATH = "/UI/CSS/style.css"; // Resource path might need adjustment depending on build

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        
        // Initialize Core Client
        client = new ChatClient();
        setupClientListener();
        
        try {
            client.connect("localhost", 5555);
        } catch (Exception e) {
            System.out.println("Could not connect to server.");
        }
        
        showLoginView();
        
        primaryStage.setTitle("SocketTalk Campus Messenger");
        primaryStage.show();
        
        primaryStage.setOnCloseRequest(e -> System.exit(0));
    }
    
    public void showLoginView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UI/FXML/LoginView.fxml"));
            Parent root = loader.load();
            
            loginController = loader.getController();
            loginController.setApp(this);
            // reset others
            registerController = null;
            mainController = null;
            
            Scene scene = new Scene(root, 900, 650); // Fixed optimized size
            scene.getStylesheets().add(getClass().getResource("/UI/CSS/style.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setResizable(false); // Fixed size window
            primaryStage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void showRegisterView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UI/FXML/RegisterView.fxml"));
            Parent root = loader.load();
            
            registerController = loader.getController();
            registerController.setApp(this);
            
            Scene scene = new Scene(root, 900, 650); // Fixed optimized size
            scene.getStylesheets().add(getClass().getResource("/UI/CSS/style.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setResizable(false); // Fixed size window
            primaryStage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void showMainView(String myId, String myName, String myRole) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UI/FXML/MainView.fxml"));
            Parent root = loader.load();
            
            mainController = loader.getController();
            mainController.setApp(this, myId, myName, myRole);
            
            // Reset admin controller
            adminController = null;
            
            Scene scene = new Scene(root, 1000, 650);
            scene.getStylesheets().add(getClass().getResource("/UI/CSS/style.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setResizable(true); // Allow resizing for main chat view
            primaryStage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void showAdminView(String myId, String myName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UI/FXML/AdminView.fxml"));
            Parent root = loader.load();
            
            adminController = loader.getController();
            adminController.setApp(this, myId, myName);
            
            // Reset main controller
            mainController = null;
            
            Scene scene = new Scene(root, 1000, 650);
            scene.getStylesheets().add(getClass().getResource("/UI/CSS/style.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupClientListener() {
        client.setListener((cmd, parts) -> {
            switch (cmd) {
                case "LOGIN_SUCCESS":
                    System.out.println("Processing LOGIN_SUCCESS with parts length: " + parts.length);
                    if (loginController != null && parts.length > 3) {
                        // parts: LOGIN_SUCCESS|ID|Name|Role
                        loginController.onLoginSuccess(parts[1], parts[2], parts[3]);
                    } else if (loginController != null) {
                        loginController.onLoginFail("Server protocol error: missing login fields");
                    }
                    break;
                case "LOGIN_FAIL":
                    if (loginController != null) {
                        loginController.onLoginFail(parts.length > 1 ? parts[1] : "Failed");
                    }
                    break;
                case "REGISTER_SUCCESS":
                    if (registerController != null) {
                        registerController.onRegisterOutcome(true, "Registration Successful! Please Login.");
                    }
                    break;
                case "REGISTER_FAIL":
                    if (registerController != null) {
                        registerController.onRegisterOutcome(false, parts.length > 1 ? parts[1] : "Failed");
                    }
                    break;
                case "USER_LIST":
                    if (mainController != null) {
                        mainController.updateUserList(parts);
                    }
                    break;
                case "USER_STATUS":
                    if (mainController != null && parts.length > 2) {
                        // USER_STATUS|userId|ONLINE/OFFLINE
                        mainController.updateUserStatus(parts[1], parts[2].equals("ONLINE"));
                    }
                    break;
                case "MSG_PRIVATE":
                    if (mainController != null) {
                        mainController.onMessageReceived(parts[1], parts[2]);
                    }
                    break;
                case "MSG_GROUP":
                    if (mainController != null && parts.length > 3) {
                         // MSG_GROUP|GROUP_ID|SENDER_ID|MESSAGE
                         mainController.onGroupMessageReceived(parts[1], parts[2], parts[3]);
                    }
                    break;
                case "GROUP_LIST":
                     if (mainController != null) {
                         mainController.updateGroupList(parts);
                     }
                     break;
                case "MSG_HISTORY":
                    if (mainController != null) {
                        mainController.onHistoryItem(parts[1], parts[2]);
                    }
                    break;
                case "ADMIN_STATS":
                    if (adminController != null && parts.length > 3) {
                        // ADMIN_STATS|totalUsers|onlineUsers|totalMessages
                        int totalUsers = Integer.parseInt(parts[1]);
                        int onlineUsers = Integer.parseInt(parts[2]);
                        int totalMessages = Integer.parseInt(parts[3]);
                        adminController.updateStatistics(totalUsers, onlineUsers, totalMessages);
                    }
                    break;
                case "ALL_USERS":
                    if (adminController != null) {
                        adminController.updateAllUsers(parts);
                    }
                    break;
                case "DELETE_SUCCESS":
                    if (adminController != null) {
                        adminController.handleRefresh();
                    }
                    break;
                case "ACCOUNT_DELETED":
                    // If the logged-in user's account was deleted by admin
                    Platform.runLater(() -> {
                        logout(); 
                        // Show message if possible, or just redirect to login
                    });
                    break;
            }
        });
    }

    public ChatClient getClient() {
        return client;
    }

    public void logout() {
        // Ideally we disconnect or send a LOGOUT command first, but for now just switch view
        // client.send("LOGOUT"); 
        showLoginView();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
