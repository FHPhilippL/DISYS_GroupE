package at.fhtw.bwi.disys.gui;

import javafx.application.Platform;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;
class GuiControllerTest {

    @BeforeAll
    static void initJavaFX() throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            latch.await();
    }

    @Test
    void assertThatWhenOnRefreshButtonIsClickedWhenASuccessfulResponseIsReceivedUpdateLabels() throws Exception{
        // Arrange
        GuiController guiController = Mockito.spy(new GuiController());

        String fakeJson = "{\"hour\":\"10\",\"communityDepleted\":90.5,\"gridPortion\":15.2}";
        Reader mockReader = new StringReader(fakeJson);

        Mockito.doReturn(mockReader).when(guiController).getReaderFromUrl(Mockito.anyString());

        guiController.CommunityPoolUsageText = new Label();
        guiController.GridPortionPercentageText = new Label();
        guiController.refreshErrorText = new Label();

        // Act
        guiController.onRefreshButtonClick();

        // Assert
        assertEquals("90,50% used", guiController.CommunityPoolUsageText.getText());
        assertEquals("15,20% of total Usage", guiController.GridPortionPercentageText.getText());
        assertEquals("", guiController.refreshErrorText.getText());
    }

    @Test
    void assertThatWhenOnShowDataButtonIsClickedWhenASuccessfulResponseIsReceivedUpdateLabels() throws Exception{
        // Arrange
        GuiController guiController = Mockito.spy(new GuiController());

        guiController.StartTimeDatePicker = new DatePicker(LocalDate.of(2025, 6, 20));
        guiController.EndTimeDatePicker = new DatePicker(LocalDate.of(2025, 6, 22));
        guiController.StartHourInput = new TextField("10");
        guiController.EndHourInput = new TextField("12");

        guiController.CommunityProducedText = new Label();
        guiController.CommunityUsedText = new Label();
        guiController.GridUsedText = new Label();
        guiController.ShowDataErrorText = new Label();

        String fakeJson = """
            {
                "totalCommunityProduced": 0.800,
                "totalCommunityUsed": 0.900,
                "totalGridUsed": 0.100
            }
        """;

        // Intercept URLConnection globally via URLStreamHandler
        URL.setURLStreamHandlerFactory(protocol -> {
            if ("http".equals(protocol)) {
                return new java.net.URLStreamHandler() {
                    @Override
                    protected URLConnection openConnection(URL u) {
                        return new URLConnection(u) {
                            @Override
                            public void connect() {}

                            @Override
                            public InputStream getInputStream() {
                                return new ByteArrayInputStream(fakeJson.getBytes());
                            }
                        };
                    }
                };
            }
            return null;
        });

        // Act
        guiController.onShowDataButtonClick();

        // Assert
        assertEquals("0,800 kWh", guiController.CommunityProducedText.getText());
        assertEquals("0,900 kWh", guiController.CommunityUsedText.getText());
        assertEquals("0,100 kWh", guiController.GridUsedText.getText());
        assertEquals("", guiController.ShowDataErrorText.getText());
    }

    @Test
    void assertThatCheckHourReturnsHourWhenHourIsValid() {
        // Arrange
        GuiController controller = new GuiController();

        // Act & Assert
        assertEquals(15, controller.checkHour("15"));
    }

    @Test
    void assertThatCheckHourThrowsExceptionWhenHourIsInvalid() {
        // Arrange
        GuiController controller = new GuiController();

        // Act
        Exception toHigh = assertThrows(IllegalArgumentException.class, () -> controller.checkHour("25"));
        Exception toLow = assertThrows(IllegalArgumentException.class, () -> controller.checkHour("-1"));
        Exception NaN = assertThrows(IllegalArgumentException.class, () -> controller.checkHour("AAA"));
        Exception empty = assertThrows(IllegalArgumentException.class, () -> controller.checkHour(" "));
        Exception blank = assertThrows(IllegalArgumentException.class, () -> controller.checkHour(""));
        Exception n = assertThrows(IllegalArgumentException.class, () -> controller.checkHour(null));

        //Assert
        assertEquals("Not a correct hour!", toHigh.getMessage());
        assertEquals("Not a correct hour!", toLow.getMessage());
        assertEquals("Hour must be a Number!", NaN.getMessage());
        assertEquals("No Hour chosen!", empty.getMessage());
        assertEquals("No Hour chosen!", blank.getMessage());
        assertEquals("No Hour chosen!", n.getMessage());
    }

    @Test
    void assertThatCheckDateDoesNotThroughExceptionWhenDatesAreValid() {
        //Arrange
        GuiController controller = new GuiController();
        LocalDate startDate = LocalDate.of(2025, 6, 20);
        LocalDate endDate = LocalDate.of(2025, 6, 22);

        //Act & Assert
        assertDoesNotThrow(() -> controller.checkDates(startDate, endDate));
    }

    @Test
    void assertThatCheckDateThrowsExceptionWhenDatesAreInvalid() {
        //Arrange
        GuiController controller = new GuiController();
        LocalDate startDate = LocalDate.of(2025, 6, 20);
        LocalDate endDate = LocalDate.of(2025, 6, 18);

        //Act
        Exception before = assertThrows(IllegalArgumentException.class, () -> controller.checkDates(startDate, endDate));
        Exception nullStart = assertThrows(IllegalArgumentException.class, () -> controller.checkDates(null, endDate));
        Exception nullEnd = assertThrows(IllegalArgumentException.class, () -> controller.checkDates(startDate, null));

        //Assert
        assertEquals("Start Date is after End Date", before.getMessage());
        assertEquals("Start Date not chosen", nullStart.getMessage());
        assertEquals("End Date not chosen", nullEnd.getMessage());
    }

}