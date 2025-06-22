package at.fhtw.bwi.disys.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
class GuiControllerTest {

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

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> controller.checkHour("25"));
        assertThrows(IllegalArgumentException.class, () -> controller.checkHour("-1"));
        assertThrows(IllegalArgumentException.class, () -> controller.checkHour("AAA"));
        assertThrows(IllegalArgumentException.class, () -> controller.checkHour(" "));
        assertThrows(IllegalArgumentException.class, () -> controller.checkHour(null));
    }

}