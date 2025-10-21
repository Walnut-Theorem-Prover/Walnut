package Main;

import Automata.Automaton;
import Automata.AutomatonLogicalOps;
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

  @Test
  void testFindAcceptedRegression() {
    String testName = "findAcceptedRegression";
    String testAddress = Session.getAddressForUnitTestResources() + "findAcceptedRegression.txt";
    Automaton M = new Automaton(testAddress);
    List<String> expected = List.of("0", "1", "00", "01", "10", "11", "000", "001", "010", "011");
    Assertions.assertEquals(expected, ProverHelper.findAccepted(M, testName, 10));
  }

  @Test
  void testInf() {
    String testName = "findAcceptedRegression";
    String testAddress = Session.getAddressForUnitTestResources() + "findAcceptedRegression.txt";
    Automaton M = new Automaton(testAddress);
    // we don't want to count multiple representations of the same value as distinct accepted values
    M.randomLabel();
    M = AutomatonLogicalOps.removeLeadingZeroes(M, M.getLabel(), false, null, null);
    Assertions.assertTrue(ProverHelper.infFromAutomaton(testName, M));
  }
}
