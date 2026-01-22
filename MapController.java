package dronefleet;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.collections.FXCollections;
import netscape.javascript.JSObject;
import javafx.stage.Stage;
import javafx.concurrent.Worker;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MapController {

    @FXML private WebView webView;
    @FXML private ComboBox<String> missionTypeCombo;
    @FXML private TextField weightField;
    @FXML private TextArea notesField;  // NOU: pentru note la cerere
    @FXML private Label statusLabel;
    @FXML private Label weatherLabel;
    @FXML private Label windLabel;
    @FXML private Label distanceLabel;
    @FXML private Label costLabel;
    @FXML private Label droneLabel;
    @FXML private Button confirmButton;
    @FXML private javafx.scene.layout.VBox weightContainer;
    @FXML private javafx.scene.layout.VBox notesContainer;  // NOU
    @FXML private javafx.scene.layout.VBox resultCard;

    private WebEngine engine;
    private Drone selectedDrone;
    private String startCoord;
    private String endCoord;
    private double calculatedDistance;
    private double calculatedCost;
    private boolean weatherSafe = true;
    
    private User currentUser;  // NOU: referință la userul curent

    @FXML
    public void initialize() {
        System.out.println("[MapController] Initialize started");
        
        currentUser = Session.getCurrentUser();
        
        setupMissionTypes();
        setupMap();
        setupUIForRole();  // NOU: configurare UI bazată pe rol
        
        if (distanceLabel != null) distanceLabel.setText("0.0 km");
        if (costLabel != null) costLabel.setText("0.00 RON");
        if (weatherLabel != null) weatherLabel.setText("--");
        if (windLabel != null) windLabel.setText("Vant: -- km/h");
        if (confirmButton != null) confirmButton.setDisable(true);
        
        System.out.println("[MapController] Initialize completed for user: " + 
            (currentUser != null ? currentUser.getUsername() + " (" + currentUser.getRole() + ")" : "NULL"));
    }

    /**
     * NOU: Configurare UI bazată pe rolul utilizatorului
     */
    private void setupUIForRole() {
        if (currentUser == null) return;
        
        if (currentUser.isOperator()) {
            // Operatorul poate doar să creeze CERERI de livrare
            // NU poate lansa direct drone
            if (missionTypeCombo != null) {
                missionTypeCombo.setItems(FXCollections.observableArrayList("Livrare"));
                missionTypeCombo.setValue("Livrare");
                missionTypeCombo.setDisable(true);  // Operatorul face doar livrări
            }
            
            // Afișează câmpul de note pentru cerere
            if (notesContainer != null) {
                notesContainer.setVisible(true);
                notesContainer.setManaged(true);
            }
            
            // Schimbă textul butonului
            if (confirmButton != null) {
                confirmButton.setText("📝 TRIMITE CEREREA");
            }
            
            // Ascunde cardul cu drona alocată (operatorul nu vede asta)
            if (resultCard != null) {
                resultCard.setVisible(false);
            }
            
            showStatus("Selectează punctele pe hartă pentru a crea o cerere de livrare", Color.web("#3498db"));
            
        } else {
            // Admin - comportament normal
            if (notesContainer != null) {
                notesContainer.setVisible(false);
                notesContainer.setManaged(false);
            }
        }
    }

    private void setupMissionTypes() {
        if (missionTypeCombo != null) {
            missionTypeCombo.setItems(FXCollections.observableArrayList(
                "Livrare", "Inspectie", "Cartografiere", "Test"
            ));
            missionTypeCombo.setValue("Livrare");
            missionTypeCombo.setOnAction(e -> onMissionTypeChanged());
        }
    }

    private void setupMap() {
        if (webView == null) {
            System.err.println("[MapController] ERROR: WebView is NULL!");
            return;
        }
        
        System.out.println("[MapController] Setting up WebView...");
        engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);
        
        URL url = getClass().getResource("/map_view.html");
        
        if (url != null) {
            System.out.println("[MapController] Loading map from: " + url.toExternalForm());
            
            engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                System.out.println("[MapController] WebView state: " + newState);
                
                if (newState == Worker.State.SUCCEEDED) {
                    System.out.println("[MapController] Map loaded successfully!");
                    
                    try {
                        JSObject window = (JSObject) engine.executeScript("window");
                        window.setMember("javaApp", new JavaScriptBridge());
                        System.out.println("[MapController] JavaScriptBridge connected successfully!");
                        
                        if (currentUser != null && currentUser.isOperator()) {
                            showStatus("Harta încărcată! Click pentru a seta punctul START", Color.GREEN);
                        } else {
                            showStatus("Harta incarcata cu succes! Click pentru START", Color.GREEN);
                        }
                    } catch (Exception e) {
                        System.err.println("[MapController] ERROR connecting bridge: " + e.getMessage());
                        e.printStackTrace();
                        showStatus("Eroare la conectarea hartii", Color.RED);
                    }
                } else if (newState == Worker.State.FAILED) {
                    System.err.println("[MapController] ERROR: Map failed to load!");
                    Throwable exception = engine.getLoadWorker().getException();
                    if (exception != null) {
                        exception.printStackTrace();
                    }
                    showStatus("Eroare la incarcarea hartii", Color.RED);
                }
            });
            
            engine.load(url.toExternalForm());
        } else {
            System.err.println("[MapController] ERROR: map_view.html not found!");
            showStatus("Eroare: Fisier harta lipsa!", Color.RED);
        }
    }

    private void onMissionTypeChanged() {
        String type = missionTypeCombo.getValue();
        
        if (weightContainer != null) {
            boolean isDelivery = "Livrare".equals(type);
            weightContainer.setVisible(isDelivery);
            weightContainer.setManaged(isDelivery);
        }
        
        // Recalculează dacă avem deja coordonate
        if (startCoord != null && endCoord != null) {
            String[] startParts = startCoord.split(",");
            String[] endParts = endCoord.split(",");
            if (startParts.length == 2 && endParts.length == 2) {
                try {
                    double startLat = Double.parseDouble(startParts[0]);
                    double startLng = Double.parseDouble(startParts[1]);
                    double endLat = Double.parseDouble(endParts[0]);
                    double endLng = Double.parseDouble(endParts[1]);
                    calculateMission(startLat, startLng, endLat, endLng);
                } catch (NumberFormatException e) {
                    // Ignoră
                }
            }
        }
    }

    public class JavaScriptBridge {
        public void receiveCoordinatesFromJS(double startLat, double startLng, double endLat, double endLng) {
            System.out.println(String.format("[JavaScriptBridge] Coordinates received: START(%.4f,%.4f) -> END(%.4f,%.4f)", 
                startLat, startLng, endLat, endLng));
            
            javafx.application.Platform.runLater(() -> {
                startCoord = startLat + "," + startLng;
                endCoord = endLat + "," + endLng;
                calculateMission(startLat, startLng, endLat, endLng);
            });
        }
    }

    private void calculateMission(double startLat, double startLng, double endLat, double endLng) {
        System.out.println("[MapController] Starting mission calculation...");
        showStatus("Se calculeaza ruta...", Color.web("#f39c12"));
        
        try {
            // 1. Calculează distanța
            calculatedDistance = calculateDistance(startLat, startLng, endLat, endLng);
            System.out.println("[MapController] Distance: " + calculatedDistance + " km");
            
            // 2. Obține tipul misiunii și greutatea
            String missionType = missionTypeCombo.getValue().toLowerCase();
            System.out.println("[MapController] Mission type: " + missionType);
            
            double weight = 0;
            if ("livrare".equals(missionType)) {
                try {
                    String weightText = weightField.getText().trim();
                    if (!weightText.isEmpty()) {
                        weight = Double.parseDouble(weightText);
                    } else {
                        weight = 1.0;
                    }
                } catch (NumberFormatException e) {
                    System.err.println("[MapController] Invalid weight, using 1.0 kg");
                    weight = 1.0;
                }
            }
            System.out.println("[MapController] Weight: " + weight + " kg");
            
            // ========== COMPORTAMENT DIFERIT PENTRU OPERATOR ==========
            if (currentUser != null && currentUser.isOperator()) {
                // Operatorul NU selectează dronă - doar trimite cererea
                calculatedCost = calculateCost(calculatedDistance, missionType, weight);
                
                // Actualizează UI
                if (distanceLabel != null) {
                    distanceLabel.setText(String.format("%.2f km", calculatedDistance));
                }
                if (costLabel != null) {
                    costLabel.setText(String.format("%.2f RON (estimat)", calculatedCost));
                }
                
                // Activează butonul de trimitere cerere
                if (confirmButton != null) {
                    confirmButton.setDisable(false);
                }
                
                showStatus("Rută calculată! Completează greutatea și apasă TRIMITE CEREREA", Color.GREEN);
                return;
            }
            
            // ========== COMPORTAMENT ADMIN (original) ==========
            
            // 3. Selectează drona optimă
            selectedDrone = selectOptimalDrone(missionType, weight, calculatedDistance);
            
            if (selectedDrone == null) {
                System.out.println("[MapController] No drone available");
                showStatus("Nicio drona disponibila pentru aceasta misiune!", Color.RED);
                if (confirmButton != null) confirmButton.setDisable(true);
                if (resultCard != null) resultCard.setVisible(false);
                return;
            }
            System.out.println("[MapController] Selected drone: " + selectedDrone.getModel());
            
            // 4. Calculează costul
            calculatedCost = calculateCost(calculatedDistance, missionType, weight);
            System.out.println("[MapController] Cost: " + calculatedCost + " RON");
            
            // 5. Încearcă să obțină datele meteo (cu fallback)
            WeatherService.WeatherData weather = null;
            try {
                System.out.println("[MapController] Fetching weather data...");
                weather = WeatherService.getWeatherAt(startLat, startLng);
                System.out.println(String.format("[MapController] Weather: %.1fC, Wind: %.1f km/h, Condition: %s, Safe: %s", 
                    weather.temperature, weather.windSpeed, weather.condition, weather.isSafeToFly));
                weatherSafe = weather.isSafeToFly;
            } catch (Exception e) {
                System.err.println("[MapController] Weather API failed, using defaults: " + e.getMessage());
                weather = new WeatherService.WeatherData(20.0, 5.0, "Clear");
                weatherSafe = true;
            }
            
            // 6. Actualizează UI-ul
            updateMissionDisplay(weather);
            
            // 7. Activează butonul dacă avem dronă și meteo sigur
            if (confirmButton != null) {
                boolean canConfirm = selectedDrone != null && weatherSafe;
                confirmButton.setDisable(!canConfirm);
                System.out.println("[MapController] Confirm button enabled: " + canConfirm);
            }
            
            // 8. Afișează statusul final
            if (weatherSafe) {
                showStatus("Misiune validata! Apasa TRIMITE DRONA", Color.GREEN);
            } else {
                showStatus("ATENTIE: Conditii meteo nefavorabile!", Color.web("#e74c3c"));
            }
            
            System.out.println("[MapController] Mission calculation completed successfully");
            
        } catch (Exception e) {
            System.err.println("[MapController] ERROR during calculation:");
            e.printStackTrace();
            showStatus("Eroare la calcul: " + e.getMessage(), Color.RED);
            if (confirmButton != null) confirmButton.setDisable(true);
        }
    }

    private Drone selectOptimalDrone(String missionType, double weight, double distance) {
        List<Drone> allDrones = DatabaseManager.getInstance().getDrones();
        Drone bestDrone = null;
        double bestScore = -1;
        
        System.out.println("[MapController] Searching optimal drone for: " + missionType + ", weight: " + weight + "kg, distance: " + distance + "km");
        System.out.println("[MapController] Total drones in DB: " + allDrones.size());
        
        for (Drone drone : allDrones) {
            System.out.println("  [CHECK] " + drone.getModel() + " - Status: " + drone.getStatus() + ", Type: " + drone.getType());
            
            if (!"activa".equals(drone.getStatus())) {
                System.out.println("    [SKIP] Not active");
                continue;
            }
            
            // Verifică compatibilitatea tipului
            if ("livrare".equals(missionType)) {
                if (!"transport".equals(drone.getType())) {
                    System.out.println("    [SKIP] Type incompatible for delivery (needs transport)");
                    continue;
                }
                if (drone.getMaxPayload() < weight) {
                    System.out.println("    [SKIP] Insufficient capacity: " + drone.getMaxPayload() + " < " + weight);
                    continue;
                }
            } else if ("inspectie".equals(missionType) || "cartografiere".equals(missionType)) {
                if (!"survey".equals(drone.getType())) {
                    System.out.println("    [SKIP] Type incompatible for survey");
                    continue;
                }
            }
            
            // Verifică autonomia (distanța dus-întors + 20% rezervă)
            double timeNeeded = (distance * 2) * 1.2;
            if (drone.getAutonomy() < timeNeeded) {
                System.out.println("    [SKIP] Insufficient autonomy: " + drone.getAutonomy() + " < " + timeNeeded);
                continue;
            }
            
            // Calculează scorul
            double autonomyRatio = drone.getAutonomy() / timeNeeded;
            double score = 1.0 / Math.abs(autonomyRatio - 1.5);
            
            System.out.println("    [OK] Score: " + score);
            
            if (score > bestScore) {
                bestScore = score;
                bestDrone = drone;
            }
        }
        
        if (bestDrone != null) {
            System.out.println("[MapController] Best drone selected: " + bestDrone.getModel());
        } else {
            System.out.println("[MapController] No suitable drone found!");
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
        
        double distance = R * c;
        
        return Math.round(distance * 100.0) / 100.0;
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
        
        if (weather != null) {
            if (weatherLabel != null) {
                String icon = weather.isSafeToFly ? "✓" : "⚠";
                weatherLabel.setText(String.format("%s %.1f°C", icon, weather.temperature));
                weatherLabel.setTextFill(weather.isSafeToFly ? Color.web("#27ae60") : Color.web("#e74c3c"));
            }
            
            if (windLabel != null) {
                windLabel.setText(String.format("Vant: %.1f km/h (%s)", weather.windSpeed, weather.condition));
            }
        }
    }

    @FXML
    private void confirmMission() {
        if (startCoord == null || endCoord == null) {
            showStatus("Seteaza start si destinatie pe harta!", Color.RED);
            return;
        }
        
        // ========== COMPORTAMENT OPERATOR: Creează cerere de livrare ==========
        if (currentUser != null && currentUser.isOperator()) {
            submitDeliveryRequest();
            return;
        }
        
        // ========== COMPORTAMENT ADMIN: Lansează direct misiunea ==========
        if (selectedDrone == null) {
            showStatus("Nu a fost selectată nicio dronă!", Color.RED);
            return;
        }
        
        try {
            LocalDateTime startTime = LocalDateTime.now();
            
            Flight newFlight = new Flight(
                selectedDrone,
                startCoord,
                endCoord,
                startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
            
            // Salvează în baza de date
            DatabaseManager.getInstance().saveFlight(newFlight);
            
            // Actualizează statusul dronei
            DatabaseManager.updateDroneStatus(selectedDrone.getId(), "in_livrare");            
            System.out.println("[MapController] Mission saved to DB for drone: " + selectedDrone.getModel());
            
            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Misiune Pornita");
            success.setHeaderText("Livrarea a inceput!");
            success.setContentText(String.format(
                "Drona %s a decolat.\n\n" +
                "Distanta: %.2f km\n" +
                "Cost: %.2f RON\n\n" +
                "Te poti intoarce in Dashboard pentru monitorizare.",
                selectedDrone.getModel(),
                calculatedDistance,
                calculatedCost
            ));
            success.showAndWait();
            
            // Închide fereastra
            Stage stage = (Stage) confirmButton.getScene().getWindow();
            stage.close();
            
        } catch (Exception e) {
            System.err.println("[MapController] ERROR saving mission:");
            e.printStackTrace();
            showStatus("Eroare la salvare: " + e.getMessage(), Color.RED);
        }
    }

    /**
     * NOU: Operator trimite o cerere de livrare către Admin
     */
    private void submitDeliveryRequest() {
        try {
            // Validare greutate
            double weight = 1.0;
            try {
                String weightText = weightField.getText().trim();
                if (!weightText.isEmpty()) {
                    weight = Double.parseDouble(weightText);
                }
            } catch (NumberFormatException e) {
                showStatus("Greutate invalidă! Folosește un număr (ex: 2.5)", Color.RED);
                return;
            }
            
            if (weight <= 0 || weight > 50) {
                showStatus("Greutatea trebuie să fie între 0.1 și 50 kg!", Color.RED);
                return;
            }
            
            // Obține notele (opționale)
            String notes = "";
            if (notesField != null && notesField.getText() != null) {
                notes = notesField.getText().trim();
            }
            
            // Creează cererea în baza de date
            DatabaseManager.getInstance().createDeliveryRequest(
                currentUser.getUserId(),
                startCoord,
                endCoord,
                weight,
                notes
            );
            
            System.out.println("[MapController] Delivery request created by operator: " + currentUser.getUsername());
            
            // Mesaj de succes
            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Cerere Trimisă");
            success.setHeaderText("Cererea de livrare a fost trimisă!");
            success.setContentText(String.format(
                "Cererea ta a fost trimisă către administrator.\n\n" +
                "Detalii:\n" +
                "• Distanță: %.2f km\n" +
                "• Greutate: %.1f kg\n" +
                "• Cost estimat: %.2f RON\n\n" +
                "Vei fi notificat când cererea va fi aprobată.",
                calculatedDistance,
                weight,
                calculatedCost
            ));
            success.showAndWait();
            
            // Închide fereastra
            Stage stage = (Stage) confirmButton.getScene().getWindow();
            stage.close();
            
        } catch (Exception e) {
            System.err.println("[MapController] ERROR creating delivery request:");
            e.printStackTrace();
            showStatus("Eroare la trimiterea cererii: " + e.getMessage(), Color.RED);
        }
    }

    private void showStatus(String message, Color color) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setTextFill(color);
        }
        System.out.println("[MapController] Status: " + message);
    }
}
