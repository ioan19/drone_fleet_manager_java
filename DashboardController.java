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
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;
import java.util.stream.Collectors;

public class DashboardController {

    // ===== STATISTICI =====
    @FXML private Label totalDronesLabel;
    @FXML private Label activeDronesLabel;
    @FXML private Label maintenanceLabel;
    @FXML private Label userInfoLabel;
    @FXML private Label inDeliveryLabel;
    @FXML private Label openTicketsLabel;  // NOU: pentru tehnician

    // ===== TABEL DRONE (Admin) =====
    @FXML private TableView<Drone> droneTable;
    @FXML private TableColumn<Drone, Integer> colId;
    @FXML private TableColumn<Drone, String> colModel;
    @FXML private TableColumn<Drone, String> colType;
    @FXML private TableColumn<Drone, String> colStatus;
    @FXML private TableColumn<Drone, String> colPayload;
    @FXML private TableColumn<Drone, String> colMissionType;
    @FXML private TableColumn<Drone, String> colTime;

    // ===== TABEL CERERI ACCEPTATE - OPERATOR =====
    @FXML private TableView<DeliveryRequest> requestTable;
    @FXML private TableColumn<DeliveryRequest, Integer> colRequestId;
    @FXML private TableColumn<DeliveryRequest, String> colRequestStatus;
    @FXML private TableColumn<DeliveryRequest, Double> colWeight;
    @FXML private TableColumn<DeliveryRequest, String> colDroneAssigned;
    @FXML private TableColumn<DeliveryRequest, String> colRequestDate;

    // ===== TABEL CERERI PENDING - ADMIN =====
    @FXML private TableView<DeliveryRequest> pendingRequestTable;
    @FXML private TableColumn<DeliveryRequest, Integer> colPendingId;
    @FXML private TableColumn<DeliveryRequest, String> colPendingOperator;
    @FXML private TableColumn<DeliveryRequest, String> colPendingStart;
    @FXML private TableColumn<DeliveryRequest, String> colPendingEnd;
    @FXML private TableColumn<DeliveryRequest, Double> colPendingWeight;
    @FXML private TableColumn<DeliveryRequest, String> colPendingNotes;
    @FXML private TableColumn<DeliveryRequest, String> colPendingDate;

    // ===== TABEL TICHETE REPARAȚIE - TEHNICIAN =====
    @FXML private TableView<MaintenanceTicket> ticketTable;
    @FXML private TableColumn<MaintenanceTicket, Integer> colTicketId;
    @FXML private TableColumn<MaintenanceTicket, String> colTicketDrone;
    @FXML private TableColumn<MaintenanceTicket, String> colTicketType;
    @FXML private TableColumn<MaintenanceTicket, String> colTicketStatus;
    @FXML private TableColumn<MaintenanceTicket, String> colTicketDate;
    @FXML private TableColumn<MaintenanceTicket, String> colTicketNotes;

    // ===== BUTOANE =====
    @FXML private Button addDroneButton;
    @FXML private Button setMaintenanceButton;
    @FXML private Button removeDroneButton;
    @FXML private Button openMapButton;
    @FXML private Button acceptRequestButton;
    @FXML private Button rejectRequestButton;
    @FXML private Button completeRepairButton;   // NOU: finalizează reparația
    @FXML private Button startRepairButton;      // NOU: începe lucrul

    // ===== CONTAINERE =====
    @FXML private VBox droneSection;
    @FXML private VBox operatorSection;
    @FXML private VBox adminRequestSection;
    @FXML private VBox technicianSection;        // NOU: secțiune tehnician
    @FXML private javafx.scene.layout.HBox statsBox;
    @FXML private VBox inDeliveryCard;
    @FXML private VBox ticketsCard;              // NOU: card tichete deschise

    @FXML private Label statusLabel;
    @FXML private Label sectionTitle;

    private ObservableList<Drone> droneList = FXCollections.observableArrayList();
    private ObservableList<DeliveryRequest> requestList = FXCollections.observableArrayList();
    private ObservableList<DeliveryRequest> pendingRequestList = FXCollections.observableArrayList();
    private ObservableList<MaintenanceTicket> ticketList = FXCollections.observableArrayList();
    
