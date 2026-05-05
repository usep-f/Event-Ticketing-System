import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.*;

public class AdminController {

    // Form fields
    @FXML private TextField txtEventName, txtEventDate, txtPrice;
    @FXML private ComboBox<String> comboVenue;

    // Table fields
    @FXML private TableView<Event> tblAdminEvents;
    @FXML private TableColumn<Event, Integer> colId;
    @FXML private TableColumn<Event, String> colName, colVenue;
    @FXML private TableColumn<Event, Double> colPrice;

    private ObservableList<Event> adminEventList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // 1. Setup Venue Blueprints
        comboVenue.setItems(FXCollections.observableArrayList(
                "The Glass Pavilion",
                "Grand Atrium",
                "Indigo Concert Hall"
        ));

        // 2. Setup Table Columns (Mapping to Event.java getters)
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colVenue.setCellValueFactory(new PropertyValueFactory<>("venue"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));

        // 3. Initial Load
        loadEvents();
    }

    private void loadEvents() {
        adminEventList.clear();
        String query = "SELECT * FROM events";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                adminEventList.add(new Event(
                        rs.getInt("event_id"),
                        rs.getString("event_name"),
                        rs.getString("event_date"),
                        rs.getString("venue"),
                        rs.getDouble("price"),
                        rs.getInt("available_seats")
                ));
            }
            tblAdminEvents.setItems(adminEventList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddEvent() {
        String name = txtEventName.getText();
        String date = txtEventDate.getText();
        String venue = comboVenue.getValue();
        String priceStr = txtPrice.getText();

        if (name.isEmpty() || date.isEmpty() || venue == null || priceStr.isEmpty()) {
            showAlert("Input Error", "Please fill in all fields.");
            return;
        }

        try {
            double price = Double.parseDouble(priceStr);

            // Logic: Set capacity based on venue choice
            int capacity = switch (venue) {
                case "The Glass Pavilion" -> 25;
                case "Grand Atrium" -> 48;
                case "Indigo Concert Hall" -> 100;
                default -> 0;
            };

            String sql = "INSERT INTO events (event_name, event_date, venue, price, available_seats) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, name);
                ps.setString(2, date);
                ps.setString(3, venue);
                ps.setDouble(4, price);
                ps.setInt(5, capacity);

                ps.executeUpdate();
                showAlert("Success", "Event '" + name + "' added to database.");

                // Clear fields and refresh
                txtEventName.clear();
                txtEventDate.clear();
                txtPrice.clear();
                loadEvents();
            }
        } catch (NumberFormatException e) {
            showAlert("Format Error", "Please enter a valid number for price.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDeleteEvent() {
        Event selected = tblAdminEvents.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selection Error", "Please select an event from the table first.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + selected.getName() + "?", ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().get() == ButtonType.YES) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                PreparedStatement ps = conn.prepareStatement("DELETE FROM events WHERE event_id = ?");
                ps.setInt(1, selected.getId());
                ps.executeUpdate();
                loadEvents();
            } catch (SQLException e) {
                showAlert("Error", "Cannot delete event. It may have active bookings.");
            }
        }
    }

    @FXML
    private void handleLogout() {
        try {
            UserSession.cleanUserSession();
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) tblAdminEvents.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Ticketing System Login");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}