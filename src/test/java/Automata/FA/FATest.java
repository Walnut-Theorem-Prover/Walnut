package Automata.FA;

import Main.WalnutException;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.automaton.fsa.impl.CompactNFA;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FATest {
  @Test
  void testCompactNFAConversions() {
    // Somewhat random NFA
    CompactNFA<Integer> compactNFA = new CompactNFA<>(Alphabets.integers(0,1));
    for(int i=0;i<5;i++) {
      compactNFA.addState(i % 2 == 0);
    }
    for(int i=0;i<5;i++) {
      for(int a=0;a<2;a++) {
        compactNFA.addTransition(i, a, (i+a) % 5);
      }
    }
    compactNFA.setInitial(0, true);

    FA fa = FA.compactNFAToFA(compactNFA);
    CompactNFA<Integer> compactNFA2 = fa.FAtoCompactNFA();
    Assertions.assertEquals(compactNFA.getInputAlphabet(), compactNFA2.getInputAlphabet());
    Assertions.assertEquals(compactNFA.getInitialStates(), compactNFA2.getInitialStates());
    for(int i : compactNFA.getStates()) {
      Assertions.assertEquals(compactNFA.isAccepting(i), compactNFA2.isAccepting(i));
      Assertions.assertEquals(compactNFA.getTransitions(i), compactNFA2.getTransitions(i));
    }
    //Assertions.assertEquals(compactNFA, compactNFA2); // equals isn't defined properly for these
    FA fa2 = FA.compactNFAToFA(compactNFA2);
    Assertions.assertTrue(fa.equals(fa2));
  }

  @Test
  void testDetermineMinOutput() {
    FA fa = new FA();
    Assertions.assertThrows(WalnutException.class, fa::determineMinOutput);

    fa.addOutput(false);
    fa.addOutput(false);
    Assertions.assertEquals(0, fa.determineMinOutput());

    fa = new FA();
    fa.addOutput(true);
    Assertions.assertEquals(1, fa.determineMinOutput());
  }
}
