package dronefleet;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WeatherService {

    // INLOCUIESTE AICI CU CHEIA TA DE PE OPENWEATHERMAP
    private static final String API_KEY = "334d5456cf3f61c46cd6d4b19c36dac8";

    public static class WeatherData {
        public double temperature;
        public double windSpeed; // km/h
        public String condition; // Clear, Rain, Snow, Clouds
        public boolean isSafeToFly;

        public WeatherData(double t, double w, String c) {
            this.temperature = t;
            this.windSpeed = w;
            this.condition = c;
            
            // Logica Business:
            // 1. Vantul trebuie sa fie sub 35 km/h
            // 2. Nu trebuie sa ploua sau sa ninga
            boolean badWeather = c.equalsIgnoreCase("Rain") || 
                                 c.equalsIgnoreCase("Snow") || 
                                 c.equalsIgnoreCase("Thunderstorm");
            
            this.isSafeToFly = (w <= 35) && !badWeather;
        }
    }

    public static WeatherData getWeatherAt(double lat, double lon) {
        try {
            // Construim URL-ul catre API
            String url = String.format(
                "https://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&appid=%s&units=metric",
                lat, lon, API_KEY
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return parseJson(response.body());
            } else {
                System.out.println("Eroare API Meteo: Cod " + response.statusCode());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Fallback in caz de eroare (returnam date neutre ca sa nu crape aplicatia)
        return new WeatherData(20.0, 5.0, "Unavailable");
    }

    // Parsare manuala JSON (fara librarii externe) folosind Regex
    private static WeatherData parseJson(String json) {
        double temp = 0.0;
        double wind = 0.0;
        String condition = "Clear";

        try {
            // 1. Extrage Temperatura ("temp":15.5)
            Pattern pTemp = Pattern.compile("\"temp\":([\\d\\.]+)");
            Matcher mTemp = pTemp.matcher(json);
            if (mTemp.find()) {
                temp = Double.parseDouble(mTemp.group(1));
            }

            // 2. Extrage Viteza vantului ("speed":3.5) - API da m/s, convertim in km/h
            Pattern pWind = Pattern.compile("\"speed\":([\\d\\.]+)");
            Matcher mWind = pWind.matcher(json);
            if (mWind.find()) {
                double speedMs = Double.parseDouble(mWind.group(1));
                wind = speedMs * 3.6; // Conversie m/s in km/h
                wind = Math.round(wind * 10.0) / 10.0; // Rotunjire
            }

            // 3. Extrage Conditia ("main":"Rain")
            // Cautam structura din array-ul weather: "main":"Ceva"
            Pattern pCond = Pattern.compile("\"main\":\"([^\"]+)\"");
            Matcher mCond = pCond.matcher(json);
            if (mCond.find()) {
                condition = mCond.group(1);
            }

        } catch (Exception e) {
            System.out.println("Eroare la parsare JSON meteo");
        }

        return new WeatherData(temp, wind, condition);
    }
}