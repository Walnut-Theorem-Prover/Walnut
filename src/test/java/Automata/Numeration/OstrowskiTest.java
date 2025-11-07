package Automata.Numeration;

import Automata.Automaton;
import Automata.Writer.AutomatonWriter;
import Main.Session;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class OstrowskiTest {
    @Test
    void createOstrowski() {
        // alpha = sqrt(3) - 1, pre-period = [] and period = [1, 2].
        Ostrowski on = new Ostrowski("testOstrowski", "", "1,2");
        String onString = on.toString().replace(" ","");
        Assertions.assertEquals("name:testOstrowski,alpha:[0,3,1,2],periodindex:2", onString);
        Assertions.assertEquals(1, on.preperiod.size()); // huh?
        Assertions.assertEquals(2, on.period.size());
        Assertions.assertEquals(0, on.totalNodes);
    }

    @Test
    void testNode() {
        // extra coverage
        NodeState nodeState = new NodeState(0,1,2);
        Assertions.assertNotEquals("x", nodeState);
        Assertions.assertEquals("[0 1 2]", nodeState.toString());
        NodeState nodeState2 = new NodeState(1,1,2);
        Assertions.assertEquals(-1, nodeState.compareTo(nodeState2));
    }

    /**
     * Test against msd_fib
     */
    @Test
    void createFib() {
        Ostrowski ost = new Ostrowski("fib", "0 2", "1");
        Assertions.assertEquals(List.of(2),ost.preperiod);
        Assertions.assertEquals(List.of(1),ost.period);

        testAgainstFile(Session.getAddressForUnitTestResources() + "msd_fib.txt",
            ost.createRepresentationAutomaton());
        testAgainstFile(Session.getAddressForUnitTestResources() + "msd_fib_addition.txt",
            ost.createAdderAutomaton());
    }

    /**
     * Test against msd_numSys. This is the example from Ostrowski documentation.
     */
    @Test
    void createNumSys() {
        Ostrowski ost = new Ostrowski("numsys", "0 3 1", "1 2");
        Assertions.assertEquals(List.of(3,1),ost.preperiod);
        Assertions.assertEquals(List.of(1,2),ost.period);

        testAgainstFile(Session.getAddressForUnitTestResources() + "msd_numsys.txt",
            ost.createRepresentationAutomaton());
        testAgainstFile(Session.getAddressForUnitTestResources() + "msd_numsys_addition.txt",
            ost.createAdderAutomaton());
    }

    /**
     * Test against msd_pell.
     */
    @Test
    void createPell() {
        Ostrowski ost = new Ostrowski("pell", "0", "2");
        Assertions.assertEquals(List.of(2),ost.preperiod); // 0+2
        Assertions.assertEquals(List.of(2),ost.period);

        testAgainstFile(Session.getAddressForUnitTestResources() + "msd_pell.txt",
            ost.createRepresentationAutomaton());
        testAgainstFile(Session.getAddressForUnitTestResources() + "msd_pell_addition.txt",
            ost.createAdderAutomaton());
    }

    /**
     * Test against msd_ns6, from https://www.sciencedirect.com/science/article/pii/S0304397521000311
     */
    @Test
    void createNs6() {
        Ostrowski ost = new Ostrowski("ns6", "0 1 2 1 1", "1 1 1 2");
        Assertions.assertEquals(List.of(3, 1, 1),ost.preperiod); // 0+1+2, 1, 1
        Assertions.assertEquals(List.of(1,1,1,2), ost.period);

        testAgainstFile(Session.getAddressForUnitTestResources() + "msd_ns6.txt",
            ost.createRepresentationAutomaton());
        testAgainstFile(Session.getAddressForUnitTestResources() + "msd_ns6_addition.txt",
            ost.createAdderAutomaton());
    }
    /**
     * Test against msd_ns7, from https://www.sciencedirect.com/science/article/pii/S0304397521000311
     */
    @Test
    void createNs7() {
        Ostrowski ost = new Ostrowski("ns7", "0 1 1 3", "1 2 1");
        Assertions.assertEquals(List.of(2,3),ost.preperiod); // 0+1+1, 3
        Assertions.assertEquals(List.of(1,2,1), ost.period);

        testAgainstFile(Session.getAddressForUnitTestResources() + "msd_ns7.txt",
            ost.createRepresentationAutomaton());
        testAgainstFile(Session.getAddressForUnitTestResources() + "msd_ns7_addition.txt",
            ost.createAdderAutomaton());
    }
    /**
     * Test against msd_ns8, from https://www.sciencedirect.com/science/article/pii/S0304397521000311
     */
    @Test
    void createNs8() {
        Ostrowski ost = new Ostrowski("ns8", "0 1 3 1", "2");
        Assertions.assertEquals(List.of(4,1),ost.preperiod); // 0+1+3, 1
        Assertions.assertEquals(List.of(2), ost.period);

        testAgainstFile(Session.getAddressForUnitTestResources() + "msd_ns8.txt",
            ost.createRepresentationAutomaton());
        testAgainstFile(Session.getAddressForUnitTestResources() + "msd_ns8_addition.txt",
            ost.createAdderAutomaton());
    }
    /**
     * Test against msd_ns9, from https://www.sciencedirect.com/science/article/pii/S0304397521000311
     */
    @Test
    void createNs9() {
        Ostrowski ost = new Ostrowski("ns9", "0 1 2 3", "2");
        Assertions.assertEquals(List.of(3,3),ost.preperiod); // 0+1+2,3
        Assertions.assertEquals(List.of(2), ost.period);

        testAgainstFile(Session.getAddressForUnitTestResources() + "msd_ns9.txt",
            ost.createRepresentationAutomaton());
        testAgainstFile(Session.getAddressForUnitTestResources() + "msd_ns9_addition.txt",
            ost.createAdderAutomaton());
    }
    /**
     * Test against msd_ns10, from https://www.sciencedirect.com/science/article/pii/S0304397521000311
     */
    @Test
    void createNs10() {
        Ostrowski ost = new Ostrowski("ns10", "0 1 4 2", "3");
        Assertions.assertEquals(List.of(5, 2),ost.preperiod); // 0+1+4, 2
        Assertions.assertEquals(List.of(3), ost.period);

        testAgainstFile(Session.getAddressForUnitTestResources() + "msd_ns10.txt",
            ost.createRepresentationAutomaton());
        testAgainstFile(Session.getAddressForUnitTestResources() + "msd_ns10_addition.txt",
            ost.createAdderAutomaton());
    }

    private static void testAgainstFile(String expectedFileLoc, Automaton actualAutomaton) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        String expectedAutomaton = null;
        try {
            expectedAutomaton = Files.readString(Path.of(expectedFileLoc));
        } catch (IOException ex) {
            Assertions.fail("Unexpected: " + ex);
        }
        AutomatonWriter.writeTxtFormatToStream(actualAutomaton, printWriter);
        Assertions.assertEquals(expectedAutomaton, stringWriter.toString());
    }
}
