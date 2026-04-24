import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    // Database credentials
    private static final String URL = "jdbc:mysql://localhost:3306/event_ticketing_db";
    private static final String USER = "root";
    private static final String PASSWORD = "admin123";

    /**
     * Establishes a connection to the MySQL database.
     * We add 'throws SQLException' so that the calling controller
     * knows if the connection failed.
     */
    public static Connection getConnection() throws SQLException {
        try {
            // This line ensures the MySQL Driver is loaded into memory
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Attempt to connect
            return DriverManager.getConnection(URL, USER, PASSWORD);

        } catch (ClassNotFoundException e) {
            // This happens if the MySQL Connector JAR is not in your Libraries
            throw new SQLException("MySQL JDBC Driver not found in project libraries.");
        }
    }

    /**
     * A small test method.
     * You can right-click this file and 'Run' it to verify connectivity.
     */
    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            if (conn != null) {
                System.out.println("Success: Connected to the database!");
            }
        } catch (SQLException e) {
            System.out.println("Error: Could not connect to the database.");
            e.printStackTrace();
        }
    }
}