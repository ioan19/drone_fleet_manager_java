package dronefleet;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    @FXML
    private void onLogin() {
        String user = usernameField.getText();
        String pass = passwordField.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            statusLabel.setText("Introduceți user și parolă!");
            return;
        }

        if (validateLogin(user, pass)) {
            openDashboard();
        } else {
            statusLabel.setText("Utilizator sau parolă incorectă!");
        }
    }

    private boolean validateLogin(String username, String password) {
        String sql = "SELECT UserID FROM Users WHERE Username = ? AND PasswordHash = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // 1. Setăm username-ul
            pstmt.setString(1, username);
            
            // 2. Criptăm parola primită din interfață pentru a o compara cu cea din DB
            String encryptedPass = hashPassword(password);
            pstmt.setString(2, encryptedPass); 
            
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); // Dacă găsim un rând, login-ul e corect
            }
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Eroare conexiune DB!");
            return false;
        }
    }

    // --- METODA NOUĂ DE CRIPTARE ---
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes());
            
            // Convertim din bytes în format Hexazecimal (text lizibil)
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
            // Închide fereastra de login
            Stage currentStage = (Stage) usernameField.getScene().getWindow();
            currentStage.close();

            // Deschide Dashboard
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/dashboard.fxml"));
            Scene scene = new Scene(loader.load());
            
            // Adaugă CSS dacă există
            if (getClass().getResource("/style.css") != null) {
                scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            }

            Stage stage = new Stage();
            stage.setTitle("Drone Fleet Manager - Dashboard");
            stage.setScene(scene);
            stage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Eroare la deschiderea Dashboard!");
        }
    }
}