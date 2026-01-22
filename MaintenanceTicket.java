package dronefleet;

public class MaintenanceTicket {
    private int maintenanceId;
    private int droneId;
    private String droneModel;
    private String droneType;
    private String datePerformed;
    private String type;          // "preventiva" sau "reparatie"
    private String repairType;    // Tipul reparației (completat de tehnician)
    private String statusTichet;  // "deschis", "in_lucru", "finalizat"
    private String notes;
    
    public MaintenanceTicket(int maintenanceId, int droneId, String droneModel, String droneType,
                           String datePerformed, String type, String repairType, 
                           String statusTichet, String notes) {
        this.maintenanceId = maintenanceId;
        this.droneId = droneId;
        this.droneModel = droneModel;
        this.droneType = droneType;
        this.datePerformed = datePerformed;
        this.type = type;
        this.repairType = repairType;
        this.statusTichet = statusTichet;
        this.notes = notes;
    }
    
    // Getters
    public int getMaintenanceId() { return maintenanceId; }
    public int getTicketId() { return maintenanceId; } // Alias pentru compatibilitate
    public int getDroneId() { return droneId; }
    public String getDroneModel() { return droneModel; }
    public String getDroneType() { return droneType; }
    public String getDatePerformed() { return datePerformed; }
    public String getDate() { return datePerformed; } // Alias pentru compatibilitate
    public String getType() { return type; }
    public String getRepairType() { return repairType != null ? repairType : "-"; }
    public String getStatusTichet() { return statusTichet; }
    public String getStatus() { return statusTichet; } // Alias pentru compatibilitate
    public String getNotes() { return notes != null ? notes : "-"; }
    
    // Setters
    public void setRepairType(String repairType) { this.repairType = repairType; }
    public void setStatusTichet(String statusTichet) { this.statusTichet = statusTichet; }
    public void setNotes(String notes) { this.notes = notes; }
    
    /**
     * Returnează data creării tichetului (alias pentru datePerformed)
     */
    public String getDateCreated() { 
        return datePerformed; 
    }
    
    /**
     * Returnează tipul dronei formatat pentru afișare
     */
    public String getDroneTypeDisplay() {
        if (droneType == null) return "N/A";
        return droneType.substring(0, 1).toUpperCase() + droneType.substring(1);
    }
    
    /**
     * Returnează tipul formatat pentru afișare
     */
    public String getTypeDisplay() {
        if (type == null) return "N/A";
        switch(type.toLowerCase()) {
            case "preventiva": return "Preventivă";
            case "reparatie": return "Reparație";
            default: return type;
        }
    }
    
    /**
     * Returnează statusul formatat pentru afișare
     */
    public String getStatusDisplay() {
        if (statusTichet == null) return "N/A";
        switch(statusTichet.toLowerCase()) {
            case "deschis": return "Deschis";
            case "in_lucru": return "În Lucru";
            case "finalizat": return "Finalizat";
            default: return statusTichet;
        }
    }
    
    @Override
    public String toString() {
        return "Tichet #" + maintenanceId + " - " + droneModel + " (" + getStatusDisplay() + ")";
    }
}
