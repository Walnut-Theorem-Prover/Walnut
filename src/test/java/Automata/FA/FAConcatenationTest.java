package Automata.FA;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Checks concatenation before determinization or minimization. AutomataLib's
 * NFA accepts() handles all possible paths; List.of() is the empty word.
 */
class FAConcatenationTest {
  /**
   * {0} · {1} = {01}. In particular, accepting 0 would mean that
   * concatenation incorrectly retained the left operand's final state.
   */
  @Test
  void testConcatenationRejectsLeftOperandAlone() {
    FA left = fa(0, 0, 1);
    left.addNewTransition(0, 1, 0);
    FA right = fa(0, 0, 1);
    right.addNewTransition(0, 1, 1);
    left.convertNFAtoDFA();
    right.convertNFAtoDFA();

    FA.concatStates(right, left, left.getQ());
    var result = left.FAtoCompactNFA();

    Assertions.assertFalse(result.accepts(List.of(0)));
    Assertions.assertTrue(result.accepts(List.of(0, 1)));
    Assertions.assertFalse(result.accepts(List.of()));
    Assertions.assertFalse(result.accepts(List.of(1)));
    Assertions.assertFalse(result.accepts(List.of(0, 1, 1)));
  }

  /**
   * Neither operand starts at state 0. Each accepts a one-letter word,
   * so the result must accept 01. The copied right start is state 1,
   * not state 0 (which is an accepting state but has no outgoing edges).
   */
  @Test
  void testConcatenationUsesActualInitialStates() {
    FA left = fa(1, 1, 0);
    left.addNewTransition(1, 0, 0);
    FA right = fa(1, 1, 0);
    right.addNewTransition(1, 0, 1);

    FA.concatStates(right, left, left.getQ());
    var result = left.FAtoCompactNFA();

    Assertions.assertTrue(result.accepts(List.of(0, 1)));
    Assertions.assertFalse(result.accepts(List.of(0)));
    Assertions.assertFalse(result.accepts(List.of()));
    Assertions.assertFalse(result.accepts(List.of(1)));
    Assertions.assertEquals(1, left.getQ0());
  }

  /**
   * {0} · 1* = 01*. Because 1* includes the empty word, the left
   * operand's accepting state must remain accepting after concatenation.
   */
  @Test
  void testRightOperandAcceptingEmptyWordPreservesLeftAcceptance() {
    // {0} concatenated with 1* is 01*.
    FA left = fa(0, 0, 1);
    left.addNewTransition(0, 1, 0);
    FA right = fa(0, 1);
    right.addNewTransition(0, 0, 1);

    FA.concatStates(right, left, left.getQ());
    var result = left.FAtoCompactNFA();

    Assertions.assertTrue(result.accepts(List.of(0)));
    Assertions.assertTrue(result.accepts(List.of(0, 1)));
    Assertions.assertTrue(result.accepts(List.of(0, 1, 1)));
    Assertions.assertFalse(result.accepts(List.of()));
    Assertions.assertFalse(result.accepts(List.of(1)));
    Assertions.assertFalse(result.accepts(List.of(0, 0)));
  }

  /** Concatenating a nonempty language with the empty language gives empty. */
  @Test
  void testEmptyRightLanguageMakesConcatenationEmpty() {
    FA left = fa(0, 0, 1);
    left.addNewTransition(0, 1, 0);
    FA right = fa(0, 0);
    right.addNewTransition(0, 0, 0);
    right.addNewTransition(0, 0, 1);

    FA.concatStates(right, left, left.getQ());
    var result = left.FAtoCompactNFA();

    Assertions.assertFalse(result.accepts(List.of(0)));
    Assertions.assertFalse(result.accepts(List.of()));
    Assertions.assertFalse(result.accepts(List.of(0, 0)));
    Assertions.assertFalse(result.accepts(List.of(0, 1)));
  }

  /** {epsilon} · {1} = {1}; an accepting left initial state is not sufficient. */
  @Test
  void testEmptyWordLeftOperandProducesRightLanguage() {
    FA left = fa(0, 1);
    FA right = fa(0, 0, 1);
    right.addNewTransition(0, 1, 1);

    FA.concatStates(right, left, left.getQ());
    var result = left.FAtoCompactNFA();

    Assertions.assertFalse(result.accepts(List.of()));
    Assertions.assertTrue(result.accepts(List.of(1)));
    Assertions.assertFalse(result.accepts(List.of(0)));
    Assertions.assertFalse(result.accepts(List.of(1, 1)));
  }

  /** The empty language stays empty even when the right operand accepts epsilon. */
  @Test
  void testEmptyLeftLanguageRemainsEmpty() {
    FA left = fa(0, 0);
    left.addNewTransition(0, 0, 0);
    left.addNewTransition(0, 0, 1);
    FA right = fa(0, 1);
    right.addNewTransition(0, 0, 1);

    FA.concatStates(right, left, left.getQ());
    var result = left.FAtoCompactNFA();

    Assertions.assertFalse(result.accepts(List.of()));
    Assertions.assertFalse(result.accepts(List.of(0)));
    Assertions.assertFalse(result.accepts(List.of(1)));
    Assertions.assertFalse(result.accepts(List.of(0, 1)));
  }

  /**
   * The left operand accepts 0+ using two alternatives on input 0.
   * Concatenating {0} should accept 00+; merging the right transitions
   * must not discard the existing nondeterministic transitions.
   */
  @Test
  void testConcatenationPreservesNondeterministicAlternatives() {
    // The left NFA accepts 0+; concatenating {0} must accept exactly 00+.
    FA left = fa(0, 0, 1);
    left.getT().setNfaDTransition(0, 0, new IntArrayList(new int[]{0, 1}));
    left.addNewTransition(1, 1, 0);
    FA right = fa(0, 0, 1);
    right.addNewTransition(0, 1, 0);

    FA.concatStates(right, left, left.getQ());
    var result = left.FAtoCompactNFA();

    Assertions.assertFalse(result.accepts(List.of(0)));
    Assertions.assertTrue(result.accepts(List.of(0, 0)));
    Assertions.assertTrue(result.accepts(List.of(0, 0, 0)));
    Assertions.assertFalse(result.accepts(List.of()));
    Assertions.assertFalse(result.accepts(List.of(1)));
    Assertions.assertFalse(result.accepts(List.of(0, 1)));
  }

  // A small binary NFA. outputs[q] is 1 exactly when state q is accepting.
  // Calls to addNewTransition() below take (source, destination, inputDigit).
  private static FA fa(int initialState, int... outputs) {
    FA fa = new FA();
    fa.initBasicFA(new IntArrayList(outputs));
    fa.setQ0(initialState);
    fa.setAlphabetSize(2);
    return fa;
  }}
