package UI.Controllers;

import UI.SocketTalkApp;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    private SocketTalkApp app;

    @FXML private TextField idField;
    @FXML private PasswordField passField;
    @FXML private Button loginBtn;
    @FXML private Label statusLabel;

    public void setApp(SocketTalkApp app) {
        this.app = app;
    }

    @FXML
    public void initialize() {
        // Init logic if needed
    }

    @FXML
    private void handleLogin() {
        String id = idField.getText();
        String pass = passField.getText();

        if (id.isEmpty() || pass.isEmpty()) {
            statusLabel.setText("Please enter ID and Password");
            return;
        }
        
        statusLabel.setText("Logging in...");
        statusLabel.setStyle("-fx-text-fill: -text-gray;");
        
        if (app != null && app.getClient() != null) {
            app.getClient().login(id, pass);
        } else {
             statusLabel.setText("Connection Error");
        }
    }

    @FXML
    private void goToRegister() {
        if (app != null) app.showRegisterView();
    }

    public void onLoginSuccess(String id, String name, String role) {
        Platform.runLater(() -> {
            if ("ADMIN".equals(role)) {
                app.showAdminView(id, name);
            } else {
                app.showMainView(id, name, role);
            }
        });
    }

    public void onLoginFail(String reason) {
        Platform.runLater(() -> {
            statusLabel.setText(reason);
            statusLabel.setStyle("-fx-text-fill: -error-color;"); // Red
        });
    }
}
