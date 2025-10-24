package Main;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;

public class ProverTest {
  @Test
  void testInfCommand() {
    Assertions.assertThrows(WalnutException.class, () -> Prover.infCommand(""));
    Assertions.assertThrows(WalnutException.class, () -> Prover.infCommand("inf NONEXISTENT"),
        WalnutException.fileDoesNotExist("inf NONEXISTENT").getMessage());

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

  @Test
  void testReadBuffer() {
    Prover p = new Prover();
    BufferedReader in = new BufferedReader(new StringReader("help;"));
    Assertions.assertTrue(p.readBuffer(in, false));
    Assertions.assertTrue(p.readBuffer(in, true));

    in = new BufferedReader(new StringReader("#blah\nh\nelp;\nh\nelp::"));
    Assertions.assertTrue(p.readBuffer(in, false));

    in = new BufferedReader(new StringReader("INVALIDCOMMAND;"));
    Assertions.assertTrue(p.readBuffer(in, false));

    in = new BufferedReader(new StringReader("exit;"));
    Assertions.assertFalse(p.readBuffer(in, false));
  }
}
