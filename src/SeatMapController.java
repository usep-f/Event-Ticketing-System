import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SeatMapController {
    @FXML private Label lblEventName, lblSelectedSeat;
    @FXML private GridPane gridSeats;
    @FXML private Button btnConfirm;

    private Event selectedEvent;
    private String selectedSeat = null;
    private Button lastSelectedButton = null;
    private List<String> takenSeats = new ArrayList<>();

    public void setData(Event event) {
        this.selectedEvent = event;
        lblEventName.setText(event.getName());
        loadTakenSeats();
        generateSeatGrid();
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

        int rows = 5;
        int cols = 5;
        String venue = selectedEvent.getVenue();

        // VENUE BLUEPRINTS
        if (venue.equalsIgnoreCase("The Glass Pavilion")) {
            rows = 5; cols = 5;
        } else if (venue.equalsIgnoreCase("Grand Atrium")) {
            rows = 6; cols = 8;
        } else if (venue.equalsIgnoreCase("Indigo Concert Hall")) {
            rows = 10; cols = 10;
        }

        boolean isSoldOut = selectedEvent.getSeatsNum() <= 0;

        for (int i = 0; i < rows; i++) {
            char rowChar = (char) ('A' + i);
            for (int j = 1; j <= cols; j++) {
                String seatId = rowChar + "-" + j;
                Button seatBtn = new Button(seatId);

                // Dynamic Sizing
                double size = (rows > 8) ? 35 : 45;
                seatBtn.setPrefSize(size, size);
                seatBtn.setStyle("-fx-font-size: " + (rows > 8 ? "9" : "10") + ";");

                if (takenSeats.contains(seatId) || isSoldOut) {
                    seatBtn.setStyle(seatBtn.getStyle() + "-fx-background-color: #EF9A9A; -fx-text-fill: #B71C1C;");
                    seatBtn.setDisable(true);
                } else {
                    seatBtn.setStyle(seatBtn.getStyle() + "-fx-background-color: #C8E6C9; -fx-text-fill: #2E7D32;");
                    seatBtn.setOnAction(e -> selectSeat(seatId, seatBtn));
                }
                gridSeats.add(seatBtn, j, i);
            }
        }

        if (isSoldOut) {
            lblSelectedSeat.setText("STATUS: SOLD OUT");
            lblSelectedSeat.setStyle("-fx-text-fill: #D32F2F; -fx-font-weight: bold;");
        }
    }

    private void selectSeat(String id, Button btn) {
        if (lastSelectedButton != null) {
            lastSelectedButton.setStyle(lastSelectedButton.getStyle().split("-fx-background-color")[0] + "-fx-background-color: #C8E6C9; -fx-text-fill: #2E7D32;");
        }
        btn.setStyle(btn.getStyle().split("-fx-background-color")[0] + "-fx-background-color: #FFEB3B; -fx-text-fill: #F57F17; -fx-font-weight: bold;");
        this.selectedSeat = id;
        this.lastSelectedButton = btn;
        lblSelectedSeat.setText("Selected Seat: " + id);
    }

    public String getSelectedSeat() { return selectedSeat; }

    @FXML
    private void handleConfirm() {
        if (selectedSeat == null) return;
        Stage stage = (Stage) btnConfirm.getScene().getWindow();
        stage.close();
    }
}