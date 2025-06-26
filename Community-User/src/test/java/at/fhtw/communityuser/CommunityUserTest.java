package at.fhtw.communityuser;

import org.junit.Test;

import static at.fhtw.communityuser.CommunityUser.calculateKWh;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

public class CommunityUserTest { // This class has only 1 method (without user input) and therefore there is not much to test

    @Test
    public void testCalculateKWhWithinExpectedRange() {
        //Arrange & Act
        double kwh = calculateKWh();

        //Assert
        assertNotNull(kwh);

        assertTrue("KWh should be >= 0.00015", kwh >= 0.00015);
        assertTrue("KWh should be <= 0.003", kwh <= 0.003);
    }

}
