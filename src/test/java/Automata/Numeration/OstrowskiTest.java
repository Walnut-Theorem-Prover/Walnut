package Automata.Numeration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class OstrowskiTest {
    @Test
    void createOstrowski() {
        // alpha = sqrt(3) - 1, pre-period = [] and period = [1, 2].
        Ostrowski on = new Ostrowski("testOstrowski", "", "1,2");
        Assertions.assertEquals(1, on.preperiod.size()); // huh?
        Assertions.assertEquals(2, on.period.size());
        Assertions.assertEquals(0, on.total_nodes);
    }
}
