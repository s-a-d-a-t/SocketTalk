package Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.Arrays;

public class MainServer {
    private static final int PORT = 5555;
    private static final Map<String, ClientHandler> activeClients = new ConcurrentHashMap<>();
    private static final UserManager userManager = new UserManager();
    private static final HistoryManager historyManager = new HistoryManager();
    private static final GroupManager groupManager = new GroupManager();

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
                case "GET_ALL_USERS_ADMIN":
                    handleGetAllUsersAdmin();
                    break;
                case "GET_ADMIN_STATS":
                    handleGetAdminStats();
                    break;
                case "DELETE_USER":
                    handleDeleteUser(parts);
                    break;
                case "CREATE_GROUP":
                    handleCreateGroup(parts);
                    break;
                case "ADD_TO_GROUP":
                    handleAddToGroup(parts);
                    break;
                case "LEAVE_GROUP":
                    handleLeaveGroup(parts);
                    break;
                case "START_CHAT":
                    handleStartChat(parts);
                    break;
                case "MSG_GROUP":
                    handleGroupMessage(parts);
                    break;
                case "GET_GROUPS":
                    handleGetGroups();
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
                // If this socket was already logged in as someone else, clean them up first
                if (this.userId != null) {
                    System.out.println("Switching user from " + this.userId + " to " + id);
                    activeClients.remove(this.userId);
                    User oldUser = userManager.getUser(this.userId);
                    if (oldUser != null) oldUser.setOnline(false);
                    broadcastUserStatus(this.userId, false);
                }

                this.userId = id;
                activeClients.put(id, this);
                user.setOnline(true); // Set user online
                send("LOGIN_SUCCESS|" + user.getId() + "|" + user.getName() + "|" + user.getRole());
                System.out.println("User logged in: " + user.getName());
                
                // Broadcast to all clients that this user is now online
                broadcastUserStatus(id, true);
                
                // Automaticaly send group list on login
                handleGetGroups();
                
                // Send Contact List (Only filtered history)
                sendContactList();
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
        
        private void handleCreateGroup(String[] parts) throws IOException {
            // CREATE_GROUP|NAME|ID1,ID2,ID3...
            if (parts.length < 3) return;
            String name = parts[1];
            String membersStr = parts[2];
            
            List<String> members = new ArrayList<>();
            for (String m : membersStr.split(",")) {
                members.add(m.trim());
            }
            
            // Ensure creator is in the group
            if (!members.contains(this.userId)) {
                members.add(this.userId);
            }
            
            Group g = groupManager.createGroup(name, members);
            
            // Notify all online members
            for (String mid : members) {
                ClientHandler ch = activeClients.get(mid);
                if (ch != null) {
                    // Send minimal info first or trigger a refresh
                    ch.handleGetGroups(); 
                }
            }
        }
        
        private void handleAddToGroup(String[] parts) throws IOException {
            // ADD_TO_GROUP|GROUP_ID|NEW_MEMBER_ID
            if (parts.length < 3) return;
            String groupId = parts[1];
            String newMemberId = parts[2].trim();
            
            Group g = groupManager.getGroup(groupId);
            if (g == null) return; 
            
            if (!g.getMemberIds().contains(this.userId)) return;
            
            boolean added = groupManager.addMember(groupId, newMemberId);
            
            if (added) {
                // ... success handling ...
                String adderName = userManager.getUser(this.userId).getName();
                String newMemberName = userManager.getUser(newMemberId).getName();
                
                String sysMsg = "System: " + adderName + " added " + newMemberName + " to the group.";
                historyManager.saveGroupMessage(groupId, "SYSTEM", sysMsg);
                
                // Notify new member
                ClientHandler newClient = activeClients.get(newMemberId);
                if (newClient != null) newClient.handleGetGroups();

                // Notify all members
                for (String mid : g.getMemberIds()) {
                    ClientHandler ch = activeClients.get(mid);
                    if (ch != null) {
                         ch.send("MSG_GROUP|" + groupId + "|SYSTEM|" + sysMsg);
                    }
                }
            } else {
                 // Member likely already exists
                 send("MSG_PRIVATE|SYSTEM|User " + newMemberId + " is already in the group.");
            }
        }
        
