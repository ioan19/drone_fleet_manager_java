package dronefleet;

import java.time.LocalDateTime;

public class Mission {
    private int id;
    private String droneModel;
    private String startCoord;
    private String endCoord;
    private LocalDateTime startTime;
    private int durationMin;
    private String status;

    public Mission(int id, String droneModel, String startCoord, String endCoord, LocalDateTime startTime, int durationMin, String status) {
        this.id = id;
        this.droneModel = droneModel;
        this.startCoord = startCoord;
        this.endCoord = endCoord;
        this.startTime = startTime;
        this.durationMin = durationMin;
        this.status = status;
    }

    public int getId() { return id; }
    public String getDroneModel() { return droneModel; }
    public String getStartCoord() { return startCoord; }
    public String getEndCoord() { return endCoord; }
    public LocalDateTime getStartTime() { return startTime; }
    public int getDurationMin() { return durationMin; }
    public String getStatus() { return status; }
}