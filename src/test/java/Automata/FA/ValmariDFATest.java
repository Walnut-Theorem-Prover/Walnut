package Automata.FA;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

class ValmariDFATest {
  @Test
  void testEmptyLanguageWithUnreachableAcceptingState() {
    for (int initialState = 0; initialState < 2; initialState++) {
      FA fa = dfa(initialState, initialState, 1 - initialState);
      for (int state = 0; state < 2; state++) {
        fa.getT().setDfaDTransition(state, 0, state);
        fa.getT().setDfaDTransition(state, 1, state);
      }

      minimize(fa);

      Assertions.assertFalse(accepts(fa));
      Assertions.assertEquals(1, fa.getQ());
      Assertions.assertEquals(0, fa.getQ0());
      Assertions.assertTrue(fa.getT().getDfaStateKeySet(0).isEmpty());
      Assertions.assertEquals(2, fa.getAlphabetSize());
    }
  }

  @Test
  void testUnreachableAcceptingComponentIsRemoved() {
    // State 0 accepts 1*, but initial state 1 accepts only the word 0.
    FA fa = dfa(1, 1, 0, 1);
    fa.getT().setDfaDTransition(0, 1, 0);
    fa.getT().setDfaDTransition(1, 0, 2);

    minimize(fa);

    Assertions.assertEquals(2, fa.getQ());
    Assertions.assertTrue(accepts(fa, 0));
    Assertions.assertFalse(accepts(fa));
    Assertions.assertFalse(accepts(fa, 1));
    Assertions.assertFalse(accepts(fa, 0, 1));
  }

  @Test
  void testUnreachablePredecessorOfAcceptingStateIsRemoved() {
    // State 2 can reach acceptance, but the initial state cannot reach state 2.
    FA fa = dfa(0, 0, 1, 0);
    fa.getT().setDfaDTransition(0, 0, 1);
    fa.getT().setDfaDTransition(2, 1, 1);

    minimize(fa);

    Assertions.assertEquals(2, fa.getQ());
    Assertions.assertTrue(accepts(fa, 0));
    Assertions.assertFalse(accepts(fa));
    Assertions.assertFalse(accepts(fa, 1));
    Assertions.assertFalse(accepts(fa, 0, 0));
  }

  @Test
  void testReachableDeadStateIsRemoved() {
    // The language is 1+; every transition on 0 enters the rejecting sink.
    FA fa = dfa(1, 0, 0, 1);
    fa.getT().setDfaDTransition(0, 0, 0);
    fa.getT().setDfaDTransition(0, 1, 0);
    fa.getT().setDfaDTransition(1, 0, 0);
    fa.getT().setDfaDTransition(1, 1, 2);
    fa.getT().setDfaDTransition(2, 0, 0);
    fa.getT().setDfaDTransition(2, 1, 2);

    minimize(fa);

    Assertions.assertEquals(2, fa.getQ());
    Assertions.assertTrue(accepts(fa, 1));
    Assertions.assertTrue(accepts(fa, 1, 1));
    Assertions.assertFalse(accepts(fa));
    Assertions.assertFalse(accepts(fa, 0));
    Assertions.assertFalse(accepts(fa, 1, 0));
    for (int state = 0; state < fa.getQ(); state++) {
      Assertions.assertFalse(fa.getT().hasDfaDTransition(state, 0));
    }
  }

  @Test
  void testAllFinalStatesWithoutTransitionsAcceptOnlyEmptyWord() {
    FA fa = dfa(2, 1, 1, 1);
    fa.setAlphabetSize(0);

    minimize(fa);

    Assertions.assertEquals(1, fa.getQ());
    Assertions.assertEquals(0, fa.getQ0());
    Assertions.assertTrue(accepts(fa));
    Assertions.assertTrue(fa.getT().getDfaStateKeySet(0).isEmpty());
    Assertions.assertEquals(0, fa.getAlphabetSize());
  }

  @Test
  void testNoFinalStatesProducesRejectingState() {
    FA fa = dfa(2, 0, 0, 0);
    fa.getT().setDfaDTransition(2, 0, 1);
    fa.getT().setDfaDTransition(1, 1, 0);
    fa.getT().setDfaDTransition(0, 0, 2);

    minimize(fa);

    Assertions.assertEquals(1, fa.getQ());
    Assertions.assertEquals(0, fa.getQ0());
    Assertions.assertFalse(accepts(fa));
    Assertions.assertTrue(fa.getT().getDfaStateKeySet(0).isEmpty());
  }

  @Test
  void testUnreachableFinalStatesWithoutTransitionsAreIgnored() {
    FA fa = dfa(1, 1, 0, 1);
    fa.setAlphabetSize(0);

    minimize(fa);

    Assertions.assertFalse(accepts(fa));
    Assertions.assertEquals(1, fa.getQ());
    Assertions.assertEquals(0, fa.getQ0());
    Assertions.assertTrue(fa.getT().getDfaStateKeySet(0).isEmpty());
  }

  private static FA dfa(int initialState, int... outputs) {
    FA fa = new FA();
    fa.initBasicFA(new IntArrayList(outputs));
    fa.setQ0(initialState);
    fa.setAlphabetSize(2);
    fa.setDfaTransitions(new ArrayList<>());
    for (int state = 0; state < outputs.length; state++) {
      fa.getT().addDfaState();
    }
    return fa;
  }

  private static void minimize(FA fa) {
    ValmariDFA minimizer = new ValmariDFA(fa, fa.getQ());
    minimizer.minValmari(fa.getO());
    minimizer.replaceFields(fa);
  }

  private static boolean accepts(FA fa, int... word) {
    int state = fa.getQ0();
    for (int symbol : word) {
      if (!fa.getT().hasDfaDTransition(state, symbol)) {
        return false;
      }
      state = fa.getT().getDfaStateDest(state, symbol);
    }
    return fa.isAccepting(state);
  }
}
