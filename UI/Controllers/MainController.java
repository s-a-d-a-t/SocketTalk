package UI.Controllers;

import UI.SocketTalkApp;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;

public class MainController {

    private SocketTalkApp app;
    private String myId;
    private String myName;

    @FXML private VBox userListContainer;
    @FXML private BorderPane chatArea;
    @FXML private Label chatTitle;
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox messageContainer;
    @FXML private TextField messageInput;
    @FXML private Button sendButton;
    @FXML private TextField searchField;

    private String currentChatTargetId;
    // Cache for User info
    private final Map<String, String> knownUsers = new HashMap<>();

    public void setApp(SocketTalkApp app, String myId, String myName, String myRole) {
        this.app = app;
        this.myId = myId;
        this.myName = myName;
        // Request users nicely
        app.getClient().requestUserList();
    }

    @FXML
    public void initialize() {
        sendButton.setDisable(true);
        // Bind scroll to bottom
        messageContainer.heightProperty().addListener((obs, old, val) -> chatScrollPane.setVvalue(1.0));
    }

    @FXML
    private void sendMessage() {
        String msg = messageInput.getText();
        if (msg.isEmpty() || currentChatTargetId == null) return;

        app.getClient().sendPrivateMessage(currentChatTargetId, msg);
        addMessageBubble(msg, true);
        messageInput.clear();
    }

    public void updateUserList(String[] parts) {
        // parts format: USER_LIST|ID:Name:Role|...
        Platform.runLater(() -> {
            userListContainer.getChildren().clear();
            for (int i = 1; i < parts.length; i++) {
                String[] info = parts[i].split(":");
                if (info.length < 3) continue;
                
                String uid = info[0];
                String uname = info[1];
                String urole = info[2];
                
                knownUsers.put(uid, uname);
                
                HBox cell = createUserCell(uid, uname, urole);
                userListContainer.getChildren().add(cell);
            }
        });
    }

    private HBox createUserCell(String uid, String uname, String urole) {
        HBox cell = new HBox(10);
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.getStyleClass().add("list-cell");
        cell.setPadding(new javafx.geometry.Insets(10));
        cell.setCursor(javafx.scene.Cursor.HAND);
        
        // Avatar
        Label avatar = new Label(uname.substring(0,1).toUpperCase());
        avatar.setStyle("-fx-background-color: " + getColor(uid) + "; -fx-text-fill: white; -fx-background-radius: 20; -fx-min-width: 40; -fx-min-height: 40; -fx-alignment: center; -fx-font-weight: bold;");
        
        VBox textBox = new VBox(2);
        Label nameLbl = new Label(uname);
        nameLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        Label roleLbl = new Label(urole);
        roleLbl.setStyle("-fx-text-fill: -text-gray; -fx-font-size: 10px;");
        
        textBox.getChildren().addAll(nameLbl, roleLbl);
        cell.getChildren().addAll(avatar, textBox);
        
        cell.setOnMouseClicked(e -> selectChat(uid, uname));
        return cell;
    }

    private void selectChat(String uid, String uname) {
        this.currentChatTargetId = uid;
        chatTitle.setText(uname);
        messageContainer.getChildren().clear();
        sendButton.setDisable(false);
        app.getClient().requestHistory(uid);
    }

    public void onMessageReceived(String senderId, String msg) {
        Platform.runLater(() -> {
            if (currentChatTargetId != null && currentChatTargetId.equals(senderId)) {
                addMessageBubble(msg, false);
            } else {
                // Could highlight user in list
            }
        });
    }

    public void onHistoryItem(String targetId, String line) {
        Platform.runLater(() -> {
            // Line format: "ID: Message"
            // We need to parse
            String[] split = line.split(": ", 2);
            if (split.length >= 2) {
                String msgSender = split[0];
                String text = split[1];
                boolean isMe = msgSender.equals(myId);
                addMessageBubble(text, isMe);
            }
        });
    }

    private void addMessageBubble(String text, boolean isMe) {
        HBox row = new HBox();
        row.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        
        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(400);
        bubble.getStyleClass().add(isMe ? "bubble-sent" : "bubble-recv");
        
        row.getChildren().add(bubble);
        messageContainer.getChildren().add(row);
    }
    
    @FXML
    private void handleLogout() {
        if (app != null) {
            app.logout();
        }
    }

    private String getColor(String id) {
        String[] colors = {"#FF5733", "#33FF57", "#3357FF", "#F1C40F", "#9B59B6", "#1ABC9C"};
        return colors[Math.abs(id.hashCode()) % colors.length];
    }
}
