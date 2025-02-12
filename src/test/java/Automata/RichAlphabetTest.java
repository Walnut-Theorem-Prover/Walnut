package Automata;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class RichAlphabetTest {
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
    RichAlphabet r = new RichAlphabet();
    r.setA(A);
    Assertions.assertEquals(B, r.expandWildcard(L));
  }
}