        private void handleLeaveGroup(String[] parts) throws IOException {
             // LEAVE_GROUP|GROUP_ID
             if (parts.length < 2) return;
             String groupId = parts[1];
             
             Group g = groupManager.getGroup(groupId);
             if (g == null) return;
             
             boolean removed = groupManager.removeMember(groupId, this.userId);
             if (removed) {
                 // Notify group
                 String leaverName = userManager.getUser(this.userId).getName();
                 String sysMsg = "System: " + leaverName + " left the group.";
                 historyManager.saveGroupMessage(groupId, "SYSTEM", sysMsg);
                 
                 for (String mid : g.getMemberIds()) {
                     ClientHandler ch = activeClients.get(mid);
                     if (ch != null) {
                         ch.send("MSG_GROUP|" + groupId + "|SYSTEM|" + sysMsg);
                     }
                 }
                 
                 // Refresh leaver's list (User is removed from group file, so GetGroups won't show it)
                 handleGetGroups();
             }
        }
        
        private void handleGroupMessage(String[] parts) throws IOException {
            // MSG_GROUP|GROUP_ID|MESSAGE
            if (parts.length < 3) return;
            String groupId = parts[1];
            String msg = parts[2];
            
            Group g = groupManager.getGroup(groupId);
            if (g == null) return;
            
            historyManager.saveGroupMessage(groupId, this.userId, msg);
            
            // Distribute to all members of group
            for (String memberId : g.getMemberIds()) {
                // Don't echo back to sender if you handle persistence locally, 
                // but typically socket apps echo or sender UI handles it. 
                // Let's echo to everyone except sender if sender logic appends locally, 
                // BUT sender needs to know it worked? 
                // For simplicity: Send to ALL, client ignores self OR client appends self and ignores incoming self.
                // We will send to everyone ELSE for now, sender handles optimistic UI.
                if (!memberId.equals(this.userId)) {
                    ClientHandler ch = activeClients.get(memberId);
                    if (ch != null) {
                        // MSG_GROUP|GROUP_ID|SENDER_ID|MESSAGE
                        ch.send("MSG_GROUP|" + groupId + "|" + this.userId + "|" + msg);
                    }
                }
            }
        }
        
        private void sendContactList() {
            List<String> contactIds = historyManager.getContactsForUser(this.userId);
            StringBuilder sb = new StringBuilder("USER_LIST");
            
            for (String cid : contactIds) {
                User u = userManager.getUser(cid);
                if (u != null) {
        sb.append("|").append(u.getId())
          .append(":").append(u.getName())
          .append(":").append(u.getRole())
          .append(":").append(u.isOnline() ? "ONLINE" : "OFFLINE");
                }
            }
            send(sb.toString());
        }

        private void handleStartChat(String[] parts) throws IOException {
             // START_CHAT|TARGET_ID
             if (parts.length < 2) return;
             String targetId = parts[1].trim();
             
             User target = userManager.getUser(targetId);
             if (target != null && !targetId.equals(this.userId)) {
                 // 1. Send update to ME
                 String myUpdate = "|"+ target.getId()+":"+target.getName()+":"+target.getRole()+":"+(target.isOnline()?"ONLINE":"OFFLINE");
                 send("USER_LIST" + myUpdate); 
                 
                 // 2. Send update to TARGET (so I appear in their list)
                 ClientHandler targetClient = activeClients.get(targetId);
                 if (targetClient != null) {
                     User me = userManager.getUser(this.userId);
                     String targetUpdate = "|"+ me.getId()+":"+me.getName()+":"+me.getRole()+":"+(me.isOnline()?"ONLINE":"OFFLINE");
                     targetClient.send("USER_LIST" + targetUpdate);
                 }
                 
                 // 3. Create a starter history file
                 historyManager.savePrivateMessage(this.userId, targetId, "Conversation started.");
             } else {
                 send("MSG_PRIVATE|SYSTEM|User ID not found.");
             }
        }

