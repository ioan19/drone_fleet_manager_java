package dronefleet;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.collections.FXCollections;
import netscape.javascript.JSObject;

import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MapController {

    @FXML private WebView webView;
    @FXML private ComboBox<String> missionTypeCombo;
    @FXML private TextField weightField;
    @FXML private Label statusLabel;
    @FXML private Label weatherLabel;
    @FXML private Label windLabel;
    @FXML private Label distanceLabel;
    @FXML private Label costLabel;
    @FXML private Label droneLabel;
    @FXML private Button confirmButton;
    @FXML private javafx.scene.layout.VBox weightContainer;
    @FXML private javafx.scene.layout.VBox resultCard;

    private WebEngine engine;
    private Drone selectedDrone;
    private String startCoord;
    private String endCoord;
    private double calculatedDistance;
    private double calculatedCost;

    @FXML
    public void initialize() {
        setupMissionTypes();
        setupMap();
    }

    private void setupMissionTypes() {
        if (missionTypeCombo != null) {
            missionTypeCombo.setItems(FXCollections.observableArrayList(
                "Livrare", "Inspecție", "Cartografiere", "Test"
            ));
            missionTypeCombo.setValue("Livrare");
            missionTypeCombo.setOnAction(e -> onMissionTypeChanged());
        }
    }

    private void setupMap() {
        if (webView == null) return;
        
        engine = webView.getEngine();
        URL url = getClass().getResource("/map_view.html");
        
        if (url != null) {
            engine.load(url.toExternalForm());
            
            engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    JSObject window = (JSObject) engine.executeScript("window");
                    window.setMember("javaApp", new JavaScriptBridge());
                }
            });
        } else {
            showStatus("❌ Eroare: Fișierul map_view.html lipsește!", Color.RED);
        }
    }

    private void onMissionTypeChanged() {
        String type = missionTypeCombo.getValue();
        
        if (weightContainer != null) {
            boolean isDelivery = "Livrare".equals(type);
            weightContainer.setVisible(isDelivery);
            weightContainer.setManaged(isDelivery);
        }
    }

    // ===== BRIDGE JAVA <-> JAVASCRIPT =====
    public class JavaScriptBridge {
        public void receiveCoordinatesFromJS(double startLat, double startLng, double endLat, double endLng) {
            javafx.application.Platform.runLater(() -> {
                startCoord = startLat + "," + startLng;
                endCoord = endLat + "," + endLng;
                calculateMission(startLat, startLng, endLat, endLng);
            });
        }
    }

    // ===== CALCUL MISIUNE COMPLETĂ =====
    private void calculateMission(double startLat, double startLng, double endLat, double endLng) {
        showStatus("⏳ Calculez ruta optimă...", Color.web("#f39c12"));
        
        try {
            // 1. DISTANȚA
            calculatedDistance = calculateDistance(startLat, startLng, endLat, endLng);
            
            // 2. TIPUL MISIUNII
            String missionType = missionTypeCombo.getValue().toLowerCase();
            
            // 3. GREUTATEA (doar pentru livrare)
            double weight = 0;
            if ("livrare".equals(missionType)) {
                try {
                    weight = Double.parseDouble(weightField.getText());
                } catch (NumberFormatException e) {
                    weight = 1.0;
                }
            }
            
            // 4. SELECTEAZĂ DRONA OPTIMĂ
            selectedDrone = selectOptimalDrone(missionType, weight, calculatedDistance);
            
            if (selectedDrone == null) {
                showStatus("❌ Nicio dronă disponibilă pentru această misiune!", Color.RED);
                if (confirmButton != null) confirmButton.setDisable(true);
                if (resultCard != null) resultCard.setVisible(false);
                return;
            }
            
            // 5. CALCULEAZĂ COSTUL
            calculatedCost = calculateCost(calculatedDistance, missionType, weight);
            
            // 6. VERIFICĂ VREMEA LA START (punctul de plecare)
            WeatherService.WeatherData weather = WeatherService.getWeatherAt(startLat, startLng);
            
            // 7. ACTUALIZEAZĂ UI
            updateMissionDisplay(weather);
            
            // 8. ACTIVEAZĂ/DEZACTIVEAZĂ BUTONUL
            if (confirmButton != null) {
                confirmButton.setDisable(!weather.isSafeToFly);
            }
            
            if (weather.isSafeToFly) {
                showStatus("✅ Misiune validată! Apasă TRIMITE DRONA", Color.GREEN);
            } else {
                showStatus("⚠️ ATENȚIE: Condiții meteo nefavorabile la START!", Color.web("#e74c3c"));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            showStatus("❌ Eroare la calcul!", Color.RED);
        }
    }

    // ===== SELECTARE DRONĂ OPTIMĂ =====
    private Drone selectOptimalDrone(String missionType, double weight, double distance) {
        List<Drone> allDrones = DatabaseManager.getInstance().getDrones();
        
        Drone bestDrone = null;
        double bestScore = -1;
        
        for (Drone drone : allDrones) {
            if (!"activa".equals(drone.getStatus())) continue;
            
            if ("livrare".equals(missionType)) {
                if (!"transport".equals(drone.getType())) continue;
                if (drone.getMaxPayload() < weight) continue;
            } else if ("inspectie".equals(missionType) || "cartografiere".equals(missionType)) {
                if (!"survey".equals(drone.getType())) continue;
            }
            
            double timeNeeded = (distance * 2) * 1.2;
            if (drone.getAutonomy() < timeNeeded) continue;
            
            double autonomyRatio = drone.getAutonomy() / timeNeeded;
            double score = 1.0 / Math.abs(autonomyRatio - 1.5);
            
            if (score > bestScore) {
                bestScore = score;
                bestDrone = drone;
            }
        }
        
        return bestDrone;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }

    private double calculateCost(double distance, String type, double weight) {
        double baseCost = distance * 8.5;
        
        if ("livrare".equals(type)) {
            baseCost *= 1.3;
            baseCost += weight * 2.5;
        } else if ("cartografiere".equals(type)) {
            baseCost *= 1.6;
        } else if ("inspectie".equals(type)) {
            baseCost *= 1.2;
        }
        
        return Math.round(baseCost * 100.0) / 100.0;
    }

    private void updateMissionDisplay(WeatherService.WeatherData weather) {
        if (distanceLabel != null) {
            distanceLabel.setText(String.format("%.2f km", calculatedDistance));
        }
        
        if (costLabel != null) {
            costLabel.setText(String.format("%.2f RON", calculatedCost));
        }
        
        if (droneLabel != null && selectedDrone != null) {
            droneLabel.setText(selectedDrone.getModel());
        }
        
        if (resultCard != null) {
            resultCard.setVisible(true);
        }
        
        if (weatherLabel != null) {
            String icon = weather.isSafeToFly ? "✓" : "⚠";
            weatherLabel.setText(String.format("%s %.1f°C", icon, weather.temperature));
            weatherLabel.setTextFill(weather.isSafeToFly ? 
                Color.web("#27ae60") : Color.web("#e74c3c"));
        }
        
        if (windLabel != null) {
            windLabel.setText(String.format("Vânt: %.1f km/h (%s)", 
                weather.windSpeed, weather.condition));
        }
    }

    @FXML
    private void confirmMission() {
        if (selectedDrone == null || startCoord == null || endCoord == null) {
            showStatus("❌ Setează start și destinație pe hartă!", Color.RED);
            return;
        }
        
        try {
            LocalDateTime startTime = LocalDateTime.now().plusHours(1);
            
            Flight newFlight = new Flight(
                selectedDrone,
                startCoord,
                endCoord,
                startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
            
            DatabaseManager.getInstance().saveFlight(newFlight);
            
            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("✅ Misiune Programată");
            success.setHeaderText("Misiunea a fost înregistrată cu succes!");
            success.setContentText(String.format(
                "🚁 Dronă: %s\n" +
                "📍 Distanță: %.2f km\n" +
                "💰 Cost: %.2f RON\n" +
                "🕐 Plecare: %s",
                selectedDrone.getModel(),
                calculatedDistance,
                calculatedCost,
                startTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            ));
            success.showAndWait();
            
            resetMission();
            
        } catch (Exception e) {
            e.printStackTrace();
            showStatus("❌ Eroare la salvare!", Color.RED);
        }
    }

    private void resetMission() {
        if (engine != null) {
            engine.executeScript("resetMap();");
        }
        
        selectedDrone = null;
        startCoord = null;
        endCoord = null;
        calculatedDistance = 0;
        calculatedCost = 0;
        
        if (resultCard != null) {
            resultCard.setVisible(false);
        }
        
        if (confirmButton != null) {
            confirmButton.setDisable(true);
        }
        
        showStatus("Click pe hartă: Punct 1 = START | Punct 2 = DESTINAȚIE", Color.web("#7f8c8d"));
    }

    private void showStatus(String message, Color color) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setTextFill(color);
        }
    }
}