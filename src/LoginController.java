import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import java.io.IOException;

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
            // 1. Store the user in the global session
            UserSession.setInstance(user);
            showAlert(AlertType.INFORMATION, "Login Successful!", "Welcome " + user.getFullName());

            // 2. Role-Based Redirection
            try {
                String fxmlFile;
                String title;

                if (user.getRole().equalsIgnoreCase("Admin")) {
                    fxmlFile = "/admin_panel.fxml";
                    title = "Admin Command Center";
                } else {
                    fxmlFile = "/ticketing.fxml";
                    title = "Event Ticketing System - " + user.getFullName();
                }

                Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
                Stage stage = (Stage) btnLogin.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle(title);
                stage.centerOnScreen();
                stage.show();

            } catch (IOException e) {
                e.printStackTrace();
                showAlert(AlertType.ERROR, "Error", "Could not load the destination screen.");
            }

        } else {
            showAlert(AlertType.ERROR, "Login Failed!", "Invalid Username or Password");
        }
    }

    @FXML
    private void handleCreateAccount() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/register.fxml"));
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Create Account");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}