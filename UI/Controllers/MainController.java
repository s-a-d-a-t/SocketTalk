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
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;

public class MainController {

    private SocketTalkApp app;
    private String myId;
    private String myName;
    private String myRole;

    @FXML private VBox userListContainer;
    @FXML private BorderPane chatArea;
    @FXML private Label chatTitle;
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox messageContainer;
    @FXML private TextField messageInput;
    @FXML private Button sendButton;
    @FXML private TextField searchField;
    @FXML private Button adminPanelBtn; // Optional button for teachers

    private String currentChatTargetId;
    // Cache for User info
    private final Map<String, String> knownUsers = new HashMap<>();
    // Track user online status
    private final Map<String, Boolean> userOnlineStatus = new HashMap<>();
    // Track unread message counts
    private final Map<String, Integer> unreadCounts = new HashMap<>();
    // Store user cell references for updates
    private final Map<String, HBox> userCells = new HashMap<>();

    public void setApp(SocketTalkApp app, String myId, String myName, String myRole) {
        this.app = app;
        this.myId = myId;
        this.myName = myName;
        this.myRole = myRole;
        
        // Show admin panel button only for admins
        if (adminPanelBtn != null) {
            adminPanelBtn.setVisible("ADMIN".equals(myRole));
            adminPanelBtn.setManaged("ADMIN".equals(myRole));
        }
        
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
        // parts format: USER_LIST|ID:Name:Role:Status|...
        Platform.runLater(() -> {
            userListContainer.getChildren().clear();
            userCells.clear();
            for (int i = 1; i < parts.length; i++) {
                String[] info = parts[i].split(":");
                if (info.length < 4) continue;
                
                String uid = info[0];
                String uname = info[1];
                String urole = info[2];
                String status = info[3];
                
                knownUsers.put(uid, uname);
                userOnlineStatus.put(uid, status.equals("ONLINE"));
                
                HBox cell = createUserCell(uid, uname, urole, status.equals("ONLINE"));
                userListContainer.getChildren().add(cell);
                userCells.put(uid, cell);
            }
        });
    }

    private HBox createUserCell(String uid, String uname, String urole, boolean isOnline) {
        HBox cell = new HBox(10);
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.getStyleClass().add("list-cell");
        cell.setPadding(new javafx.geometry.Insets(10));
        cell.setCursor(javafx.scene.Cursor.HAND);
        cell.setUserData(uid); // Store user ID for reference
        
        // Avatar with status indicator
        javafx.scene.layout.StackPane avatarStack = new javafx.scene.layout.StackPane();
        Label avatar = new Label(uname.substring(0,1).toUpperCase());
        avatar.setStyle("-fx-background-color: " + getColor(uid) + "; -fx-text-fill: white; -fx-background-radius: 20; -fx-min-width: 40; -fx-min-height: 40; -fx-alignment: center; -fx-font-weight: bold;");
        
        // Status indicator
        Circle statusDot = new Circle(6);
        statusDot.setFill(isOnline ? Color.web("#10b981") : Color.web("#6b7280"));
        statusDot.setStroke(Color.WHITE);
        statusDot.setStrokeWidth(2);
        javafx.scene.layout.StackPane.setAlignment(statusDot, Pos.BOTTOM_RIGHT);
        statusDot.getStyleClass().add(isOnline ? "status-online" : "status-offline");
        
        avatarStack.getChildren().addAll(avatar, statusDot);
        
        VBox textBox = new VBox(2);
        Label nameLbl = new Label(uname);
        nameLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        Label roleLbl = new Label(urole);
        roleLbl.setStyle("-fx-text-fill: -text-gray; -fx-font-size: 10px;");
        
        textBox.getChildren().addAll(nameLbl, roleLbl);
        
        // Notification badge (initially hidden)
        Label notifBadge = new Label("0");
        notifBadge.getStyleClass().add("notification-badge");
        notifBadge.setVisible(false);
        javafx.scene.layout.HBox.setMargin(notifBadge, new javafx.geometry.Insets(0, 0, 0, 10));
        
        cell.getChildren().addAll(avatarStack, textBox, notifBadge);
        
        cell.setOnMouseClicked(e -> selectChat(uid, uname));
        return cell;
    }

    private void selectChat(String uid, String uname) {
        this.currentChatTargetId = uid;
        chatTitle.setText(uname);
        messageContainer.getChildren().clear();
        sendButton.setDisable(false);
        
        // Clear unread count for this user
        unreadCounts.put(uid, 0);
        updateNotificationBadge(uid, 0);
        
        app.getClient().requestHistory(uid);
    }

    public void onMessageReceived(String senderId, String msg) {
        Platform.runLater(() -> {
            if (currentChatTargetId != null && currentChatTargetId.equals(senderId)) {
                addMessageBubble(msg, false);
            } else {
                // Increment unread count and update badge
                int count = unreadCounts.getOrDefault(senderId, 0) + 1;
                unreadCounts.put(senderId, count);
                updateNotificationBadge(senderId, count);
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
    
    @FXML
    private void handleAdminPanel() {
        if (app != null && "ADMIN".equals(myRole)) {
            app.showAdminView(myId, myName);
        }
    }
    
    // Update user online/offline status dynamically
    public void updateUserStatus(String userId, boolean isOnline) {
        Platform.runLater(() -> {
            userOnlineStatus.put(userId, isOnline);
            HBox cell = userCells.get(userId);
            if (cell != null) {
                // Find the status dot in the cell (it's inside the StackPane)
                for (javafx.scene.Node node : cell.getChildren()) {
                    if (node instanceof javafx.scene.layout.StackPane) {
                        javafx.scene.layout.StackPane avatarStack = (javafx.scene.layout.StackPane) node;
                        for (javafx.scene.Node child : avatarStack.getChildren()) {
                            if (child instanceof Circle) {
                                Circle statusDot = (Circle) child;
                                statusDot.setFill(isOnline ? Color.web("#10b981") : Color.web("#6b7280"));
                                break;
                            }
                        }
                        break;
                    }
                }
            }
        });
    }
    
    // Update notification badge for a user
    private void updateNotificationBadge(String userId, int count) {
        HBox cell = userCells.get(userId);
        if (cell != null) {
            // Find the notification badge (last child in the cell)
            for (javafx.scene.Node node : cell.getChildren()) {
                if (node instanceof Label && node.getStyleClass().contains("notification-badge")) {
                    Label badge = (Label) node;
                    if (count > 0) {
                        badge.setText(String.valueOf(count));
                        badge.setVisible(true);
                    } else {
                        badge.setVisible(false);
                    }
                    break;
                }
            }
        }
    }

    private String getColor(String id) {
        String[] colors = {"#FF5733", "#33FF57", "#3357FF", "#F1C40F", "#9B59B6", "#1ABC9C"};
        return colors[Math.abs(id.hashCode()) % colors.length];
    }
}
