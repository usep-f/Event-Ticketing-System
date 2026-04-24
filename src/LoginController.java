import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage; // Import this!

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnLogin;

    private UserDAO userDAO = new UserDAO();

    @FXML
    private void handleLogin() {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(AlertType.ERROR, "Form Error!", "Please enter username and password");
            return;
        }

        User user = userDAO.login(username, password);

        if (user != null) {
            UserSession.setInstance(user);
            // 1. Success Message
            showAlert(AlertType.INFORMATION, "Login Successful!", "Welcome " + user.getFullName());

            // 2. SWITCH TO THE TICKETING DASHBOARD
            try {
                // Point this to your actual dashboard FXML file name
                Parent root = FXMLLoader.load(getClass().getResource("/ticketing.fxml"));

                // Get the current window (Stage)
                Stage stage = (Stage) btnLogin.getScene().getWindow();

                // Set the new scene (The Ticketing Dashboard)
                stage.setScene(new Scene(root));
                stage.setTitle("Event Ticketing System - " + user.getFullName());
                stage.show();

            } catch (java.io.IOException e) {
                e.printStackTrace();
                showAlert(AlertType.ERROR, "Error", "Could not load the dashboard file. Check the file name.");
            }

        } else {
            showAlert(AlertType.ERROR, "Login Failed!", "Invalid Username or Password");
        }
    }

    private void showAlert(AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleCreateAccount() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/register.fxml"));
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}