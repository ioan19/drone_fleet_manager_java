package dronefleet;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.TabPane;

public class MainController {

    @FXML private TabPane mainTabPane;

    @FXML
    private void onLogout() {
        try {
            Stage currentStage = (Stage) mainTabPane.getScene().getWindow();
            currentStage.close();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Scene scene = new Scene(loader.load());
            Stage loginStage = new Stage();
            loginStage.setScene(scene);
            loginStage.setTitle("Drone Fleet Login");
            loginStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}