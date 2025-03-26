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
    AutomatonReader.readAutomaton(A, Session.getAddressForTestResources() + "unitTests/tf1.txt");
    Assertions.assertTrue(A.fa.isTRUE_FALSE_AUTOMATON());
    Assertions.assertTrue(A.fa.isTRUE_AUTOMATON());

    A = new Automaton();
    AutomatonReader.readAutomaton(A, Session.getAddressForTestResources() + "unitTests/tf2.txt");
    Assertions.assertTrue(A.fa.isTRUE_FALSE_AUTOMATON());
    Assertions.assertFalse( A.fa.isTRUE_AUTOMATON());
  }

  @Test
  void testBogus1() {
    // transition declared before states
    Assertions.assertThrows(WalnutException.class, () ->
        AutomatonReader.readAutomaton(A, Session.getAddressForTestResources() + "unitTests/bogus1.txt"));
  }

  @Test
  void testBogus2() {
    // invalid syntax
    Assertions.assertThrows(WalnutException.class, () ->
        AutomatonReader.readAutomaton(A, Session.getAddressForTestResources() + "unitTests/bogus2.txt"));
  }

  @Test
  void testBogus3() {
    // Alphabet doesn't match inputs
    Assertions.assertThrows(WalnutException.class, () ->
        AutomatonReader.readAutomaton(A, Session.getAddressForTestResources() + "unitTests/bogus3.txt"));
  }

  @Test
  void testBogus4() {
    // File doesn't exist
    Assertions.assertThrows(IllegalArgumentException.class, () ->
        AutomatonReader.readAutomaton(A, Session.getAddressForTestResources() + "NONEXISTENTFILE"));
  }
}
