package Main;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class EvalOutputTest {
  @BeforeEach
  void setUp() {
    Session.setPathsAndNamesIntegrationTests();
  }

  @Test
  void testEvalAndDefScalarWordOutput() throws IOException {
    assertPrinted("eval \"F[1]\";", "1");
    assertPrinted("def \"F[1]\";", "1");
  }

  @Test
  void testScalarWordArithmeticOutput() throws IOException {
    assertPrinted("eval \"T[1]+T[2]\";", "2");
  }

  @Test
  void testEvalAndDefRangeOutput() throws IOException {
    String expected = "n: 1 2 3 4 5" + System.lineSeparator() + "   1 0 0 1 0";
    assertPrinted("eval n=1..5 \"F[n]\";", expected);
    assertPrinted("def n=1..5 \"F[n]\";", expected);
  }

  @Test
  void testAffineWordIndexOutput() throws IOException {
    assertPrinted(
        "eval n=1..3 \"T[2*n+1]\";",
        "n: 1 2 3" + System.lineSeparator() + "   0 0 1");
  }

  @Test
  void testBooleanAutomatonRangeOutput() throws IOException {
    assertPrinted(
        "def n=1..4 \"E k n=2*k\";",
        "n: 1 2 3 4" + System.lineSeparator() + "   0 1 0 1");
  }

  @Test
  void testMultidimensionalPrettyPrinting() throws IOException {
    String nl = System.lineSeparator();
    String expected = "i=1:" + nl
        + "  j=2:" + nl
        + "    k: 1 2" + nl
        + "       0 1" + nl
        + "  j=3:" + nl
        + "    k: 1 2" + nl
        + "       1 0" + nl
        + "i=2:" + nl
        + "  j=2:" + nl
        + "    k: 1 2" + nl
        + "       1 0" + nl
        + "  j=3:" + nl
        + "    k: 1 2" + nl
        + "       0 1";
    assertPrinted("eval i=1..2 j=2..3 k=1..2 \"multi_eval_output[i][j][k]\";", expected);
    assertPrinted("def i=1..2 j=2..3 k=1..2 \"multi_eval_output[i][j][k]\";", expected);
  }

  @Test
  void testDescendingRange() throws IOException {
    assertPrinted(
        "eval n=3..1 \"F[n]\";",
        "n: 3 2 1" + System.lineSeparator() + "   0 0 1");
  }

  @Test
  void testMissingBindingRejected() {
    WalnutException error = Assertions.assertThrows(
        WalnutException.class,
        () -> new Prover().dispatchForIntegrationTest(
            "def i=1..2 \"multi_eval_output[i][j][1]\";", "missing eval/def binding"));
    Assertions.assertTrue(error.getMessage().contains("Missing eval/def binding for free variable j"));
  }

  @Test
  void testNamedEvalAndDefStillRejectWordResult() {
    for (String command : new String[]{
        "eval invalid_word_result \"F[1]\";",
        "def invalid_word_result \"F[1]\";"}) {
      WalnutException error = Assertions.assertThrows(
          WalnutException.class,
          () -> new Prover().dispatchForIntegrationTest(command, "named word result"));
      Assertions.assertTrue(error.getMessage().contains("final result of the evaluation is not of type automaton"));
    }
  }

  private static void assertPrinted(String command, String expected) throws IOException {
    new Prover().dispatchForIntegrationTest(command, command);
    String suffix = System.lineSeparator() + expected + System.lineSeparator();
    Assertions.assertTrue(
        Logging.getCommandLog().endsWith(suffix),
        () -> "Expected output suffix:\n" + expected + "\nActual command log:\n" + Logging.getCommandLog());
  }
}
