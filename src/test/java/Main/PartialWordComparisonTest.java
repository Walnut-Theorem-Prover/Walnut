package Main;

import Automata.Automaton;
import Main.Commands.EvalDef;
import Main.EvalComputations.Token.RelationalOperator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.List;

class PartialWordComparisonTest {
  @BeforeEach
  void setUp() {
    Session.setPathsAndNamesIntegrationTests();
  }

  @TestFactory
  List<DynamicTest> testPartialWordComparisons() {
    // The fixture defines partial_word[0]=5 and partial_word[1]=9, and nothing else.
    // Example:
    // An,x ( (partial_word[n] < x+1)  <=>  ((n=0 & 5 < x+1) | (n=1 & 9 < x+1))  )
    List<DynamicTest> tests = new ArrayList<>();
    for (RelationalOperator.Ops operator : RelationalOperator.Ops.values()) {
      final String op = operator.getSymbol();
      for (String operand : List.of("x", "x+1")) {
        for (String comparison : List.of(
            "partial_word[n] " + op + " " + operand,
            operand + " " + op + " partial_word[n]")) {
          final String expected = "(n=0 & " + comparison.replace("partial_word[n]", "5")
              + ") | (n=1 & " + comparison.replace("partial_word[n]", "9") + ")";
          // Check the whole relation, including every index where the word is undefined.
          final String predicate = "An,x ((" + comparison + ") <=> (" + expected + "))";
          tests.add(DynamicTest.dynamicTest(comparison, () -> assertTruePredicate(predicate)));
        }
      }
    }
    return tests;
  }

  @Test
  void testWordIndexConstraintAndQuantification() {
    assertTruePredicate("An,x ((partial_word[n+1] = x) <=> (n=0 & x=9))");
  }

  private static void assertTruePredicate(String predicate) {
    Automaton result = EvalDef.evaluatePredicate("?msd_2 " + predicate);
    Assertions.assertTrue(result.fa.isTRUE_FALSE_AUTOMATON(), predicate);
    Assertions.assertTrue(result.fa.isTRUE_AUTOMATON(), predicate);
  }
}
