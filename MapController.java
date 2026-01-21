package dronefleet;

import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import java.net.URL;
import java.util.List;

public class MapController {

    @FXML
    private WebView webView;

    @FXML
    public void initialize() {
        WebEngine engine = webView.getEngine();
        
        // Asigură-te că map_view.html este în src/resources sau lângă clase
        URL url = getClass().getResource("/map_view.html");
        
        if (url != null) {
            engine.load(url.toExternalForm());
            
            // Așteptăm să se încarce pagina pentru a pune markerele
            engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    loadMarkersFromDB(engine);
                }
            });
        } else {
            System.err.println("Eroare: Nu s-a găsit fișierul map_view.html");
        }
    }

    private void loadMarkersFromDB(WebEngine engine) {
        // Folosim DatabaseManager
        List<Destination> locations = DatabaseManager.getInstance().getLocations();
        
        for (Destination loc : locations) {
            try {
                // Presupunem format "lat,lng" în DB (ex: "44.42,26.10")
                String[] coords = loc.getCoordinates().split(",");
                if (coords.length == 2) {
                    String lat = coords[0].trim();
                    String lng = coords[1].trim();
                    String name = loc.getName();
                    
                    // Apelăm funcția JavaScript din HTML
                    engine.executeScript("addMarker(" + lat + ", " + lng + ", '" + name + "')");
                }
            } catch (Exception e) {
                System.err.println("Eroare la parsarea coordonatelor pentru: " + loc.getName());
            }
        }
    }
}