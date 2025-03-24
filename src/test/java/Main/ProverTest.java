package Main;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ProverTest {
  @Test
  void testInfCommand() {
    Assertions.assertThrows(WalnutException.class, () -> Prover.infCommand(""));
    Assertions.assertThrows(IllegalArgumentException.class, () -> Prover.infCommand("inf NONEXISTENT"));

    Session.setPathsAndNamesIntegrationTests();
    Assertions.assertTrue(Prover.infCommand("inf diffbyone"));
  }

  @Test
  void testTestCommand() {
    Assertions.assertThrows(WalnutException.class, () -> Prover.testCommand(""));
    Assertions.assertThrows(WalnutException.class, () -> Prover.testCommand("test NONEXISTENT"));

    Session.setPathsAndNamesIntegrationTests();
    Assertions.assertThrows(WalnutException.class, () -> Prover.testCommand("test diffbyone"));
    Assertions.assertTrue(Prover.testCommand("test diffbyone 1"));
  }
}
