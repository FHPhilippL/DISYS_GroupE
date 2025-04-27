package at.fhtw.bwi.disys.gui;

import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class GuiController {

    @FXML
    public Label CommunityPoolUsageText;

    @FXML
    public Label GridPortionPercentageText;

    @FXML
    public Button RefreshButton;

    @FXML
    public DatePicker StartTimeDatePicker;

    @FXML
    public DatePicker EndTimeDatePicker;

    @FXML
    public Button ShowDataButton;

    @FXML
    public Label CommunityProducedText;

    @FXML
    public Label CommunityUsedText;

    @FXML
    public Label GridUsedText;

    @FXML
    public Label refreshErrorText;

    @FXML
    public Label StartHourText;

    private static final Gson gson = new Gson();
    public TextField StartHourInput;
    public TextField StartMinuteInput;
    public TextField EndHourInput;
    public TextField EndMinuteInput;

    @FXML
    protected void onRefreshButtonClick() {
       try{
            String urlString = "http://localhost:8080/energy/current";
            System.out.println(urlString);

            final URLConnection connection = new URL(urlString).openConnection();
            try (
                    final InputStreamReader isr = new InputStreamReader(connection.getInputStream());
                    final BufferedReader br = new BufferedReader(isr)

            ){
                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    responseBuilder.append(line);
                }

                String jsonResponse = responseBuilder.toString();
                System.out.println("Response JSON: " + jsonResponse);

                // Parse JSON into a Java object
                ServerResponse response = gson.fromJson(jsonResponse, ServerResponse.class);

                // Update GUI labels
                CommunityPoolUsageText.setText(String.format("%.2f%% used", response.communityDepleted));
                GridPortionPercentageText.setText(String.format("%.2f%%", response.gridPortion));
            }
       } catch (IOException e) {
           e.printStackTrace();
           refreshErrorText.setText("ERROR: " + e.getLocalizedMessage());

       }
    }

    private static class ServerResponse {
        String hour;
        double communityDepleted;
        double gridPortion;
    }


    @FXML
    protected void onShowDataButtonClick() {

    }


}