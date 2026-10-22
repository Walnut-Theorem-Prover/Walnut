package Main;

import Automata.FA.DeterminizationStrategies.Strategy;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MetaCommandsTest {
  @Test
  void wildcardsWorkWithEveryTerminator() throws IOException {
    for (String ending : List.of(";", ":", "::")) {
      Prover prover = Prover.mainProver = new Prover();
      // An empty command exercises setup without requiring any automata or output files.
      assertNull(prover.dispatchForIntegrationTest(
          "[strategy * CCLS][export * BA]" + ending, "Wildcard setup"));
      assertEquals(Strategy.CCLS, prover.metaCommands.getStrategy(23));
      assertEquals("ba", prover.metaCommands.getExportFormat(23));
      assertTrue(Prover.usingOTF);
      assertEquals(!ending.equals(";"), prover.printFlag);
      assertEquals(ending.equals("::"), prover.printDetails);
    }
  }

  @Test
  void indexedAndEarlyTerminationCommandsRequireDetailedLogging() {
    for (String ending : List.of(";", ":")) {
      for (String meta : List.of("[strategy 0 CCLS]", "[export 0 BA]", "[earlyExistTermination]",
          "[strategy * SC][export 0 GV]", "[export * BA][strategy 1 BRZ]")) {
        WalnutException error = assertThrows(WalnutException.class,
            () -> new Prover().dispatchForIntegrationTest(meta + ending, "Invalid verbosity"));
        assertTrue(error.getMessage().contains("require '::'"));
      }
    }
  }

  @Test
  void detailedCommandsKeepIndexedStrategiesAndExports() throws IOException {
    Prover prover = new Prover();
    assertNull(prover.dispatchForIntegrationTest(
        "[strategy 2 BRZ][export 3 BA][earlyExistTermination]::", "Detailed setup"));
    assertEquals(Strategy.SC, prover.metaCommands.getStrategy(1));
    assertEquals(Strategy.BRZ, prover.metaCommands.getStrategy(2));
    assertNull(prover.metaCommands.getExportFormat(2));
    assertEquals("ba", prover.metaCommands.getExportFormat(3));
    assertTrue(Prover.earlyExistTermination);
  }

  @Test
  void commandsResetNamesFlagsMapsAndAutomataNumbers() throws IOException {
    Prover prover = new Prover();
    prover.dispatchForIntegrationTest("[strategy * CCLS][export * BA]::", "First command");
    Prover.currentEvalName = "previous_result";
    assertEquals(0, prover.metaCommands.incrementAutomataIndex());
    prover.dispatchForIntegrationTest(";", "Next command");
    assertNull(Prover.currentEvalName);
    assertEquals(Strategy.SC, prover.metaCommands.getStrategy(0));
    assertNull(prover.metaCommands.getExportName(0));
    assertEquals(0, prover.metaCommands.incrementAutomataIndex());
    assertFalse(prover.metaCommands.hasWildcardCommands());
    assertFalse(Prover.usingOTF);
    assertFalse(Prover.earlyExistTermination);
    assertFalse(prover.printFlag);
    assertFalse(prover.printDetails);
  }

  @Test
  void malformedCommandsAreRejectedBeforeReadingMissingArguments() {
    for (boolean details : List.of(false, true)) {
      for (String command : List.of("[strategy]eval", "[strategy *]eval", "[export]eval",
          "[export *]eval", "[earlyExistTermination extra args]eval", "[unknown * SC]eval")) {
        assertThrows(WalnutException.class, () -> new MetaCommands().parseMetaCommands(command, details));
      }
    }
  }
}
