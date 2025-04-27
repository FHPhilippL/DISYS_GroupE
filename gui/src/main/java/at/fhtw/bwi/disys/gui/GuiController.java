package at.fhtw.bwi.disys.gui;

import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
    public TextField StartHourInput;

    @FXML
    public TextField StartMinuteInput;

    @FXML
    public TextField EndHourInput;

    @FXML
    public TextField EndMinuteInput;

    @FXML
    public Label ShowDataErrorText;

    private static final Gson gson = new Gson();



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
                ServerResponseCurrent response = gson.fromJson(jsonResponse, ServerResponseCurrent.class);

                // Update GUI labels
                CommunityPoolUsageText.setText(String.format("%.2f%% used", response.communityDepleted));
                GridPortionPercentageText.setText(String.format("%.2f%%", response.gridPortion));

                refreshErrorText.setText("");
            }
       } catch (IOException e) {
           refreshErrorText.setText("ERROR: " + e.getLocalizedMessage());

       }
    }

    @FXML
    protected void onShowDataButtonClick() {
        try{
            String startHour = String.format("%02d", correctHour(StartHourInput));
            String startMinute = String.format("%02d", correctMinute(StartMinuteInput));
            String endHour = String.format("%02d", correctHour(EndHourInput));
            String endMinute = String.format("%02d", correctMinute(EndMinuteInput));

            if(StartTimeDatePicker.getValue() == null ||
                    EndTimeDatePicker.getValue() == null){
                throw new IllegalArgumentException("At least one Date not chosen");
            }

            String urlString = "http://localhost:8080/energy/historical?start="
                    +StartTimeDatePicker.getValue().toString()+ "T"+startHour+":"+startMinute+":00&end="
                    +EndTimeDatePicker.getValue().toString()+"T"+endHour+":" +endMinute+":00";

            System.out.println(urlString);

            final URLConnection connection = new URL(urlString).openConnection();
            try(
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
                ServerResponseHistorical response = gson.fromJson(jsonResponse, ServerResponseHistorical.class);

                // Update GUI labels
                CommunityProducedText.setText(String.format("%.3f kWh", response.totalCommunityProduced));
                CommunityUsedText.setText(String.format("%.3f kWh", response.totalCommunityUsed));
                GridUsedText.setText(String.format("%.3f kWh", response.totalGridUsed));

                ShowDataErrorText.setText("");
            }

        } catch(IllegalArgumentException | IOException e) {
            ShowDataErrorText.setText("ERROR: " + e.getLocalizedMessage());
        }

    }

    private static class ServerResponseCurrent {
        String hour;
        double communityDepleted;
        double gridPortion;
    }

    private static class ServerResponseHistorical {
        double totalCommunityProduced;
        double totalCommunityUsed;
        double totalGridUsed;
    }

    public int correctHour (TextField textField) {
        int hour = Integer.parseInt(textField.getText());
        if(hour < 0 || hour > 23) {throw new IllegalArgumentException("Not a correct hour!");}
        return hour;
    }

    public int correctMinute (TextField textField) {
        int minute = Integer.parseInt(textField.getText());
        if(minute < 0 || minute > 59) {throw new IllegalArgumentException("Not a correct minute!");}
        return minute;
    }


}