package at.fhtw.communityproducer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class WeatherAPI {
    private static final String API_KEY = "078c9d4925b2cffaea94e61b8f2b2ad6";
    private static final String CITY = "Vienna";
    //OpenWeatherMapClient openWeatherClient = new OpenWeatherMapClient("b51c69604385f4b7cdc3155e6d12fa33");

    public static double getSunlightFactor() throws Exception {
        String url = String.format(
                "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s",
                CITY, API_KEY
        );


        try {
            HttpURLConnection apiConnection = (HttpURLConnection) new URL(url).openConnection();
            // check for response status
            // 200 - means that the connection was a success
            if(apiConnection.getResponseCode() != 200){
                System.out.println("Error: Could not connect to API");
                return 0.3;
            }

            // 2. Read the response and convert store String type
            String jsonResponse = readApiResponse(apiConnection);

            // 3. Parse the string into a JSON Object
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(jsonResponse);
            //System.out.println(jsonObject);

            // 4. Store the data into their corresponding data type
            JSONArray weatherArray = (JSONArray) jsonObject.get("weather");
            JSONObject firstWeather = (JSONObject) weatherArray.get(0);

            String weather = (String) firstWeather.get("main");
            //System.out.println("Current Weather: " + weather);

            return switch (weather.toLowerCase()) {
                case "clear" -> 1.0;
                case "clouds" -> 0.5;
                case "rain", "snow" -> 0.2;
                default -> 0.3;
            };
        } catch(Exception e) {
            e.printStackTrace();
            return 0.3;
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
            e.printStackTrace();
        }

        // Return null if there was an issue reading the response
        return null;
    }
}
