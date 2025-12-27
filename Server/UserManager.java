package Server;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class UserManager {
    private static final String USER_FILE = "users.txt";
    // Map<ID, User>
    private final Map<String, User> users;

    public UserManager() {
        users = new HashMap<>();
        loadUsers();
    }

    private void loadUsers() {
        File file = new File(USER_FILE);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Format: ID,Name,Password,Role
                String[] parts = line.split(",");
                if (parts.length == 4) {
                    User user = new User(parts[0], parts[1], parts[2], parts[3]);
                    users.put(parts[0], user);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveUsers() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(USER_FILE))) {
            for (User user : users.values()) {
                bw.write(user.getId() + "," + user.getName() + "," + user.getPassword() + "," + user.getRole());
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized boolean register(String id, String name, String password, String role) {
        if (users.containsKey(id)) {
            return false;
        }
        User newUser = new User(id, name, password, role);
        users.put(id, newUser);
        saveUsers();
        return true;
    }

    public synchronized User login(String id, String password) {
        User user = users.get(id);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    public User getUser(String id) {
        return users.get(id);
    }
    
    public synchronized boolean deleteUser(String id) {
        if (users.containsKey(id)) {
            users.remove(id);
            saveUsers();
            return true;
        }
        return false;
    }

    public Map<String, User> getAllUsers() {
        return users;
    }
}
