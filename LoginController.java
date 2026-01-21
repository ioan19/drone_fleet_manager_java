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
            statusLabel.setText("⚠ Completează toate câmpurile!");
            return;
        }

        // Hash-uim parola introdusă
        String hashedPassword = hashPassword(pass);
        
        if (hashedPassword == null) {
            statusLabel.setText("❌ Eroare la procesare parolă!");
            return;
        }

        // Validăm cu baza de date
        if (DatabaseManager.getInstance().validateUser(user, hashedPassword)) {
            openDashboard();
        } else {
            statusLabel.setText("❌ Utilizator sau parolă incorectă!");
        }
    }

    /**
     * Hash-uire SHA-256 pentru parole
     */
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

    private void openDashboard() {
        try {
            Stage currentStage = (Stage) usernameField.getScene().getWindow();
            currentStage.close();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/dashboard.fxml"));
            Scene scene = new Scene(loader.load());
            
            if (getClass().getResource("/style.css") != null) {
                scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            }

            Stage stage = new Stage();
            stage.setTitle("Drone Fleet Manager - Dashboard");
            stage.setScene(scene);
            stage.setMaximized(true); // Deschide maximizat
            stage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("❌ Eroare la deschidere Dashboard!");
        }
    }
}