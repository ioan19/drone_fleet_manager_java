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

    // ==================== STATIC WRAPPER METHODS ====================
    
    public static List<Drone> getAllDrones() {
        return getInstance().getDrones();
    }
    
    public static int countTotal() {
        return getInstance().getTotalDrones();
    }
    
    public static int countActive() {
        return getInstance().getActiveDrones();
    }
    
    public static void updateDroneStatus(int droneId, String status) {
        getInstance().updateDroneStatusInstance(droneId, status);
    }
    
    public static void saveMission(int droneId, String startCoord, String endCoord, int durationMin, String type) {
        getInstance().saveMissionInstance(droneId, startCoord, endCoord, durationMin, type);
    }

    // ==================== USERS & AUTENTIFICARE ====================

    public User validateUser(String username, String passwordHash) {
        String sql = "SELECT UserID, Username, Role, FullName FROM Users WHERE Username = ? AND PasswordHash = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, passwordHash);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                        rs.getInt("UserID"),
                        rs.getString("Username"),
                        rs.getString("Role"),
                        rs.getString("FullName")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public String getUserFullName(int userId) {
        String sql = "SELECT FullName FROM Users WHERE UserID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("FullName");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Unknown";
    }

    // ==================== DRONE ====================

    /**
     * Obține toate dronele cu verificare corectă a misiunilor active
     * O misiune este considerată activă DOAR dacă:
     * 1. MissionStatus = 'in_desfasurare'
     * 2. StartTime + DurationMin > NOW (nu a expirat)
     */
    public List<Drone> getDrones() {
        List<Drone> list = new ArrayList<>();
        
        // Mai întâi, curățăm misiunile expirate
        cleanupExpiredMissions();
        
        String sql = "SELECT d.DroneID, d.Model, d.Type, d.Status, d.PayloadCapacity, d.AutonomyMin, " +
                     "m.Type as MissionType, m.StartTime, m.DurationMin " +
                     "FROM Drones d " +
                     "LEFT JOIN Missions m ON d.DroneID = m.DroneID " +
                     "AND m.MissionStatus = 'in_desfasurare' " +
                     "AND DATE_ADD(m.StartTime, INTERVAL m.DurationMin MINUTE) > NOW() " +
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
                
                String missionType = rs.getString("MissionType");
                Timestamp startTime = rs.getTimestamp("StartTime");
                int duration = rs.getInt("DurationMin");
                
                if (missionType != null && startTime != null) {
                    long endTime = startTime.getTime() + (duration * 60000L);
                    long now = System.currentTimeMillis();
                    
                    if (endTime > now) {
                        d.setMissionType(capitalizeFirst(missionType));
                        d.setMissionEndTime(endTime);
                        
                        if (!"mentenanta".equals(status) && !"inactiva".equals(status)) {
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

    /**
     * Curăță misiunile expirate - le marchează ca finalizate
     */
    public void cleanupExpiredMissions() {
        String sql = "UPDATE Missions SET MissionStatus = 'finalizat' " +
                     "WHERE MissionStatus = 'in_desfasurare' " +
                     "AND DATE_ADD(StartTime, INTERVAL DurationMin MINUTE) < NOW()";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            int updated = stmt.executeUpdate(sql);
            if (updated > 0) {
                System.out.println("[DB] Cleanup: " + updated + " misiuni expirate marcate ca finalizate");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Cleanup warning: " + e.getMessage());
        }
    }

    public List<Drone> getMaintenanceDrones() {
        List<Drone> list = new ArrayList<>();
        String sql = "SELECT DroneID, Model, Type, Status, PayloadCapacity, AutonomyMin " +
                     "FROM Drones WHERE Status = 'mentenanta' ORDER BY DroneID";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Drone d = new Drone(
                    rs.getInt("DroneID"),
                    rs.getString("Model"),
                    rs.getString("Type"),
                    rs.getString("Status"),
                    rs.getFloat("PayloadCapacity"),
                    rs.getInt("AutonomyMin")
                );
                list.add(d);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    
    public List<Drone> getActiveDronesList() {
        List<Drone> list = new ArrayList<>();
        String sql = "SELECT DroneID, Model, Type, Status, PayloadCapacity, AutonomyMin " +
                     "FROM Drones WHERE Status = 'activa' ORDER BY DroneID";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Drone d = new Drone(
                    rs.getInt("DroneID"),
                    rs.getString("Model"),
                    rs.getString("Type"),
                    rs.getString("Status"),
                    rs.getFloat("PayloadCapacity"),
                    rs.getInt("AutonomyMin")
                );
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
        String deleteMissions = "DELETE FROM Missions WHERE DroneID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(deleteMissions)) {
            pstmt.setInt(1, d.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        String deleteRequests = "UPDATE DeliveryRequests SET AssignedDroneID = NULL WHERE AssignedDroneID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(deleteRequests)) {
            pstmt.setInt(1, d.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            // Ignorăm dacă tabelul nu există
        }
        
        String sql = "DELETE FROM Drones WHERE DroneID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, d.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateDroneStatusInstance(int droneId, String newStatus) {
        String sql = "UPDATE Drones SET Status = ? WHERE DroneID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, droneId);
            pstmt.executeUpdate();
            System.out.println("[DB] Drone " + droneId + " status updated to: " + newStatus);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void saveMissionInstance(int droneId, String startCoord, String endCoord, int durationMin, String type) {
        String sql = "INSERT INTO Missions (DroneID, StartCoord, EndCoord, StartTime, DurationMin, Type, MissionStatus) " +
                     "VALUES (?, ?, ?, ?, ?, ?, 'in_desfasurare')";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, droneId);
            pstmt.setString(2, startCoord);
            pstmt.setString(3, endCoord);
            pstmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            pstmt.setInt(5, durationMin);
            pstmt.setString(6, type);
            
            pstmt.executeUpdate();
            System.out.println("[DB] Misiune salvată pentru drona " + droneId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ==================== CERERI DE LIVRARE ====================
    
    public void createDeliveryRequest(int operatorId, String startCoord, String endCoord, 
                                     double weight, String notes) {
        String sql = "INSERT INTO DeliveryRequests (OperatorID, StartCoord, EndCoord, Weight, Notes, Status, RequestDate) " +
                     "VALUES (?, ?, ?, ?, ?, 'pending', ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, operatorId);
            pstmt.setString(2, startCoord);
            pstmt.setString(3, endCoord);
            pstmt.setDouble(4, weight);
            pstmt.setString(5, notes);
            pstmt.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
            
            pstmt.executeUpdate();
            System.out.println("[DB] Cerere de livrare creata pentru operator " + operatorId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<DeliveryRequest> getAllDeliveryRequestsForOperator(int operatorId) {
        List<DeliveryRequest> list = new ArrayList<>();
        String sql = "SELECT dr.RequestID, dr.OperatorID, dr.StartCoord, dr.EndCoord, dr.Weight, dr.Notes, " +
                     "dr.Status, dr.RequestDate, dr.AssignedDroneID, d.Model as DroneModel " +
                     "FROM DeliveryRequests dr " +
                     "LEFT JOIN Drones d ON dr.AssignedDroneID = d.DroneID " +
                     "WHERE dr.OperatorID = ? " +
                     "ORDER BY dr.RequestDate DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, operatorId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    DeliveryRequest req = new DeliveryRequest(
                        rs.getInt("RequestID"),
                        rs.getInt("OperatorID"),
                        rs.getString("StartCoord"),
                        rs.getString("EndCoord"),
                        rs.getDouble("Weight"),
                        rs.getString("Notes"),
                        rs.getString("Status"),
                        formatTimestamp(rs.getTimestamp("RequestDate")),
                        rs.getInt("AssignedDroneID"),
                        rs.getString("DroneModel")
                    );
                    list.add(req);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<DeliveryRequest> getAcceptedDeliveryRequests(int operatorId) {
        List<DeliveryRequest> list = new ArrayList<>();
        String sql = "SELECT dr.RequestID, dr.OperatorID, dr.StartCoord, dr.EndCoord, dr.Weight, dr.Notes, " +
                     "dr.Status, dr.RequestDate, dr.AssignedDroneID, d.Model as DroneModel " +
                     "FROM DeliveryRequests dr " +
                     "LEFT JOIN Drones d ON dr.AssignedDroneID = d.DroneID " +
                     "WHERE dr.OperatorID = ? AND dr.Status IN ('assigned', 'in_progress', 'completed') " +
                     "ORDER BY dr.RequestDate DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, operatorId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    DeliveryRequest req = new DeliveryRequest(
                        rs.getInt("RequestID"),
                        rs.getInt("OperatorID"),
                        rs.getString("StartCoord"),
                        rs.getString("EndCoord"),
                        rs.getDouble("Weight"),
                        rs.getString("Notes"),
                        rs.getString("Status"),
                        formatTimestamp(rs.getTimestamp("RequestDate")),
                        rs.getInt("AssignedDroneID"),
                        rs.getString("DroneModel")
                    );
                    list.add(req);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    
    public List<DeliveryRequest> getPendingDeliveryRequests() {
        List<DeliveryRequest> list = new ArrayList<>();
        String sql = "SELECT dr.RequestID, dr.OperatorID, dr.StartCoord, dr.EndCoord, dr.Weight, dr.Notes, " +
                     "dr.Status, dr.RequestDate, dr.AssignedDroneID, d.Model as DroneModel, " +
                     "u.FullName as OperatorName " +
                     "FROM DeliveryRequests dr " +
                     "LEFT JOIN Drones d ON dr.AssignedDroneID = d.DroneID " +
                     "LEFT JOIN Users u ON dr.OperatorID = u.UserID " +
                     "WHERE dr.Status = 'pending' " +
                     "ORDER BY dr.RequestDate ASC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                DeliveryRequest req = new DeliveryRequest(
                    rs.getInt("RequestID"),
                    rs.getInt("OperatorID"),
                    rs.getString("StartCoord"),
                    rs.getString("EndCoord"),
                    rs.getDouble("Weight"),
                    rs.getString("Notes"),
                    rs.getString("Status"),
                    formatTimestamp(rs.getTimestamp("RequestDate")),
                    rs.getInt("AssignedDroneID"),
                    rs.getString("DroneModel")
                );
                req.setOperatorName(rs.getString("OperatorName"));
                list.add(req);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void assignDroneToRequest(int requestId, int droneId) {
        String sql = "UPDATE DeliveryRequests SET AssignedDroneID = ?, Status = 'assigned' WHERE RequestID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, droneId);
            pstmt.setInt(2, requestId);
            pstmt.executeUpdate();
            System.out.println("[DB] Cererea #" + requestId + " acceptata cu drona " + droneId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void rejectDeliveryRequest(int requestId) {
        String sql = "UPDATE DeliveryRequests SET Status = 'rejected' WHERE RequestID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, requestId);
            pstmt.executeUpdate();
            System.out.println("[DB] Cererea #" + requestId + " respinsa");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ==================== MENTENANȚĂ ====================
    
    public void createMaintenanceTicket(int droneId, String problemDescription) {
        updateDroneStatusInstance(droneId, "mentenanta");
        
        String sql = "INSERT INTO Maintenance (DroneID, DatePerformed, Type, RepairType, StatusTichet, Notes) " +
                     "VALUES (?, ?, 'Reparatie', 'In asteptare diagnostic', 'deschis', ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, droneId);
            pstmt.setDate(2, new java.sql.Date(System.currentTimeMillis()));
            pstmt.setString(3, problemDescription);
            pstmt.executeUpdate();
            System.out.println("[DB] Tichet mentenanta creat pentru drona " + droneId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public List<MaintenanceTicket> getOpenMaintenanceTickets() {
        List<MaintenanceTicket> list = new ArrayList<>();
        String sql = "SELECT m.MaintenanceID, m.DroneID, m.DatePerformed, m.Type, m.RepairType, " +
                     "m.StatusTichet, m.Notes, d.Model as DroneModel, d.Type as DroneType " +
                     "FROM Maintenance m " +
                     "JOIN Drones d ON m.DroneID = d.DroneID " +
                     "WHERE m.StatusTichet IN ('deschis', 'in_lucru') " +
                     "ORDER BY m.DatePerformed ASC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                MaintenanceTicket ticket = new MaintenanceTicket(
                    rs.getInt("MaintenanceID"),
                    rs.getInt("DroneID"),
                    rs.getString("DroneModel"),
                    rs.getString("DroneType"),
                    rs.getDate("DatePerformed").toString(),
                    rs.getString("Type"),
                    rs.getString("RepairType"),
                    rs.getString("StatusTichet"),
                    rs.getString("Notes")
                );
                list.add(ticket);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    
    public void completeMaintenanceTicket(int ticketId, int droneId, String repairType, String notes) {
        String sqlTicket = "UPDATE Maintenance SET StatusTichet = 'finalizat', RepairType = ?, Notes = ? " +
                          "WHERE MaintenanceID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlTicket)) {
            pstmt.setString(1, repairType);
            pstmt.setString(2, notes);
            pstmt.setInt(3, ticketId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        updateDroneStatusInstance(droneId, "activa");
        
        String sqlDrone = "UPDATE Drones SET LastCheckDate = ? WHERE DroneID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlDrone)) {
            pstmt.setDate(1, new java.sql.Date(System.currentTimeMillis()));
            pstmt.setInt(2, droneId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void startWorkingOnTicket(int ticketId) {
        String sql = "UPDATE Maintenance SET StatusTichet = 'in_lucru' WHERE MaintenanceID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, ticketId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ==================== MISSIONS ====================

    public void saveFlight(Flight flight) {
        String sql = "INSERT INTO Missions (DroneID, StartCoord, EndCoord, StartTime, DurationMin, Type, MissionStatus) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, flight.getDrone().getId());
            pstmt.setString(2, flight.getOrigin());
            pstmt.setString(3, flight.getDestination());
            pstmt.setTimestamp(4, Timestamp.valueOf(flight.getTime()));
            pstmt.setInt(5, 30);
            pstmt.setString(6, "livrare");
            pstmt.setString(7, "in_desfasurare");
            
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
                
                flights.add(new Flight(drone, rs.getString("StartCoord"), 
                           rs.getString("EndCoord"), rs.getString("StartTime")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return flights;
    }
    
    // ==================== STATISTICI ====================

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
    
    public int getMaintenanceDronesCount() {
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
    
    public int getInDeliveryDronesCount() {
        // Numără DOAR dronele cu misiuni active (neexpirate)
        String sql = "SELECT COUNT(DISTINCT DroneID) as total FROM Missions " +
                     "WHERE MissionStatus = 'in_desfasurare' " +
                     "AND DATE_ADD(StartTime, INTERVAL DurationMin MINUTE) > NOW()";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt("total");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    public int getOpenTicketsCount() {
        String sql = "SELECT COUNT(*) as total FROM Maintenance WHERE StatusTichet IN ('deschis', 'in_lucru')";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt("total");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    // ==================== UTILITAR ====================
    
    private String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }
    
    private String formatTimestamp(Timestamp ts) {
        if (ts == null) return "N/A";
        return ts.toString().substring(0, 19);
    }
}
