package Server;

import java.io.*;
import java.util.*;

public class GroupManager {
    private static final String GROUP_FILE = "groups.txt";
    // Map<GroupID, Group>
    private final Map<String, Group> groups;

    public GroupManager() {
        groups = new HashMap<>();
        loadGroups();
    }

    private void loadGroups() {
        File file = new File(GROUP_FILE);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Format: ID,Name,MemberID1;MemberID2;MemberID3...
                String[] parts = line.split(",", 3);
                if (parts.length == 3) {
                    String id = parts[0];
                    String name = parts[1];
                    String[] members = parts[2].split(";");
                    List<String> memberList = new ArrayList<>(Arrays.asList(members));
                    groups.put(id, new Group(id, name, memberList));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void saveGroups() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(GROUP_FILE))) {
            for (Group group : groups.values()) {
                StringBuilder members = new StringBuilder();
                for (String m : group.getMemberIds()) {
                    if (members.length() > 0) members.append(";");
                    members.append(m);
                }
                bw.write(group.getId() + "," + group.getName() + "," + members.toString());
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized Group createGroup(String name, List<String> members) {
        String id = UUID.randomUUID().toString();
        Group group = new Group(id, name, members);
        groups.put(id, group);
        saveGroups();
        return group;
    }
    
    public synchronized boolean addMember(String groupId, String userId) {
        Group g = groups.get(groupId);
        if (g != null && !g.getMemberIds().contains(userId)) {
            g.addMember(userId);
            saveGroups();
            return true;
        }
        return false;
    }
    
    public synchronized boolean removeMember(String groupId, String userId) {
        Group g = groups.get(groupId);
        if (g != null && g.getMemberIds().contains(userId)) {
            g.removeMember(userId);
            saveGroups();
            return true;
        }
        return false;
    }

    public Group getGroup(String id) {
        return groups.get(id);
    }
    
    public List<Group> getGroupsForUser(String userId) {
        List<Group> result = new ArrayList<>();
        for (Group g : groups.values()) {
            if (g.getMemberIds().contains(userId)) {
                result.add(g);
            }
        }
        return result;
    }

    public Map<String, Group> getAllGroups() {
        return groups;
    }
}
