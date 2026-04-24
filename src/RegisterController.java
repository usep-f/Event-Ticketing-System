import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;

public class RegisterController {

    @FXML private TextField txtFullName, txtUsername;
    @FXML private PasswordField txtPassword;

    private UserDAO userDAO = new UserDAO();

    @FXML
    private void handleRegister() {
        String name = txtFullName.getText();
        String user = txtUsername.getText();
        String pass = txtPassword.getText();

        if (name.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            showAlert("Error", "Please fill in all fields.");
            return;
        }

        if (userDAO.registerUser(user, name, pass)) {
            showAlert("Success", "Account created successfully!");
            switchToLogin(); // Go back to login after success
        } else {
            showAlert("Error", "Username might already be taken.");
        }
    }

    @FXML
    private void switchToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            stage.setScene(new Scene(root));
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