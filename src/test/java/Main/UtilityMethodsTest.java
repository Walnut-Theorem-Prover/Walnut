package Main;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UtilityMethodsTest {
  @Test
  void testRemoveDuplicates() {
    // L = [1,3,2,1,3] the result is [1,3,2]
    List<Integer> L = new ArrayList<>(List.of(1,3,2,1,3));
    UtilityMethods.removeDuplicates(L);
    Assertions.assertEquals(List.of(1,3,2), L);
  }

  @Test
  void testCommonRoot() {
    Assertions.assertEquals(2, UtilityMethods.commonRoot(4, 2));
    Assertions.assertEquals(-1, UtilityMethods.commonRoot(1, 2));
    Assertions.assertEquals(-1, UtilityMethods.commonRoot(2, 3));
    Assertions.assertEquals(-1, UtilityMethods.commonRoot(6, 10));
    Assertions.assertEquals(4, UtilityMethods.commonRoot(4, 16));
    Assertions.assertEquals(-1, UtilityMethods.commonRoot(12, 24));
    Assertions.assertEquals(3, UtilityMethods.commonRoot(27, 81));
  }

  //@Test
  void testPermute() {
    //permutation = [1,2,0] then the return value is
    //     * [L[1],L[2],L[0]]
    List<String> L = List.of("a","b","c");
    Assertions.assertEquals(List.of("b","c","a"), UtilityMethods.permute(L, new int[]{1,2,0}));
  }

  @Test
  void testLabelPermutation() {
    /**
     * For example if label_permutation[1]=[3], then input number 1 becomes input number 3 after sorting.
     * For example if label = ["z","a","c"], and A = [[-1,2],[0,1],[1,2,3]],
     * then label_permutation = [2,0,1] and permuted_A = [[0,1],[1,2,3],[-1,2]].
     */
    List<String> label = List.of("z","a","c");
    List<String> sorted_label = new ArrayList<>(label);
    Collections.sort(sorted_label);
    int[] labelPermutation = UtilityMethods.getLabelPermutation(label, sorted_label);
    Assertions.assertArrayEquals(new int[]{2,0,1}, labelPermutation);

    List<List<Integer>> A = List.of(List.of(-1,2), List.of(0,1), List.of(1,2,3));
    List<List<Integer>> permutedA = UtilityMethods.permute(A, labelPermutation);
    List<List<Integer>> expectedPermutedA =
        List.of(List.of(0,1), List.of(1,2,3), List.of(-1,2));
    Assertions.assertEquals(expectedPermutedA, permutedA);

    Assertions.assertEquals(List.of(1,2,6), UtilityMethods.getPermutedEncoder(A, permutedA));
  }

  @Test
  void testIsSorted() {
    Assertions.assertTrue(UtilityMethods.isSorted(List.of("1")));
    Assertions.assertTrue(UtilityMethods.isSorted(List.of("1","2","3")));
    Assertions.assertFalse(UtilityMethods.isSorted(List.of("1","3","2")));
    Assertions.assertFalse(UtilityMethods.isSorted(List.of("3","2","1")));
  }
}
