package dronefleet;

public class Drone {
    private int id; // Acum este INT conform bazei de date
    private String model;
    private String type; 
    private String status;
    private double maxPayload;
    private double autonomy;
    
    // Timpi limita
    private long missionEndTime = 0;
    private long maintenanceEndTime = 0;

    public Drone(int id, String model, String type, String status, double maxPayload, double autonomy) {
        this.id = id;
        this.model = model;
        this.type = type;
        this.status = status;
        this.maxPayload = maxPayload;
        this.autonomy = autonomy;
    }

    public int getId() { return id; }
    public String getModel() { return model; }
    public String getType() { return type; }
    public String getStatus() { 
        checkAutoStatus();
        return status; 
    }
    public double getMaxPayload() { return maxPayload; }
    public double getAutonomy() { return autonomy; }

    public void setStatus(String status) { this.status = status; }
    public void setMissionEndTime(long time) { this.missionEndTime = time; }
    public void setMaintenanceEndTime(long time) { this.maintenanceEndTime = time; }

    public void checkAutoStatus() {
        long now = System.currentTimeMillis();
        if ("in_livrare".equals(status) && missionEndTime > 0 && now >= missionEndTime) {
            status = "activa";
            missionEndTime = 0;
            // FIX: Folosim getInstance()
            DatabaseManager.getInstance().updateDroneStatus(id, "activa");
        }
        if ("mentenanta".equals(status) && maintenanceEndTime > 0 && now >= maintenanceEndTime) {
            status = "activa";
            maintenanceEndTime = 0;
            // FIX: Folosim getInstance()
            DatabaseManager.getInstance().updateDroneStatus(id, "activa");
        }
    }

    public String getTimeRemainingDisplay() {
        checkAutoStatus();
        long now = System.currentTimeMillis();
        long end = 0;

        if ("in_livrare".equals(status)) end = missionEndTime;
        else if ("mentenanta".equals(status)) end = maintenanceEndTime;
        else return "-";

        long diff = end - now;
        if (diff <= 0) return "Finalizare...";

        long min = diff / 60000;
        long sec = (diff % 60000) / 1000;
        
        String prefix = "mentenanta".equals(status) ? "Ment: " : "Zbor: ";
        return prefix + String.format("%02d:%02d", min, sec);
    }
    
    @Override
    public String toString() { return model + " (ID: " + id + ")"; }
}