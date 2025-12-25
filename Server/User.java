package Server;

public class User {
    private String id;
    private String name;
    private String password;
    private String role; // "STUDENT" or "TEACHER"

    public User(String id, String name, String password, String role) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.role = role;
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
    
    @Override
    public String toString() {
        return name + " (" + role + ")";
    }
}
