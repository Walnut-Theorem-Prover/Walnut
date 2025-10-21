package Automata;

import Main.MetaCommands;
import Main.Session;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class AutomatonTest {
  @Test
  void testNSDiffering() {
    Automaton A = new Automaton();
    List<NumberSystem> first = new ArrayList<>();
    List<List<Integer>> N1 = new ArrayList<>();
    Automaton B = new Automaton();
    Assertions.assertFalse(NumberSystem.isNSDiffering(A.getNS(), first, N1, B.richAlphabet.getA()));
    NumberSystem ns = new NumberSystem("msd_3");
    first.add(ns);
    Assertions.assertTrue(NumberSystem.isNSDiffering(A.getNS(), first, N1, B.richAlphabet.getA()));
    A.getNS().add(ns);
    Assertions.assertFalse(NumberSystem.isNSDiffering(A.getNS(), first, N1, B.richAlphabet.getA()));
    first.clear();
    first.add(new NumberSystem("msd_5"));
    Assertions.assertTrue(NumberSystem.isNSDiffering(A.getNS(), first, N1, B.richAlphabet.getA()));
  }

  @Test
  void testTrueAutomaton() {
    Automaton A = new Automaton(true);
    Assertions.assertFalse(A.isEmpty());
    Assertions.assertTrue(A.isBound());
    Assertions.assertEquals(0, A.getArity());
    A.sortLabel();
    Assertions.assertThrows(RuntimeException.class, () -> A.bind(List.of()));
    Assertions.assertFalse(A.isEmpty());
  }

  @Test
  void testFalseAutomaton() {
    Automaton A = new Automaton(false);
    Assertions.assertTrue(A.isEmpty());
    Assertions.assertTrue(A.isBound());
    Assertions.assertEquals(0, A.getArity());
    A.sortLabel();
    Assertions.assertThrows(RuntimeException.class, () -> A.bind(List.of()));
    Assertions.assertTrue(A.isEmpty());
  }


  @Test
  void testGetArity() {
    Automaton A = new Automaton(Session.getAddressForUnitTestResources() + "T.txt");
    Assertions.assertFalse(A.isEmpty());
    Assertions.assertEquals(1, A.richAlphabet.getA().size());
    Assertions.assertEquals(List.of(0,1), A.richAlphabet.getA().get(0));
    Assertions.assertEquals(1, A.getArity()); // Thue-Morse alphabet [[0,1]] is arity 1
  }
}
