package Automata;

import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ParseMethodsTest {
  @Test
  void testParseMorphism() {
    Map<Integer, IntList> map = ParseMethods.parseMorphism("0 -> 0010");
    Assertions.assertEquals(1, map.size());
    Assertions.assertEquals(List.of(0,0,1,0), map.get(0));
  }
}
