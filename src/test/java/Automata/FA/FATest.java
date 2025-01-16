package Automata.FA;

import MRC.Model.MyNFA;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.automaton.fsa.impl.CompactNFA;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;

public class FATest {
  @Test
  void testCompactNFAConversions() {
    CompactNFA<Integer> compactNFA = MRC.TabakovVardiRandomNFA.generateNFA(
        new Random(0), 10, 1.5f, 0.5f, Alphabets.integers(0,1), new CompactNFA.Creator<>());
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
  void testMyNFAConversions() {
    CompactNFA<Integer> compactNFA = MRC.TabakovVardiRandomNFA.generateNFA(
        new Random(0), 10, 1.5f, 0.5f, Alphabets.integers(0,1), new CompactNFA.Creator<>());
    FA fa = FA.compactNFAToFA(compactNFA);
    MyNFA<Integer> myNFA = fa.FAtoMyNFA();
    Assertions.assertEquals(myNFA.size(), fa.getQ());
    Assertions.assertEquals(1, myNFA.getInitialStates().size());
    Assertions.assertEquals(myNFA.getInitialStates().iterator().next(), fa.getQ0());
    for(int i: myNFA.getStates()) {
      Assertions.assertEquals(myNFA.isAccepting(i), fa.getO().getInt(i) == 1);
    }
  }
}
