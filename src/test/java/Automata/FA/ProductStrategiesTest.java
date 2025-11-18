package Automata.FA;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static Automata.FA.ProductStrategies.NOT_SAME_INPUT_IN_BOTH;

public class ProductStrategiesTest {
  @Test
  void testJoinTwoInputsForCrossProduct() {
    /*
    Add all of first, and then nonequal elements of second.
    For example, suppose that first = [1,2,3], second = [-1,4,2], and equalIndices = [-1,-1,1].
     * Then the result is [1,2,3,-1,4].
     * However, if second = [-1,4,3] then the result is null
     * because 3rd element of the second does not equal element 1 (0-indexed) of the first.
     */
    int[] equalIndices = new int[]{NOT_SAME_INPUT_IN_BOTH,NOT_SAME_INPUT_IN_BOTH,1};
    Assertions.assertEquals(
        List.of(1,2,3,-1,4),
        ProductStrategies.joinTwoInputsForCrossProduct(List.of(1,2,3), List.of(-1,4,2), equalIndices));

    Assertions.assertNull(
        ProductStrategies.joinTwoInputsForCrossProduct(List.of(1,2,3), List.of(-1,4,3), equalIndices));

    Assertions.assertEquals(List.of(1,3,-1,4),
        ProductStrategies.joinTwoInputsForCrossProduct(List.of(1,3), List.of(-1,4,3), equalIndices));

    Assertions.assertEquals(List.of(1,2,3),
        ProductStrategies.joinTwoInputsForCrossProduct(List.of(1,2,3), List.of(), equalIndices));
  }
}
