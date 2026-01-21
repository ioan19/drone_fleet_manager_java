package dronefleet;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

public class DashboardController {

    // --- STATISTICI ---
    @FXML private Label totalDronesLabel;
    @FXML private Label activeDronesLabel;
    @FXML private Label maintenanceLabel;

    // --- TABEL DRONE ---
    @FXML private TableView<Drone> droneTable;
    @FXML private TableColumn<Drone, Integer> colId;
    @FXML private TableColumn<Drone, String> colModel;
    @FXML private TableColumn<Drone, String> colStatus;
    @FXML private TableColumn<Drone, String> colPayload; // Schimbat la String pentru formatare
    @FXML private TableColumn<Drone, String> colMissionType; // Coloană nouă
    @FXML private TableColumn<Drone, String> colTime; // Timp rămas

    @FXML private Label statusLabel;

    private ObservableList<Drone> droneList = FXCollections.observableArrayList();
    private Timeline refreshTimeline;

    @FXML
    public void initialize() {
        setupDroneTable();
        loadDataFromDB();
        updateStatistics();
        
        // Auto-refresh la fiecare secundă pentru countdown
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            droneTable.refresh(); // Refresh pentru a actualiza timpul rămas
            updateStatistics();
        }));
        refreshTimeline.setCycleCount(Animation.INDEFINITE);
        refreshTimeline.play();
    }

    private void setupDroneTable() {
        if (colId != null) {
            colId.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getId()));
        }
        
        colModel.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getModel()));
        
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(
            capitalizeFirst(cell.getValue().getStatus())
        ));
        
        // Capacitate cu 1 zecimală
        colPayload.setCellValueFactory(cell -> new SimpleStringProperty(
            String.format("%.1f kg", cell.getValue().getMaxPayload())
        ));
        
        // Tip misiune alocată
        if (colMissionType != null) {
            colMissionType.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getMissionType()
            ));
        }
        
        // Timp rămas
        if (colTime != null) {
            colTime.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getTimeRemaining()
            ));
            
            // Stilizare celulă pentru timp rămas
            colTime.setCellFactory(column -> new TableCell<Drone, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null || "-".equals(item)) {
                        setText("-");
                        setStyle("");
                    } else {
                        setText(item);
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }
                }
            });
        }

        droneTable.setItems(droneList);
        
        // Colorare rânduri în funcție de status
        droneTable.setRowFactory(tv -> new TableRow<Drone>() {
            @Override
            protected void updateItem(Drone item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else {
                    String status = item.getStatus();
                    if ("activa".equals(status)) {
                        setStyle("-fx-background-color: #d4edda;");
                    } else if ("mentenanta".equals(status)) {
                        setStyle("-fx-background-color: #fff3cd;");
                    } else if ("inactiva".equals(status)) {
                        setStyle("-fx-background-color: #f8d7da;");
                    } else if ("in_livrare".equals(status)) {
                        setStyle("-fx-background-color: #d1ecf1;"); // Albastru deschis
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }

    private void loadDataFromDB() {
        DatabaseManager db = DatabaseManager.getInstance();
        droneList.setAll(db.getDrones());
    }

    private void updateStatistics() {
        DatabaseManager db = DatabaseManager.getInstance();
        
        if (totalDronesLabel != null) {
            totalDronesLabel.setText(String.valueOf(db.getTotalDrones()));
        }
        if (activeDronesLabel != null) {
            activeDronesLabel.setText(String.valueOf(db.getActiveDrones()));
        }
        if (maintenanceLabel != null) {
            maintenanceLabel.setText(String.valueOf(db.getMaintenanceDrones()));
        }
    }

    @FXML
    private void addDrone() {
        try {
            Dialog<Drone> dialog = new Dialog<>();
            dialog.setTitle("Adaugă Dronă Nouă");
            dialog.setHeaderText("Completează detaliile dronei");

            ButtonType addButtonType = new ButtonType("Adaugă", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

            TextField modelField = new TextField();
            modelField.setPromptText("Model (ex: DJI Matrice 300)");
            
            ComboBox<String> typeCombo = new ComboBox<>();
            typeCombo.getItems().addAll("transport", "survey");
            typeCombo.setValue("transport");
            
            TextField payloadField = new TextField();
            payloadField.setPromptText("Capacitate (kg)");
            
            TextField autonomyField = new TextField();
            autonomyField.setPromptText("Autonomie (minute)");

            javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.add(new Label("Model:"), 0, 0);
            grid.add(modelField, 1, 0);
            grid.add(new Label("Tip:"), 0, 1);
            grid.add(typeCombo, 1, 1);
            grid.add(new Label("Capacitate (kg):"), 0, 2);
            grid.add(payloadField, 1, 2);
            grid.add(new Label("Autonomie (min):"), 0, 3);
            grid.add(autonomyField, 1, 3);

            dialog.getDialogPane().setContent(grid);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == addButtonType) {
                    try {
                        String model = modelField.getText();
                        String type = typeCombo.getValue();
                        double payload = Double.parseDouble(payloadField.getText());
                        double autonomy = Double.parseDouble(autonomyField.getText());
                        
                        return new Drone(0, model, type, "activa", payload, autonomy);
                    } catch (NumberFormatException e) {
                        showStatus("Valori numerice invalide!", Color.RED);
                        return null;
                    }
                }
                return null;
            });

            dialog.showAndWait().ifPresent(drone -> {
                DatabaseManager.getInstance().addDrone(drone);
                showStatus("Dronă adăugată: " + drone.getModel(), Color.GREEN);
                loadDataFromDB();
                updateStatistics();
            });

        } catch (Exception e) {
            e.printStackTrace();
            showStatus("Eroare: " + e.getMessage(), Color.RED);
        }
    }

    @FXML
    private void setMaintenance() {
        Drone selected = droneTable.getSelectionModel().getSelectedItem();
        
        if (selected == null) {
            showStatus("Selectează o dronă din tabel!", Color.RED);
            return;
        }
        
        if ("mentenanta".equals(selected.getStatus())) {
            showStatus("Drona este deja în mentenanță!", Color.ORANGE);
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmare Mentenanță");
        confirm.setHeaderText("Trimite drona în service?");
        confirm.setContentText(selected.getModel() + " (ID: " + selected.getId() + ")");
        
        if (confirm.showAndWait().get() == ButtonType.OK) {
            try {
                DatabaseManager.getInstance().updateDroneStatus(selected.getId(), "mentenanta");
                showStatus("✓ Drona trimisă în mentenanță", Color.GREEN);
                loadDataFromDB();
                updateStatistics();
            } catch (Exception e) {
                e.printStackTrace();
                showStatus("Eroare la actualizare!", Color.RED);
            }
        }
    }

    @FXML
    private void removeDrone() {
        Drone selected = droneTable.getSelectionModel().getSelectedItem();
        
        if (selected == null) {
            showStatus("Selectează o dronă pentru ștergere!", Color.RED);
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.WARNING);
        confirmDialog.setTitle("⚠ ATENȚIE - Ștergere Dronă");
        confirmDialog.setHeaderText("Ești sigur că vrei să ștergi această dronă?");
        confirmDialog.setContentText(
            "Dronă: " + selected.getModel() + " (ID: " + selected.getId() + ")\n" +
            "⚠ Această acțiune va șterge și toate misiunile asociate!"
        );
        
        if (confirmDialog.showAndWait().get() == ButtonType.OK) {
            try {
                DatabaseManager.getInstance().deleteDrone(selected);
                showStatus("✓ Dronă ștearsă", Color.GREEN);
                loadDataFromDB();
                updateStatistics();
            } catch (Exception e) {
                e.printStackTrace();
                showStatus("❌ Eroare: Posibil să aibă misiuni active!", Color.RED);
            }
        }
    }
    
    @FXML
    private void openMap() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/map.fxml"));
            Scene scene = new Scene(loader.load());
            
            if (getClass().getResource("/style.css") != null) {
                scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            }

            Stage mapStage = new Stage();
            mapStage.setTitle("📍 Planificare Misiune - Hartă Interactivă");
            mapStage.setScene(scene);
            mapStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showStatus("❌ Eroare la deschiderea hărții!", Color.RED);
        }
    }

    @FXML
    private void onLogout() {
        try {
            if (refreshTimeline != null) {
                refreshTimeline.stop();
            }
            
            Stage currentStage = (Stage) droneTable.getScene().getWindow();
            currentStage.close();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Scene scene = new Scene(loader.load());
            
            if (getClass().getResource("/style.css") != null) {
                scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            }

            Stage loginStage = new Stage();
            loginStage.setTitle("Drone Fleet Manager - Login");
            loginStage.setScene(scene);
            loginStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void showStatus(String message, Color color) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setTextFill(color);
            
            Timeline clearTimeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
                statusLabel.setText("");
            }));
            clearTimeline.play();
        }
    }
    
    private String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }
}