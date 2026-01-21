package dronefleet;

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

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DashboardController {

    // --- TABEL DRONE ---
    @FXML private TableView<Drone> dronesTable;
    @FXML private TableColumn<Drone, String> colModel;
    @FXML private TableColumn<Drone, String> colType;
    @FXML private TableColumn<Drone, String> colStatus;
    @FXML private TableColumn<Drone, Double> colBattery; 

    // --- TABEL ZBORURI ---
    @FXML private TableView<Flight> flightsTable;
    @FXML private TableColumn<Flight, String> colFlightDrone;
    @FXML private TableColumn<Flight, String> colOrigin;
    @FXML private TableColumn<Flight, String> colDest;
    @FXML private TableColumn<Flight, String> colTime;

    // --- FORMULAR PROGRAMARE ZBOR ---
    @FXML private ComboBox<Drone> cmbDrone;
    @FXML private ComboBox<Destination> cmbSource;
    @FXML private ComboBox<Destination> cmbDestination;
    @FXML private DatePicker dpDate;
    @FXML private TextField txtTime; // Format HH:mm
    
    // --- FORMULAR ADAUGARE DRONA ---
    @FXML private TextField txtNewModel;
    @FXML private ComboBox<String> cmbNewType; 
    @FXML private TextField txtNewPayload;
    @FXML private TextField txtNewAutonomy;

    @FXML private Label statusLabel;

    // Liste pentru date
    private ObservableList<Drone> droneList = FXCollections.observableArrayList();
    private ObservableList<Flight> flightList = FXCollections.observableArrayList();
    private ObservableList<Destination> locationList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupDroneTable();
        setupFlightTable();
        
        // Configurare ComboBox Tip Drona
        if (cmbNewType != null) {
            cmbNewType.setItems(FXCollections.observableArrayList("transport", "survey"));
        }
        
        loadDataFromDB();
    }

    private void setupDroneTable() {
        colModel.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getModel()));
        colType.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getType()));
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus()));
        colBattery.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getAutonomy()));

        dronesTable.setItems(droneList);
    }

    private void setupFlightTable() {
        colFlightDrone.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDrone().getModel()));
        colOrigin.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getOrigin()));
        colDest.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDestination()));
        colTime.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTime()));

        flightsTable.setItems(flightList);

        // Colorare randuri (Zboruri Trecute = Rosu)
        flightsTable.setRowFactory(tv -> new TableRow<Flight>() {
            @Override
            protected void updateItem(Flight item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else {
                    try {
                        Timestamp flightTime = Timestamp.valueOf(item.getTime());
                        Timestamp now = new Timestamp(System.currentTimeMillis());
                        if (flightTime.before(now)) {
                            setStyle("-fx-background-color: #ffcccc;");
                        } else {
                            setStyle("");
                        }
                    } catch (Exception e) {
                        setStyle(""); 
                    }
                }
            }
        });
    }

    private void loadDataFromDB() {
        DatabaseManager db = DatabaseManager.getInstance();

        droneList.setAll(db.getDrones());
        
        List<Destination> locs = db.getLocations();
        locationList.setAll(locs);
        
        if (cmbSource != null) cmbSource.setItems(locationList);
        if (cmbDestination != null) cmbDestination.setItems(locationList);
        if (cmbDrone != null) cmbDrone.setItems(droneList); 

        flightList.setAll(db.getFlights());
    }

    @FXML
    private void addDrone() {
        try {
            String model = txtNewModel.getText();
            String type = cmbNewType.getValue();
            String payloadStr = txtNewPayload.getText();
            String autonomyStr = txtNewAutonomy.getText();

            if (model.isEmpty() || type == null || payloadStr.isEmpty() || autonomyStr.isEmpty()) {
                statusLabel.setText("Completează detaliile dronei!");
                statusLabel.setTextFill(Color.RED);
                return;
            }

            double payload = Double.parseDouble(payloadStr);
            double autonomy = Double.parseDouble(autonomyStr);

            Drone d = new Drone(0, model, type, "activa", payload, autonomy);
            
            DatabaseManager.getInstance().addDrone(d);
            
            statusLabel.setText("Dronă adăugată!");
            statusLabel.setTextFill(Color.GREEN);
            
            txtNewModel.clear();
            txtNewPayload.clear();
            txtNewAutonomy.clear();
            
            loadDataFromDB();

        } catch (NumberFormatException e) {
            statusLabel.setText("Payload/Autonomie trebuie să fie numere!");
            statusLabel.setTextFill(Color.RED);
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Eroare la adăugare: " + e.getMessage());
        }
    }

    @FXML
    private void setMaintenance() {
        Drone selected = dronesTable.getSelectionModel().getSelectedItem();
        
        if (selected == null) {
            statusLabel.setText("Selectează o dronă din tabel!");
            statusLabel.setTextFill(Color.RED);
            return;
        }
        
        try {
            DatabaseManager.getInstance().updateDroneStatus(selected.getId(), "mentenanta");
            statusLabel.setText("Drona este acum în mentenanță.");
            statusLabel.setTextFill(Color.ORANGE);
            loadDataFromDB();
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Eroare la actualizare status!");
        }
    }

    // --- METODA NOUĂ PENTRU STERGRERE ---
    @FXML
    private void removeDrone() {
        Drone selected = dronesTable.getSelectionModel().getSelectedItem();
        
        if (selected == null) {
            statusLabel.setText("Selectează o dronă pentru a o șterge!");
            statusLabel.setTextFill(Color.RED);
            return;
        }

        try {
            // Apelăm metoda de ștergere din DatabaseManager
            DatabaseManager.getInstance().deleteDrone(selected);
            
            statusLabel.setText("Drona a fost ștearsă!");
            statusLabel.setTextFill(Color.GREEN);
            
            // Reîmprospătăm tabelul
            loadDataFromDB();
            
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Eroare la ștergere (Posibil să aibă zboruri asociate!)");
            statusLabel.setTextFill(Color.RED);
        }
    }

    @FXML
    private void onScheduleFlight() {
        try {
            Drone selectedDrone = cmbDrone.getValue();
            Destination source = cmbSource.getValue();
            Destination dest = cmbDestination.getValue();
            LocalDate date = dpDate.getValue();
            String timeStr = txtTime.getText(); 

            if (selectedDrone == null || source == null || dest == null || date == null || timeStr.isEmpty()) {
                statusLabel.setText("Completează datele zborului!");
                statusLabel.setTextFill(Color.RED);
                return;
            }

            LocalTime time = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
            LocalDateTime flightDateTime = LocalDateTime.of(date, time);
            Timestamp timestamp = Timestamp.valueOf(flightDateTime);

            Flight newFlight = new Flight(
                selectedDrone, 
                source.getCoordinates(), 
                dest.getCoordinates(),   
                timestamp.toString()
            );

            DatabaseManager.getInstance().saveFlight(newFlight);

            statusLabel.setText("Zbor programat!");
            statusLabel.setTextFill(Color.GREEN);
            loadDataFromDB(); 
            DatabaseManager.getInstance().updateDroneStatus(selectedDrone.getId(), "in_livrare");

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Eroare (Format ora HH:mm): " + e.getMessage());
        }
    }
    
    @FXML
    private void onRefresh() {
        loadDataFromDB();
    }

    @FXML
    private void onLogout() {
        try {
            Stage currentStage = (Stage) dronesTable.getScene().getWindow();
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
            statusLabel.setText("Eroare la delogare!");
        }
    }
}