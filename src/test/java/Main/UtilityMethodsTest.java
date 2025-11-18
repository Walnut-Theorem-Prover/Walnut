package Main;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static Main.UtilityMethods.NO_COMMON_ROOT;

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
    Assertions.assertEquals(NO_COMMON_ROOT, UtilityMethods.commonRoot(1, 2));
    Assertions.assertEquals(NO_COMMON_ROOT, UtilityMethods.commonRoot(2, 3));
    Assertions.assertEquals(NO_COMMON_ROOT, UtilityMethods.commonRoot(6, 10));
    Assertions.assertEquals(4, UtilityMethods.commonRoot(4, 16));
    Assertions.assertEquals(NO_COMMON_ROOT, UtilityMethods.commonRoot(12, 24));
    Assertions.assertEquals(3, UtilityMethods.commonRoot(27, 81));
  }

  @Test
  void testIsSorted() {
    Assertions.assertTrue(UtilityMethods.isSorted(List.of("1")));
    Assertions.assertTrue(UtilityMethods.isSorted(List.of("1","2","3")));
    Assertions.assertFalse(UtilityMethods.isSorted(List.of("1","3","2")));
    Assertions.assertFalse(UtilityMethods.isSorted(List.of("3","2","1")));
  }
}
