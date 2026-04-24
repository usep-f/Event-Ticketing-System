import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Ensure "your_design.fxml" is actually named this and is in the src folder
        URL fxmlLocation = getClass().getResource("/login.fxml");

        if (fxmlLocation == null) {
            System.err.println("Error: Could not find 'your_design.fxml'. Check your file name and location.");
            return;
        }

        Parent root = FXMLLoader.load(fxmlLocation);

        primaryStage.setTitle("Ticketing System Login");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}