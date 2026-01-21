package dronefleet;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
        String sql = "SELECT d.DroneID, d.Model, d.Type, d.Status, d.PayloadCapacity, d.AutonomyMin, " +
                     "m.Type as MissionType, m.StartTime, m.DurationMin " +
                     "FROM Drones d " +
                     "LEFT JOIN Missions m ON d.DroneID = m.DroneID " +
                     "AND m.MissionStatus = 'in_desfasurare' " +
                     "ORDER BY d.DroneID";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("DroneID");
                String model = rs.getString("Model");
                String type = rs.getString("Type");
                String status = rs.getString("Status");
                double payload = rs.getFloat("PayloadCapacity");
                double autonomy = rs.getInt("AutonomyMin");

                Drone d = new Drone(id, model, type, status, payload, autonomy);
                
                // Setează tipul misiunii și timpul rămas dacă există
                String missionType = rs.getString("MissionType");
                if (missionType != null && !"activa".equals(status)) {
                    d.setMissionType(capitalizeFirst(missionType));
                    
                    // Calculează timpul de final al misiunii
                    Timestamp startTime = rs.getTimestamp("StartTime");
                    int duration = rs.getInt("DurationMin");
                    if (startTime != null) {
                        long endTime = startTime.getTime() + (duration * 60000L);
                        d.setMissionEndTime(endTime);
                        
                        // Actualizează statusul dacă drona e în livrare
                        if ("activa".equals(status)) {
                            d.setStatus("in_livrare");
                        }
                    }
                }
                
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
            pstmt.setString(3, d.getStatus());
            pstmt.setFloat(4, (float) d.getMaxPayload());
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
            pstmt.setInt(1, d.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateDroneStatus(int droneId, String newStatus) {
        String sql = "UPDATE Drones SET Status = ? WHERE DroneID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, droneId);
            pstmt.executeUpdate();

            if ("mentenanta".equals(newStatus)) {
                logMaintenance(droneId);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
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
                
                String displayName = name + " (" + type + ")";
                locations.add(new Destination(displayName, type, coords));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return locations;
    }

    // --- METODE PENTRU MISSIONS (ZBORURI) ---
    public void saveFlight(Flight flight) {
        String sql = "INSERT INTO Missions (DroneID, StartCoord, EndCoord, StartTime, DurationMin, Type, MissionStatus) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, flight.getDrone().getId());
            pstmt.setString(2, flight.getOrigin());
            pstmt.setString(3, flight.getDestination());
            pstmt.setTimestamp(4, Timestamp.valueOf(flight.getTime()));
            pstmt.setInt(5, 30); // Durata estimată default
            pstmt.setString(6, "livrare");
            pstmt.setString(7, "in_desfasurare"); // Status pentru misiuni noi
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Flight> getFlights() {
        List<Flight> flights = new ArrayList<>();
        String sql = "SELECT m.DroneID, m.StartCoord, m.EndCoord, m.StartTime, " +
                     "d.Model, d.Type, d.Status, d.PayloadCapacity, d.AutonomyMin " +
                     "FROM Missions m " +
                     "JOIN Drones d ON m.DroneID = d.DroneID " +
                     "ORDER BY m.StartTime DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while(rs.next()) {
                Drone drone = new Drone(
                    rs.getInt("DroneID"),
                    rs.getString("Model"),
                    rs.getString("Type"),
                    rs.getString("Status"),
                    rs.getFloat("PayloadCapacity"),
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
    
    // --- ÎNREGISTRARE MENTENANȚĂ ---
    private void logMaintenance(int droneId) {
        String sql = "INSERT INTO Maintenance (DroneID, DatePerformed, Type, RepairType, StatusTichet) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, droneId);
            pstmt.setDate(2, new java.sql.Date(System.currentTimeMillis()));
            pstmt.setString(3, "Mentenanță preventivă");
            pstmt.setString(4, "Inspecție generală");
            pstmt.setString(5, "deschis");
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // --- METODE PENTRU STATISTICI DASHBOARD ---
    public int getTotalDrones() {
        String sql = "SELECT COUNT(*) as total FROM Drones";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt("total");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    public int getActiveDrones() {
        String sql = "SELECT COUNT(*) as total FROM Drones WHERE Status = 'activa'";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt("total");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    public int getMaintenanceDrones() {
        String sql = "SELECT COUNT(*) as total FROM Drones WHERE Status = 'mentenanta'";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt("total");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    // --- METODE PENTRU USERS (Autentificare) ---
    public boolean validateUser(String username, String passwordHash) {
        String sql = "SELECT UserID FROM Users WHERE Username = ? AND PasswordHash = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, passwordHash);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // --- UTILITAR ---
    private String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }
}