package Main;

import Automata.RichAlphabet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ProverHelperTest {
  @Test
  void testDetermineEncodedRegex() {
    RichAlphabet r = new RichAlphabet();
    r.setA(List.of(List.of(0,1,2,3), List.of(0,1,2,3), List.of(0,1,2,3)));
    String s = ProverHelper.determineEncodedRegex("([3,1,2]*)", 3, r);
    Assertions.assertEquals("(§*)", s); // extended-ascii 167
  }
}
