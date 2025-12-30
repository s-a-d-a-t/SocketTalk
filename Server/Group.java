package Server;

import java.util.ArrayList;
import java.util.List;

public class Group {
    private String id;
    private String name;
    private List<String> memberIds;

    public Group(String id, String name, List<String> memberIds) {
        this.id = id;
        this.name = name;
        this.memberIds = memberIds;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getMemberIds() {
        return memberIds;
    }
    
    public void addMember(String userId) {
        if (!memberIds.contains(userId)) {
            memberIds.add(userId);
        }
    }
    
    public void removeMember(String userId) {
        memberIds.remove(userId);
    }
}
