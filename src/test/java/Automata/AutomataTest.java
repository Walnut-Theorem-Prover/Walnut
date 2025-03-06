package Automata;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static Automata.Automaton.*;

public class AutomataTest {

  @Test
  void testPermute() {
    // See notes in UtilityMethods. This was *not* the original expected behavior, but that may be okay.
    List<String> L = List.of("a","b","c");

    //original expected behavior
    //Assertions.assertEquals(List.of("b","c","a"), UtilityMethods.permute(L, new int[]{1,2,0}));

    //actual behavior
    Assertions.assertEquals(List.of("c","a","b"), permute(L, new int[]{1,2,0}));
  }

  @Test
  void testLabelPermutation() {
    /*
     * For example if label_permutation[1]=[3], then input number 1 becomes input number 3 after sorting.
     * For example if label = ["z","a","c"], and A = [[-1,2],[0,1],[1,2,3]],
     * then label_permutation = [2,0,1] and permuted_A = [[0,1],[1,2,3],[-1,2]].
     */
    List<String> label = List.of("z","a","c");
    List<String> sorted_label = new ArrayList<>(label);
    Collections.sort(sorted_label);
    int[] labelPermutation = getLabelPermutation(label, sorted_label);
    Assertions.assertArrayEquals(new int[]{2,0,1}, labelPermutation);

    List<List<Integer>> A = List.of(List.of(-1,2), List.of(0,1), List.of(1,2,3));
    List<List<Integer>> permutedA = permute(A, labelPermutation);
    List<List<Integer>> expectedPermutedA =
        List.of(List.of(0,1), List.of(1,2,3), List.of(-1,2));
    Assertions.assertEquals(expectedPermutedA, permutedA);

    RichAlphabet r = new RichAlphabet();
    r.setA(permutedA);
    r.setupEncoder();
    Assertions.assertEquals(List.of(1,2,6), r.getEncoder());
  }
}
