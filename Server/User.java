package Server;

public class User {
    private String id;
    private String name;
    private String password;
    private String role; // "STUDENT", "TEACHER", or "ADMIN"
    private boolean isOnline;

    public User(String id, String name, String password, String role) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.role = role;
        this.isOnline = false;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }
    
    public String getRole() {
        return role;
    }
    
    public boolean isOnline() {
        return isOnline;
    }
    
    public void setOnline(boolean online) {
        this.isOnline = online;
    }
    
    @Override
    public String toString() {
        return name + " (" + role + ")";
    }
}
