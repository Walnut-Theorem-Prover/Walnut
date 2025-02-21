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

        try {
            Assertions.assertFalse(a.equals(null));
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
            Assertions.assertTrue(a.equals(b), a.fa + " != " + b.fa);
            Assertions.assertTrue(a.equals(b.clone()));
            AutomatonLogicalOps.reverse(b, false, "", null, false);
            Assertions.assertTrue(a.equals(b), a.fa + " != " + b.fa);

            b = new Automaton(false);
            Assertions.assertFalse(a.equals(b), a.fa + " == " + b.fa);
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

            a = new Automaton("01*", alphabet, null);
            //Assertions.assertEquals("[{0=>[1]}, {1=>[1]}]", a.d.toString());
            Assertions.assertTrue(a.equals(a.clone()));
            List<String> labels = new ArrayList<>();
            labels.add("");
            Assertions.assertEquals(labels.toString(), a.getLabel().toString());

            b = new Automaton("10*", alphabet, null);
            Assertions.assertFalse(a.equals(b), a.fa + " == " + b.fa);

            b = a.clone();
            AutomatonLogicalOps.reverse(b, false, "", null, false);
            Assertions.assertFalse(a.equals(b), a.fa + " == " + b.fa);
            Assertions.assertEquals("[{0=>[1], 1=>[0]}, {}]", b.getFa().getNfaD().toString());
            AutomatonLogicalOps.reverse(b, false, "", null, false);
            Assertions.assertTrue(a.equals(b), a.fa + " != " + b.fa);

            b = a.clone();
            AutomatonLogicalOps.not(b, false, "", null);
            Assertions.assertFalse(a.equals(b), a.fa + " == " + b.fa);
            AutomatonLogicalOps.not(b, false, "", null);
            Assertions.assertTrue(a.equals(b), a.fa + " != " + b.fa);
        }
        catch (RuntimeException ex) {
            // Hack because everything s
            Assertions.fail(ex);
        }
    }

    @Test
    void testAddressAutomatonConstructor() {
        try {
            new Automaton(Session.getAddressForTestResources() + "LUCAS.txt");
        } catch (RuntimeException ex) {
            // Hack because everything s
            Assertions.fail(ex);
        }
    }
}