        private void handleGetGroups() {
            List<Group> myGroups = groupManager.getGroupsForUser(this.userId);
            StringBuilder sb = new StringBuilder("GROUP_LIST");
            for (Group g : myGroups) {
                sb.append("|").append(g.getId()).append(":").append(g.getName());
            }
            send(sb.toString()); 
        }

        private void handleGetUsers() throws IOException {
             // Replaced global list with contact list
             sendContactList();
        }

        private void handleGetHistory(String[] parts) throws IOException {
            // GET_HISTORY|TARGET_ID
            // TARGET_ID could be a UserID or a GroupID.
            // We need a way to distinguish. 
            // Current system: Group UUIDs vs User custom IDs.
            // Simple check: if groupManager has it, it's a group.
            if (parts.length < 2) return;
            String targetId = parts[1];
            
            Group g = groupManager.getGroup(targetId);
            if (g != null) {
                // Group History
                 List<String> history = historyManager.getGroupHistory(targetId);
                 for(String line : history) {
                     // History Format: "senderId: message"
                     // We send: MSG_HISTORY|GROUP_ID|senderId: message
                     send("MSG_HISTORY|" + targetId + "|" + line);
                 }
            } else {
                // Private History
                List<String> history = historyManager.getPrivateHistory(this.userId, targetId);
                for(String line : history) {
                    send("MSG_HISTORY|" + targetId + "|" + line);
                }
            }
        }
        
        private void handleGetAdminStats() throws IOException {
            // GET_ADMIN_STATS - return total users, online users, total messages
            int totalUsers = userManager.getAllUsers().size();
            int onlineUsers = activeClients.size();
            int totalMessages = historyManager.getTotalMessageCount();
            
            send("ADMIN_STATS|" + totalUsers + "|" + onlineUsers + "|" + totalMessages);
        }
        
        private void handleGetAllUsersAdmin() throws IOException {
            // GET_ALL_USERS_ADMIN - return all users including self
            StringBuilder sb = new StringBuilder("ALL_USERS");
            for (User u : userManager.getAllUsers().values()) {
                if ("ADMIN".equals(u.getRole())) continue; // Admins can't see or delete other admins
                sb.append("|").append(u.getId()).append(":").append(u.getName())
                  .append(":").append(u.getRole()).append(":").append(u.isOnline() ? "ONLINE" : "OFFLINE");
            }
            send(sb.toString());
        }

        private void handleDeleteUser(String[] parts) throws IOException {
            // DELETE_USER|ID
            if (parts.length < 2) return;
            String targetId = parts[1];

            // 1. Remove from UserManager (file/memory)
            boolean success = userManager.deleteUser(targetId);
            
            if (success) {
                // 2. If online, disconnect them
                ClientHandler target = activeClients.get(targetId);
                if (target != null) {
                    target.send("ACCOUNT_DELETED|Your account has been removed by an admin.");
                    target.close();
                }
                
                send("DELETE_SUCCESS|" + targetId);
                System.out.println("Admin deleted user: " + targetId);
                
                // 3. Broadcast status update
                broadcastUserStatus(targetId, false);
            } else {
                send("DELETE_FAIL|User not found");
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

        private synchronized void close() {
            if (userId == null) {
                try { socket.close(); } catch (IOException ignored) {}
                return;
            }
            
            String idToClose = userId;
            userId = null; // Mark as closed early to prevent recursion
            
            activeClients.remove(idToClose);
            User user = userManager.getUser(idToClose);
            if (user != null) {
                user.setOnline(false);
                broadcastUserStatus(idToClose, false);
            }
            System.out.println("User removed: " + idToClose);
            
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
        
        // Broadcast user status to all connected clients
        private void broadcastUserStatus(String userId, boolean isOnline) {
            String statusMsg = "USER_STATUS|" + userId + "|" + (isOnline ? "ONLINE" : "OFFLINE");
            for (ClientHandler client : activeClients.values()) {
                client.send(statusMsg);
            }
        }
    }
}
