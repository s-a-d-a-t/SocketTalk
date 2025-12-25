package UI.Controllers;

import UI.SocketTalkApp;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class RegisterController {

    private SocketTalkApp app;

    @FXML private TextField idField;
    @FXML private TextField nameField;
    @FXML private PasswordField passField;
    @FXML private ComboBox<String> roleBox;
    @FXML private Label statusLabel;
    @FXML private Button registerBtn;

    public void setApp(SocketTalkApp app) {
        this.app = app;
    }

    @FXML
    private void handleRegister() {
        String id = idField.getText();
        String name = nameField.getText();
        String pass = passField.getText();
        String role = roleBox.getValue();

        if (id.isEmpty() || name.isEmpty() || pass.isEmpty() || role == null) {
            statusLabel.setText("All fields are required");
            return;
        }

        statusLabel.setText("Registering...");
        statusLabel.setStyle("-fx-text-fill: -text-gray;");
        app.getClient().register(id, name, pass, role);
    }

    @FXML
    private void backToLogin() {
        app.showLoginView();
    }

    public void onRegisterOutcome(boolean success, String message) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            if (success) {
                statusLabel.setStyle("-fx-text-fill: -success-color;"); // Green
            } else {
                statusLabel.setStyle("-fx-text-fill: -error-color;"); // Red
            }
        });
    }
}
