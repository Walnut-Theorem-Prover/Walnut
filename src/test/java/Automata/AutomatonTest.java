package Automata;

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
    Assertions.assertFalse(NumberSystem.isNSDiffering(A.getNS(), first, N1, B.getA()));
    NumberSystem ns = new NumberSystem("msd_3");
    first.add(ns);
    Assertions.assertTrue(NumberSystem.isNSDiffering(A.getNS(), first, N1, B.getA()));
    A.getNS().add(ns);
    Assertions.assertFalse(NumberSystem.isNSDiffering(A.getNS(), first, N1, B.getA()));
    first.clear();
    first.add(new NumberSystem("msd_5"));
    Assertions.assertTrue(NumberSystem.isNSDiffering(A.getNS(), first, N1, B.getA()));
  }

  @Test
  void testDecode() {
    List<List<Integer>> A = new ArrayList<>();
    A.add(List.of(0,1));
    A.add(List.of(-1,2,3));
    RichAlphabet r = new RichAlphabet();
    r.setA(A);
    Assertions.assertEquals(List.of(0,-1), r.decode(0));
    Assertions.assertEquals(List.of(1,-1), r.decode(1));
    Assertions.assertEquals(List.of(0,2), r.decode(2));
    Assertions.assertEquals(List.of(1,2), r.decode(3));
    Assertions.assertEquals(List.of(0,3), r.decode(4));
    Assertions.assertEquals(List.of(1,3), r.decode(5));
  }
}
