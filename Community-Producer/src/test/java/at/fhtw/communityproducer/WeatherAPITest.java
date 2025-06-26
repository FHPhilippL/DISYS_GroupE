package at.fhtw.communityproducer;

import org.junit.Test;

import static org.junit.Assert.*;

public class WeatherAPITest {

    private WeatherAPI weatherAPI;

    @Test
    public void getSunlightFactorTest() {
        //Arrange & Act
        double sunlight = weatherAPI.getSunlightFactor();

        //Assert
        assertNotNull(sunlight);
    }

    @Test
    public void SunlightFactorInValidRangeTest() {
        //Arrange & Act
        double factor = weatherAPI.getSunlightFactor();

        //Assert
        assertTrue(factor >= 0.2 && factor <= 1.0);
    }

    @Test
    public void CachingBehaviorTest() {
        //Arrange & Act
        double first = weatherAPI.getSunlightFactor();
        double second = weatherAPI.getSunlightFactor(); // should not call API again

        //Assert
        assertEquals(first, second, 0.0001);
    }

}