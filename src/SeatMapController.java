import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SeatMapController {
    @FXML private Label lblEventName, lblSelectedSeat, lblRegularPrice, lblVipPrice;
    @FXML private GridPane gridSeats;
    @FXML private Button btnConfirm;

    private Event selectedEvent;
    private User currentUser;
    private String selectedSeat = null;
    private Button lastSelectedButton = null;
    private List<String> takenSeats = new ArrayList<>();
    private double currentSeatPrice = 0.0;

    // Updated to receive both Event and User
    public void setData(Event event, User user) {
        this.selectedEvent = event;
        this.currentUser = user;
        lblEventName.setText(event.getName());

        updateLegend();
        loadTakenSeats();
        generateSeatGrid();
    }

    private void updateLegend() {
        double base = selectedEvent.getPrice(); // This comes in already discounted from the table
        // We need to re-calculate the "Full" version to show the differences
        // Regular is base, VIP is base + (100 * discount)
        double vipPremium = currentUser.getTier().equalsIgnoreCase("Elite") ? 80.0 :
                currentUser.getTier().equalsIgnoreCase("Gold") ? 90.0 : 100.0;

        lblRegularPrice.setText("Regular: $" + String.format("%.2f", base));
        lblVipPrice.setText("VIP (Row A): $" + String.format("%.2f", base + vipPremium));
    }

    private void loadTakenSeats() {
        takenSeats.clear();
        String query = "SELECT seat_number FROM tickets WHERE event_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, selectedEvent.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String seat = rs.getString("seat_number");
                if (seat != null) takenSeats.add(seat);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void generateSeatGrid() {
        gridSeats.getChildren().clear();
        String venue = selectedEvent.getVenue();
        int rows = venue.equals("Indigo Concert Hall") ? 10 : venue.equals("Grand Atrium") ? 6 : 5;
        int cols = venue.equals("Indigo Concert Hall") ? 10 : venue.equals("Grand Atrium") ? 8 : 5;

        boolean isSoldOut = selectedEvent.getSeatsNum() <= 0;

        for (int i = 0; i < rows; i++) {
            char rowChar = (char) ('A' + i);
            for (int j = 1; j <= cols; j++) {
                String seatId = rowChar + "-" + j;
                Button seatBtn = new Button(seatId);
                seatBtn.setPrefSize(rows > 8 ? 35 : 45, rows > 8 ? 35 : 45);

                if (takenSeats.contains(seatId) || isSoldOut) {
                    seatBtn.setStyle("-fx-background-color: #EF9A9A; -fx-text-fill: #B71C1C;");
                    seatBtn.setDisable(true);
                } else {
                    // VIP seats (Row A) are yellow-ish green, Regular are green
                    String color = seatId.startsWith("A") ? "#FFF9C4" : "#C8E6C9";
                    String text = seatId.startsWith("A") ? "#FBC02D" : "#2E7D32";
                    seatBtn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: " + text + ";");
                    seatBtn.setOnAction(e -> selectSeat(seatId, seatBtn));
                }
                gridSeats.add(seatBtn, j, i);
            }
        }
    }

    private void selectSeat(String id, Button btn) {
        // Reset last selection
        if (lastSelectedButton != null) {
            String oldId = lastSelectedButton.getText();
            String color = oldId.startsWith("A") ? "#FFF9C4" : "#C8E6C9";
            String text = oldId.startsWith("A") ? "#FBC02D" : "#2E7D32";
            lastSelectedButton.setStyle("-fx-background-color: " + color + "; -fx-text-fill: " + text + ";");
        }

        // Calculate price for THIS seat
        double vipPremium = currentUser.getTier().equalsIgnoreCase("Elite") ? 80.0 :
                currentUser.getTier().equalsIgnoreCase("Gold") ? 90.0 : 100.0;
        this.currentSeatPrice = selectedEvent.getPrice() + (id.startsWith("A") ? vipPremium : 0);

        // Highlight new selection
        btn.setStyle("-fx-background-color: #3F51B5; -fx-text-fill: white; -fx-font-weight: bold;");
        this.selectedSeat = id;
        this.lastSelectedButton = btn;
        lblSelectedSeat.setText("Selected: " + id + " | Total: $" + String.format("%.2f", currentSeatPrice));
    }

    public String getSelectedSeat() { return selectedSeat; }
    public double getFinalPrice() { return currentSeatPrice; }

    @FXML
    private void handleConfirm() {
        if (selectedSeat == null) return;
        Stage stage = (Stage) btnConfirm.getScene().getWindow();
        stage.close();
    }
}