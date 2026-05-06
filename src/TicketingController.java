import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

public class TicketingController {

    // FXML Header & Sidebar Elements
    @FXML private Label lblWelcome, lblTableTitle, lblTier, lblBalance;
    @FXML private TextField txtSearch;
    @FXML private Button btnBrowse, btnMyTickets, btnMemberships, btnBook, btnCancel, btnDownload;

    // FXML Panes (Inside the StackPane)
    @FXML private VBox paneMain, paneMemberships;

    // FXML Bank Linking Fields
    @FXML private TextField txtLinkCard;
    @FXML private PasswordField txtLinkPin;

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

        // 1. Setup Browse Table Columns
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colVenue.setCellValueFactory(new PropertyValueFactory<>("venue"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colSeats.setCellValueFactory(new PropertyValueFactory<>("seats"));

        // 2. Setup Bookings Table Columns
        colTicketId.setCellValueFactory(new PropertyValueFactory<>("ticketId"));
        colBookedEvent.setCellValueFactory(new PropertyValueFactory<>("eventName"));
        colBookedDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colBookedSeat.setCellValueFactory(new PropertyValueFactory<>("seatNumber"));
        colPaid.setCellValueFactory(new PropertyValueFactory<>("amountPaid"));

        // 3. Search Listener
        txtSearch.textProperty().addListener((obs, old, val) -> searchEvents(val));

        // Start on Browse mode
        handleBrowse();
    }

    public void refreshHeader() {
        if (UserSession.getInstance() != null) {
            User u = UserSession.getInstance().getCurrentUser();
            lblWelcome.setText("Welcome, " + u.getFullName());
            lblTier.setText(u.getTier().toUpperCase() + " TIER");
            lblBalance.setText("Wallet: $" + String.format("%.2f", u.getBalance()));
        }
    }

    // --- NAVIGATION LOGIC ---

    @FXML
    private void handleBrowse() {
        updateView(true, false, false, "Upcoming Experiences");
        loadEvents("SELECT * FROM events");
        setActiveButtonStyle(btnBrowse);
    }

    @FXML
    private void handleMyTickets() {
        updateView(false, true, false, "My Booked Tickets");
        loadMyTickets();
        setActiveButtonStyle(btnMyTickets);
    }

    @FXML
    private void handleShowMemberships() {
        updateView(false, false, true, "Membership & Finance");
        setActiveButtonStyle(btnMemberships);
    }

    private void updateView(boolean showEvents, boolean showBookings, boolean showShop, String title) {
        paneMain.setVisible(!showShop);
        paneMemberships.setVisible(showShop);
        tblEvents.setVisible(showEvents);
        tblBookings.setVisible(showBookings);

        lblTableTitle.setText(title);
        btnBook.setVisible(showEvents);
        btnCancel.setVisible(showBookings);
        btnDownload.setVisible(showBookings);
    }

    // --- DATA LOADING ---

    private void loadEvents(String query) {
        allEvents.clear();
        User u = UserSession.getInstance().getCurrentUser();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                double discounted = u.calculateDiscountedPrice(rs.getDouble("price"));
                allEvents.add(new Event(rs.getInt("event_id"), rs.getString("event_name"),
                        rs.getString("event_date"), rs.getString("venue"), discounted, rs.getInt("available_seats")));
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

    // --- BOOKING LOGIC ---

    @FXML
    private void handleBookTicket() {
        Event selected = tblEvents.getSelectionModel().getSelectedItem();
        User user = UserSession.getInstance().getCurrentUser();
        if (selected == null || selected.getSeatsNum() <= 0) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/seat_selection.fxml"));
            Parent root = loader.load();
            SeatMapController smc = loader.getController();
            smc.setData(selected, user);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Select Seat - " + selected.getName());
            stage.showAndWait();

            String seat = smc.getSelectedSeat();
            double finalPrice = smc.getFinalPrice();

            if (seat == null) return;

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Confirm booking for $" + String.format("%.2f", finalPrice) + "?");
            if (confirm.showAndWait().get() == ButtonType.OK) {
                if (user.getBalance() < finalPrice) {
                    showSimpleAlert("Error", "Insufficient wallet balance.");
                    return;
                }
                executeBooking(user, selected, seat, finalPrice);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void executeBooking(User u, Event e, String seat, double price) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                PreparedStatement ps1 = conn.prepareStatement("INSERT INTO tickets (username, event_id, amount_paid, seat_number) VALUES (?,?,?,?)");
                ps1.setString(1, u.getUsername()); ps1.setInt(2, e.getId()); ps1.setDouble(3, price); ps1.setString(4, seat);
                ps1.executeUpdate();

                conn.prepareStatement("UPDATE events SET available_seats = available_seats - 1 WHERE event_id = " + e.getId()).executeUpdate();
                conn.prepareStatement("UPDATE users SET balance = balance - " + price + " WHERE username = '" + u.getUsername() + "'").executeUpdate();

                conn.commit();
                u.setBalance(u.getBalance() - price);
                refreshHeader();
                handleBrowse();
                showSimpleAlert("Success", "Booking confirmed! Enjoy the show.");
            } catch (SQLException ex) { conn.rollback(); throw ex; }
        }
    }

    @FXML
    private void handleCancelTicket() {
        Ticket selected = tblBookings.getSelectionModel().getSelectedItem();
        User u = UserSession.getInstance().getCurrentUser();
        if (selected == null) return;

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                conn.prepareStatement("DELETE FROM tickets WHERE ticket_id = " + selected.getTicketId()).executeUpdate();
                conn.prepareStatement("UPDATE events SET available_seats = available_seats + 1 WHERE event_id = " + selected.getEventId()).executeUpdate();
                conn.prepareStatement("UPDATE users SET balance = balance + " + selected.getAmountPaid() + " WHERE username = '" + u.getUsername() + "'").executeUpdate();

                conn.commit();
                u.setBalance(u.getBalance() + selected.getAmountPaid());
                refreshHeader();
                handleMyTickets();
                showSimpleAlert("Refunded", "Booking cancelled. Money returned to wallet.");
            } catch (SQLException ex) { conn.rollback(); }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- FINANCE & BANKING ---

    @FXML
    private void handleLinkAccount() {
        String card = txtLinkCard.getText();
        String pin = txtLinkPin.getText();
        User u = UserSession.getInstance().getCurrentUser();

        if (card.length() != 16 || pin.length() != 4) {
            showSimpleAlert("Format Error", "Card must be 16 digits, PIN must be 4 digits.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO mock_bank_accounts (card_number, pin, owner_username) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE pin = VALUES(pin), card_number = VALUES(card_number)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, card); ps.setString(2, pin); ps.setString(3, u.getUsername());
            ps.executeUpdate();

            showSimpleAlert("Success", "Bank account linked successfully.");
            txtLinkCard.clear(); txtLinkPin.clear();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleOpenTopUp() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/topup_window.fxml"));
        Stage stage = new Stage();
        stage.setScene(new Scene(loader.load()));
        stage.setTitle("Secure Top-Up Gateway");
        stage.showAndWait();
        refreshHeader();
    }

    @FXML private void handleUpgradeGold() { processTierChange("Gold", 1000.0); }
    @FXML private void handleUpgradeElite() { processTierChange("Elite", 2500.0); }

    private void processTierChange(String tier, double cost) {
        User u = UserSession.getInstance().getCurrentUser();
        if (u.getBalance() < cost) {
            showSimpleAlert("Error", "Insufficient wallet balance for upgrade.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("UPDATE users SET tier = ?, balance = balance - ? WHERE username = ?");
            ps.setString(1, tier); ps.setDouble(2, cost); ps.setString(3, u.getUsername());
            ps.executeUpdate();

            u.setTier(tier);
            u.setBalance(u.getBalance() - cost);

            showSimpleAlert("Congratulations!", "You are now a " + tier + " member!");
            refreshHeader();
            handleBrowse();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- FILE I/O ---

    @FXML
    private void handleDownloadVoucher() {
        Ticket sel = tblBookings.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        FileChooser fc = new FileChooser();
        fc.setInitialFileName("Voucher_" + sel.getEventName().replace(" ", "_") + ".txt");
        File file = fc.showSaveDialog(lblWelcome.getScene().getWindow());

        if (file != null) {
            try (PrintWriter pw = new PrintWriter(file)) {
                pw.println("===============================");
                pw.println("      ELITE EVENT CENTER       ");
                pw.println("===============================");
                pw.println(" TICKET ID: #" + sel.getTicketId());
                pw.println(" EVENT:     " + sel.getEventName());
                pw.println(" SEAT:      " + sel.getSeatNumber());
                pw.println(" PAID:      $" + String.format("%.2f", sel.getAmountPaid()));
                pw.println("===============================");
                pw.close();
                showSimpleAlert("Saved", "Voucher saved successfully!");
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // --- UTILS ---

    private void setActiveButtonStyle(Button active) {
        btnBrowse.setStyle("-fx-background-color: transparent; -fx-text-fill: #c5cae9;");
        btnMyTickets.setStyle("-fx-background-color: transparent; -fx-text-fill: #c5cae9;");
        btnMemberships.setStyle("-fx-background-color: transparent; -fx-text-fill: #c5cae9;");
        active.setStyle("-fx-background-color: #3949AB; -fx-text-fill: white; -fx-background-radius: 8;");
    }

    @FXML
    private void handleLogout() throws IOException {
        UserSession.cleanUserSession();
        Stage s = (Stage) lblWelcome.getScene().getWindow();
        s.setScene(new Scene(FXMLLoader.load(getClass().getResource("/login.fxml"))));
        s.centerOnScreen();
    }

    private void showSimpleAlert(String t, String c) {
        new Alert(Alert.AlertType.INFORMATION, c).showAndWait();
    }
}