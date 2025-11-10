package Automata;

import Main.Session;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TransducerTest {
    @Test
    void testTransducerRUNSUM2_T() {
        Session.setPathsAndNamesIntegrationTests();
        Session.cleanPathsAndNamesIntegrationTest();
        // Adapting an integration test to a unit test
        StringBuilder log = new StringBuilder();

        Automaton M =  new Automaton(Session.getReadFileForWordsLibrary("T.txt"));
        Assertions.assertEquals(2, M.fa.getQ());
        Assertions.assertEquals(
                "[{0=>[0], 1=>[1]}, {0=>[1], 1=>[0]}]",
                M.getFa().getT().getNfaD().toString());

        Transducer T = new Transducer(Session.getTransducerFile("RUNSUM2.txt"));
        Automaton C = T.transduceNonDeterministic(M, true, "", log);
        Assertions.assertEquals(
                "[{0=>[0], 1=>[1]}, {0=>[2], 1=>[3]}, {0=>[4], 1=>[5]}, {0=>[6], 1=>[7]}, {0=>[4], 1=>[5]}, {0=>[6], 1=>[7]}, {0=>[0], 1=>[1]}, {0=>[2], 1=>[3]}]",
                C.getFa().getT().getNfaD().toString());
    }
}
