package Automata;

import Main.Session;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class AutomatonExpressionTest {
    @Test
    void testBaseAutomatonConstructor() {
        Automaton a = new Automaton();
        Automaton b = new Automaton();

        try {
            Assertions.assertFalse(a.equals(null));
            //Assertions.assertTrue(a.equals(a));
            //Assertions.assertTrue(a.equals(a.clone()));

        }
        catch (RuntimeException ex) {
            // Hack because everything s
            Assertions.fail(ex);
        }


    }

    @Test
    void testBooleanAutomatonConstructor() {
        Automaton a, b;
        try {
            a = new Automaton(true);
            b = new Automaton(true);
            Assertions.assertTrue(a.equals(b));
            Assertions.assertTrue(a.equals(b.clone()));
            AutomatonLogicalOps.reverse(b, false, "", null, false);
            Assertions.assertTrue(a.equals(b));

            b = new Automaton(false);
            Assertions.assertFalse(a.equals(b));
            Assertions.assertFalse(a.equals(b.clone()));

        }
        catch (RuntimeException ex) {
            // Hack because everything s
            Assertions.fail(ex);
        }
    }

    @Test
    void testRegexAutomatonConstructor() {
        Automaton a, b;
        try {
            // regularExpression = "01*" and alphabet = [0,1,2], then the resulting automaton accepts
            //     * words of the form 01* over the alphabet {0,1,2}.<br>
            List<Integer> alphabet = new ArrayList<>();
            alphabet.add(0);
            alphabet.add(1);
            alphabet.add(2);

            a = new Automaton("01*", alphabet);
            //Assertions.assertEquals("[{0=>[1]}, {1=>[1]}]", a.d.toString());
            Assertions.assertTrue(a.equals(a.clone()));
            List<String> labels = new ArrayList<>();
            labels.add("");
            Assertions.assertEquals(labels.toString(), a.getLabel().toString());

            b = new Automaton("10*", alphabet);
            Assertions.assertFalse(a.equals(b));

            b = a.clone();
            AutomatonLogicalOps.reverse(b, false, "", null, false);
            Assertions.assertFalse(a.equals(b));
            Assertions.assertEquals("[{0=>[1], 1=>[0]}, {}]", b.getD().toString());
            AutomatonLogicalOps.reverse(b, false, "", null, false);
            Assertions.assertTrue(a.equals(b));

            b = a.clone();
            AutomatonLogicalOps.not(b, false, "", null);
            Assertions.assertFalse(a.equals(b));
            AutomatonLogicalOps.not(b, false, "", null);
            Assertions.assertTrue(a.equals(b));


        }
        catch (RuntimeException ex) {
            // Hack because everything s
            Assertions.fail(ex);
        }
    }

    @Test
    void testAddressAutomatonConstructor() {
        Automaton a, b;
        try {
            a = new Automaton(Session.getAddressForTestResources() + "LUCAS.txt");
        } catch (RuntimeException ex) {
            // Hack because everything s
            Assertions.fail(ex);
        }
    }

}
