package Main;

import Automata.Automaton;
import Automata.AutomatonLogicalOps;
import Automata.RichAlphabet;
import Main.Commands.Reg;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ProverHelperTest {
  @Test
  void testDetermineEncodedRegex() {
    new ProverHelper(); // just for coverage
    RichAlphabet r = new RichAlphabet();
    r.setA(List.of(List.of(0,1,2,3), List.of(0,1,2,3), List.of(0,1,2,3)));
    String s = Reg.determineEncodedRegex("([3,1,2]*)", 3, r);
    Assertions.assertEquals("(§*)", s); // extended-ascii 167
  }

  @Test
  void testInf() {
    String testName = "findAcceptedRegression";
    String testAddress = Session.getAddressForUnitTestResources() + "findAcceptedRegression.txt";
    Automaton M = new Automaton(testAddress);
    // we don't want to count multiple representations of the same value as distinct accepted values
    M.randomLabel();
    M = AutomatonLogicalOps.removeLeadingZeros(M, M.getLabel());
    Assertions.assertTrue(ProverHelper.infFromAutomaton(testName, M));
  }

  @Test
  void testInf2() {
    String testName = "hardInfTest";
    String testAddress = Session.getAddressForUnitTestResources() + "hardInfTest.txt";
    Automaton M = new Automaton(testAddress);
    // we don't want to count multiple representations of the same value as distinct accepted values
    M.randomLabel();
    M = AutomatonLogicalOps.removeLeadingZeros(M, M.getLabel());
    Assertions.assertTrue(ProverHelper.infFromAutomaton(testName, M));
  }
}
