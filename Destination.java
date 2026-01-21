package dronefleet;

public class Destination {
    private String name;
    private String address;
    private String coordinates; // Câmp nou pentru coloana Coordinates din DB
    
    public Destination(String name, String address, String coordinates) {
        this.name = name;
        this.address = address;
        this.coordinates = coordinates;
    }

    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getCoordinates() { return coordinates; }
    
    @Override
    public String toString() {
        return name; // Pentru a afișa corect în ComboBox
    }
}