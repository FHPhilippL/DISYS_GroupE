package at.fhtw.bwi.disys.gui;

import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    @FXML
    public LineChart<String, Number> lineChartUsage;

    private static final Gson gson = new Gson();

    @FXML
    protected void onRefreshButtonClick() {
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

                String jsonResponse = responseBuilder.toString();
                ServerResponseCurrent response = gson.fromJson(jsonResponse, ServerResponseCurrent.class);

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
        try {
            checkDates();

            String startHour = String.format("%02d", checkHour(StartHourInput));
            String endHour = String.format("%02d", checkHour(EndHourInput) - 1);

            String startStr = StartTimeDatePicker.getValue().toString() + "T" + startHour + ":00:00";
            String endStr = EndTimeDatePicker.getValue().toString() + "T" + endHour + ":00:00";

            // Gesamtdaten abrufen
            String urlString = "http://localhost:8080/energy/historical?start=" + startStr + "&end=" + endStr;
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

                String jsonResponse = responseBuilder.toString();
                ServerResponseHistorical response = gson.fromJson(jsonResponse, ServerResponseHistorical.class);

                CommunityProducedText.setText(String.format("%.3f kWh", response.totalCommunityProduced));
                CommunityUsedText.setText(String.format("%.3f kWh", response.totalCommunityUsed));
                GridUsedText.setText(String.format("%.3f kWh", response.totalGridUsed));
                ShowDataErrorText.setText("");
            }

            // Detaildaten für Chart abrufen
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
            lineChartUsage.getData().addAll(producedSeries, usedSeries);

        } catch (IllegalArgumentException | IOException e) {
            ShowDataErrorText.setText("ERROR: " + e.getLocalizedMessage());
        } catch (DateTimeParseException e) {
            ShowDataErrorText.setText("ERROR: No Proper Date Chosen");
        }
    }

    // === Hilfsklassen ===

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

    // === Hilfsmethoden ===

    public int checkHour(TextField textField) {
        int hour = Integer.parseInt(textField.getText());
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("Not a correct hour!");
        }
        return hour;
    }

    public void checkDates() {
        if (StartTimeDatePicker.getValue() == null) {
            throw new IllegalArgumentException("Start Date not chosen");
        }
        if (EndTimeDatePicker.getValue() == null) {
            throw new IllegalArgumentException("End Date not chosen");
        }

        if (StartTimeDatePicker.getValue().isAfter(EndTimeDatePicker.getValue())) {
            throw new IllegalArgumentException("Start Date is after End Date");
        }
    }
}
