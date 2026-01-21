package dronefleet;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// FIX 1: Numele clasei trebuie sa coincida cu numele fisierului
public class DatabaseManager {
    private static DatabaseManager instance;

    private DatabaseManager() {}

    public static DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    // --- METODE PENTRU DRONE ---

    public List<Drone> getDrones() {
        List<Drone> list = new ArrayList<>();
        String sql = "SELECT DroneID, Model, Type, Status, PayloadCapacity, AutonomyMin FROM Drones";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                // FIX 2: DroneID este int in baza de date si in clasa Drone
                int id = rs.getInt("DroneID");
                String model = rs.getString("Model");
                String type = rs.getString("Type");
                String dbStatus = rs.getString("Status");
                double payload = rs.getDouble("PayloadCapacity");
                
                // FIX 3: AutonomyMin e int in DB, dar double in constructor
                double autonomy = rs.getInt("AutonomyMin"); 

                // Apelam constructorul corect (int, String, String, String, double, double)
                Drone d = new Drone(id, model, type, dbStatus, payload, autonomy);
                list.add(d);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void addDrone(Drone d) {
        String sql = "INSERT INTO Drones (Model, Type, Status, PayloadCapacity, AutonomyMin, LastCheckDate) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, d.getModel());
            pstmt.setString(2, d.getType());
            String statusToSend = d.getStatus().equals("in_livrare") ? "activa" : d.getStatus();
            pstmt.setString(3, statusToSend);
            pstmt.setDouble(4, d.getMaxPayload());
            pstmt.setInt(5, (int) d.getAutonomy());
            pstmt.setDate(6, new java.sql.Date(System.currentTimeMillis()));
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteDrone(Drone d) {
        String sql = "DELETE FROM Drones WHERE DroneID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // FIX 4: d.getId() returneaza deja int, nu folosim Integer.parseInt
            pstmt.setInt(1, d.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateDroneStatus(String droneId, String newStatus) {
        String sql = "UPDATE Drones SET Status = ? WHERE DroneID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String dbStatus = newStatus.equals("in_livrare") ? "activa" : newStatus;
            
            pstmt.setString(1, dbStatus);
            // FIX 5: droneId vine ca String aici (din apeluri externe), deci il parsam
            pstmt.setInt(2, Integer.parseInt(droneId));
            pstmt.executeUpdate();

            if ("mentenanta".equals(newStatus)) {
                logMaintenance(Integer.parseInt(droneId));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Supraincarcare metoda pentru cand avem ID-ul ca int direct
    public void updateDroneStatus(int droneId, String newStatus) {
        updateDroneStatus(String.valueOf(droneId), newStatus);
    }

    // --- METODE PENTRU LOCATII ---
    public List<Destination> getLocations() {
        List<Destination> locations = new ArrayList<>();
        String sql = "SELECT LocationName, Type, Coordinates FROM Locations";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String name = rs.getString("LocationName");
                String type = rs.getString("Type"); 
                String coords = rs.getString("Coordinates");
                
                locations.add(new Destination(name + " (" + type + ")", coords, coords));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return locations;
    }

    // --- METODE PENTRU ZBORURI ---
    public void saveFlight(Flight flight) {
        String sql = "INSERT INTO Missions (DroneID, StartCoord, EndCoord, StartTime, DurationMin, Type, MissionStatus) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // FIX 6: flight.getDrone().getId() este int
            pstmt.setInt(1, flight.getDrone().getId());
            pstmt.setString(2, flight.getOrigin());
            pstmt.setString(3, flight.getDestination());
            pstmt.setTimestamp(4, new Timestamp(System.currentTimeMillis())); 
            pstmt.setInt(5, 30);
            pstmt.setString(6, "livrare");
            pstmt.setString(7, "planificata");
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Flight> getFlights() {
        List<Flight> flights = new ArrayList<>();
        String sql = "SELECT m.DroneID, m.StartCoord, m.EndCoord, m.StartTime, d.Model, d.Type, d.Status, d.PayloadCapacity, d.AutonomyMin " +
                     "FROM Missions m JOIN Drones d ON m.DroneID = d.DroneID " +
                     "ORDER BY m.StartTime DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while(rs.next()) {
                // FIX 7: Constructorul Drone cere int la ID
                Drone drone = new Drone(
                    rs.getInt("DroneID"),
                    rs.getString("Model"),
                    rs.getString("Type"),
                    rs.getString("Status"),
                    rs.getDouble("PayloadCapacity"),
                    rs.getInt("AutonomyMin")
                );
                
                String origin = rs.getString("StartCoord");
                String dest = rs.getString("EndCoord");
                String time = rs.getString("StartTime"); 
                
                flights.add(new Flight(drone, origin, dest, time));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return flights;
    }
    
    private void logMaintenance(int droneId) {
        String sql = "INSERT INTO Maintenance (DroneID, DatePerformed, Type, RepairType, StatusTichet) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, droneId);
            pstmt.setDate(2, new java.sql.Date(System.currentTimeMillis()));
            pstmt.setString(3, "Routine Check");
            pstmt.setString(4, "General Inspection");
            pstmt.setString(5, "deschis");
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}