package at.fhtw.communityproducer;

import junit.framework.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;

public class WeatherAPITest { // This class has only few methods (without user input) and therefore there is not much to test

    @Test
    public void getSunlightFactorTest() {
        //Assert
        WeatherAPI weatherAPI = new WeatherAPI();
        double sunlight = weatherAPI.getSunlightFactor();

        //Assess
        assertNotNull(sunlight);
    }
}