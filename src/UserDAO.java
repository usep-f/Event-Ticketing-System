import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    public User login(String username, String password) {
        String query = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // IMPORTANT: You must include rs.getString("tier") and rs.getDouble("balance")
                // so the User object actually knows its own status!
                return new User(
                        rs.getString("username"),
                        rs.getString("full_name"),
                        rs.getString("role"),
                        rs.getString("tier"),   // Added
                        rs.getDouble("balance") // Added
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean registerUser(String username, String fullName, String password) {
        // Updated to include default values for tier and balance
        String query = "INSERT INTO users (username, full_name, password, role, tier, balance) VALUES (?, ?, ?, 'User', 'Standard', 1000.0)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, fullName);
            stmt.setString(3, password);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    // A quick test to make sure our login logic works
    public static void main(String[] args) {
        UserDAO dao = new UserDAO();
        User user = dao.login("admin", "admin123");

        if (user != null) {
            System.out.println("Login Success! Welcome " + user.getFullName());
            System.out.println("Your role is: " + user.getRole());
        } else {
            System.out.println("Login Failed: Incorrect username or password.");
        }
    }
}