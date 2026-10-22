package Automata.FA;

import Main.EqualityUtils;
import Main.WalnutException;
import it.unimi.dsi.fastutil.ints.IntArrayList;
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
    Assertions.assertTrue(EqualityUtils.faEqual(fa, fa2));
  }

  @Test
  void testClonePreservesNfaStorage() {
    FA fa = new FA();
    fa.initBasicFA(new IntArrayList(new int[]{0, 1, 0}));
    fa.setQ0(1);
    fa.setAlphabetSize(2);
    fa.setCanonized(true);
    fa.getT().setNfaDTransition(0, 0, new IntArrayList(new int[]{1, 2}));
    fa.getT().setNfaDTransition(1, 1, new IntArrayList(new int[]{2}));

    FA copy = fa.clone();
    Assertions.assertFalse(copy.getT().hasDfaTransitions());
    Assertions.assertNotSame(fa.getT(), copy.getT());
    Assertions.assertEquals(fa.toString(), copy.toString());
    Assertions.assertNotSame(fa.getT().getNfaState(0), copy.getT().getNfaState(0));
    Assertions.assertNotSame(fa.getT().getNfaStateDests(0, 0), copy.getT().getNfaStateDests(0, 0));

    copy.getT().getNfaStateDests(0, 0).add(0);
    copy.getT().getNfaState(1).clear();
    copy.getO().set(0, 1);
    Assertions.assertEquals(new IntArrayList(new int[]{1, 2}), fa.getT().getNfaStateDests(0, 0));
    Assertions.assertEquals(new IntArrayList(new int[]{2}), fa.getT().getNfaStateDests(1, 1));
    Assertions.assertEquals(0, fa.getO().getInt(0));
  }

  @Test
  void testClonePreservesDfaStorage() {
    FA fa = new FA();
    fa.initBasicFA(new IntArrayList(new int[]{0, 1}));
    fa.setQ0(1);
    fa.setAlphabetSize(2);
    fa.setCanonized(true);
    fa.getT().setNfaDTransition(0, 0, new IntArrayList(new int[]{1}));
    fa.getT().setNfaDTransition(1, 1, new IntArrayList(new int[]{0}));
    fa.convertNFAtoDFA();

    FA copy = fa.clone();
    Assertions.assertTrue(copy.getT().hasDfaTransitions());
    Assertions.assertNotSame(fa.getT(), copy.getT());
    Assertions.assertEquals(fa.toString(), copy.toString());
    Assertions.assertEquals(fa.getT().getDfaStateCount(), copy.getT().getDfaStateCount());

    copy.getT().setDfaDTransition(0, 0, 0);
    copy.getT().setDfaDTransition(1, 0, 1);
    copy.getO().set(0, 1);
    Assertions.assertEquals(1, fa.getT().getDfaStateDest(0, 0));
    Assertions.assertFalse(fa.getT().hasDfaDTransition(1, 0));
    Assertions.assertEquals(0, fa.getO().getInt(0));
  }

  @Test
  void testStarAndConcatFromClonedDfa() {
    FA fa = new FA();
    fa.initBasicFA(new IntArrayList(new int[]{0, 1}));
    fa.setAlphabetSize(2);
    fa.getT().setNfaDTransition(0, 0, new IntArrayList(new int[]{1}));
    fa.getT().setNfaDTransition(1, 1, new IntArrayList(new int[]{0}));
    fa.convertNFAtoDFA();

    FA star = fa.clone();
    FA.starStates(fa, star);
    Assertions.assertFalse(star.getT().hasDfaTransitions());
    Assertions.assertEquals(3, star.getQ());

    FA concat = fa.clone();
    FA.concatStates(fa, concat, fa.getQ());
    Assertions.assertFalse(concat.getT().hasDfaTransitions());
    Assertions.assertEquals(4, concat.getQ());
    Assertions.assertTrue(fa.getT().hasDfaTransitions());
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
