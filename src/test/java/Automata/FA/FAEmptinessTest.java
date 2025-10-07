package Automata.FA;

import it.unimi.dsi.fastutil.ints.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test languageEmpty logic in FA, and also verify it matches old brics logic.
 */
class FAEmptinessTest {
  private static FA newNFA(int q0, int Q, int alphabetSize, int... acceptingStates) {
    FA fa = new FA();
    fa.setQ(Q);
    fa.setQ0(q0);
    fa.setAlphabetSize(alphabetSize);

    // outputs
    IntArrayList O = new IntArrayList(Q);
    for (int i = 0; i < Q; i++) O.add(0);
    for (int acc : acceptingStates) O.set(acc, 1);
    fa.initBasicFA(O);

    // ensure NFA storage is present for all states
    for (int i = 0; i < Q; i++) {
      if (fa.t.getNfaD().size() <= i) fa.t.addMapToNfaD();
      else if (fa.t.getNfaState(i) == null) fa.t.addMapToNfaD();
    }
    return fa;
  }

  private static void addNfaEdge(FA fa, int src, int sym, int... dests) {
    IntArrayList list = new IntArrayList();
    for (int d : dests) list.add(d);
    fa.t.getNfaState(src).put(sym, list);
  }

  @Test
  void singleStateNonAcceptingIsEmpty() {
    FA fa = newNFA(0, 1, 0 /* alphabetSize=0, no edges */);
    assertTrue(fa.toDkBricsAutomaton().isEmpty());
    assertTrue(fa.isLanguageEmpty());

    Trimmer.trimAutomaton(fa);
    assertTrue(fa.toDkBricsAutomaton().isEmpty());
    assertTrue(fa.isLanguageEmpty());
  }

  @Test
  void singleStateAcceptingIsNotEmpty() {
    FA fa = newNFA(0, 1, 0, 0); // q0 is accepting, accepts ε
    assertFalse(fa.toDkBricsAutomaton().isEmpty());
    assertFalse(fa.isLanguageEmpty());

    Trimmer.trimAutomaton(fa);
    assertFalse(fa.toDkBricsAutomaton().isEmpty());
    assertFalse(fa.isLanguageEmpty());
  }

  @Test
  void nfaUnreachableAcceptingIsEmpty() {
    // q0 --(no edges)--> cannot reach q1(accept)
    FA fa = newNFA(0, 2, 1, 1); // accepting state is 1, but no transitions
    // add an unrelated self-loop on accepting state so it exists but is unreachable
    addNfaEdge(fa, 1, 0, 1);
    assertTrue(fa.toDkBricsAutomaton().isEmpty());
    assertTrue(fa.isLanguageEmpty());

    Trimmer.trimAutomaton(fa);
    assertTrue(fa.toDkBricsAutomaton().isEmpty());
    assertTrue(fa.isLanguageEmpty());
  }

  @Test
  void nfaReachableAcceptingIsNotEmpty() {
    FA fa = newNFA(0, 2, 1, 1);
    addNfaEdge(fa, 0, 0, 1); // 0 --0--> 1(accept)
    assertFalse(fa.toDkBricsAutomaton().isEmpty());
    assertFalse(fa.isLanguageEmpty());

    Trimmer.trimAutomaton(fa);
    assertFalse(fa.toDkBricsAutomaton().isEmpty());
    assertFalse(fa.isLanguageEmpty());
  }

  @Test
  void nfaSparseKeyReachableAcceptingIsNotEmpty() {
    // Ensure we iterate actual keys present, not 0..alphabetSize-1
    FA fa = newNFA(0, 2, 1, 1); // alphabetSize=1 but we will use symbol=5
    addNfaEdge(fa, 0, 5, 1);    // sparse key!
    assertFalse(fa.toDkBricsAutomaton().isEmpty());
    assertFalse(fa.isLanguageEmpty());

    Trimmer.trimAutomaton(fa);
    assertFalse(fa.toDkBricsAutomaton().isEmpty());
    assertFalse(fa.isLanguageEmpty());
  }

  @Test
  void zeroAlphabetWithNonAcceptingIsEmpty() {
    // No symbols, no transitions: only ε could be accepted; q0 is non-accepting.
    FA fa = newNFA(0, 3, 0 /* alphabetSize=0 */);
    assertTrue(fa.toDkBricsAutomaton().isEmpty());
    assertTrue(fa.isLanguageEmpty());

    Trimmer.trimAutomaton(fa);
    assertTrue(fa.toDkBricsAutomaton().isEmpty());
    assertTrue(fa.isLanguageEmpty());
  }

  @Test
  void zeroAlphabetWithAcceptingInitialIsNotEmpty() {
    FA fa = newNFA(0, 2, 0, 0); // accepting initial -> accepts ε even with no alphabet
    assertFalse(fa.toDkBricsAutomaton().isEmpty());
    assertFalse(fa.isLanguageEmpty());

    Trimmer.trimAutomaton(fa);
    assertFalse(fa.toDkBricsAutomaton().isEmpty());
    assertFalse(fa.isLanguageEmpty());
  }
}

