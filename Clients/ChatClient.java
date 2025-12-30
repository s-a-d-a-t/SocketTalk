package Clients;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChatClient {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean running = false;
    private MessageListener listener;
    
    public interface MessageListener {
        void onMessage(String command, String... parts);
    }

    public void setListener(MessageListener listener) {
        this.listener = listener;
    }

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        running = true;
        new Thread(this::listen).start();
    }

    private void listen() {
        try {
            while (running) {
                String msg = in.readUTF();
                System.out.println("Client Rcv: " + msg);
                if (listener != null) {
                    String[] parts = msg.split("\\|");
                    if (parts.length > 0) {
                        listener.onMessage(parts[0], parts);
                    }
                }
            }
        } catch (IOException e) {
            running = false;
            // e.printStackTrace();
        }
    }

    public void send(String msg) {
        if (out != null) {
            try {
                out.writeUTF(msg);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void login(String id, String password) {
        send("LOGIN|" + id + "|" + password);
    }

    public void register(String id, String name, String password, String role) {
        send("REGISTER|" + id + "|" + name + "|" + password + "|" + role);
    }

    public void sendPrivateMessage(String targetId, String message) {
        send("MSG_PRIVATE|" + targetId + "|" + message);
    }
    
    public void requestUserList() {
        send("GET_USERS");
    }
    
    public void requestHistory(String targetId) {
        send("GET_HISTORY|" + targetId);
    }
    
    public void createGroup(String name, String memberIds) {
        // memberIds is comma separated
        send("CREATE_GROUP|" + name + "|" + memberIds);
    }
    
    public void addGroupMember(String groupId, String newMemberId) {
        send("ADD_TO_GROUP|" + groupId + "|" + newMemberId);
    }
    
    public void leaveGroup(String groupId) {
        send("LEAVE_GROUP|" + groupId);
    }
    
    public void startConversation(String targetId) {
        send("START_CHAT|" + targetId);
    }
    
    public void sendGroupMessage(String groupId, String message) {
        send("MSG_GROUP|" + groupId + "|" + message);
    }
    
    public void requestGroups() {
        send("GET_GROUPS");
    }
}
