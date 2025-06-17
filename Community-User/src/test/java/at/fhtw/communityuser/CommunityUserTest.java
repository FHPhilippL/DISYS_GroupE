package at.fhtw.communityuser;

import org.junit.Test;

import static at.fhtw.communityuser.CommunityUser.calculateKWh;
import static org.junit.Assert.assertNotNull;

public class CommunityUserTest { // This class has only 1 method (without user input) and therefore there is not much to test

    @Test
    public void testCalculateKWh() {
        //Assert & Act
        double kwh = calculateKWh();

        //Assess
        assertNotNull(kwh);
    }

}
