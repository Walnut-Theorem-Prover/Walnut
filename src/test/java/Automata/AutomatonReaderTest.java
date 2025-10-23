package Automata;

import Main.Session;
import Main.WalnutException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AutomatonReaderTest {
  private Automaton A;

  @BeforeEach
  void setUp() {
    Session.setPathsAndNamesIntegrationTests();
      A = new Automaton();
  }

  @Test
  void testLucas() {
    AutomatonReader.readAutomaton(A, Session.getAddressForTestResources() + "LUCAS.txt");
    Assertions.assertEquals(6, A.fa.getQ());
  }

  @Test
  void testTrueFalse() {
    AutomatonReader.readAutomaton(A, Session.getAddressForUnitTestResources() + "tf1.txt");
    Assertions.assertTrue(A.fa.isTRUE_FALSE_AUTOMATON());
    Assertions.assertTrue(A.fa.isTRUE_AUTOMATON());

    A = new Automaton();
    AutomatonReader.readAutomaton(A, Session.getAddressForUnitTestResources() + "tf2.txt");
    Assertions.assertTrue(A.fa.isTRUE_FALSE_AUTOMATON());
    Assertions.assertFalse( A.fa.isTRUE_AUTOMATON());
  }

  @Test
  void testTransitionDeclaredFirst() {
    // transition declared before states
    Assertions.assertThrows(WalnutException.class, () ->
        AutomatonReader.readAutomaton(A,
            Session.getAddressForUnitTestResources() + "bogusTransitionDeclaredFirst.txt"));
  }

  @Test
  void testInvalidSyntax() {
    // invalid syntax
    Assertions.assertThrows(WalnutException.class, () ->
        AutomatonReader.readAutomaton(A,
            Session.getAddressForUnitTestResources() + "bogusInvalidSyntax.txt"));
  }

  @Test
  void testAlphabetNotMatch() {
    // Alphabet doesn't match inputs
    Assertions.assertThrows(WalnutException.class, () ->
        AutomatonReader.readAutomaton(A,
            Session.getAddressForUnitTestResources() + "bogusAlphabetNotMatch.txt"));
  }

  @Test
  void testTrueWithConflict() {
    Assertions.assertThrows(WalnutException.class, () ->
        AutomatonReader.readAutomaton(A,
            Session.getAddressForUnitTestResources() + "bogusTrueWithConflict.txt"));
  }

  @Test
  void testNonexistentFile() {
    // File doesn't exist
    String address = Session.getAddressForUnitTestResources() + "NONEXISTENTFILE";
    Assertions.assertThrows(WalnutException.class, () ->
        AutomatonReader.readAutomaton(A, address),
        WalnutException.fileDoesNotExist(address).getMessage());
  }

  @Test
  void testEmptyFile() {
    Assertions.assertThrows(WalnutException.class, () ->
        AutomatonReader.readAutomaton(A, Session.getAddressForUnitTestResources() + "emptyFile.txt"));
  }

  @Test
  void testReadComments() {
    // This can be a bogus automaton
    String c = AutomatonReader.readComments(
        Session.getAddressForUnitTestResources() + "bogusTransitionDeclaredFirst.txt");
    Assertions.assertEquals("# No states declared", c);
  }
  @Test
  void testReadCommentsNonexistentFile() {
    String address = Session.getAddressForUnitTestResources() + "NONEXISTENTFILE";
    Assertions.assertThrows(WalnutException.class, () ->
            AutomatonReader.readComments(address),
        WalnutException.fileDoesNotExist(address).getMessage());
  }
}
