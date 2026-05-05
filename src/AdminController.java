import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.*;

public class AdminController {

    // Panes and Sidebar
    @FXML private VBox paneEvents, paneUsers;
    @FXML private Button btnViewEvents, btnViewUsers;

    // Event Management Fields
    @FXML private TextField txtEventName, txtEventDate, txtPrice;
    @FXML private ComboBox<String> comboVenue;
    @FXML private TableView<Event> tblAdminEvents;
    @FXML private TableColumn<Event, Integer> colId;
    @FXML private TableColumn<Event, String> colName, colVenue;
    @FXML private TableColumn<Event, Double> colPrice;

    // User Management Fields
    @FXML private TableView<User> tblUsers;
    @FXML private TableColumn<User, String> colUserUsername, colUserFullName, colUserTier;
    @FXML private TableColumn<User, Double> colUserBalance;
    @FXML private TextField txtEditFullName, txtEditUsername, txtEditPassword;

    private String selectedOldUsername = ""; // Tracks the username before it is edited

    @FXML
    public void initialize() {
        // 1. Setup Venue ComboBox
        comboVenue.setItems(FXCollections.observableArrayList("The Glass Pavilion", "Grand Atrium", "Indigo Concert Hall"));

        // 2. Setup Event Table
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colVenue.setCellValueFactory(new PropertyValueFactory<>("venue"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));

        // 3. Setup User Table
        colUserUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colUserFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colUserTier.setCellValueFactory(new PropertyValueFactory<>("tier"));
        colUserBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));

        // 4. Selection Listener for User Table
        tblUsers.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                txtEditFullName.setText(newVal.getFullName());
                txtEditUsername.setText(newVal.getUsername());
                selectedOldUsername = newVal.getUsername();
            }
        });

        loadEvents();
        loadUsers();
    }

    // --- NAVIGATION ---
    @FXML private void showEventsPane() {
        paneEvents.setVisible(true); paneUsers.setVisible(false);
        btnViewEvents.setStyle("-fx-background-color: #3949AB; -fx-text-fill: white;");
        btnViewUsers.setStyle("-fx-background-color: transparent; -fx-text-fill: #c5cae9;");
    }

    @FXML private void showUsersPane() {
        paneEvents.setVisible(false); paneUsers.setVisible(true);
        btnViewUsers.setStyle("-fx-background-color: #3949AB; -fx-text-fill: white;");
        btnViewEvents.setStyle("-fx-background-color: transparent; -fx-text-fill: #c5cae9;");
        loadUsers();
    }

    // --- USER MANAGEMENT ---
    private void loadUsers() {
        ObservableList<User> userList = FXCollections.observableArrayList();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE role = 'User'")) {
            while (rs.next()) {
                userList.add(new User(rs.getString("username"), rs.getString("full_name"),
                        "User", rs.getString("tier"), rs.getDouble("balance")));
            }
            tblUsers.setItems(userList);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleUpdateUser() {
        if (selectedOldUsername.isEmpty()) return;
        String nName = txtEditFullName.getText();
        String nUser = txtEditUsername.getText();
        String nPass = txtEditPassword.getText();

        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement ps;
            if (nPass.isEmpty()) {
                ps = conn.prepareStatement("UPDATE users SET full_name = ?, username = ? WHERE username = ?");
                ps.setString(1, nName); ps.setString(2, nUser); ps.setString(3, selectedOldUsername);
            } else {
                ps = conn.prepareStatement("UPDATE users SET full_name = ?, username = ?, password = ? WHERE username = ?");
                ps.setString(1, nName); ps.setString(2, nUser); ps.setString(3, nPass); ps.setString(4, selectedOldUsername);
            }
            ps.executeUpdate();
            showAlert("Success", "User credentials updated successfully.");
            loadUsers();
        } catch (SQLException e) {
            showAlert("Error", "Could not update user. Username may already exist.");
        }
    }

    @FXML
    private void handleDeleteUser() {
        User sel = tblUsers.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Permanently delete " + sel.getFullName() + "?", ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().get() == ButtonType.YES) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE username = ?");
                ps.setString(1, sel.getUsername());
                ps.executeUpdate();
                loadUsers();
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    // --- EVENT MANAGEMENT ---
    private void loadEvents() {
        ObservableList<Event> list = FXCollections.observableArrayList();
        try (Connection conn = DatabaseConnection.getConnection();
             ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM events")) {
            while (rs.next()) {
                list.add(new Event(rs.getInt("event_id"), rs.getString("event_name"),
                        rs.getString("event_date"), rs.getString("venue"), rs.getDouble("price"), rs.getInt("available_seats")));
            }
            tblAdminEvents.setItems(list);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleAddEvent() {
        String name = txtEventName.getText();
        String date = txtEventDate.getText();
        String venue = comboVenue.getValue();
        String priceStr = txtPrice.getText();
        if (name.isEmpty() || venue == null || priceStr.isEmpty()) return;

        int cap = venue.equals("Indigo Concert Hall") ? 100 : venue.equals("Grand Atrium") ? 48 : 25;
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO events (event_name, event_date, venue, price, available_seats) VALUES (?,?,?,?,?)");
            ps.setString(1, name); ps.setString(2, date); ps.setString(3, venue);
            ps.setDouble(4, Double.parseDouble(priceStr)); ps.setInt(5, cap);
            ps.executeUpdate();
            loadEvents();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void handleDeleteEvent() {
        Event sel = tblAdminEvents.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.prepareStatement("DELETE FROM events WHERE event_id = " + sel.getId()).executeUpdate();
            loadEvents();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleLogout() throws IOException {
        UserSession.cleanUserSession();
        Stage s = (Stage) tblAdminEvents.getScene().getWindow();
        s.setScene(new Scene(FXMLLoader.load(getClass().getResource("/login.fxml"))));
        s.setTitle("Login");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, content);
        alert.setTitle(title);
        alert.showAndWait();
    }
}