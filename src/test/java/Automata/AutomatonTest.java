package Automata;

import Main.UtilityMethods;
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
    Assertions.assertFalse(Automaton.isNSDiffering(A, first, N1, B));
    NumberSystem ns = new NumberSystem("msd_3");
    first.add(ns);
    Assertions.assertTrue(Automaton.isNSDiffering(A, first, N1, B));
    A.NS.add(ns);
    Assertions.assertFalse(Automaton.isNSDiffering(A, first, N1, B));
    first.clear();
    first.add(new NumberSystem("msd_5"));
    Assertions.assertTrue(Automaton.isNSDiffering(A, first, N1, B));
  }

  @Test
  void testDecode() {
    List<List<Integer>> A = new ArrayList<>();
    A.add(List.of(0,1));
    A.add(List.of(-1,2,3));
    Assertions.assertEquals(List.of(0,-1), Automaton.decode(A, 0));
    Assertions.assertEquals(List.of(1,-1), Automaton.decode(A, 1));
    Assertions.assertEquals(List.of(0,2), Automaton.decode(A, 2));
    Assertions.assertEquals(List.of(1,2), Automaton.decode(A, 3));
    Assertions.assertEquals(List.of(0,3), Automaton.decode(A, 4));
    Assertions.assertEquals(List.of(1,3), Automaton.decode(A, 5));
  }

  @Test
  void textExpandWildcard() {
    List<List<Integer>> A = new ArrayList<>();
    A.add(List.of(1,2));
    A.add(List.of(0,-1));
    A.add(List.of(3,4,5));
    List<Integer> L = new ArrayList<>();
    L.add(1);
    L.add(null); // wildcard
    L.add(4);
    List<List<Integer>> B = new ArrayList<>();
    B.add(List.of(1,0,4));
    B.add(List.of(1,-1,4));
    Assertions.assertEquals(B, Automaton.expandWildcard(A, L));
  }
}
