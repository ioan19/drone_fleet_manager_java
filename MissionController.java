package dronefleet;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MissionController {

    @FXML private WebView webView;
    @FXML private ComboBox<String> missionTypeCombo;
    @FXML private TextField weightField;
    @FXML private VBox weightContainer;
    
    @FXML private Label droneLabel, distanceLabel, costLabel, statusLabel;
    @FXML private VBox resultCard;
    @FXML private Button confirmButton;

    private WebEngine webEngine;
    // Cheia ta API
    private static final String LOCATION_API_KEY = "pk.158719224b8587f1e2f1cd81fed13147";
    
    private Drone selectedDroneForMission;
    private double currentMissionDurationMin;
    // Variabile pentru coordonatele primite de la JS
    private double currentStartLat, currentStartLon, currentEndLat, currentEndLon;

    @FXML
    public void initialize() {
        // 1. Initializare Harta
        webEngine = webView.getEngine();
        webEngine.load(getClass().getResource("/map_view.html").toExternalForm());

        // Conectare Bridge Java <-> JS
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaApp", this);
            }
        });

        // 2. UI Setup
        missionTypeCombo.setItems(FXCollections.observableArrayList("Livrare (Transport)", "Survey (Inspectie)"));
        missionTypeCombo.setValue("Livrare (Transport)");
        
        missionTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isSurvey = newVal.contains("Survey");
            weightContainer.setVisible(!isSurvey);
            weightContainer.setManaged(!isSurvey);
            // Resetam harta cand schimbam tipul misiunii
            webEngine.executeScript("if(window.resetMap) resetMap()");
            resetUI();
        });

        confirmButton.setOnAction(this::launchMission);
    }

    // --- METODA APELATA DE JAVASCRIPT ---
    public void receiveCoordinatesFromJS(double startLat, double startLon, double endLat, double endLon) {
        this.currentStartLat = startLat;
        this.currentStartLon = startLon;
        this.currentEndLat = endLat;
        this.currentEndLon = endLon;

        // Calculele trebuie sa ruleze pe thread-ul JavaFX
        Platform.runLater(() -> calculateMissionLogic(startLat, startLon, endLat, endLon));
    }

    private void calculateMissionLogic(double startLat, double startLon, double destLat, double destLon) {
        String typeStr = missionTypeCombo.getValue();
        boolean isDelivery = typeStr.contains("Livrare");
        selectedDroneForMission = null;

        // 1. Calcul Distanta / Arie
        double distKm = haversine(startLat, startLon, destLat, destLon);
        
        // Formula simpla: 40km/h viteza + 10 min setup
        currentMissionDurationMin = (distKm / 40.0) * 60 + 10;

        // 2. Preluare Greutate
        double weight = 0;
        try { 
            if (isDelivery) weight = Double.parseDouble(weightField.getText()); 
        } catch(Exception e) { 
            weight = 0; 
        }
        final double finalWeight = weight;

        // 3. FILTRARE INTELIGENTA DRONE
        // Cautam doar dronele ACTIVE si care suporta greutatea
        List<Drone> candidates = DatabaseManager.getAllDrones().stream()
                .filter(d -> "activa".equals(d.getStatus())) // Ignoram mentenanta/ocupate
                .filter(d -> d.getMaxPayload() >= finalWeight)
                .sorted(Comparator.comparingDouble(Drone::getMaxPayload)) // Cea mai eficienta prima
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            showError("Nu există drone active capabile să transporte " + finalWeight + " kg!");
            return;
        }

        // 4. Afisare Rezultat
        selectedDroneForMission = candidates.get(0);
        double cost = isDelivery ? (20 + distKm * 2 + finalWeight * 5) : (100 + distKm * 10);
        
        showResult(selectedDroneForMission, cost, "Distanță", String.format("%.2f km", distKm));
    }

    private void launchMission(ActionEvent event) {
        if (selectedDroneForMission == null) return;

        // 1. Actualizare Baza de Date
        // Setam statusul in DB (asigura-te ca ENUM-ul din DB suporta 'in_livrare'!)
        DatabaseManager.updateDroneStatus(selectedDroneForMission.getId(), "in_livrare");
        
        String sCoord = currentStartLat + "," + currentStartLon;
        String eCoord = currentEndLat + "," + currentEndLon;
        String dbType = missionTypeCombo.getValue().contains("Livrare") ? "livrare" : "inspectie";
        
        DatabaseManager.saveMission(selectedDroneForMission.getId(), sCoord, eCoord, (int)currentMissionDurationMin, dbType);

        // 2. Simulare (Thread separat)
        // PENTRU DEMO: 1 minut = 1 secunda. Pt realitate: * 60 * 1000
        long durationMillis = (long) (currentMissionDurationMin * 1000); 
        
        // Actualizam obiectul local pentru a reflecta schimbarea imediat (daca folosim aceeasi instanta)
        selectedDroneForMission.setStatus("in_livrare");
        selectedDroneForMission.setMissionEndTime(System.currentTimeMillis() + durationMillis);

        new Thread(() -> {
            try { 
                Thread.sleep(durationMillis); 
                
                // Misiune Gata -> Update DB
                DatabaseManager.updateDroneStatus(selectedDroneForMission.getId(), "activa");
                System.out.println("Misiune finalizata pentru drona " + selectedDroneForMission.getId());
                
            } catch (Exception e) { e.printStackTrace(); }
        }).start();

        // 3. Feedback Utilizator
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Misiune Lansată");
        alert.setHeaderText("Succes!");
        alert.setContentText("Drona " + selectedDroneForMission.getModel() + " a plecat.\nVerifică tab-ul 'Monitorizare Flotă' pentru status.");
        alert.showAndWait();

        // 4. Resetare Interfata
        webEngine.executeScript("if(window.resetMap) resetMap()");
        resetUI();
    }

    // --- Metode Utilitare ---

    private void showResult(Drone d, double cost, String label, String val) {
        resultCard.setVisible(true);
        statusLabel.setText("Parametri Validați");
        statusLabel.setTextFill(Color.GREEN);
        droneLabel.setText(d.getModel());
        distanceLabel.setText(val);
        costLabel.setText(String.format("%.2f RON", cost));
        confirmButton.setDisable(false);
    }
    
    private void showError(String msg) {
        statusLabel.setText(msg);
        statusLabel.setTextFill(Color.RED);
        resultCard.setVisible(false);
        confirmButton.setDisable(true);
    }

    private void resetUI() {
        resultCard.setVisible(false);
        statusLabel.setText("Așteptare input...");
        statusLabel.setTextFill(Color.BLACK);
        confirmButton.setDisable(true);
        selectedDroneForMission = null;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2) + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.sin(dLon/2)*Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }
}