package dronefleet;

public class Drone {
    private int id;
    private String model;
    private String type;
    private String status;
    private double maxPayload;
    private double autonomy;
    private String missionType;
    private long missionEndTime;
    private long maintenanceEndTime;

    public Drone(int id, String model, String type, String status, double maxPayload, double autonomy) {
        this.id = id;
        this.model = model;
        this.type = type;
        this.status = status;
        this.maxPayload = maxPayload;
        this.autonomy = autonomy;
        this.missionType = "-";
        this.missionEndTime = 0;
        this.maintenanceEndTime = 0;
    }

    public int getId() { return id; }
    public String getModel() { return model; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public double getMaxPayload() { return maxPayload; }
    public double getAutonomy() { return autonomy; }
    public String getMissionType() { return missionType; }
    
    public void setStatus(String status) { this.status = status; }
    public void setMissionType(String missionType) { this.missionType = missionType; }
    public void setMissionEndTime(long endTime) { this.missionEndTime = endTime; }
    public void setMaintenanceEndTime(long endTime) { this.maintenanceEndTime = endTime; }
    
    public String getTimeRemaining() {
        checkAndUpdateStatus();
        
        if (!"in_livrare".equals(status) || missionEndTime == 0) {
            return "-";
        }
        
        long now = System.currentTimeMillis();
        long diff = missionEndTime - now;
        
        if (diff <= 0) {
            return "Finalizare...";
        }
        
        long minutes = diff / 60000;
        long seconds = (diff % 60000) / 1000;
        
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    public String getTimeRemainingDisplay() {
        checkAndUpdateStatus();
        
        if ("mentenanta".equals(status) && maintenanceEndTime > 0) {
            long now = System.currentTimeMillis();
            long diff = maintenanceEndTime - now;
            
            if (diff <= 0) {
                return "Finalizare...";
            }
            
            long minutes = diff / 60000;
            long seconds = (diff % 60000) / 1000;
            
            return String.format("%02d:%02d", minutes, seconds);
        }
        
        if ("in_livrare".equals(status) && missionEndTime > 0) {
            long now = System.currentTimeMillis();
            long diff = missionEndTime - now;
            
            if (diff <= 0) {
                return "Finalizare...";
            }
            
            long minutes = diff / 60000;
            long seconds = (diff % 60000) / 1000;
            
            return String.format("%02d:%02d", minutes, seconds);
        }
        
        return "-";
    }
    
    private void checkAndUpdateStatus() {
        long now = System.currentTimeMillis();
        
        if ("mentenanta".equals(status) && maintenanceEndTime > 0 && now >= maintenanceEndTime) {
            status = "activa";
            maintenanceEndTime = 0;
            DatabaseManager.updateDroneStatus(id, "activa");
        }
        
        if ("in_livrare".equals(status) && missionEndTime > 0 && now >= missionEndTime) {
            status = "activa";
            missionType = "-";
            missionEndTime = 0;
            DatabaseManager.updateDroneStatus(id, "activa");
        }
    }
    
    @Override
    public String toString() { 
        return model + " (ID: " + id + " | " + type + ")"; 
    }
}
