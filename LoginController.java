package dronefleet;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    @FXML
    private void onLogin() {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            statusLabel.setText("Completeaza toate campurile!");
            return;
        }

        String hashedPassword = hashPassword(pass);
        
        if (hashedPassword == null) {
            statusLabel.setText("Eroare la procesare parola!");
            return;
        }

        User loggedUser = DatabaseManager.getInstance().validateUser(user, hashedPassword);
        
        if (loggedUser != null) {
            Session.setCurrentUser(loggedUser);
            openDashboard(loggedUser);
        } else {
            statusLabel.setText("Utilizator sau parola incorecta!");
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes());
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
            
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void openDashboard(User user) {
        try {
            Stage currentStage = (Stage) usernameField.getScene().getWindow();
            currentStage.close();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/dashboard.fxml"));
            Scene scene = new Scene(loader.load());
            
            if (getClass().getResource("/style.css") != null) {
                scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            }

            Stage stage = new Stage();
            stage.setTitle("Drone Fleet Manager - " + user.getFullName() + " (" + user.getRole().toUpperCase() + ")");
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Eroare la deschidere Dashboard!");
        }
    }
}