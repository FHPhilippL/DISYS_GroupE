package at.fhtw.communityproducer;

import org.junit.Test;

import static at.fhtw.communityproducer.CommunityProducer.calculateKWh;
import static org.junit.Assert.assertNotNull;

public class CommunityProducerTest { // This class has only 1 method (without user input) and therefore there is not much to test

    @Test
    public void testCalculateKWh() throws Exception {
        //Assert & Act
        double kwh = calculateKWh();

        //Assess
        assertNotNull(kwh);
    }

}