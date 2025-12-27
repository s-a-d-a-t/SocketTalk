package UI.Controllers;

import UI.SocketTalkApp;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class AdminController {

    private SocketTalkApp app;
    private String myId;
    private String myName;

    // Top section
    @FXML private Label adminNameLabel;
    @FXML private Button refreshBtn;
    @FXML private Button backBtn;

    // Statistics Labels
    @FXML private Label totalUsersLabel;
    @FXML private Label onlineUsersLabel;
    @FXML private Label totalMessagesLabel;

    // User Table
    @FXML private TableView<UserData> usersTable;
    @FXML private TableColumn<UserData, String> idColumn;
    @FXML private TableColumn<UserData, String> nameColumn;
    @FXML private TableColumn<UserData, String> roleColumn;
    @FXML private TableColumn<UserData, String> statusColumn;
    @FXML private TableColumn<UserData, UserData> actionColumn;

    private ObservableList<UserData> userData = FXCollections.observableArrayList();

    public void setApp(SocketTalkApp app, String myId, String myName) {
        this.app = app;
        this.myId = myId;
        this.myName = myName;
        adminNameLabel.setText("Welcome, " + myName);
        
        // Request admin statistics from server
        requestAdminData();
    }

    @FXML
    public void initialize() {
        // Set up table columns
        idColumn.setCellValueFactory(cellData -> cellData.getValue().idProperty());
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        roleColumn.setCellValueFactory(cellData -> cellData.getValue().roleProperty());
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        
        // Actions Column setup
        actionColumn.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue()));
        actionColumn.setCellFactory(param -> new javafx.scene.control.TableCell<UserData, UserData>() {
            private final Button deleteBtn = new Button("Delete");

            {
                deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 4; -fx-font-size: 11px; -fx-cursor: hand;");
                deleteBtn.setOnAction(event -> {
                    UserData data = getTableView().getItems().get(getIndex());
                    handleDeleteUser(data);
                });
            }

            @Override
            protected void updateItem(UserData item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteBtn);
                }
            }
        });

        usersTable.setItems(userData);
    }

    @FXML
    public void handleRefresh() {
        requestAdminData();
    }

    private void handleDeleteUser(UserData selected) {
        if (selected == null) return;

        // Show confirmation dialog
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete User");
        alert.setHeaderText("Delete " + selected.getName() + " (ID: " + selected.getId() + ")?");
        alert.setContentText("This will permanently remove the user and disconnect them if they are online.");

        alert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                if (app != null && app.getClient() != null) {
                    app.getClient().send("DELETE_USER|" + selected.getId());
                }
            }
        });
    }

    @FXML
    private void handleLogout() {
        if (app != null) {
            app.logout();
        }
    }

    private void requestAdminData() {
        if (app != null && app.getClient() != null) {
            app.getClient().send("GET_ADMIN_STATS");
            app.getClient().send("GET_ALL_USERS_ADMIN");
        }
    }

    // Called from SocketTalkApp when server responds with admin stats
    public void updateStatistics(int totalUsers, int onlineUsers, int totalMessages) {
        Platform.runLater(() -> {
            totalUsersLabel.setText(String.valueOf(totalUsers));
            onlineUsersLabel.setText(String.valueOf(onlineUsers));
            totalMessagesLabel.setText(String.valueOf(totalMessages));
        });
    }

    // Called from SocketTalkApp when server responds with all users
    public void updateAllUsers(String[] parts) {
        Platform.runLater(() -> {
            userData.clear();
            // parts format: ALL_USERS|ID:Name:Role:Status|...
            for (int i = 1; i < parts.length; i++) {
                String[] info = parts[i].split(":");
                if (info.length >= 4) {
                    String id = info[0];
                    String name = info[1];
                    String role = info[2];
                    String status = info[3];
                    userData.add(new UserData(id, name, role, status));
                }
            }
        });
    }

    // Inner class for table data
    public static class UserData {
        private final SimpleStringProperty id;
        private final SimpleStringProperty name;
        private final SimpleStringProperty role;
        private final SimpleStringProperty status;

        public UserData(String id, String name, String role, String status) {
            this.id = new SimpleStringProperty(id);
            this.name = new SimpleStringProperty(name);
            this.role = new SimpleStringProperty(role);
            this.status = new SimpleStringProperty(status);
        }

        public SimpleStringProperty idProperty() {
            return id;
        }

        public SimpleStringProperty nameProperty() {
            return name;
        }

        public SimpleStringProperty roleProperty() {
            return role;
        }

        public SimpleStringProperty statusProperty() {
            return status;
        }

        public String getId() {
            return id.get();
        }

        public String getName() {
            return name.get();
        }
    }
}
