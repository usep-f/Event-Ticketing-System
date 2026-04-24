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
    private Button lastSelectedButton = null; // To track and reset colors
    private List<String> takenSeats = new ArrayList<>();

    public void setData(Event event) {
        this.selectedEvent = event;
        lblEventName.setText(event.getName());
        loadTakenSeats();
        generateSeatGrid();
    }

    private void loadTakenSeats() {
        String query = "SELECT seat_number FROM tickets WHERE event_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, selectedEvent.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) { takenSeats.add(rs.getString("seat_number")); }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void generateSeatGrid() {
        char row = 'A';
        for (int i = 0; i < 5; i++) {
            for (int j = 1; j <= 5; j++) {
                String seatId = row + "-" + j;
                Button seatBtn = new Button(seatId);
                seatBtn.setPrefSize(55, 50);

                if (takenSeats.contains(seatId)) {
                    seatBtn.setStyle("-fx-background-color: #EF9A9A; -fx-text-fill: #B71C1C; -fx-font-weight: bold;");
                    seatBtn.setDisable(true);
                } else {
                    seatBtn.setStyle("-fx-background-color: #C8E6C9; -fx-text-fill: #2E7D32;");
                    seatBtn.setOnAction(e -> selectSeat(seatId, seatBtn));
                }
                gridSeats.add(seatBtn, j, i);
            }
            row++;
        }
    }

    private void selectSeat(String id, Button btn) {
        // Reset previous selection color
        if (lastSelectedButton != null) {
            lastSelectedButton.setStyle("-fx-background-color: #C8E6C9; -fx-text-fill: #2E7D32;");
        }
        // Highlight new selection
        btn.setStyle("-fx-background-color: #FFEB3B; -fx-text-fill: #F57F17; -fx-font-weight: bold;");
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