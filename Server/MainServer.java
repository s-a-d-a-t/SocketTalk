package Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MainServer {
    private static final int PORT = 5555;
    private static final Map<String, ClientHandler> activeClients = new ConcurrentHashMap<>();
    private static final UserManager userManager = new UserManager();
    private static final HistoryManager historyManager = new HistoryManager();

    public static void main(String[] args) {
        System.out.println("Server started on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New connection: " + socket);
                new Thread(new ClientHandler(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private DataInputStream in;
        private DataOutputStream out;
        private String userId;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());

                while (true) {
                    String req = in.readUTF();
                    System.out.println("Received: " + req);
                    handleRequest(req);
                }
            } catch (IOException e) {
                // Client disconnected
            } finally {
                close();
            }
        }

        private void handleRequest(String req) throws IOException {
            String[] parts = req.split("\\|");
            String command = parts[0];

            switch (command) {
                case "LOGIN":
                    handleLogin(parts);
                    break;
                case "REGISTER":
                    handleRegister(parts);
                    break;
                case "MSG_PRIVATE":
                    handlePrivateMessage(parts);
                    break;
                case "GET_USERS":
                    handleGetUsers();
                    break;
                case "GET_HISTORY":
                    handleGetHistory(parts);
                    break;
                default:
                    // send("ERROR|Unknown command");
            }
        }

        private void handleLogin(String[] parts) throws IOException {
            // LOGIN|ID|PASSWORD
            if (parts.length < 3) return;
            String id = parts[1];
            String pass = parts[2];

            User user = userManager.login(id, pass);
            if (user != null) {
                this.userId = id;
                activeClients.put(id, this);
                send("LOGIN_SUCCESS|" + user.getName() + "|" + user.getRole());
                System.out.println("User logged in: " + user.getName());
            } else {
                send("LOGIN_FAIL|Invalid credentials");
            }
        }

        private void handleRegister(String[] parts) throws IOException {
            // REGISTER|ID|NAME|PASS|ROLE
            if (parts.length < 5) return;
            String id = parts[1];
            String name = parts[2];
            String pass = parts[3];
            String role = parts[4];

            boolean success = userManager.register(id, name, pass, role);
            if (success) {
                send("REGISTER_SUCCESS");
            } else {
                send("REGISTER_FAIL|User ID already exists");
            }
        }

        private void handlePrivateMessage(String[] parts) throws IOException {
            // MSG_PRIVATE|TARGET_ID|MESSAGE
            if (parts.length < 3) return;
            String targetId = parts[1];
            String msg = parts[2];

            historyManager.savePrivateMessage(this.userId, targetId, msg);

            ClientHandler target = activeClients.get(targetId);
            if (target != null) {
                target.send("MSG_PRIVATE|" + this.userId + "|" + msg);
            }
            // Send acknowledgement to sender (optional, can be handled by UI optimistically)
        }

        private void handleGetUsers() throws IOException {
             StringBuilder sb = new StringBuilder("USER_LIST");
             for (User u : userManager.getAllUsers().values()) {
                 // Don't send self in the list
                 if (!u.getId().equals(this.userId)) {
                      sb.append("|").append(u.getId()).append(":").append(u.getName()).append(":").append(u.getRole());
                 }
             }
             send(sb.toString());
        }

        private void handleGetHistory(String[] parts) throws IOException {
            // GET_HISTORY|TARGET_ID
            if (parts.length < 2) return;
            String targetId = parts[1];
            List<String> history = historyManager.getPrivateHistory(this.userId, targetId);
            for(String line : history) {
                send("MSG_HISTORY|" + targetId + "|" + line);
            }
        }

        public void send(String msg) {
            try {
                out.writeUTF(msg);
                out.flush();
            } catch (IOException e) {
                close();
            }
        }

        private void close() {
            try {
                if (userId != null) {
                    activeClients.remove(userId);
                    System.out.println("User removed: " + userId);
                }
                socket.close();
            } catch (IOException ignored) {}
        }
    }
}
