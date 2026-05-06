import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.*;

public class AdminController {

    // Navigation
    @FXML private VBox paneEvents, paneUsers, paneAnalytics;
    @FXML private Button btnViewEvents, btnViewUsers, btnViewAnalytics;

    // Event Management
    @FXML private TextField txtEventName, txtEventDate, txtPrice;
    @FXML private ComboBox<String> comboVenue;
    @FXML private TableView<Event> tblAdminEvents;
    @FXML private TableColumn<Event, Integer> colId;
    @FXML private TableColumn<Event, String> colName, colVenue;
    @FXML private TableColumn<Event, Double> colPrice;

    // User Management
    @FXML private TableView<User> tblUsers;
    @FXML private TableColumn<User, String> colUserUsername, colUserFullName, colUserTier;
    @FXML private TableColumn<User, Double> colUserBalance;
    @FXML private TextField txtEditFullName, txtEditUsername, txtEditPassword;

    // Analytics
    @FXML private BarChart<String, Number> chartSales;
    @FXML private Label lblTotalRevenue;

    private String selectedOldUsername = "";

    @FXML
    public void initialize() {
        // Setup Combo
        comboVenue.setItems(FXCollections.observableArrayList("The Glass Pavilion", "Grand Atrium", "Indigo Concert Hall"));

        // Event Table
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colVenue.setCellValueFactory(new PropertyValueFactory<>("venue"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));

        // User Table
        colUserUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colUserFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colUserTier.setCellValueFactory(new PropertyValueFactory<>("tier"));
        colUserBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));

        tblUsers.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
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
    @FXML private void showEventsPane() { switchPane(paneEvents, btnViewEvents); }
    @FXML private void showUsersPane() { switchPane(paneUsers, btnViewUsers); loadUsers(); }
    @FXML private void showAnalyticsPane() { switchPane(paneAnalytics, btnViewAnalytics); loadAnalytics(); }

    private void switchPane(VBox activePane, Button activeBtn) {
        paneEvents.setVisible(false); paneUsers.setVisible(false); paneAnalytics.setVisible(false);
        activePane.setVisible(true);

        btnViewEvents.setStyle("-fx-background-color: transparent; -fx-text-fill: #c5cae9;");
        btnViewUsers.setStyle("-fx-background-color: transparent; -fx-text-fill: #c5cae9;");
        btnViewAnalytics.setStyle("-fx-background-color: transparent; -fx-text-fill: #c5cae9;");

        activeBtn.setStyle("-fx-background-color: #3949AB; -fx-text-fill: white; -fx-background-radius: 8;");
    }

    // --- ANALYTICS LOGIC ---
    private void loadAnalytics() {
        chartSales.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        double totalRev = 0;

        String query = "SELECT e.event_name, SUM(IFNULL(t.amount_paid, 0)) as revenue " +
                "FROM events e LEFT JOIN tickets t ON e.event_id = t.event_id " +
                "GROUP BY e.event_name";

        try (Connection conn = DatabaseConnection.getConnection();
             ResultSet rs = conn.createStatement().executeQuery(query)) {
            while (rs.next()) {
                String name = rs.getString("event_name");
                double rev = rs.getDouble("revenue");
                series.getData().add(new XYChart.Data<>(name, rev));
                totalRev += rev;
            }
            chartSales.getData().add(series);
            lblTotalRevenue.setText("$" + String.format("%.2f", totalRev));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- USER LOGIC ---
    private void loadUsers() {
        ObservableList<User> list = FXCollections.observableArrayList();
        try (Connection conn = DatabaseConnection.getConnection();
             ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM users WHERE role = 'User'")) {
            while (rs.next()) {
                list.add(new User(rs.getString("username"), rs.getString("full_name"), "User", rs.getString("tier"), rs.getDouble("balance")));
            }
            tblUsers.setItems(list);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleUpdateUser() {
        if (selectedOldUsername.isEmpty()) return;
        try (Connection conn = DatabaseConnection.getConnection()) {
            String pass = txtEditPassword.getText();
            PreparedStatement ps;
            if (pass.isEmpty()) {
                ps = conn.prepareStatement("UPDATE users SET full_name=?, username=? WHERE username=?");
                ps.setString(1, txtEditFullName.getText()); ps.setString(2, txtEditUsername.getText()); ps.setString(3, selectedOldUsername);
            } else {
                ps = conn.prepareStatement("UPDATE users SET full_name=?, username=?, password=? WHERE username=?");
                ps.setString(1, txtEditFullName.getText()); ps.setString(2, txtEditUsername.getText()); ps.setString(3, pass); ps.setString(4, selectedOldUsername);
            }
            ps.executeUpdate(); loadUsers();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleDeleteUser() {
        User sel = tblUsers.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE username = ?");
            ps.setString(1, sel.getUsername()); ps.executeUpdate(); loadUsers();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- EVENT LOGIC ---
    private void loadEvents() {
        ObservableList<Event> list = FXCollections.observableArrayList();
        try (Connection conn = DatabaseConnection.getConnection();
             ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM events")) {
            while (rs.next()) {
                list.add(new Event(rs.getInt("event_id"), rs.getString("event_name"), rs.getString("event_date"), rs.getString("venue"), rs.getDouble("price"), rs.getInt("available_seats")));
            }
            tblAdminEvents.setItems(list);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleAddEvent() {
        String name = txtEventName.getText(); String date = txtEventDate.getText();
        String venue = comboVenue.getValue(); String priceStr = txtPrice.getText();
        if (name.isEmpty() || venue == null || priceStr.isEmpty()) return;
        int cap = venue.equals("Indigo Concert Hall") ? 100 : venue.equals("Grand Atrium") ? 48 : 25;
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO events (event_name, event_date, venue, price, available_seats) VALUES (?,?,?,?,?)");
            ps.setString(1, name); ps.setString(2, date); ps.setString(3, venue); ps.setDouble(4, Double.parseDouble(priceStr)); ps.setInt(5, cap);
            ps.executeUpdate(); loadEvents();
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
        Stage s = (Stage) btnViewEvents.getScene().getWindow();
        s.setScene(new Scene(FXMLLoader.load(getClass().getResource("/login.fxml"))));
        s.setTitle("Login");
    }
}