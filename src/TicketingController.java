import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

public class TicketingController {

    @FXML private Label lblWelcome, lblTableTitle, lblTier, lblBalance;
    @FXML private TextField txtSearch;
    @FXML private Button btnBrowse, btnMyTickets, btnBook, btnCancel, btnDownload;

    // Table 1: Browse Events
    @FXML private TableView<Event> tblEvents;
    @FXML private TableColumn<Event, String> colName, colDate, colVenue, colSeats;
    @FXML private TableColumn<Event, Double> colPrice;

    // Table 2: My Booked Tickets
    @FXML private TableView<Ticket> tblBookings;
    @FXML private TableColumn<Ticket, Integer> colTicketId;
    @FXML private TableColumn<Ticket, String> colBookedEvent, colBookedDate, colBookedSeat;
    @FXML private TableColumn<Ticket, Double> colPaid;

    private ObservableList<Event> allEvents = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        refreshHeader();

        // Setup Columns for Event Table
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colVenue.setCellValueFactory(new PropertyValueFactory<>("venue"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colSeats.setCellValueFactory(new PropertyValueFactory<>("seats"));

        // Setup Columns for Bookings Table
        colTicketId.setCellValueFactory(new PropertyValueFactory<>("ticketId"));
        colBookedEvent.setCellValueFactory(new PropertyValueFactory<>("eventName"));
        colBookedDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colBookedSeat.setCellValueFactory(new PropertyValueFactory<>("seatNumber"));
        colPaid.setCellValueFactory(new PropertyValueFactory<>("amountPaid"));

        // Real-time Search Listener
        txtSearch.textProperty().addListener((obs, old, val) -> searchEvents(val));

        // Default View
        handleBrowse();
    }

    private void refreshHeader() {
        if (UserSession.getInstance() != null) {
            User u = UserSession.getInstance().getCurrentUser();
            lblWelcome.setText("Welcome, " + u.getFullName());
            lblTier.setText(u.getTier().toUpperCase() + " TIER");
            lblBalance.setText("Wallet: $" + String.format("%.2f", u.getBalance()));
        }
    }

    @FXML
    private void handleBrowse() {
        lblTableTitle.setText("Upcoming Experiences");
        tblEvents.setVisible(true);
        tblBookings.setVisible(false);
        btnBook.setVisible(true);
        btnCancel.setVisible(false);
        btnDownload.setVisible(false);

        btnBrowse.setStyle("-fx-background-color: #3949AB; -fx-text-fill: white; -fx-background-radius: 8;");
        btnMyTickets.setStyle("-fx-background-color: transparent; -fx-text-fill: #c5cae9;");

        loadEvents("SELECT * FROM events");
    }

    @FXML
    private void handleMyTickets() {
        lblTableTitle.setText("My Booked Tickets");
        tblEvents.setVisible(false);
        tblBookings.setVisible(true);
        btnBook.setVisible(false);
        btnCancel.setVisible(true);
        btnDownload.setVisible(true);

        btnMyTickets.setStyle("-fx-background-color: #3949AB; -fx-text-fill: white; -fx-background-radius: 8;");
        btnBrowse.setStyle("-fx-background-color: transparent; -fx-text-fill: #c5cae9;");

        loadMyTickets();
    }

    private void loadEvents(String query) {
        allEvents.clear();
        User u = UserSession.getInstance().getCurrentUser();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                // Apply individual user discount to the price shown in the table
                double memberPrice = u.calculateDiscountedPrice(rs.getDouble("price"));
                allEvents.add(new Event(rs.getInt("event_id"), rs.getString("event_name"),
                        rs.getString("event_date"), rs.getString("venue"), memberPrice, rs.getInt("available_seats")));
            }
            tblEvents.setItems(allEvents);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadMyTickets() {
        ObservableList<Ticket> myList = FXCollections.observableArrayList();
        User u = UserSession.getInstance().getCurrentUser();
        String query = "SELECT t.ticket_id, t.event_id, e.event_name, e.venue, e.event_date, t.seat_number, t.amount_paid " +
                "FROM tickets t JOIN events e ON t.event_id = e.event_id WHERE t.username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, u.getUsername());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                myList.add(new Ticket(rs.getInt("ticket_id"), rs.getInt("event_id"), rs.getString("event_name"),
                        rs.getString("venue"), rs.getString("event_date"), rs.getString("seat_number"),
                        rs.getDouble("amount_paid"), u.getFullName()));
            }
            tblBookings.setItems(myList);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void searchEvents(String text) {
        if (text == null || text.isEmpty()) { tblEvents.setItems(allEvents); return; }
        ObservableList<Event> filtered = FXCollections.observableArrayList();
        for (Event e : allEvents) {
            if (e.getName().toLowerCase().contains(text.toLowerCase()) || e.getVenue().toLowerCase().contains(text.toLowerCase())) {
                filtered.add(e);
            }
        }
        tblEvents.setItems(filtered);
    }

    @FXML
    private void handleBookTicket() {
        Event selected = tblEvents.getSelectionModel().getSelectedItem();
        User user = UserSession.getInstance().getCurrentUser();

        if (selected == null) {
            showSimpleAlert("Selection Required", "Please select an event from the table.");
            return;
        }

        if (selected.getSeatsNum() <= 0) {
            showSimpleAlert("Sold Out", "Sorry, this event is already sold out!");
            return;
        }

        try {
            // 1. Open Dynamic Seat Map
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/seat_selection.fxml"));
            Parent root = loader.load();
            SeatMapController smc = loader.getController();

            // Pass the current user so the map can calculate tier-specific prices
            smc.setData(selected, user);

            Stage stage = new Stage();
            stage.setTitle("Select Seat - " + selected.getName());
            stage.setScene(new Scene(root));
            stage.showAndWait(); // Wait for user to pick a seat

            String chosenSeat = smc.getSelectedSeat();
            double calculatedPrice = smc.getFinalPrice();

            if (chosenSeat == null) return; // User closed without picking

            // 2. Summary & Confirmation
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Booking Confirmation");
            confirm.setHeaderText("Verify Transaction Details");
            confirm.setContentText("Event: " + selected.getName() +
                    "\nSeat: " + chosenSeat +
                    "\nFinal Total (Discount Applied): $" + String.format("%.2f", calculatedPrice));

            if (confirm.showAndWait().get() == ButtonType.OK) {
                if (user.getBalance() < calculatedPrice) {
                    showSimpleAlert("Insufficient Funds", "You do not have enough credits in your wallet.");
                    return;
                }
                processTransaction(user, selected, chosenSeat, calculatedPrice);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void processTransaction(User u, Event e, String seat, double price) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Enable Transaction atomic safety
            try {
                // A. Record the ticket with seat number
                PreparedStatement ps1 = conn.prepareStatement("INSERT INTO tickets (username, event_id, amount_paid, seat_number) VALUES (?,?,?,?)");
                ps1.setString(1, u.getUsername());
                ps1.setInt(2, e.getId());
                ps1.setDouble(3, price);
                ps1.setString(4, seat);
                ps1.executeUpdate();

                // B. Reduce capacity
                PreparedStatement ps2 = conn.prepareStatement("UPDATE events SET available_seats = available_seats - 1 WHERE event_id = ?");
                ps2.setInt(1, e.getId());
                ps2.executeUpdate();

                // C. Deduct Wallet
                PreparedStatement ps3 = conn.prepareStatement("UPDATE users SET balance = balance - ? WHERE username = ?");
                ps3.setDouble(1, price);
                ps3.setString(2, u.getUsername());
                ps3.executeUpdate();

                conn.commit(); // Save all changes at once

                u.setBalance(u.getBalance() - price); // Update local object
                refreshHeader();
                handleBrowse();
                showSimpleAlert("Success!", "Booking confirmed. You can find your ticket in 'My Bookings'.");

            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    @FXML
    private void handleCancelTicket() {
        Ticket selected = tblBookings.getSelectionModel().getSelectedItem();
        User u = UserSession.getInstance().getCurrentUser();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.WARNING, "Cancel ticket for " + selected.getEventName() + "?", ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().get() == ButtonType.YES) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    // Delete from tickets
                    PreparedStatement ps1 = conn.prepareStatement("DELETE FROM tickets WHERE ticket_id = ?");
                    ps1.setInt(1, selected.getTicketId());
                    ps1.executeUpdate();

                    // Restore seat count
                    PreparedStatement ps2 = conn.prepareStatement("UPDATE events SET available_seats = available_seats + 1 WHERE event_id = ?");
                    ps2.setInt(1, selected.getEventId());
                    ps2.executeUpdate();

                    // Refund balance (Uses amount_paid stored in ticket)
                    PreparedStatement ps3 = conn.prepareStatement("UPDATE users SET balance = balance + ? WHERE username = ?");
                    ps3.setDouble(1, selected.getAmountPaid());
                    ps3.setString(2, u.getUsername());
                    ps3.executeUpdate();

                    conn.commit();
                    u.setBalance(u.getBalance() + selected.getAmountPaid());
                    refreshHeader();
                    handleMyTickets();
                    showSimpleAlert("Cancelled", "Ticket has been refunded to your wallet.");
                } catch (SQLException ex) { conn.rollback(); }
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    @FXML
    private void handleDownloadVoucher() {
        Ticket sel = tblBookings.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showSimpleAlert("Selection Required", "Please select a ticket from your bookings list.");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Save Digital Voucher");
        fc.setInitialFileName("Ticket_" + sel.getEventName().replace(" ", "_") + ".txt");
        File file = fc.showSaveDialog(lblWelcome.getScene().getWindow());

        if (file != null) {
            try (PrintWriter pw = new PrintWriter(file)) {
                pw.println("==========================================");
                pw.println("       ELITE EVENT CENTER VOUCHER         ");
                pw.println("==========================================");
                pw.println(" TICKET ID : #" + sel.getTicketId());
                pw.println(" EVENT     : " + sel.getEventName());
                pw.println(" DATE      : " + sel.getDate());
                pw.println(" VENUE     : " + sel.getVenue());
                pw.println(" SEAT      : " + sel.getSeatNumber());
                pw.println("------------------------------------------");
                pw.println(" ATTENDEE  : " + sel.getBuyerName());
                pw.println(" STATUS    : PAID ($" + String.format("%.2f", sel.getAmountPaid()) + ")");
                pw.println("==========================================");
                showSimpleAlert("Voucher Saved", "Digital ticket saved to: " + file.getName());
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @FXML
    private void handleLogout() throws IOException {
        UserSession.cleanUserSession();
        Stage s = (Stage) lblWelcome.getScene().getWindow();
        s.setScene(new Scene(FXMLLoader.load(getClass().getResource("/login.fxml"))));
        s.setTitle("Event Center Login");
        s.centerOnScreen();
    }

    private void showSimpleAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}