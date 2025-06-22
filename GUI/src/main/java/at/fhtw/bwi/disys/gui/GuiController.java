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
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDate;
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


    /**
     * Methode when the Button "Refresh" is clicked
     * Gets Data from the Server and inputs them into the corresponding labels.
     */
    @FXML
    protected void onRefreshButtonClick() {
       try{
            String urlString = "http://localhost:8080/energy/current"; // sets URL
            System.out.println(urlString);

            try (BufferedReader br = new BufferedReader(getReaderFromUrl(urlString))){
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

    /**
     * Methode when the Button "Show Data" is clicked
     * Sets the chosen Date and Hour and checks them by calling other methods.
     * Gets Data from the Server and inputs them into the corresponding labels.
     */
    @FXML
    protected void onShowDataButtonClick() {
        try{
            LocalDate startDate = StartTimeDatePicker.getValue();
            LocalDate endDate = EndTimeDatePicker.getValue();
            checkDates(startDate, endDate);

            String startHour = String.format("%02d", checkHour(StartHourInput.getText()));
            String endHour;
            if(checkHour(EndHourInput.getText())==0) endHour = String.format("%02d", 0);
            else endHour = String.format("%02d", checkHour(EndHourInput.getText())-1);


            String urlString = "http://localhost:8080/energy/historical?start="
                    +startDate.toString()+ "T"+startHour+":00:00&end="
                    +endDate.toString()+"T"+endHour+":00:00";

            System.out.println(urlString);

            try(BufferedReader br = new BufferedReader(getReaderFromUrl(urlString))){
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

    /**
     * Generates a New InputStreamReader to read the Json Messages
     * Seperated from the Methods, so a Mock test can be used
     * @param urlString the Url, with which the data is being received
     * @return the generated Reader
     * @throws IOException when the Connection cannot be build
     */
    protected Reader getReaderFromUrl(String urlString) throws IOException {
        return new InputStreamReader(new URL(urlString).openConnection().getInputStream());
    }

    /**
     * Class where the Data Read from the Server is being saved.
     * This Class saves the current percentage.
     */
    private static class ServerResponseCurrent {
        String hour;
        double communityDepleted;
        double gridPortion;
    }

    /**
     * Class where the Data Read from the Server is being saved.
     * This Class saves the total Usage in a given timeframe.
     */
    private static class ServerResponseHistorical {
        double totalCommunityProduced;
        double totalCommunityUsed;
        double totalGridUsed;
    }


    /**
     * Checks if the hour inputted in the GUI is of a correct format.
     * The Hour can only be between 0 and 24
     * @param textField the chosen hour
     * @return the hour if it is valid
     * @throws IllegalArgumentException when the hour is not valid
     */
    public int checkHour (String textField) {
        if(textField == null || textField.isEmpty() || textField.isBlank()) throw new IllegalArgumentException("No Hour chosen!");
        int hour = -1;
        try {
            hour = Integer.parseInt(textField);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Hour must be a Number!");
        }
        if(hour < 0 || hour > 24) throw new IllegalArgumentException("Not a correct hour!");
        return hour;
    }

    /**
     * checks if the Dates chosen are valid
     * For a Date to be valid, it needs to be not null.
     * The End Date is also not allowed to be after the Start Date
     * @param startDate the chosen start Date
     * @param endDate the chosen End Date
     * @throws IllegalArgumentException when Date is Null or End Date is before Start Date
     */
    public void checkDates(LocalDate startDate, LocalDate endDate) {
        if(startDate == null){
            throw new IllegalArgumentException("Start Date not chosen");
        }
        if(endDate == null){
            throw new IllegalArgumentException("End Date not chosen");
        }

        if(startDate.isAfter(endDate)){
            throw new IllegalArgumentException("Start Date is after End Date");
        }
    }
}