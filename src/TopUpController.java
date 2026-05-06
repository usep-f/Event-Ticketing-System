import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.sql.*;
import java.time.LocalDate;

public class TopUpController {

    @FXML private VBox paneLink, paneTopUp;
    @FXML private TextField txtNewCard, txtCardNumber, txtAmount;
    @FXML private PasswordField txtNewPin, txtPin;

    private User user = UserSession.getInstance().getCurrentUser();

    @FXML
    public void initialize() {
        checkIfCardLinked();
    }

    private void checkIfCardLinked() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM mock_bank_accounts WHERE owner_username = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, user.getUsername());

            if (ps.executeQuery().next()) {
                paneTopUp.setVisible(true);
                paneLink.setVisible(false);
            } else {
                paneLink.setVisible(true);
                paneTopUp.setVisible(false);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleLinkCard() {
        String card = txtNewCard.getText();
        String pin = txtNewPin.getText();

        if (card.length() != 16 || pin.length() != 4) {
            showAlert("Format Error", "Card must be 16 digits, PIN must be 4 digits.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO mock_bank_accounts (card_number, pin, owner_username) VALUES (?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, card);
            ps.setString(2, pin);
            ps.setString(3, user.getUsername());
            ps.executeUpdate();

            showAlert("Success", "Account linked! You can now top up.");
            checkIfCardLinked(); // Refresh to show Top-Up pane
        } catch (SQLException e) { showAlert("Error", "Card number already in use."); }
    }

    @FXML
    private void handleConfirmTopUp() {
        String card = txtCardNumber.getText();
        String pin = txtPin.getText();
        String amountStr = txtAmount.getText();

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount > 500) { showAlert("Limit", "Max $500 per day."); return; }

            try (Connection conn = DatabaseConnection.getConnection()) {
                // 1. Verify card belongs to user
                PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM mock_bank_accounts WHERE card_number=? AND pin=? AND owner_username=?");
                ps1.setString(1, card); ps1.setString(2, pin); ps1.setString(3, user.getUsername());
                if (!ps1.executeQuery().next()) { showAlert("Error", "Invalid details."); return; }

                // 2. Check Daily Limit
                PreparedStatement ps2 = conn.prepareStatement("SELECT last_topup_date FROM users WHERE username=?");
                ps2.setString(1, user.getUsername());
                ResultSet rs = ps2.executeQuery();
                if (rs.next() && rs.getDate("last_topup_date") != null) {
                    if (rs.getDate("last_topup_date").toLocalDate().equals(LocalDate.now())) {
                        showAlert("Limit", "Daily top-up already used."); return;
                    }
                }

                // 3. Update Balance
                PreparedStatement ps3 = conn.prepareStatement("UPDATE users SET balance = balance + ?, last_topup_date = ? WHERE username = ?");
                ps3.setDouble(1, amount);
                ps3.setDate(2, Date.valueOf(LocalDate.now()));
                ps3.setString(3, user.getUsername());
                ps3.executeUpdate();

                user.setBalance(user.getBalance() + amount);
                showAlert("Success", "Wallet Updated!");
                ((Stage) txtAmount.getScene().getWindow()).close();
            }
        } catch (Exception e) { showAlert("Error", "Invalid amount."); }
    }

    private void showAlert(String t, String c) {
        new Alert(Alert.AlertType.INFORMATION, c).showAndWait();
    }
}