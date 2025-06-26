package at.fhtw.bwi.disys.gui;

import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class GuiController {

    @FXML public Label CommunityPoolUsageText;
    @FXML public Label GridPortionPercentageText;
    @FXML public Button RefreshButton;
    @FXML public DatePicker StartTimeDatePicker;
    @FXML public DatePicker EndTimeDatePicker;
    @FXML public Button ShowDataButton;
    @FXML public Label CommunityProducedText;
    @FXML public Label CommunityUsedText;
    @FXML public Label GridUsedText;
    @FXML public Label refreshErrorText;
    @FXML public TextField StartHourInput;
    @FXML public TextField EndHourInput;
    @FXML public Label ShowDataErrorText;
    @FXML public LineChart<String, Number> lineChartUsage;

    private static final Gson gson = new Gson();
    private static final Logger logger = LoggerFactory.getLogger(GuiController.class);

    /**
     * Called when the "Refresh" button is clicked.
     * Sends a request to the /energy/current endpoint to get the current usage status
     * and updates the corresponding GUI labels.
     */
    @FXML
    protected void onRefreshButtonClick() {
        logger.info("Refresh button clicked");
        try {
            String urlString = "http://localhost:8080/energy/current";
            final URLConnection connection = new URL(urlString).openConnection();
            try (
                    final InputStreamReader isr = new InputStreamReader(connection.getInputStream());
                    final BufferedReader br = new BufferedReader(isr)
            ) {
                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    responseBuilder.append(line);
                }
                logger.info("Percentage Message received: {}", responseBuilder);
                String jsonResponse = responseBuilder.toString();
                ServerResponseCurrent response = gson.fromJson(jsonResponse, ServerResponseCurrent.class);

                CommunityPoolUsageText.setText(String.format("%.2f%% used", response.communityDepleted));
                GridPortionPercentageText.setText(String.format("%.2f%% of total Usage", response.gridPortion));
                refreshErrorText.setText("");
                logger.info("Percentage is updated");
            }
        } catch (IOException e) {
            refreshErrorText.setText("ERROR: " + e.getLocalizedMessage());
            logger.error(e.getLocalizedMessage());
        }
    }

    /**
     * Called when the "Show Data" button is clicked.
     * Gathers the selected date range and hour inputs, validates them,
     * fetches total usage data and detailed hourly usage data from the API,
     * and updates both the text labels and the LineChart.
     */
    @FXML
    protected void onShowDataButtonClick() {
        logger.info("Show data button clicked");
        try {
            LocalDate startDate = StartTimeDatePicker.getValue();
            LocalDate endDate = EndTimeDatePicker.getValue();
            checkDates(startDate, endDate);

            String startHour = String.format("%02d", checkHour(StartHourInput.getText()));
            String endHour = checkHour(EndHourInput.getText()) == 0 ?
                    "00" : String.format("%02d", checkHour(EndHourInput.getText()) - 1);

            String startStr = startDate + "T" + startHour + ":00:00";
            String endStr = endDate + "T" + endHour + ":00:00";

            // Fetch total usage data
            String urlString = "http://localhost:8080/energy/historical?start=" + startStr + "&end=" + endStr;
            try (BufferedReader br = new BufferedReader(getReaderFromUrl(urlString))) {
                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    responseBuilder.append(line);
                }
                logger.info("Data Message received for the time from {} {} o'clock till {} {} o'clock: {}",
                        startDate, startHour, endDate, endHour, responseBuilder);


                String jsonResponse = responseBuilder.toString();
                ServerResponseHistorical response = gson.fromJson(jsonResponse, ServerResponseHistorical.class);

                CommunityProducedText.setText(String.format("%.3f kWh", response.totalCommunityProduced));
                CommunityUsedText.setText(String.format("%.3f kWh", response.totalCommunityUsed));
                GridUsedText.setText(String.format("%.3f kWh", response.totalGridUsed));
                ShowDataErrorText.setText("");
                logger.info("Historical Data is updated");
            }

            // Fetch detailed hourly data
            String detailedUrl = String.format("http://localhost:8080/energy/historical-detailed?start=%s&end=%s",
                    URLEncoder.encode(startStr, StandardCharsets.UTF_8),
                    URLEncoder.encode(endStr, StandardCharsets.UTF_8));

            HttpURLConnection conn = (HttpURLConnection) new URL(detailedUrl).openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder content = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            conn.disconnect();

            HourlyUsage[] usageData = gson.fromJson(content.toString(), HourlyUsage[].class);

            XYChart.Series<String, Number> producedSeries = new XYChart.Series<>();
            producedSeries.setName("Community Produced");

            XYChart.Series<String, Number> usedSeries = new XYChart.Series<>();
            usedSeries.setName("Community Used");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM. HH:mm");

            for (HourlyUsage usage : usageData) {
                LocalDateTime time = LocalDateTime.parse(usage.hour);
                String label = time.format(formatter);
                producedSeries.getData().add(new XYChart.Data<>(label, usage.communityProduced));
                usedSeries.getData().add(new XYChart.Data<>(label, usage.communityUsed));
            }

            lineChartUsage.getData().clear();
            lineChartUsage.getData().add(producedSeries);
            lineChartUsage.getData().add(usedSeries);
            lineChartUsage.applyCss();
            lineChartUsage.layout();
            logger.info("Line Chart is updated");

        } catch (IllegalArgumentException | IOException e) {
            ShowDataErrorText.setText("ERROR: " + e.getLocalizedMessage());
            logger.error(e.getLocalizedMessage());
        } catch (DateTimeParseException e) {
            ShowDataErrorText.setText("ERROR: No Proper Date Chosen");
            logger.error("No Proper Date Chosen");
        }
    }

    /**
     * Helper method to fetch data from a URL using a Reader.
     * Useful for unit testing by allowing this method to be mocked.
     *
     * @param urlString the URL to fetch data from
     * @return a Reader for the URL connection
     * @throws IOException if the connection cannot be opened
     */
    protected Reader getReaderFromUrl(String urlString) throws IOException {
        return new InputStreamReader(new URL(urlString).openConnection().getInputStream());
    }

    /**
     * Validates the hour input field.
     *
     * @param textField the hour value as string
     * @return the parsed hour as int
     * @throws IllegalArgumentException if the value is invalid or out of range
     */
    public int checkHour(String textField) {
        if (textField == null || textField.isBlank()) throw new IllegalArgumentException("No hour selected!");
        try {
            int hour = Integer.parseInt(textField);
            if (hour < 0 || hour > 24) throw new IllegalArgumentException("Hour must be between 0 and 24.");
            return hour;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Hour must be a number.");
        }
    }

    /**
     * Validates selected start and end dates.
     *
     * @param startDate the selected start date
     * @param endDate the selected end date
     * @throws IllegalArgumentException if either is null or the range is invalid
     */
    public void checkDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) throw new IllegalArgumentException("Start date not selected.");
        if (endDate == null) throw new IllegalArgumentException("End date not selected.");
        if (startDate.isAfter(endDate)) throw new IllegalArgumentException("Start date is after end date.");
    }

    // === Inner classes to hold JSON response data ===

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

    private static class HourlyUsage {
        String hour;
        double communityProduced;
        double communityUsed;
    }
}