package at.fhtw.communityproducer;

import org.junit.Test;

import static at.fhtw.communityproducer.CommunityProducer.calculateKWh;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

public class CommunityProducerTest { // This class has only 1 method (without user input) and therefore there is not much to test

    @Test
    public void testCalculateKWhWithinExpectedRange() {
        //Arrange & Act
        double kwh = calculateKWh();

        //Assert
        assertNotNull(kwh);

        assertTrue("KWh should be >= 0", kwh >= 0.0);
        assertTrue("KWh should be <= 0.005", kwh <= 0.005);
    }

}