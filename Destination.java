package dronefleet;

public class Destination {
    private String name;        // Numele complet pentru afișare
    private String address;     // Tipul locației (baza, destinatie, etc.)
    private String coordinates; // Format: "lat,lng"
    
    public Destination(String name, String address, String coordinates) {
        this.name = name;
        this.address = address;
        this.coordinates = coordinates;
    }

    public String getName() { 
        return name; 
    }
    
    public String getAddress() { 
        return address; 
    }
    
    public String getCoordinates() { 
        return coordinates; 
    }
    
    @Override
    public String toString() {
        return name; // Pentru ComboBox
    }
}