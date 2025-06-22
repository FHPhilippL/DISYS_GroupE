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
import java.time.format.DateTimeParseException;

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
    public TextField EndHourInput;

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
                GridPortionPercentageText.setText(String.format("%.2f%% of total Usage", response.gridPortion));

                refreshErrorText.setText("");
            }
       } catch (IOException e) {
           refreshErrorText.setText("ERROR: " + e.getLocalizedMessage());

       }
    }

    @FXML
    protected void onShowDataButtonClick() {
        try{
            checkDates();

            String startHour = String.format("%02d", checkHour(StartHourInput.getText()));
            String endHour = String.format("%02d", checkHour(EndHourInput.getText())-1);

            String urlString = "http://localhost:8080/energy/historical?start="
                    +StartTimeDatePicker.getValue().toString()+ "T"+startHour+":00:00&end="
                    +EndTimeDatePicker.getValue().toString()+"T"+endHour+":00:00";

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
        } catch(DateTimeParseException e){
            ShowDataErrorText.setText("ERROR: No Proper Date Chosen");
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

    public int checkHour (String textField) {
        int hour = Integer.parseInt(textField);
        if(hour < 0 || hour > 23) {throw new IllegalArgumentException("Not a correct hour!");}
        return hour;
    }

    public void checkDates() {
        if(StartTimeDatePicker.getValue() == null){
            throw new IllegalArgumentException("Start Date not chosen");
        }
        if(EndTimeDatePicker.getValue() == null){
            throw new IllegalArgumentException("End Date not chosen");
        }

        if(StartTimeDatePicker.getValue().isAfter(EndTimeDatePicker.getValue())){
            throw new IllegalArgumentException("Start Date is after End Date");
        }
    }
}