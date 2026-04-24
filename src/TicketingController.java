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

public class TicketingController {

    @FXML private Label lblWelcome, lblViewTitle, lblTier, lblBalance;
    @FXML private TableView<Event> tblEvents;
    @FXML private TableColumn<Event, String> colName, colDate, colVenue, colSeats;
    @FXML private TableColumn<Event, Double> colPrice;
    @FXML private Button btnBrowse, btnMyTickets, btnBook, btnCancel;
    @FXML private TextField txtSearch;

    private ObservableList<Event> allEvents = FXCollections.observableArrayList();

    public void initialize() {
        refreshHeader();

        // Setup Columns
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colVenue.setCellValueFactory(new PropertyValueFactory<>("venue"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colSeats.setCellValueFactory(new PropertyValueFactory<>("seats"));

        // Search logic
        txtSearch.textProperty().addListener((obs, old, newValue) -> searchEvents(newValue));

        // Start on Browse mode
        handleBrowse();
    }

    private void refreshHeader() {
        User u = UserSession.getInstance().getCurrentUser();
        lblWelcome.setText("Welcome, " + u.getFullName());
        lblTier.setText(u.getTier().toUpperCase() + " TIER");
        lblBalance.setText("Wallet: $" + String.format("%.2f", u.getBalance()));
    }

    @FXML
    private void handleBrowse() {
        lblViewTitle.setText("CURRENTLY VIEWING: ALL EVENTS");

        // Fix Sidebar Styling
        btnBrowse.setStyle("-fx-background-color: #3949AB; -fx-background-radius: 8; -fx-text-fill: white;");
        btnMyTickets.setStyle("-fx-background-color: transparent; -fx-text-fill: #c5cae9;");

        btnBook.setDisable(false);
        btnCancel.setDisable(true);
        loadEvents("SELECT * FROM events");
    }

    @FXML
    private void handleMyTickets() {
        lblViewTitle.setText("CURRENTLY VIEWING: MY BOOKINGS");

        // Fix Sidebar Styling
        btnMyTickets.setStyle("-fx-background-color: #3949AB; -fx-background-radius: 8; -fx-text-fill: white;");
        btnBrowse.setStyle("-fx-background-color: transparent; -fx-text-fill: #c5cae9;");

        btnBook.setDisable(true);
        btnCancel.setDisable(false);

        String user = UserSession.getInstance().getCurrentUser().getUsername();
        loadEvents("SELECT e.* FROM events e JOIN tickets t ON e.event_id = t.event_id WHERE t.username = '" + user + "'");
    }

    private void loadEvents(String query) {
        allEvents.clear();
        User currentUser = UserSession.getInstance().getCurrentUser();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                double basePrice = rs.getDouble("price");
                // APPLY DISCOUNT HERE so the table shows the Member Price
                double memberPrice = currentUser.calculateDiscountedPrice(basePrice);

                allEvents.add(new Event(
                        rs.getInt("event_id"),
                        rs.getString("event_name"),
                        rs.getString("event_date"),
                        rs.getString("venue"),
                        memberPrice, // Use the calculated price
                        rs.getInt("available_seats")
                ));
            }
            tblEvents.setItems(allEvents);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void searchEvents(String text) {
        if (text == null || text.isEmpty()) { tblEvents.setItems(allEvents); return; }
        ObservableList<Event> filtered = FXCollections.observableArrayList();
        for (Event e : allEvents) {
            if (e.getName().toLowerCase().contains(text.toLowerCase())) filtered.add(e);
        }
        tblEvents.setItems(filtered);
    }

    @FXML
    private void handleBookTicket() {
        Event selected = tblEvents.getSelectionModel().getSelectedItem();
        User user = UserSession.getInstance().getCurrentUser();

        if (selected == null) {
            showSimpleAlert("Selection Required", "Please select an event first.");
            return;
        }

        try {
            // 1. OPEN THE SEAT MAP WINDOW
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/seat_selection.fxml"));
            Parent root = loader.load();
            SeatMapController controller = loader.getController();
            controller.setData(selected);

            Stage stage = new Stage();
            stage.setTitle("Select Seat - " + selected.getName());
            stage.setScene(new Scene(root));
            stage.showAndWait(); // Program pauses here until you close seat map

            String chosenSeat = controller.getSelectedSeat();
            if (chosenSeat == null) return; // User closed window without picking

            // 2. CALCULATE DIFFERENTIAL PRICING
            double basePrice = selected.getPrice();
            double seatPremium = chosenSeat.startsWith("A") ? 100.0 : 0.0; // Row A is VIP
            double subtotal = basePrice + seatPremium;
            double finalPrice = user.calculateDiscountedPrice(subtotal);

            // 3. SHOW SUMMARY POPUP
            Alert summary = new Alert(Alert.AlertType.CONFIRMATION);
            summary.setTitle("Booking Summary");
            summary.setHeaderText("Confirm your transaction");
            summary.setContentText(
                    "Event: " + selected.getName() + "\n" +
                            "Seat: " + chosenSeat + (seatPremium > 0 ? " (VIP Premium)" : "") + "\n\n" +
                            "Base Price: $" + basePrice + "\n" +
                            "Seat Premium: +$" + seatPremium + "\n" +
                            "Tier Discount (" + user.getTier() + "): -$" + (subtotal - finalPrice) + "\n" +
                            "----------------------------\n" +
                            "FINAL TOTAL: $" + String.format("%.2f", finalPrice)
            );

            if (summary.showAndWait().get() == ButtonType.OK) {
                // 4. CHECK BALANCE & EXECUTE SQL
                if (user.getBalance() < finalPrice) {
                    showSimpleAlert("Insufficient Funds", "You need more money!");
                    return;
                }

                processBooking(user, selected, chosenSeat, finalPrice);
            }

        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    private void processBooking(User u, Event e, String seat, double price) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // A. Insert Ticket
                PreparedStatement ps1 = conn.prepareStatement("INSERT INTO tickets (username, event_id, amount_paid, seat_number) VALUES (?, ?, ?, ?)");
                ps1.setString(1, u.getUsername());
                ps1.setInt(2, e.getId());
                ps1.setDouble(3, price);
                ps1.setString(4, seat);
                ps1.executeUpdate();

                // B. Update Seats & Balance
                conn.prepareStatement("UPDATE events SET available_seats = available_seats - 1 WHERE event_id = " + e.getId()).executeUpdate();
                conn.prepareStatement("UPDATE users SET balance = balance - " + price + " WHERE username = '" + u.getUsername() + "'").executeUpdate();

                conn.commit();
                u.setBalance(u.getBalance() - price);
                refreshHeader();
                handleBrowse();
                showSimpleAlert("Success", "Enjoy the show! Seat " + seat + " is yours.");
            } catch (SQLException ex) { conn.rollback(); throw ex; }
        }
    }

    @FXML
    private void handleCancelTicket() {
        Event sel = tblEvents.getSelectionModel().getSelectedItem();
        User u = UserSession.getInstance().getCurrentUser();
        if (sel == null) return;

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ResultSet rs = conn.createStatement().executeQuery("SELECT amount_paid FROM tickets WHERE username = '"+u.getUsername()+"' AND event_id = "+sel.getId()+" LIMIT 1");
                double refund = rs.next() ? rs.getDouble("amount_paid") : 0;

                conn.prepareStatement("DELETE FROM tickets WHERE username = '"+u.getUsername()+"' AND event_id = "+sel.getId()+" LIMIT 1").executeUpdate();
                conn.prepareStatement("UPDATE events SET available_seats = available_seats + 1 WHERE event_id = "+sel.getId()).executeUpdate();
                conn.prepareStatement("UPDATE users SET balance = balance + "+refund+" WHERE username = '"+u.getUsername()+"'").executeUpdate();

                conn.commit();
                u.setBalance(u.getBalance() + refund);
                refreshHeader();
                handleMyTickets();
            } catch (SQLException e) { conn.rollback(); }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleLogout() throws IOException {
        UserSession.cleanUserSession();
        Stage s = (Stage) lblWelcome.getScene().getWindow();
        s.setScene(new Scene(FXMLLoader.load(getClass().getResource("/login.fxml"))));
    }

    private void showSimpleAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}