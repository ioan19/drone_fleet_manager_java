package dronefleet;

public class DeliveryRequest {
    private int requestId;
    private int operatorId;
    private String startCoord;
    private String endCoord;
    private double weight;
    private String notes;
    private String status; // "pending", "assigned", "in_progress", "completed", "rejected"
    private String requestDate;
    private int assignedDroneId;
    private String droneModel;
    private String operatorName; // Numele operatorului care a creat cererea
    
    // Constructor complet
    public DeliveryRequest(int requestId, int operatorId, String startCoord, String endCoord, 
                          double weight, String notes, String status, 
                          String requestDate, int assignedDroneId, String droneModel) {
        this.requestId = requestId;
        this.operatorId = operatorId;
        this.startCoord = startCoord;
        this.endCoord = endCoord;
        this.weight = weight;
        this.notes = notes;
        this.status = status;
        this.requestDate = requestDate;
        this.assignedDroneId = assignedDroneId;
        this.droneModel = droneModel;
        this.operatorName = "N/A";
    }
    
    // Constructor simplificat pentru backward compatibility
    public DeliveryRequest(int requestId, String startCoord, String endCoord, 
                          double weight, String notes, String status, 
                          String requestDate, int assignedDroneId, String droneModel) {
        this(requestId, 0, startCoord, endCoord, weight, notes, status, requestDate, assignedDroneId, droneModel);
    }
    
    // Getters
    public int getRequestId() { return requestId; }
    public int getOperatorId() { return operatorId; }
    public String getStartCoord() { return startCoord; }
    public String getEndCoord() { return endCoord; }
    public double getWeight() { return weight; }
    public String getNotes() { return notes != null ? notes : "-"; }
    public String getStatus() { return status; }
    public String getRequestDate() { return requestDate; }
    public int getAssignedDroneId() { return assignedDroneId; }
    public String getDroneModel() { return droneModel != null ? droneModel : "N/A"; }
    public String getOperatorName() { return operatorName != null ? operatorName : "N/A"; }
    
    // Alias methods for compatibility
    public String getOperator() { return getOperatorName(); }
    public String getDestination() { return getEndCoord(); }
    
    // Setters
    public void setOperatorName(String operatorName) { 
        this.operatorName = operatorName; 
    }
    
    public String getStatusDisplay() {
        if (status == null) return "N/A";
        switch(status) {
            case "pending": return "In asteptare";
            case "assigned": return "Alocata";
            case "in_progress": return "In curs";
            case "completed": return "Finalizata";
            case "rejected": return "Respinsa";
            default: return status;
        }
    }
    
    /**
     * Formatează coordonatele pentru afișare mai scurtă
     */
    public String getStartCoordShort() {
        if (startCoord == null || startCoord.isEmpty()) return "N/A";
        try {
            String[] parts = startCoord.split(",");
            if (parts.length == 2) {
                double lat = Double.parseDouble(parts[0].trim());
                double lng = Double.parseDouble(parts[1].trim());
                return String.format("%.3f, %.3f", lat, lng);
            }
        } catch (Exception e) {
            // Ignoră și returnează originalul
        }
        return startCoord;
    }
    
    public String getEndCoordShort() {
        if (endCoord == null || endCoord.isEmpty()) return "N/A";
        try {
            String[] parts = endCoord.split(",");
            if (parts.length == 2) {
                double lat = Double.parseDouble(parts[0].trim());
                double lng = Double.parseDouble(parts[1].trim());
                return String.format("%.3f, %.3f", lat, lng);
            }
        } catch (Exception e) {
            // Ignoră și returnează originalul
        }
        return endCoord;
    }
    
    @Override
    public String toString() {
        return "Cerere #" + requestId + " - " + getStatusDisplay();
    }
}