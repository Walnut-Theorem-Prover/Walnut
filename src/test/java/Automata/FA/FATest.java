package Automata.FA;

import net.automatalib.alphabet.Alphabets;
import net.automatalib.automaton.fsa.CompactNFA;
//import net.automatalib.util.automaton.random.TabakovVardiRandomAutomata;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;

public class FATest {
  /*@Test
  void testCompactNFAConversions() {
    CompactNFA<Integer> compactNFA = TabakovVardiRandomAutomata.generateNFA(
        new Random(0), 100, 1.5f, 0.5f, Alphabets.integers(0,1));
    FA fa = FA.compactNFAToFA(compactNFA);
    CompactNFA<Integer> compactNFA2 = fa.FAtoCompactNFA();
    Assertions.assertEquals(compactNFA2, compactNFA);
  }*/
}
