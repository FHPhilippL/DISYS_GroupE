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

}