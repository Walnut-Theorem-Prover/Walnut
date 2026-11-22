package Automata;

import Main.EqualityUtils;
import Main.Session;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

class WordAutomatonReversalTest {
  @Test
  void testReversalPreservesRenumberedThueMorseSequence() {
    // Both resources compute the Thue-Morse word: t(n) is the parity of the
    // number of 1-bits in n (Integer.bitCount(n) % 2). The second resource
    // declares state 1 first, making its initial-state index nonzero.
    Automaton standard =
        new Automaton(Session.getAddressForUnitTestResources() + "reversal_thue_morse.txt");
    Automaton renumbered =
        new Automaton(Session.getAddressForUnitTestResources() + "reversal_thue_morse_nonzero_initial.txt");

    Assertions.assertEquals(0, standard.fa.getQ0());
    Assertions.assertEquals(1, renumbered.fa.getQ0());
    assertOutputLanguagesEqual(standard, renumbered, false);

    for (Automaton original : List.of(standard, renumbered)) {
      Automaton reversed = original.clone();
      WordAutomaton.reverseWithOutput(reversed, true);

      // Reversing the digit order also changes the number-system direction.
      Assertions.assertEquals("lsd_2", reversed.getNS().get(0).getName());
      assertOutputLanguagesEqual(original, reversed, true);
    }
  }

  @Test
  void testReversalAndDoubleReversalWithNonzeroInitialState() {
    // This three-state DFAO starts in state 2 and outputs 7 on the empty word.
    // On nonempty inputs, the output is 3 or 5 according to the last digit.
    // First verify that reversal moves this dependence to the first digit;
    // then verify that reversing again restores every original output.
    // Both checks cover all input strings, not just a finite sample.
    Automaton original =
        new Automaton(Session.getAddressForUnitTestResources() + "reversal_nonzero_initial.txt");
    Assertions.assertEquals(2, original.fa.getQ0());
    Automaton reversed = original.clone();

    WordAutomaton.reverseWithOutput(reversed, false);
    assertOutputLanguagesEqual(original, reversed, true);
    WordAutomaton.reverseWithOutput(reversed, false);
    assertOutputLanguagesEqual(original, reversed, false);
  }

  @Test
  void testReversalCanHaveFewerStatesThanOldInitialStateNumber() {
    // The resource has three states, initial state 2, and constant output 8.
    // Reversal minimizes it to one state. Reusing the old q0=2 would then
    // address a nonexistent state, which was another manifestation of the bug.
    Automaton original =
        new Automaton(Session.getAddressForUnitTestResources() + "reversal_collapsing_states.txt");
    Assertions.assertEquals(2, original.fa.getQ0());
    Automaton reversed = original.clone();

    WordAutomaton.reverseWithOutput(reversed, false);

    Assertions.assertEquals(1, reversed.fa.getQ());
    Assertions.assertEquals(0, reversed.fa.getQ0());
    Assertions.assertEquals(8, reversed.fa.getO().getInt(0));
    assertOutputLanguagesEqual(original, reversed, true);

  }

  private static void assertOutputLanguagesEqual(Automaton original, Automaton actual,
                                                 boolean reverseInputs) {
    // A DFAO is determined by the regular language of inputs producing each
    // output. Compare those languages using Walnut's ordinary DFA reversal
    // and the existing language-equivalence helper (BRICS), rather than
    // sampling WORDS or manually stepping transitions in an output() helper.
    Assertions.assertEquals(original.getAlphabetSize(), actual.getAlphabetSize());
    Set<Integer> outputs = new HashSet<>();
    for (int output : original.fa.getO()) outputs.add(output);
    for (int output : actual.fa.getO()) outputs.add(output);

    for (int output : outputs) {
      Automaton expected = WordAutomaton.uncombine(original, List.of(output)).get(0);
      Automaton observed = WordAutomaton.uncombine(actual, List.of(output)).get(0);
      if (reverseInputs) {
        AutomatonLogicalOps.reverse(expected, false);
      }
      Assertions.assertTrue(EqualityUtils.faEqual(expected.fa, observed.fa),
          "Different output language for value " + output);
    }
  }
}
