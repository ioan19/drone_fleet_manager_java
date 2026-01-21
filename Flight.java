package dronefleet;

public class Flight {
    private Drone drone;
    private String origin;
    private String destination;
    private String time; // Format "yyyy-MM-dd HH:mm:ss"

    public Flight(Drone drone, String origin, String destination, String time) {
        this.drone = drone;
        this.origin = origin;
        this.destination = destination;
        this.time = time;
    }

    public Drone getDrone() { return drone; }
    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
    public String getTime() { return time; }
}