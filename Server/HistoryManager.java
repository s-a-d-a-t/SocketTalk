package Server;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HistoryManager {
    private static final String HISTORY_DIR = "history";

    public HistoryManager() {
        File dir = new File(HISTORY_DIR);
        if (!dir.exists()) {
            dir.mkdir();
        }
    }

    private String getFileName(String id1, String id2) {
        // Sort IDs to ensure consistency for private chats (e.g. 101-102 is same as 102-101)
        if (id1.compareTo(id2) < 0) {
            return HISTORY_DIR + "/chat_" + id1 + "_" + id2 + ".txt";
        } else {
            return HISTORY_DIR + "/chat_" + id2 + "_" + id1 + ".txt";
        }
    }
    
    private String getGroupFileName(String groupId) {
        return HISTORY_DIR + "/group_" + groupId + ".txt";
    }

    public synchronized void savePrivateMessage(String senderId, String receiverId, String message) {
        String fileName = getFileName(senderId, receiverId);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, true))) {
            bw.write(senderId + ": " + message);
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public synchronized void saveGroupMessage(String groupId, String senderId, String message) {
        String fileName = getGroupFileName(groupId);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, true))) {
            bw.write(senderId + ": " + message);
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getPrivateHistory(String id1, String id2) {
        String fileName = getFileName(id1, id2);
        return readHistory(fileName);
    }
    
    public List<String> getGroupHistory(String groupId) {
        String fileName = getGroupFileName(groupId);
        return readHistory(fileName);
    }

    private List<String> readHistory(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            return Collections.emptyList();
        }

        List<String> history = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                history.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return history;
    }
    
    // Get total message count for admin statistics
    public int getTotalMessageCount() {
        int count = 0;
        File historyDir = new File(HISTORY_DIR);
        if (historyDir.exists() && historyDir.isDirectory()) {
            File[] files = historyDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".txt")) {
                        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                            while (br.readLine() != null) {
                                count++;
                            }
                        } catch (IOException e) {
                            // Skip this file
                        }
                    }
                }
            }
        }
        return count;
    }

    
    // Get list of User IDs that the given user has a chat history with
    public List<String> getContactsForUser(String userId) {
        List<String> contacts = new ArrayList<>();
        File historyDir = new File(HISTORY_DIR);
        if (!historyDir.exists() || !historyDir.isDirectory()) {
            return contacts;
        }

        File[] files = historyDir.listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                if (name.startsWith("chat_") && name.endsWith(".txt")) {
                    // Format: chat_ID1_ID2.txt
                    String content = name.substring(5, name.length() - 4);
                    String[] parts = content.split("_");
                    if (parts.length == 2) {
                        String id1 = parts[0];
                        String id2 = parts[1];
                        
                        if (id1.equals(userId)) {
                            if (!contacts.contains(id2)) contacts.add(id2);
                        } else if (id2.equals(userId)) {
                            if (!contacts.contains(id1)) contacts.add(id1);
                        }
                    }
                }
            }
        }
        return contacts;
    }
}
