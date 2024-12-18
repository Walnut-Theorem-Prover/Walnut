package Automata;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.TreeMap;

public class ParseMethodsTest {
  @Test
  void testParseMorphism() {
    TreeMap<Integer, List<Integer>> map = ParseMethods.parseMorphism("0 -> 0010");
    Assertions.assertEquals(1, map.keySet().size());
    Assertions.assertEquals(List.of(0,0,1,0), map.get(0));
  }
}