    private Timeline refreshTimeline;
    private User currentUser;

    @FXML
    public void initialize() {
        currentUser = Session.getCurrentUser();
        
        if (currentUser == null) {
            System.err.println("[Dashboard] ERROR: No user in session!");
            showStatus("Sesiune invalida!", Color.RED);
            return;
        }

        System.out.println("[Dashboard] Initializing for user: " + currentUser.getUsername() + " (Role: " + currentUser.getRole() + ")");
        
        setupUI();
        setupTables();
        loadDataFromDB();
        updateStatistics();
        
        // Auto-refresh la fiecare secundă
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (droneTable != null && droneTable.isVisible()) {
                droneTable.refresh();
            }
            updateStatistics();
        }));
        refreshTimeline.setCycleCount(Animation.INDEFINITE);
        refreshTimeline.play();
        
        System.out.println("[Dashboard] Initialization completed");
    }

    private void setupUI() {
        if (userInfoLabel != null) {
            userInfoLabel.setText("Utilizator: " + currentUser.getFullName() + " | Rol: " + currentUser.getRole().toUpperCase());
        }

        // =========== OPERATOR ===========
        if (currentUser.isOperator()) {
            System.out.println("[Dashboard] Setting up UI for OPERATOR");
            
            if (sectionTitle != null) sectionTitle.setText("Livrările Mele Acceptate");
            
            // Ascunde secțiunile care nu sunt pentru operator
            hideSection(droneSection);
            hideSection(droneTable);
            hideSection(adminRequestSection);
            hideSection(technicianSection);
            hideSection(ticketTable);
            
            // Afișează secțiunea operator
            showSection(operatorSection);
            showSection(requestTable);
            
            // Butoane
            hideButton(addDroneButton);
            hideButton(removeDroneButton);
            hideButton(setMaintenanceButton);
            hideButton(acceptRequestButton);
            hideButton(rejectRequestButton);
            hideButton(completeRepairButton);
            hideButton(startRepairButton);
            
            showButton(openMapButton); // Poate trimite cereri noi
            
            // Ascunde statistici complete
            if (statsBox != null) {
                statsBox.setVisible(false);
                statsBox.setManaged(false);
            }
            
        // =========== TEHNICIAN ===========
        } else if (currentUser.isTechnician()) {
            System.out.println("[Dashboard] Setting up UI for TECHNICIAN");
            
            if (sectionTitle != null) sectionTitle.setText("Tichete de Reparație");
            
            // Ascunde secțiunile care nu sunt pentru tehnician
            hideSection(droneSection);
            hideSection(droneTable);
            hideSection(operatorSection);
            hideSection(requestTable);
            hideSection(adminRequestSection);
            hideSection(pendingRequestTable);
            hideSection(inDeliveryCard);
            
            // Afișează secțiunea tehnician
            showSection(technicianSection);
            showSection(ticketTable);
            showSection(ticketsCard);
            
            // Butoane
            hideButton(addDroneButton);
            hideButton(removeDroneButton);
            hideButton(setMaintenanceButton);
            hideButton(openMapButton);
            hideButton(acceptRequestButton);
            hideButton(rejectRequestButton);
            
            showButton(startRepairButton);
            showButton(completeRepairButton);
            
        // =========== ADMIN ===========
        } else {
            System.out.println("[Dashboard] Setting up UI for ADMIN");
            
            if (sectionTitle != null) sectionTitle.setText("Monitorizare Flotă (Live)");
            
            // Afișează secțiunile admin
            showSection(droneSection);
            showSection(droneTable);
            showSection(adminRequestSection);
            showSection(pendingRequestTable);
            showSection(inDeliveryCard);
            
            // Ascunde secțiunile care nu sunt pentru admin
            hideSection(operatorSection);
            hideSection(requestTable);
            hideSection(technicianSection);
            hideSection(ticketTable);
            hideSection(ticketsCard);
            
            // Butoane admin
            showButton(addDroneButton);
            showButton(removeDroneButton);
            showButton(setMaintenanceButton);
            showButton(openMapButton);
            showButton(acceptRequestButton);
            showButton(rejectRequestButton);
            
            hideButton(completeRepairButton);
            hideButton(startRepairButton);
        }
    }
    
    private void hideSection(javafx.scene.Node node) {
        if (node != null) {
            node.setVisible(false);
            node.setManaged(false);
        }
    }
    
    private void showSection(javafx.scene.Node node) {
        if (node != null) {
            node.setVisible(true);
            node.setManaged(true);
        }
    }
    
    private void hideButton(Button btn) {
        if (btn != null) {
            btn.setVisible(false);
            btn.setManaged(false);
        }
    }
    
    private void showButton(Button btn) {
        if (btn != null) {
            btn.setVisible(true);
            btn.setManaged(true);
        }
    }

    private void setupTables() {
        if (currentUser.isAdmin()) {
            setupDroneTable();
            setupPendingRequestTable();
        } else if (currentUser.isOperator()) {
            setupOperatorRequestTable();
        } else if (currentUser.isTechnician()) {
            setupTicketTable();
        }
    }

    private void setupDroneTable() {
        if (droneTable == null) return;
        
        if (colId != null) colId.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getId()));
        if (colModel != null) colModel.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getModel()));
        if (colType != null) colType.setCellValueFactory(cell -> {
            String type = cell.getValue().getType();
            return new SimpleStringProperty(type.equals("transport") ? "Transport" : "Survey");
        });
        if (colStatus != null) colStatus.setCellValueFactory(cell -> new SimpleStringProperty(formatStatus(cell.getValue().getStatus())));
        if (colPayload != null) colPayload.setCellValueFactory(cell -> new SimpleStringProperty(String.format("%.1f kg", cell.getValue().getMaxPayload())));
        if (colMissionType != null) colMissionType.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMissionType()));
        if (colTime != null) {
            colTime.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTimeRemaining()));
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
        
        // Row styling
        droneTable.setRowFactory(tv -> new TableRow<Drone>() {
            @Override
            protected void updateItem(Drone item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else {
                    String status = item.getStatus();
                    if ("activa".equals(status)) setStyle("-fx-background-color: #d4edda;");
                    else if ("mentenanta".equals(status)) setStyle("-fx-background-color: #fff3cd;");
                    else if ("inactiva".equals(status)) setStyle("-fx-background-color: #f8d7da;");
                    else if ("in_livrare".equals(status)) setStyle("-fx-background-color: #d1ecf1;");
                    else setStyle("");
                }
            }
        });
    }

    private void setupOperatorRequestTable() {
        if (requestTable == null) return;
        
        System.out.println("[Dashboard] Setting up operator accepted requests table");
        
        if (colRequestId != null) colRequestId.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getRequestId()));
        if (colRequestStatus != null) colRequestStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatusDisplay()));
        if (colWeight != null) colWeight.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getWeight()));
        if (colDroneAssigned != null) colDroneAssigned.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDroneModel()));
        if (colRequestDate != null) colRequestDate.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRequestDate()));

        requestTable.setItems(requestList);
    }

    private void setupPendingRequestTable() {
        if (pendingRequestTable == null) return;
        
        System.out.println("[Dashboard] Setting up admin pending request table");
        
        if (colPendingId != null) colPendingId.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getRequestId()));
        if (colPendingOperator != null) colPendingOperator.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getOperatorName()));
        if (colPendingStart != null) colPendingStart.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStartCoordShort()));
        if (colPendingEnd != null) colPendingEnd.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEndCoordShort()));
        if (colPendingWeight != null) colPendingWeight.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getWeight()));
        if (colPendingNotes != null) colPendingNotes.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNotes()));
        if (colPendingDate != null) colPendingDate.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRequestDate()));

        pendingRequestTable.setItems(pendingRequestList);
    }

    private void setupTicketTable() {
        if (ticketTable == null) return;
        
        System.out.println("[Dashboard] Setting up technician ticket table");
        
        if (colTicketId != null) colTicketId.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getTicketId()));
        if (colTicketDrone != null) colTicketDrone.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDroneModel()));
        if (colTicketType != null) colTicketType.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDroneTypeDisplay()));
        if (colTicketStatus != null) colTicketStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatusDisplay()));
        if (colTicketDate != null) colTicketDate.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDateCreated()));
        if (colTicketNotes != null) colTicketNotes.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNotes()));

        ticketTable.setItems(ticketList);
        
        // Row styling pentru tichete
        ticketTable.setRowFactory(tv -> new TableRow<MaintenanceTicket>() {
            @Override
            protected void updateItem(MaintenanceTicket item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else {
                    String status = item.getStatus();
                    if ("deschis".equals(status)) setStyle("-fx-background-color: #fff3cd;");
                    else if ("in_lucru".equals(status)) setStyle("-fx-background-color: #d1ecf1;");
                    else setStyle("");
                }
            }
        });
    }

    private void loadDataFromDB() {
        DatabaseManager db = DatabaseManager.getInstance();
        
        if (currentUser.isTechnician()) {
            System.out.println("[Dashboard] Loading maintenance tickets for technician");
            ticketList.setAll(db.getOpenMaintenanceTickets());
            
        } else if (currentUser.isOperator()) {
            // Operatorul vede DOAR cererile ACCEPTATE
            System.out.println("[Dashboard] Loading ACCEPTED requests for operator: " + currentUser.getUserId());
            requestList.setAll(db.getAcceptedDeliveryRequests(currentUser.getUserId()));
            
        } else {
            // Admin vede toate dronele și cererile pending
            System.out.println("[Dashboard] Loading all drones for admin");
            droneList.setAll(db.getDrones());
            
            System.out.println("[Dashboard] Loading pending requests for admin");
            pendingRequestList.setAll(db.getPendingDeliveryRequests());
        }
    }

    private void updateStatistics() {
        DatabaseManager db = DatabaseManager.getInstance();
        
        if (totalDronesLabel != null) totalDronesLabel.setText(String.valueOf(db.getTotalDrones()));
        if (activeDronesLabel != null) activeDronesLabel.setText(String.valueOf(db.getActiveDrones()));
        if (maintenanceLabel != null) maintenanceLabel.setText(String.valueOf(db.getMaintenanceDronesCount()));
        if (inDeliveryLabel != null) inDeliveryLabel.setText(String.valueOf(db.getInDeliveryDronesCount()));
        if (openTicketsLabel != null) openTicketsLabel.setText(String.valueOf(db.getOpenTicketsCount()));
    }
    
    private String formatStatus(String status) {
        if (status == null) return "N/A";
        switch (status) {
            case "activa": return "Activa";
            case "mentenanta": return "Mentenanta";
            case "inactiva": return "Inactiva";
            case "in_livrare": return "In Livrare";
            default: return status;
        }
    }

    // ==================== ACȚIUNI ADMIN ====================

    @FXML
    private void addDrone() {
        if (!currentUser.isAdmin()) {
            showStatus("Acces interzis!", Color.RED);
            return;
        }
        
        try {
            Dialog<Drone> dialog = new Dialog<>();
            dialog.setTitle("Adauga Drona Noua");
            dialog.setHeaderText("Completeaza detaliile dronei");

            ButtonType addButtonType = new ButtonType("Adauga", ButtonBar.ButtonData.OK_DONE);
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

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));
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
                showStatus("Drona adaugata: " + drone.getModel(), Color.GREEN);
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
        if (!currentUser.isAdmin()) {
            showStatus("Acces interzis!", Color.RED);
            return;
        }
        
        Drone selected = droneTable.getSelectionModel().getSelectedItem();
        
        if (selected == null) {
            showStatus("Selecteaza o drona din tabel!", Color.RED);
            return;
        }
        
        if ("mentenanta".equals(selected.getStatus())) {
            showStatus("Drona este deja in mentenanta!", Color.ORANGE);
            return;
        }
        
        if ("in_livrare".equals(selected.getStatus())) {
            showStatus("Nu poti trimite in mentenanta o drona in livrare!", Color.RED);
            return;
        }
        
        // Dialog pentru descrierea problemei
        TextInputDialog problemDialog = new TextInputDialog();
        problemDialog.setTitle("Trimite in Service");
        problemDialog.setHeaderText("Descriere problema pentru " + selected.getModel());
        problemDialog.setContentText("Problema raportata:");
        
        problemDialog.showAndWait().ifPresent(problem -> {
            try {
                DatabaseManager.getInstance().createMaintenanceTicket(selected.getId(), problem);
                showStatus("Drona trimisa in mentenanta cu tichet creat", Color.GREEN);
                loadDataFromDB();
                updateStatistics();
            } catch (Exception e) {
                e.printStackTrace();
                showStatus("Eroare la creare tichet!", Color.RED);
            }
        });
    }

    @FXML
    private void removeDrone() {
        if (!currentUser.isAdmin()) {
            showStatus("Acces interzis!", Color.RED);
            return;
        }
        
        Drone selected = droneTable.getSelectionModel().getSelectedItem();
        
        if (selected == null) {
            showStatus("Selecteaza o drona pentru stergere!", Color.RED);
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.WARNING);
        confirmDialog.setTitle("ATENTIE - Stergere Drona");
        confirmDialog.setHeaderText("Esti sigur ca vrei sa stergi aceasta drona?");
        confirmDialog.setContentText("Drona: " + selected.getModel() + " (ID: " + selected.getId() + ")");
        
        if (confirmDialog.showAndWait().get() == ButtonType.OK) {
            try {
                DatabaseManager.getInstance().deleteDrone(selected);
                showStatus("Drona stearsa", Color.GREEN);
                loadDataFromDB();
                updateStatistics();
            } catch (Exception e) {
                e.printStackTrace();
                showStatus("Eroare la stergere!", Color.RED);
            }
        }
    }

    @FXML
    private void acceptRequest() {
        if (!currentUser.isAdmin()) {
            showStatus("Acces interzis!", Color.RED);
            return;
        }
        
        DeliveryRequest selected = pendingRequestTable.getSelectionModel().getSelectedItem();
        
        if (selected == null) {
            showStatus("Selecteaza o cerere din lista!", Color.RED);
            return;
        }
        
        // Găsește drone disponibile
        List<Drone> availableDrones = DatabaseManager.getInstance().getDrones()
            .stream()
            .filter(d -> "activa".equals(d.getStatus()) && "transport".equals(d.getType()))
            .filter(d -> d.getMaxPayload() >= selected.getWeight())
            .collect(Collectors.toList());
        
        if (availableDrones.isEmpty()) {
            showStatus("Nu exista drone disponibile pentru aceasta cerere!", Color.RED);
            return;
        }
        
        ChoiceDialog<Drone> droneDialog = new ChoiceDialog<>(availableDrones.get(0), availableDrones);
        droneDialog.setTitle("Aloca Drona");
        droneDialog.setHeaderText("Selecteaza drona pentru livrare");
        droneDialog.setContentText("Drona:");
        
        droneDialog.showAndWait().ifPresent(drone -> {
            try {
                DatabaseManager.getInstance().assignDroneToRequest(selected.getRequestId(), drone.getId());
                showStatus("Cererea #" + selected.getRequestId() + " acceptata!", Color.GREEN);
                loadDataFromDB();
            } catch (Exception e) {
                e.printStackTrace();
                showStatus("Eroare la acceptare!", Color.RED);
            }
        });
    }

    @FXML
    private void rejectRequest() {
        if (!currentUser.isAdmin()) {
            showStatus("Acces interzis!", Color.RED);
            return;
        }
        
        DeliveryRequest selected = pendingRequestTable.getSelectionModel().getSelectedItem();
        
        if (selected == null) {
            showStatus("Selecteaza o cerere din lista!", Color.RED);
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Respinge Cerere");
        confirm.setHeaderText("Esti sigur ca vrei sa respingi aceasta cerere?");
        confirm.setContentText("Cererea #" + selected.getRequestId() + " de la " + selected.getOperatorName());
        
        if (confirm.showAndWait().get() == ButtonType.OK) {
            try {
                DatabaseManager.getInstance().rejectDeliveryRequest(selected.getRequestId());
                showStatus("Cererea a fost respinsa", Color.ORANGE);
                loadDataFromDB();
            } catch (Exception e) {
                e.printStackTrace();
                showStatus("Eroare la respingere!", Color.RED);
            }
        }
    }

    // ==================== ACȚIUNI TEHNICIAN ====================

    @FXML
    private void startRepair() {
        if (!currentUser.isTechnician()) {
            showStatus("Acces interzis!", Color.RED);
            return;
        }
        
        MaintenanceTicket selected = ticketTable.getSelectionModel().getSelectedItem();
        
        if (selected == null) {
            showStatus("Selecteaza un tichet din lista!", Color.RED);
            return;
        }
        
        if ("in_lucru".equals(selected.getStatus())) {
            showStatus("Tichetul este deja in lucru!", Color.ORANGE);
            return;
        }
        
        try {
            DatabaseManager.getInstance().startWorkingOnTicket(selected.getTicketId());
            showStatus("Ai inceput lucrul la " + selected.getDroneModel(), Color.GREEN);
            loadDataFromDB();
        } catch (Exception e) {
            e.printStackTrace();
            showStatus("Eroare!", Color.RED);
        }
    }

    @FXML
    private void completeRepair() {
        if (!currentUser.isTechnician()) {
            showStatus("Acces interzis!", Color.RED);
            return;
        }
        
        MaintenanceTicket selected = ticketTable.getSelectionModel().getSelectedItem();
        
        if (selected == null) {
            showStatus("Selecteaza un tichet din lista!", Color.RED);
            return;
        }
        
        // Dialog pentru finalizarea reparației
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Finalizeaza Reparatia");
        dialog.setHeaderText("Completeaza detaliile reparatiei pentru " + selected.getDroneModel());

        ButtonType completeButton = new ButtonType("Finalizeaza", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(completeButton, ButtonType.CANCEL);

        // ComboBox pentru tipul reparației
        ComboBox<String> repairTypeCombo = new ComboBox<>();
        repairTypeCombo.getItems().addAll(
            "Inlocuire motor",
            "Inlocuire elice",
            "Reparatie sistem GPS",
            "Inlocuire baterie",
            "Calibrare senzori",
            "Reparatie camera",
            "Actualizare firmware",
            "Curatare generala",
            "Inlocuire componente electronice",
            "Altele"
        );
        repairTypeCombo.setValue("Calibrare senzori");
        repairTypeCombo.setEditable(true);

        // TextArea pentru note
        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Descriere detaliata a reparatiei efectuate...");
        notesArea.setPrefRowCount(4);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));
        grid.add(new Label("Tip Reparatie:"), 0, 0);
        grid.add(repairTypeCombo, 1, 0);
        grid.add(new Label("Note / Detalii:"), 0, 1);
        grid.add(notesArea, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(450);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == completeButton) {
                return new String[] { repairTypeCombo.getValue(), notesArea.getText() };
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            try {
                String repairType = result[0];
                String notes = result[1];
                
                DatabaseManager.getInstance().completeMaintenanceTicket(
                    selected.getTicketId(),
                    selected.getDroneId(),
                    repairType,
                    notes
                );
                
                showStatus("Reparatie finalizata! Drona " + selected.getDroneModel() + " este activa.", Color.GREEN);
                loadDataFromDB();
                updateStatistics();
            } catch (Exception e) {
                e.printStackTrace();
                showStatus("Eroare la finalizare!", Color.RED);
            }
        });
    }

    // ==================== NAVIGARE ====================
    
    @FXML
    private void openMap() {
        try {
            System.out.println("[Dashboard] Opening Mission Planner");
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/map.fxml"));
            Scene scene = new Scene(loader.load());
            
            if (getClass().getResource("/style.css") != null) {
                scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            }

            Stage mapStage = new Stage();
            
            // Titlu diferit pentru operator
            if (currentUser.isOperator()) {
                mapStage.setTitle("Trimite Cerere de Livrare - Harta");
            } else {
                mapStage.setTitle("Planificare Misiune - Harta Interactiva");
            }
            
            mapStage.setScene(scene);
            mapStage.setOnHidden(e -> {
                loadDataFromDB();
                updateStatistics();
            });
            mapStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showStatus("Eroare la deschiderea hartii!", Color.RED);
        }
    }

    @FXML
    private void onLogout() {
        try {
            System.out.println("[Dashboard] User logout");
            
            if (refreshTimeline != null) {
                refreshTimeline.stop();
            }
            
            Session.clearSession();
            
            Stage currentStage = (Stage) statusLabel.getScene().getWindow();
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
    
    // ==================== UTILITĂȚI ====================
    
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
}