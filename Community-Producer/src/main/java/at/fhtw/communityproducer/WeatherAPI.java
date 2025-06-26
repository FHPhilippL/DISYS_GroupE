package at.fhtw.communityproducer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Scanner;

public class WeatherAPI {

    private static final Logger logger = LoggerFactory.getLogger(CommunityProducer.class);

    private static final String API_KEY = "078c9d4925b2cffaea94e61b8f2b2ad6";
    private static final String CITY = "Vienna";

    private static double sunlightFactor = 0.3;
    private static LocalTime lastFetchTime = null;

    private static LocalTime sunriseTime = null;
    private static LocalTime sunsetTime = null;

    public static double getSunlightFactor() {
        LocalTime now = LocalTime.now();

        /* Check if we need to refresh (older than 10 minutes or never fetched)
        --> API has a free call limit of 1000 / day --> reduces call volume */
        if (lastFetchTime == null || lastFetchTime.plusMinutes(10).isBefore(now)) {
            getWeatherData();
            lastFetchTime = now;
            logger.info("[i] New API-Data fetched at " + lastFetchTime);
        }

        return sunlightFactor;
    }

    public static boolean isSunShining() {
        LocalTime now = LocalTime.now();

        if (now.isBefore(sunriseTime) || now.isAfter(sunsetTime)) {
            return true;
        }
        return false;
    }

    // calls API to get weather conditions and times for sunset/rise, returns sunlightFactor
    private static void getWeatherData() {
        String url = String.format(
                "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s",
                CITY, API_KEY
        );

        try {
            HttpURLConnection apiConnection = (HttpURLConnection) new URL(url).openConnection();
            // check for response status
            // 200 - means that the connection was a success
            if(apiConnection.getResponseCode() != 200){
                logger.error("Could not connect to API");
                sunlightFactor = 0.3;
            }

            // 2. Read the response and convert store String type
            String jsonResponse = readApiResponse(apiConnection);

            // 3. Parse the string into a JSON Object
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(jsonResponse);
            //System.out.println(jsonObject);

            // 4. Store the data into their corresponding data type

            // Sunset & Sunrise
            JSONObject sysArray = (JSONObject) jsonObject.get("sys");

            long sunrise = (long) sysArray.get("sunrise");
            long sunset = (long) sysArray.get("sunset");
            logger.debug("[i] Sunrise (Unix): " + sunrise + ", Sunset (Unix): " + sunset);

            // Convert "Unix, UTC" time from API response to local time
            ZoneId viennaZone = ZoneId.of("Europe/Vienna");
            sunriseTime = Instant.ofEpochSecond(sunrise).atZone(viennaZone).toLocalTime();
            sunsetTime = Instant.ofEpochSecond(sunset).atZone(viennaZone).toLocalTime();
            logger.debug("[i] Sunrise: " + sunriseTime + ", Sunset: " + sunsetTime);

            // Weather conditions
            JSONArray weatherArray = (JSONArray) jsonObject.get("weather");
            JSONObject firstWeather = (JSONObject) weatherArray.get(0);

            String weather = (String) firstWeather.get("main");
            logger.info("[i] Current weather: " + weather + ", sunrise at: " + sunriseTime + ", sunset at: " + sunsetTime);

            // Cloudiness (% of clouds) for more accurate cloud factor
            JSONObject cloudsObject = (JSONObject) jsonObject.get("clouds");
            long cloudIntensity = (long) cloudsObject.get("all");

            double cloudFactor = 1.0 - (cloudIntensity / 100.0);
            logger.debug("[i] Cloudiness: " + cloudIntensity + " %, as sunFactor: " + cloudFactor);

            // Theoretically the switch statement could be replaced by returning the cloudFactor
            sunlightFactor = switch (weather.toLowerCase()) {
                case "clear" -> 1.0;
                case "clouds" -> cloudFactor; // former cloud factor was 0.5
                case "rain", "snow" -> 0.2;
                default -> 0.3;
            };

        } catch(Exception e) {
            logger.error("Error while fetching weather data", e);
            sunlightFactor = 0.3;
        }
    }

    private static String readApiResponse(HttpURLConnection apiConnection) {
        try {
            // Create a StringBuilder to store the resulting JSON data
            StringBuilder resultJson = new StringBuilder();

            // Create a Scanner to read from the InputStream of the HttpURLConnection
            Scanner scanner = new Scanner(apiConnection.getInputStream());

            // Loop through each line in the response and append it to the StringBuilder
            while (scanner.hasNext()) {
                // Read and append the current line to the StringBuilder
                resultJson.append(scanner.nextLine());
            }

            // Close the Scanner to release resources associated with it
            scanner.close();

            // Return the JSON data as a String
            return resultJson.toString();

        } catch (IOException e) {
            // Print the exception details in case of an IOException
            logger.error("Error while reading API response", e);
        }

        // Return null if there was an issue reading the response
        return null;
    }
}

// The base code for calling the weather API has been sourced from https://github.com/curadProgrammer/Java-Tutorials/blob/main/WeatherAPIData.java