package dronefleet;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import java.util.Optional;

public class FleetController {

    @FXML private TableView<Drone> droneTable;
    @FXML private TableColumn<Drone, Integer> colId;
    @FXML private TableColumn<Drone, String> colModel;
    @FXML private TableColumn<Drone, String> colStatus;
    @FXML private TableColumn<Drone, Double> colPayload;
    @FXML private TableColumn<Drone, String> colTime;

    @FXML private Label totalLabel;
    @FXML private Label activeLabel;
    @FXML private Label maintLabel;

    private Timeline autoRefresh;

    @FXML
    public void initialize() {
        // 1. Configurare Coloane
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colModel.setCellValueFactory(new PropertyValueFactory<>("model"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colPayload.setCellValueFactory(new PropertyValueFactory<>("maxPayload"));
        
        // Coloana dinamica pentru timp (foloseste metoda din Drone.java)
        colTime.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getTimeRemainingDisplay()));

        // 2. Incarcare initiala
        refreshData();

        // 3. Auto-refresh la fiecare secunda (pentru cronometre si status)
        autoRefresh = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            droneTable.refresh(); // Doar redeseneaza tabelul, nu face query in DB
            updateStats();       // Actualizeaza cifrele de sus
        }));
        autoRefresh.setCycleCount(Timeline.INDEFINITE);
        autoRefresh.play();
    }

    @FXML
    public void refreshData() {
        // Reincarca datele din Baza de Date
        droneTable.setItems(FXCollections.observableArrayList(DatabaseManager.getAllDrones()));
        updateStats();
    }

    private void updateStats() {
        if (totalLabel != null) {
            totalLabel.setText(String.valueOf(DatabaseManager.countTotal()));
            activeLabel.setText(String.valueOf(DatabaseManager.countActive()));
            
            // Numaram dronele care NU sunt active (mentenanta + in livrare)
            long unavailable = DatabaseManager.getAllDrones().stream()
                    .filter(d -> !"activa".equals(d.getStatus()))
                    .count();
            maintLabel.setText(String.valueOf(unavailable));
        }
    }

    // --- LOGICA ADAUGARE DRONA ---
    @FXML
    private void addDrone() {
        Dialog<Drone> dialog = new Dialog<>();
        dialog.setTitle("Adaugă Dronă Nouă");
        dialog.setHeaderText("Introduceți detaliile noii drone");

        // Butoane
        ButtonType addButton = new ButtonType("Adaugă", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButton, ButtonType.CANCEL);

        // Formular
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);

        TextField modelField = new TextField(); modelField.setPromptText("Model (ex: DJI Mini)");
        TextField payloadField = new TextField("2.5");
        TextField autonomyField = new TextField("30");
        
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("activa", "in mentenanta", "defecta");
        statusCombo.setValue("activa");

        grid.add(new Label("Model:"), 0, 0); grid.add(modelField, 1, 0);
        grid.add(new Label("Status:"), 0, 1); grid.add(statusCombo, 1, 1);
        grid.add(new Label("Capacitate (kg):"), 0, 2); grid.add(payloadField, 1, 2);
        grid.add(new Label("Autonomie (min):"), 0, 3); grid.add(autonomyField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // Convertor rezultat
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButton) {
                try {
                    // Nota: ID-ul e auto-increment in DB, punem 0 aici
                    return new Drone(0, modelField.getText(), "transport", 
                                     statusCombo.getValue(), 
                                     Double.parseDouble(payloadField.getText()), 
                                     Integer.parseInt(autonomyField.getText()));
                } catch (Exception e) { return null; }
            }
            return null;
        });

        Optional<Drone> result = dialog.showAndWait();
        result.ifPresent(drone -> {
            // AICI TREBUIE IMPLEMENTAT UN INSERT IN DATABASEMANAGER
            // Momentan doar dam refresh pentru a simula
            System.out.println("Drona de adaugat: " + drone.getModel());
            // DatabaseManager.insertDrone(drone); <--- De implementat in DatabaseManager
            refreshData(); 
        });
    }

    // --- LOGICA MENTENANTA ---
    @FXML
    private void setMaintenance() {
        Drone selected = droneTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Eroare", "Selectează o dronă din tabel!");
            return;
        }
        
        if ("in_livrare".equals(selected.getStatus())) {
            showAlert("Imposibil", "Drona este în misiune!");
            return;
        }

        TextInputDialog td = new TextInputDialog("10");
        td.setTitle("Trimite în Service");
        td.setHeaderText("Setare Mentenanță pentru " + selected.getModel());
        td.setContentText("Durata (minute):");

        td.showAndWait().ifPresent(val -> {
            try {
                int minutes = Integer.parseInt(val);
                // Calculam timpul
                long durationMs = minutes * 60 * 1000; // Timp real
                // Demo: long durationMs = minutes * 1000; 
                
                selected.setMaintenanceEndTime(System.currentTimeMillis() + durationMs);
                selected.setStatus("in mentenanta");
                
                // Actualizam in DB
                DatabaseManager.updateDroneStatus(selected.getId(), "in mentenanta");
                
                refreshData();
            } catch (NumberFormatException e) {
                showAlert("Eroare", "Introdu un număr valid!");
            }
        });
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}