package UI;

import Clients.ChatClient;
import UI.Controllers.LoginController;
import UI.Controllers.MainController;
import UI.Controllers.RegisterController;
import javafx.application.Application;
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
            
            Scene scene = new Scene(root, 950, 700); // Larger window for the new layout
            scene.getStylesheets().add(getClass().getResource("/UI/CSS/style.css").toExternalForm());
            primaryStage.setScene(scene);
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
            
            Scene scene = new Scene(root, 950, 700);
            scene.getStylesheets().add(getClass().getResource("/UI/CSS/style.css").toExternalForm());
            primaryStage.setScene(scene);
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
            
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/UI/CSS/style.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupClientListener() {
        client.setListener((cmd, parts) -> {
            switch (cmd) {
                case "LOGIN_SUCCESS":
                    if (loginController != null) {
                        // Assuming parts: LOGIN_SUCCESS|Name|Role
                        loginController.onLoginSuccess(parts[1], parts[2]);
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
                case "MSG_PRIVATE":
                    if (mainController != null) {
                        mainController.onMessageReceived(parts[1], parts[2]);
                    }
                    break;
                case "MSG_HISTORY":
                    if (mainController != null) {
                        mainController.onHistoryItem(parts[1], parts[2]);
                    }
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
