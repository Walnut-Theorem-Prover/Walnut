package Main;

import Automata.FA.FA;

public class EqualityUtils {
  public static boolean faEqual(FA a, FA b) {
    if (a.isTRUE_FALSE_AUTOMATON() != b.isTRUE_FALSE_AUTOMATON()) return false;
    if (a.isTRUE_FALSE_AUTOMATON() && b.isTRUE_FALSE_AUTOMATON()) {
      return a.isTRUE_AUTOMATON() == b.isTRUE_AUTOMATON();
    }
    dk.brics.automaton.Automaton Y = b.toDkBricsAutomaton();
    dk.brics.automaton.Automaton X = a.toDkBricsAutomaton();
    return X.equals(Y);
  }
}
