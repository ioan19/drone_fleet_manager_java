package dronefleet;

public class User {
    private int userId;
    private String username;
    private String role; // "admin", "operator", "tehnician"
    private String fullName;
    
    public User(int userId, String username, String role, String fullName) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.fullName = fullName;
    }
    
    public int getUserId() { 
        return userId; 
    }
    
    public String getUsername() { 
        return username; 
    }
    
    public String getRole() { 
        return role; 
    }
    
    public String getFullName() { 
        return fullName; 
    }
    
    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }
    
    public boolean isOperator() {
        return "operator".equalsIgnoreCase(role);
    }
    
    public boolean isTechnician() {
        return "tehnician".equalsIgnoreCase(role);
    }
    
    @Override
    public String toString() {
        return fullName + " (" + role + ")";
    }
}