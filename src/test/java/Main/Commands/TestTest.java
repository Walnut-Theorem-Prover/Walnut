package Main.Commands;

import Automata.AutomatonDFA;
import Main.Prover;
import Main.Session;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TestTest {
  @Test
  void testFindAcceptedRegression() {
    String testName = "findAcceptedRegression";
    String testAddress = Session.getAddressForUnitTestResources() + testName + Prover.TXT_EXTENSION;
    AutomatonDFA M = new AutomatonDFA(testAddress);
    List<String> expected = List.of("0", "1", "00", "01", "10", "11", "000", "001", "010", "011");
    Assertions.assertEquals(expected, Main.Commands.Test.findAccepted(M, 10));
  }

  @Test
  void TestTestCommand() {
    String testName = "hardInfTest";
    String testAddress = Session.getAddressForUnitTestResources() + testName + Prover.TXT_EXTENSION;
    AutomatonDFA M = new AutomatonDFA(testAddress);
    Assertions.assertEquals(List.of(), Main.Commands.Test.findAccepted(M, 0));
    Assertions.assertEquals(List.of("101"), Main.Commands.Test.findAccepted(M, 1));
    Assertions.assertEquals(List.of("101","1010"), Main.Commands.Test.findAccepted(M, 2));
  }
}
