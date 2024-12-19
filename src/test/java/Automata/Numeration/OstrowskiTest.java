package Automata.Numeration;

import Automata.Automaton;
import Automata.AutomatonWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

public class OstrowskiTest {
    @Test
    void createOstrowski() {
        // alpha = sqrt(3) - 1, pre-period = [] and period = [1, 2].
        Ostrowski on = new Ostrowski("testOstrowski", "", "1,2");
        Assertions.assertEquals("testOstrowski", on.getName());
        String onString = on.toString().replace(" ","");
        Assertions.assertEquals("name:testOstrowski,alpha:[0,3,1,2],periodindex:2", onString);
        Assertions.assertEquals(1, on.preperiod.size()); // huh?
        Assertions.assertEquals(2, on.period.size());
        Assertions.assertEquals(0, on.total_nodes);
    }

    @Test
    void createFib() {
        Ostrowski ost = new Ostrowski("fib", "0 2", "1");
        Assertions.assertEquals(List.of(2),ost.preperiod);
        Assertions.assertEquals(List.of(1),ost.period);

        ost.createRepresentationAutomaton();
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        AutomatonWriter.writeToStream(ost.repr, printWriter);
        String reprString = stringWriter.toString();
        reprString = reprString.replace(" ","").replace("\n","");
        Assertions.assertEquals("{0,1}010->01->1110->0", reprString);

        ost.createAdderAutomaton();
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        AutomatonWriter.writeToStream(ost.adder, printWriter);
        String adderString = stringWriter.toString();
        adderString = adderString.replace(" ","").replace("\n","");
        Assertions.assertEquals(
            "{0,1}{0,1}{0,1}01000->0001->1101->0011->010000->2100->3010->3110->4101->2011->2111->320100->2010->2110->3111->230000->1100->0010->0101->1011->1111->041000->5001->6101->5011->550001->061000->3100->4010->4001->2101->3011->3111->4",
            adderString);
    }
}
