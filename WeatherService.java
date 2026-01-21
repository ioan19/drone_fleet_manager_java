package dronefleet;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WeatherService {

    private static final String API_KEY = "334d5456cf3f61c46cd6d4b19c36dac8";

    public static class WeatherData {
        public double temperature;    // Celsius
        public double windSpeed;       // km/h
        public String condition;       // Clear, Rain, Snow, Clouds, etc.
        public boolean isSafeToFly;
        
        public WeatherData(double temp, double wind, String cond) {
            this.temperature = temp;
            this.windSpeed = wind;
            this.condition = cond;
            
            // CRITERII SIGURANȚĂ ZBOR:
            // 1. Vânt sub 35 km/h
            // 2. Fără precipitații (ploaie, zăpadă, furtună)
            boolean badWeather = cond.equalsIgnoreCase("Rain") || 
                                 cond.equalsIgnoreCase("Snow") || 
                                 cond.equalsIgnoreCase("Thunderstorm") ||
                                 cond.equalsIgnoreCase("Drizzle");
            
            this.isSafeToFly = (wind <= 35) && !badWeather;
        }
    }

    /**
     * Obține date meteo pentru coordonate specifice
     */
    public static WeatherData getWeatherAt(double lat, double lon) {
        try {
            String url = String.format(
                "https://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&appid=%s&units=metric",
                lat, lon, API_KEY
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return parseWeatherJson(response.body());
            } else {
                System.err.println("⚠ API Meteo error: HTTP " + response.statusCode());
                System.err.println("Response: " + response.body());
            }

        } catch (Exception e) {
            System.err.println("❌ Eroare la interogare API meteo:");
            e.printStackTrace();
        }
        
        // Fallback: date neutre pentru a nu bloca aplicația
        return new WeatherData(20.0, 5.0, "Clear");
    }

    /**
     * Parsare manuală JSON (fără librării externe)
     */
    private static WeatherData parseWeatherJson(String json) {
        double temp = 20.0;
        double wind = 0.0;
        String condition = "Clear";

        try {
            // 1. Temperatură: "temp":15.5
            Pattern pTemp = Pattern.compile("\"temp\"\\s*:\\s*([\\d\\.-]+)");
            Matcher mTemp = pTemp.matcher(json);
            if (mTemp.find()) {
                temp = Double.parseDouble(mTemp.group(1));
            }

            // 2. Viteză vânt: "speed":3.5 (vine în m/s, convertim la km/h)
            Pattern pWind = Pattern.compile("\"speed\"\\s*:\\s*([\\d\\.]+)");
            Matcher mWind = pWind.matcher(json);
            if (mWind.find()) {
                double speedMs = Double.parseDouble(mWind.group(1));
                wind = speedMs * 3.6; // m/s -> km/h
                wind = Math.round(wind * 10.0) / 10.0;
            }

            // 3. Condiție meteo: în array-ul "weather": "main":"Rain"
            // Structura: "weather":[{"main":"Clear",...}]
            Pattern pCond = Pattern.compile("\"weather\"\\s*:\\s*\\[\\s*\\{[^}]*\"main\"\\s*:\\s*\"([^\"]+)\"");
            Matcher mCond = pCond.matcher(json);
            if (mCond.find()) {
                condition = mCond.group(1);
            }

        } catch (Exception e) {
            System.err.println("⚠ Eroare la parsare JSON meteo");
            e.printStackTrace();
        }

        return new WeatherData(temp, wind, condition);
    }

    /**
     * Test manual - apelează din main pentru verificare
     */
    public static void main(String[] args) {
        // Test pentru București
        System.out.println("🌦 Test API Meteo pentru București...");
        WeatherData data = getWeatherAt(44.4268, 26.1025);
        
        System.out.println("Temperatură: " + data.temperature + "°C");
        System.out.println("Vânt: " + data.windSpeed + " km/h");
        System.out.println("Condiție: " + data.condition);
        System.out.println("Sigur pentru zbor: " + (data.isSafeToFly ? "✅ DA" : "❌ NU"));
    }
}