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

  @Test
  void testIsSubsetA() {
    List<List<Integer>> A = new ArrayList<>();
    A.add(List.of(1,2));
    RichAlphabet r1 = new RichAlphabet();
    r1.setA(A);

    Assertions.assertTrue(RichAlphabet.isSubsetA(r1, r1));

    List<List<Integer>> B = new ArrayList<>();
    B.add(List.of(1,2));
    B.add(List.of(2,3));
    RichAlphabet r2 = new RichAlphabet();
    r2.setA(B);

    Assertions.assertFalse(RichAlphabet.isSubsetA(r1, r2));
    Assertions.assertFalse(RichAlphabet.isSubsetA(r2, r1));

    List<List<Integer>> C = new ArrayList<>();
    C.add(List.of(1,2,3));
    RichAlphabet r3 = new RichAlphabet();
    r3.setA(C);

    Assertions.assertFalse(RichAlphabet.isSubsetA(r3, r1));
    Assertions.assertTrue(RichAlphabet.isSubsetA(r1, r3));


  }
}
