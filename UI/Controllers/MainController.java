package UI.Controllers;

import UI.SocketTalkApp;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ListView;
import java.util.Optional;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

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
    @FXML private Button adminPanelBtn; 
    @FXML private MenuButton optionsBtn;

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
        app.getClient().requestGroups();
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

        if (knownGroups.containsKey(currentChatTargetId)) {
             app.getClient().sendGroupMessage(currentChatTargetId, msg);
             // For groups, we might need to wait for echo or add immediately? 
             // Server echoes to others. Consistency: add to self immediately.
             addMessageBubble(msg, true, "Me");
        } else {
             app.getClient().sendPrivateMessage(currentChatTargetId, msg);
             addMessageBubble(msg, true, null); // Private msg, no name needed for self usually
        }
        
        messageInput.clear();
    }

    public void updateUserList(String[] parts) {
        // parts format: USER_LIST|ID:Name:Role:Status|...
        Platform.runLater(() -> {
            // NOTE: We do NOT clear the list anymore, because we receive incremental updates
            // OR we receive the full list on login. Server sends small updates now.
            // If it's a huge list rebuild, maybe we should clear? 
            // The protocol is ambiguous: Login sends "USER_LIST|..." (full contact list).
            // StartChat sends "USER_LIST|..." (single user).
            // MainServer sends 'USER_LIST' followed by items.
            // Let's assume server sends everything needed or we handle duplicates.
            
            // To be safe for "fresh" login lists vs updates:
            // Ideally we detect context. But simple duplicate check works.
            
            // Actually, for the initial login, we might want to clear IF it's the first load?
            // But 'userCells' tracks what we have.
            
            for (int i = 1; i < parts.length; i++) {
                String[] info = parts[i].split(":");
                if (info.length < 4) continue;
                
                String uid = info[0];
                String uname = info[1];
                String urole = info[2];
                String status = info[3];
                
                if (uid.equals(myId)) continue; 
                
                knownUsers.put(uid, uname);
                userOnlineStatus.put(uid, status.equals("ONLINE"));
                
                // If cell exists, update it? Or just ignore?
                if (!userCells.containsKey(uid)) {
                    HBox cell = createUserCell(uid, uname, urole, status.equals("ONLINE"));
                    userListContainer.getChildren().add(cell);
                    userCells.put(uid, cell);
                } else {
                    // Update status if needed
                    updateUserStatus(uid, status.equals("ONLINE"));
                }
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
        nameLbl.setStyle("-fx-text-fill: -text-main; -fx-font-weight: 800; -fx-font-size: 14px;");
        
        textBox.getChildren().addAll(nameLbl);
        
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
        if (uid == null) return;
        if (uid.equals(currentChatTargetId)) return; // Already selected
        
        // Remove active style from previous
        if (currentChatTargetId != null && userCells.containsKey(currentChatTargetId)) {
            userCells.get(currentChatTargetId).getStyleClass().remove("list-cell-selected");
             userCells.get(currentChatTargetId).setStyle("");
        }
        
        this.currentChatTargetId = uid;
        chatTitle.setText(uname);
        messageContainer.getChildren().clear();
        sendButton.setDisable(false);
        
        // Show/Hide Options Button
        if (optionsBtn != null) {
            boolean isGroup = knownGroups.containsKey(uid);
            optionsBtn.setVisible(isGroup);
            optionsBtn.setManaged(isGroup);
        }
        
        // Highlight active cell
        if (userCells.containsKey(uid)) {
            userCells.get(uid).getStyleClass().add("list-cell-selected");
             userCells.get(uid).setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); -fx-background-radius: 8;");
        }
        
        // Clear unread count for this user
        unreadCounts.put(uid, 0);
        updateNotificationBadge(uid, 0);
        
        app.getClient().requestHistory(uid);
    }
    
    @FXML
    private void handleLeaveGroup() {
        if (currentChatTargetId == null || !knownGroups.containsKey(currentChatTargetId)) return;
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Leave Group");
        alert.setHeaderText("Leave " + knownGroups.get(currentChatTargetId) + "?");
        alert.setContentText("Are you sure you want to leave this group?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String groupIdToRemove = currentChatTargetId; // Capture ID before resetting
            app.getClient().leaveGroup(groupIdToRemove);
            
            // 1. Remove from List and Cache
            if (userCells.containsKey(groupIdToRemove)) {
                userListContainer.getChildren().remove(userCells.get(groupIdToRemove));
                userCells.remove(groupIdToRemove);
            }
            knownGroups.remove(groupIdToRemove);
            unreadCounts.remove(groupIdToRemove);
            
            // 2. Reset Chat Area
            messageContainer.getChildren().clear();
            chatTitle.setText("Select a conversation");
            currentChatTargetId = null;
            optionsBtn.setVisible(false);
            sendButton.setDisable(true);
        }
    }
    
    // Distinguish if current chat is a group
    private boolean isCurrentChatGroup() {
        // Simple heuristic: if the ID is in knownGroups map (which we need to add)
        // Or check ID format (UUIDs are long).
        // Better: Maintain a set of GroupIDs.
        return knownGroups.containsKey(currentChatTargetId);
    }
    
    private final Map<String, String> knownGroups = new HashMap<>();

    public void onMessageReceived(String senderId, String msg) {
        Platform.runLater(() -> {
            if (currentChatTargetId != null && currentChatTargetId.equals(senderId)) {
                // Private message from open chat
                addMessageBubble(msg, false, null);
            } else {
                // Increment unread count and update badge
                int count = unreadCounts.getOrDefault(senderId, 0) + 1;
                unreadCounts.put(senderId, count);
                updateNotificationBadge(senderId, count);
            }
        });
    }
    
    public void onGroupMessageReceived(String groupId, String senderId, String msg) {
         Platform.runLater(() -> {
            if (currentChatTargetId != null && currentChatTargetId.equals(groupId)) {
                // Message for currently open group
                // Show Sender Name!
                String senderName = knownUsers.getOrDefault(senderId, senderId);
                addMessageBubble(msg, false, senderName);
            } else {
                // Increment unread count for group
                int count = unreadCounts.getOrDefault(groupId, 0) + 1;
                unreadCounts.put(groupId, count);
                updateNotificationBadge(groupId, count);
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
                
                String displayName = null;
                // If it's a group chat, we ALWAYS need a name, even for me?
                // User requirement: "senders name must appear at the top of each message... regardless"
                if (knownGroups.containsKey(targetId)) {
                    displayName = isMe ? "Me" : knownUsers.getOrDefault(msgSender, msgSender); 
                }
                
                addMessageBubble(text, isMe, displayName);
            }
        });
    }

    private void addMessageBubble(String text, boolean isMe, String senderName) {
        VBox container = new VBox(2); // Container for Name + Bubble
        container.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        
        if (senderName != null) {
            Label nameLbl = new Label(senderName);
            nameLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-gray; -fx-padding: 0 5 0 5;");
            container.getChildren().add(nameLbl);
        }
        
        HBox row = new HBox();
        row.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        
        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(400);
        bubble.getStyleClass().add(isMe ? "bubble-sent" : "bubble-recv");
        
        row.getChildren().add(bubble);
        container.getChildren().add(row);
        
        messageContainer.getChildren().add(container);
    }
    
    public void updateGroupList(String[] parts) {
        // GROUP_LIST|ID:Name|ID:Name...
        Platform.runLater(() -> {
            // We don't want to clear users, just maybe append or smart merge?
            // Simpler: Maintain a separate container or just add them to the same list but with different styling.
            // For now, let's just add them. Issue: Duplicate calls might duplicate UI.
            // Ideally we need to clear and rebuild Everything or track what's added.
            // Let's rely on map.
            
            for (int i = 1; i < parts.length; i++) {
                String[] info = parts[i].split(":");
                if (info.length < 2) continue;
                String gid = info[0];
                String gname = info[1];
                
                knownGroups.put(gid, gname);
                
                if (!userCells.containsKey(gid)) {
                     HBox cell = createGroupCell(gid, gname);
                     userListContainer.getChildren().add(0, cell); // Add groups at top
                     userCells.put(gid, cell);
                }
            }
        });
    }
    
    private HBox createGroupCell(String gid, String gname) {
        HBox cell = new HBox(10);
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.getStyleClass().add("list-cell");
        cell.setPadding(new javafx.geometry.Insets(10));
        cell.setCursor(javafx.scene.Cursor.HAND);
        cell.setUserData(gid); 
        
        // Group Icon (Square)
        Label avatar = new Label(gname.substring(0,1).toUpperCase());
        avatar.setStyle("-fx-background-color: #6366f1; -fx-text-fill: white; -fx-background-radius: 8; -fx-min-width: 40; -fx-min-height: 40; -fx-alignment: center; -fx-font-weight: bold;");
        
        VBox textBox = new VBox(2);
        Label nameLbl = new Label(gname);
        nameLbl.setStyle("-fx-text-fill: -text-main; -fx-font-weight: 800; -fx-font-size: 14px;");
        Label subLbl = new Label("Group Chat");
        subLbl.setStyle("-fx-text-fill: -text-gray; -fx-font-size: 11px;");
        
        textBox.getChildren().addAll(nameLbl, subLbl);
        
        // Notification badge
        Label notifBadge = new Label("0");
        notifBadge.getStyleClass().add("notification-badge");
        notifBadge.setVisible(false);
        javafx.scene.layout.HBox.setMargin(notifBadge, new javafx.geometry.Insets(0, 0, 0, 10));
        
        cell.getChildren().addAll(avatar, textBox, notifBadge);
        
        cell.setOnMouseClicked(e -> selectChat(gid, gname));
        return cell;
    }
    
    @FXML
    private void handleLogout() {
        if (app != null) {
            app.logout();
        }
    }
    
    @FXML
    private void handleNewConversation() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Conversation");
        dialog.setHeaderText("Start a new private chat");
        dialog.setContentText("Enter User ID:");
        
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String targetId = result.get().trim();
            if(!targetId.isEmpty()) {
                // If we already have this chat, select it
                if (knownUsers.containsKey(targetId)) {
                    selectChat(targetId, knownUsers.get(targetId));
                } else {
                    // Send request to server
                    app.getClient().startConversation(targetId);
                }
            }
        }
    }

    @FXML
    private void handleCreateGroup() {
        // 1. Group Name
        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setTitle("Create Group");
        nameDialog.setHeaderText("Create a New Group Chat");
        nameDialog.setContentText("Enter Group Name:");
        
        Optional<String> nameResult = nameDialog.showAndWait();
        if (!nameResult.isPresent() || nameResult.get().trim().isEmpty()) return;
        String groupName = nameResult.get().trim();
        
        // 2. Advanced Member Picker
        List<String> members = showMemberPickerDialog("Add Members to " + groupName);
        if (members == null || members.isEmpty()) return; // User cancelled or added none
        
        String membersStr = String.join(",", members);
        
        if (app != null) {
            app.getClient().createGroup(groupName, membersStr);
        }
    }
    
    @FXML
    private void handleAddMember() {
        if (currentChatTargetId == null || !knownGroups.containsKey(currentChatTargetId)) return;
        
        String groupName = knownGroups.get(currentChatTargetId);
        List<String> newMembers = showMemberPickerDialog("Add User to " + groupName);
        
        if (newMembers != null && !newMembers.isEmpty()) {
            for (String newId : newMembers) {
                 app.getClient().addGroupMember(currentChatTargetId, newId);
            }
        }
    }
    
    // Returns list of selected User IDs, or null if cancelled
    private List<String> showMemberPickerDialog(String title) {
        Dialog<List<String>> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText("Enter User IDs to add to the list.");
        
        ButtonType confirmButtonType = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);
        
        VBox content = new VBox(10);
        content.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        
        HBox inputBox = new HBox(10);
        TextField idInput = new TextField();
        idInput.setPromptText("User ID");
        Button addButton = new Button("Add");
        inputBox.getChildren().addAll(idInput, addButton);
        
        // List stores formatted strings: "Name (ID)"
        ListView<String> memberList = new ListView<>();
        memberList.setPrefHeight(200);
        
        // Keep track of added IDs separately to avoid duplicates accurately
        List<String> addedIds = new ArrayList<>();
        
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");
        
        addButton.setOnAction(e -> {
            String uid = idInput.getText().trim();
            if (uid.isEmpty()) return;
            
            // Allow checking global users (server will validate existence later)
            // If we know the user locally, use their name. Otherwise use ID.
            if (!addedIds.contains(uid)) {
                addedIds.add(uid);
                
                String displayName;
                if (knownUsers.containsKey(uid)) {
                    displayName = knownUsers.get(uid) + " (" + uid + ")";
                } else {
                    displayName = uid; // We don't know the name yet, server will handle it
                }
                
                memberList.getItems().add(displayName);
                idInput.clear();
                errorLabel.setText("");
            } else {
                errorLabel.setText("User already in list.");
            }
        });
        
        content.getChildren().addAll(inputBox, errorLabel, new Label("Members to Add:"), memberList);
        dialog.getDialogPane().setContent(content);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                // Return the raw IDs
                return new ArrayList<>(addedIds);
            }
            return null;
        });
        
        return dialog.showAndWait().orElse(null);
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
